#!/usr/bin/env bash
set -euo pipefail

LOG_DIR="${LT_LOG_DIR:?LT_LOG_DIR is required}"
SCENARIO_LOG="${LT_SCENARIO_LOG:?LT_SCENARIO_LOG is required}"
DETAILS_DIR="${LT_DETAILS_DIR:?LT_DETAILS_DIR is required}"
FAILURE_FILE="${LT_SCENARIO_FAILURE_FILE:?LT_SCENARIO_FAILURE_FILE is required}"
LOCALSTACK_CONTAINER="${LT_LOCALSTACK_CONTAINER:?LT_LOCALSTACK_CONTAINER is required}"
AWS_REGION="${LT_AWS_REGION:?LT_AWS_REGION is required}"
REQUESTS_TABLE_NAME="${LT_REQUESTS_TABLE_NAME:?LT_REQUESTS_TABLE_NAME is required}"
UPLOAD_BUCKET_NAME="${LT_UPLOAD_BUCKET_NAME:?LT_UPLOAD_BUCKET_NAME is required}"
ROOT_DIR="${LT_ROOT_DIR:?LT_ROOT_DIR is required}"
DOCKER_NETWORK="${LT_DOCKER_NETWORK:?LT_DOCKER_NETWORK is required}"

log_main() {
  local line="[request_status_update_simulated_event_scenario] $*"
  echo "${line}" | tee -a "${SCENARIO_LOG}"
}

fail_phase() {
  local phase="$1"
  local detail_file="$2"
  mkdir -p "${DETAILS_DIR}"
  cp "${detail_file}" "${DETAILS_DIR}/${phase}.log"
  echo "${phase}" >"${FAILURE_FILE}"
  log_main "FAILED phase=${phase} (details: ${DETAILS_DIR}/${phase}.log)"
  exit 1
}

# 1) 段1シナリオの出力コンテキストを読み込み、入力値を検証
context_file="${LOG_DIR}/request_context.json"
if [[ ! -f "${context_file}" ]]; then
  detail_file="$(mktemp)"
  echo "Missing context file: ${context_file}" >"${detail_file}"
  fail_phase "status_update_context" "${detail_file}"
fi

request_id="$(jq -r '.requestId // empty' "${context_file}")"
s3_key="$(jq -r '.s3Key // empty' "${context_file}")"
if [[ -z "${request_id}" || -z "${s3_key}" ]]; then
  detail_file="$(mktemp)"
  {
    echo "Invalid context file"
    cat "${context_file}"
  } >"${detail_file}"
  fail_phase "status_update_context" "${detail_file}"
fi

# 2) 作業用ログファイルを初期化し、シナリオ開始を記録
invoke_detail_file="$(mktemp)"
ddb_detail_file="$(mktemp)"

log_main "status_update_invoke start request_id=${request_id}"

# 3) 最小イベント入力でRequestStatusUpdateFunctionを直接起動
invoke_payload_file="${LOG_DIR}/simulated-event-request-status-update.json"
printf '%s\n' "{\"detail\": {\"bucket\": {\"name\": \"${UPLOAD_BUCKET_NAME}\"}, \"object\": {\"key\": \"${s3_key}\"}}}" >"${invoke_payload_file}"
invoke_output_file="${LOG_DIR}/simulated-event-request-status-update.out.json"

if ! (
  cd "${ROOT_DIR}" &&
  sam local invoke RequestStatusUpdateFunction \
    --event "${invoke_payload_file}" \
    --env-vars "${LOG_DIR}/env.lt.json" \
    --docker-network "${DOCKER_NETWORK}"
) >"${invoke_output_file}" 2>"${invoke_detail_file}"; then
  {
    echo "invoke failed with network/context"
    echo "LT_DOCKER_NETWORK=${DOCKER_NETWORK}"
    echo "DDB_ENDPOINT=$(jq -r '.RequestStatusUpdateFunction.DDB_ENDPOINT // \"\"' "${LOG_DIR}/env.lt.json")"
  } >>"${invoke_detail_file}"
  fail_phase "status_update_invoke" "${invoke_detail_file}"
fi

if ! grep -q "UPDATED" "${invoke_output_file}"; then
  {
    echo "Expected UPDATED from RequestStatusUpdateFunction"
    cat "${invoke_output_file}"
  } >>"${invoke_detail_file}"
  fail_phase "status_update_invoke" "${invoke_detail_file}"
fi
log_main "status_update_invoke returned UPDATED"

# 4) DynamoDBのstatusがCOMPLETEDへ遷移したことをリトライ付きで確認
completed_status=""
for _ in 1 2 3 4 5 6; do
  if docker exec "${LOCALSTACK_CONTAINER}" awslocal dynamodb get-item \
    --table-name "${REQUESTS_TABLE_NAME}" \
    --key "{\"requestId\":{\"S\":\"${request_id}\"}}" \
    --region "${AWS_REGION}" >"${ddb_detail_file}" 2>&1; then
    completed_status="$(jq -r '.Item.status.S // empty' "${ddb_detail_file}" 2>/dev/null || true)"
    if [[ "${completed_status}" == "COMPLETED" ]]; then
      break
    fi
  fi
  sleep 1
done

if [[ "${completed_status}" != "COMPLETED" ]]; then
  {
    echo "Expected status COMPLETED after status_update_invoke"
    echo "actual_status=${completed_status}"
    cat "${ddb_detail_file}"
  } >>"${ddb_detail_file}"
  fail_phase "ddb_status_check" "${ddb_detail_file}"
fi

# 5) 成功ログを出力し、一時ファイルを削除
log_main "phase2 confirmed status=COMPLETED request_id=${request_id}"
rm -f "${invoke_detail_file}" "${ddb_detail_file}"
log_main "SCENARIO=request_status_update_simulated_event_scenario success"
