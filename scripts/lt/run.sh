#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG_ROOT="${LT_LOG_DIR:-${ROOT_DIR}/.artifacts/lt}"
SCENARIO="${LT_SCENARIO:-request_upload_flow_scenario}"
API_PORT="${LT_API_PORT:-3001}"
AWS_REGION="${LT_AWS_REGION:-ap-northeast-1}"
REQUESTS_TABLE_NAME="${LT_REQUESTS_TABLE_NAME:-requests}"
UPLOAD_BUCKET_NAME="${LT_UPLOAD_BUCKET_NAME:-fileupapi-local-uploads}"
LOCALSTACK_CONTAINER="${LT_LOCALSTACK_CONTAINER:-localstack-fileup}"
LOCALSTACK_IMAGE="${LT_LOCALSTACK_IMAGE:-localstack/localstack:3.5}"
LT_DOCKER_NETWORK="${LT_DOCKER_NETWORK:-fileup-lt-net}"
FAILED_PHASE=""
SAM_PID=""
RUN_ID="$(date +%Y%m%d-%H%M%S)-${SCENARIO}"
RUN_LOG_DIR="${LOG_ROOT}/${RUN_ID}"
DETAILS_DIR="${RUN_LOG_DIR}/details"
SCENARIO_LOG="${RUN_LOG_DIR}/${SCENARIO}.log"

mkdir -p "${RUN_LOG_DIR}"

# 共通ログ出力
log_info() {
  local line="[LT] $*"
  echo "${line}"
  echo "${line}" >>"${SCENARIO_LOG}"
}

# 失敗段階の記録と詳細ログ保存
register_failure() {
  local phase="$1"
  local source_log="$2"
  mkdir -p "${DETAILS_DIR}"
  if [[ -f "${source_log}" ]]; then
    cp "${source_log}" "${DETAILS_DIR}/${phase}.log"
  fi
  echo "${phase}" >"${RUN_LOG_DIR}/failed_phase"
  echo "FAILED_PHASE=${phase}"
  if [[ -f "${DETAILS_DIR}/${phase}.log" ]]; then
    echo "[LT] tail - ${DETAILS_DIR}/${phase}.log"
    tail -n 120 "${DETAILS_DIR}/${phase}.log" || true
  fi
}

# 実行に必要なコマンドの存在確認
require_commands() {
  local required=(docker sam curl jq)
  local missing=()
  local cmd
  for cmd in "${required[@]}"; do
    if ! command -v "${cmd}" >/dev/null 2>&1; then
      # コマンドが見つからない場合はmissing配列に追加
      missing+=("${cmd}")
    fi
  done
  if [[ ${#missing[@]} -gt 0 ]]; then
    echo "Missing required command(s): ${missing[*]}" >&2
    exit 1
  fi
}

# LTは3001固定で実行するため、既存LISTENを事前に掃除する。
# sam local start-api 由来のプロセスのみ停止対象とし、判定できない場合は失敗終了する。
cleanup_existing_api_port_listener() {
  local pids=()
  local pid

  while IFS= read -r pid; do
    [[ -n "${pid}" ]] && pids+=("${pid}")
  done < <(lsof -t -nP -iTCP:"${API_PORT}" -sTCP:LISTEN 2>/dev/null | sort -u || true)

  if [[ ${#pids[@]} -eq 0 ]]; then
    return 0
  fi

  log_info "detected existing listener(s) on port ${API_PORT}: ${pids[*]}"

  for pid in "${pids[@]}"; do
    local cmdline
    cmdline="$(ps -p "${pid}" -o command= 2>/dev/null || true)"
    if [[ "${cmdline}" != *"sam local start-api"* ]]; then
      echo "Refusing to kill PID ${pid} on port ${API_PORT}: not a sam local start-api process (${cmdline})" >&2
      exit 1
    fi
    kill "${pid}" >/dev/null 2>&1 || {
      echo "Failed to stop existing sam local start-api process PID=${pid} on port ${API_PORT}" >&2
      exit 1
    }
  done

  sleep 1
  if lsof -t -nP -iTCP:"${API_PORT}" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Port ${API_PORT} is still in use after cleanup." >&2
    exit 1
  fi
  log_info "existing sam listener cleanup completed on port ${API_PORT}"
}

# 終了時に起動したSAMプロセスを停止
cleanup() {
  if [[ -n "${SAM_PID}" ]] && kill -0 "${SAM_PID}" >/dev/null 2>&1; then
    kill "${SAM_PID}" >/dev/null 2>&1 || true
    wait "${SAM_PID}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

# macOS(Docker Desktop)向けにDOCKER_HOSTを補完
ensure_docker_host_for_macos() {
  if [[ "$(uname -s)" == "Darwin" ]] && [[ -z "${DOCKER_HOST:-}" ]]; then
    # macOSの場合かつDOCKER_HOSTが未設定の場合、Docker DesktopのUNIXソケットをDOCKER_HOSTとして設定
    local mac_sock="${HOME}/.docker/run/docker.sock"
    if [[ -S "${mac_sock}" ]]; then
      export DOCKER_HOST="unix://${mac_sock}"
      log_info "DOCKER_HOST is set for Docker Desktop on macOS."
    fi
  fi
}

# LocalStack起動/再利用とS3・DynamoDBの初期化
run_bootstrap() {
  local logfile
  logfile="$(mktemp)"

  {
    set -euo pipefail

    # LT専用Dockerネットワークを作成/再利用する
    if ! docker network inspect "${LT_DOCKER_NETWORK}" >/dev/null 2>&1; then
      docker network create "${LT_DOCKER_NETWORK}"
    fi

    if docker ps --format '{{.Names}}' | grep -Fxq "${LOCALSTACK_CONTAINER}"; then
      echo "LocalStack container is already running: ${LOCALSTACK_CONTAINER}"
      if ! docker inspect \
        --format '{{json .NetworkSettings.Networks}}' \
        "${LOCALSTACK_CONTAINER}" | grep -q "\"${LT_DOCKER_NETWORK}\":"; then
        docker network connect "${LT_DOCKER_NETWORK}" "${LOCALSTACK_CONTAINER}"
      fi
      # 既存起動中でもaliasを必ず再付与して、DynamoDB向け接続名を固定する
      docker network disconnect "${LT_DOCKER_NETWORK}" "${LOCALSTACK_CONTAINER}" >/dev/null 2>&1 || true
      docker network connect --alias localstack "${LT_DOCKER_NETWORK}" "${LOCALSTACK_CONTAINER}"
    elif docker ps -a --format '{{.Names}}' | grep -Fxq "${LOCALSTACK_CONTAINER}"; then
      echo "Starting existing LocalStack container: ${LOCALSTACK_CONTAINER}"
      docker start "${LOCALSTACK_CONTAINER}"
      if ! docker inspect \
        --format '{{json .NetworkSettings.Networks}}' \
        "${LOCALSTACK_CONTAINER}" | grep -q "\"${LT_DOCKER_NETWORK}\":"; then
        docker network connect "${LT_DOCKER_NETWORK}" "${LOCALSTACK_CONTAINER}"
      fi
      # 既存コンテナ再利用時もaliasを必ず再付与して、DynamoDB向け接続名を固定する
      docker network disconnect "${LT_DOCKER_NETWORK}" "${LOCALSTACK_CONTAINER}" >/dev/null 2>&1 || true
      docker network connect --alias localstack "${LT_DOCKER_NETWORK}" "${LOCALSTACK_CONTAINER}"
    else
      echo "Creating LocalStack container: ${LOCALSTACK_CONTAINER}"
      docker run --rm -d \
        --name "${LOCALSTACK_CONTAINER}" \
        --network "${LT_DOCKER_NETWORK}" \
        --network-alias localstack \
        -p 4566:4566 \
        -e SERVICES=s3,dynamodb,events \
        "${LOCALSTACK_IMAGE}"
    fi

    local retries=0
    until docker exec "${LOCALSTACK_CONTAINER}" awslocal dynamodb list-tables --region "${AWS_REGION}" >/dev/null 2>&1; do
      retries=$((retries + 1))
      if [[ ${retries} -gt 30 ]]; then
        echo "LocalStack startup timeout." >&2
        exit 1
      fi
      sleep 1
    done

    if ! docker exec "${LOCALSTACK_CONTAINER}" awslocal s3 ls "s3://${UPLOAD_BUCKET_NAME}" >/dev/null 2>&1; then
      docker exec "${LOCALSTACK_CONTAINER}" awslocal s3 mb "s3://${UPLOAD_BUCKET_NAME}"
    fi

    if ! docker exec "${LOCALSTACK_CONTAINER}" awslocal dynamodb describe-table \
      --table-name "${REQUESTS_TABLE_NAME}" \
      --region "${AWS_REGION}" >/dev/null 2>&1; then
      docker exec "${LOCALSTACK_CONTAINER}" awslocal dynamodb create-table \
        --table-name "${REQUESTS_TABLE_NAME}" \
        --attribute-definitions AttributeName=requestId,AttributeType=S \
        --key-schema AttributeName=requestId,KeyType=HASH \
        --billing-mode PAY_PER_REQUEST \
        --region "${AWS_REGION}"
    fi

    docker exec "${LOCALSTACK_CONTAINER}" awslocal s3 ls "s3://${UPLOAD_BUCKET_NAME}"
    docker exec "${LOCALSTACK_CONTAINER}" awslocal dynamodb list-tables --region "${AWS_REGION}"
  } >"${logfile}" 2>&1 || {
    FAILED_PHASE="bootstrap"
    register_failure "${FAILED_PHASE}" "${logfile}"
    exit 1
  }
  rm -f "${logfile}"
  log_info "bootstrap phase completed"
  log_info "docker network=${LT_DOCKER_NETWORK}"
  log_info "s3 host contract=*.localstack (manual verified)"
}

# SAMビルド
run_build() {
  local logfile
  logfile="$(mktemp)"
  if ! (cd "${ROOT_DIR}" && sam build >"${logfile}" 2>&1); then
    FAILED_PHASE="build"
    register_failure "${FAILED_PHASE}" "${logfile}"
    exit 1
  fi
  rm -f "${logfile}"
  log_info "build phase completed"
}

# LT専用の環境変数ファイルを都度生成
# 障害発生や開発の差分比較用
create_env_file() {
  local env_file="${RUN_LOG_DIR}/env.lt.json"
  cat >"${env_file}" <<EOF
{
  "RequestApiFunction": {
    "REQUESTS_TABLE_NAME": "${REQUESTS_TABLE_NAME}",
    "UPLOAD_BUCKET_NAME": "${UPLOAD_BUCKET_NAME}",
    "DDB_ENDPOINT": "http://localstack:4566",
    "S3_ENDPOINT": "http://s3.localhost.localstack.cloud:4566",
    "AWS_REGION": "${AWS_REGION}",
    "AWS_DEFAULT_REGION": "${AWS_REGION}",
    "AWS_ACCESS_KEY_ID": "test",
    "AWS_SECRET_ACCESS_KEY": "test"
  },
  "RequestStatusUpdateFunction": {
    "REQUESTS_TABLE_NAME": "${REQUESTS_TABLE_NAME}",
    "DDB_ENDPOINT": "http://localstack:4566",
    "AWS_REGION": "${AWS_REGION}",
    "AWS_DEFAULT_REGION": "${AWS_REGION}",
    "AWS_ACCESS_KEY_ID": "test",
    "AWS_SECRET_ACCESS_KEY": "test"
  }
}
EOF
  echo "${env_file}"
}

# sam local start-api をバックグラウンド起動し、疎通可能になるまで待機
run_start_api() {
  local env_file="$1"
  local logfile
  logfile="$(mktemp)"

  local sam_args=(
    local
    start-api
    --port "${API_PORT}"
    --env-vars "${env_file}"
    --docker-network "${LT_DOCKER_NETWORK}"
  )

  (
    cd "${ROOT_DIR}"
    sam "${sam_args[@]}"
  ) >"${logfile}" 2>&1 &
  SAM_PID=$!

  local retries=0
  until curl -s -o /dev/null "http://127.0.0.1:${API_PORT}/requests"; do
    retries=$((retries + 1))
    if [[ ${retries} -gt 30 ]]; then
      FAILED_PHASE="start-api"
      register_failure "${FAILED_PHASE}" "${logfile}"
      exit 1
    fi
    sleep 1
  done
  rm -f "${logfile}"
  log_info "start-api phase completed"
}

# 指定シナリオを呼び出し、失敗時はFAILED_PHASEを引き継ぐ
run_scenario() {
  local scenario_script="${ROOT_DIR}/scripts/lt/scenarios/${SCENARIO}.sh"
  local failure_file="${RUN_LOG_DIR}/failed_phase"
  rm -f "${failure_file}"

  if [[ ! -f "${scenario_script}" ]]; then
    FAILED_PHASE="scenario"
    echo "Scenario script not found: ${scenario_script}" >&2
    echo "FAILED_PHASE=${FAILED_PHASE}"
    exit 1
  fi

  export LT_API_BASE_URL="http://127.0.0.1:${API_PORT}"
  export LT_LOG_DIR="${RUN_LOG_DIR}"
  export LT_SCENARIO_LOG="${SCENARIO_LOG}"
  export LT_DETAILS_DIR="${DETAILS_DIR}"
  export LT_SCENARIO_FAILURE_FILE="${failure_file}"

  if ! bash "${scenario_script}"; then
    if [[ -f "${failure_file}" ]]; then
      FAILED_PHASE="$(cat "${failure_file}")"
    else
      FAILED_PHASE="scenario"
    fi
    echo "FAILED_PHASE=${FAILED_PHASE}"
    if [[ -f "${DETAILS_DIR}/${FAILED_PHASE}.log" ]]; then
      tail -n 120 "${DETAILS_DIR}/${FAILED_PHASE}.log" || true
    fi
    exit 1
  fi
  log_info "scenario phase completed"
}

# LTの実行本体
main() {
  log_info "Starting LT automation with scenario=${SCENARIO}"
  log_info "RUN_ID=${RUN_ID}"
  log_info "LOG_DIR=${RUN_LOG_DIR}"
  log_info "API_PORT is fixed at ${API_PORT} for LT runs"
  require_commands
  ensure_docker_host_for_macos
  cleanup_existing_api_port_listener
  run_bootstrap
  run_build
  local env_file
  env_file="$(create_env_file)"
  run_start_api "${env_file}"
  run_scenario
  log_info "LT automation completed successfully."
  echo "LT_RUN_ID=${RUN_ID}"
  echo "LT_LOG_DIR=${RUN_LOG_DIR}"
}

main "$@"
