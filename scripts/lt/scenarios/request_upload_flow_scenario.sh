#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${LT_API_BASE_URL:?LT_API_BASE_URL is required}"
LOG_DIR="${LT_LOG_DIR:?LT_LOG_DIR is required}"
SCENARIO_LOG="${LT_SCENARIO_LOG:?LT_SCENARIO_LOG is required}"
DETAILS_DIR="${LT_DETAILS_DIR:?LT_DETAILS_DIR is required}"
FAILURE_FILE="${LT_SCENARIO_FAILURE_FILE:?LT_SCENARIO_FAILURE_FILE is required}"

mkdir -p "${LOG_DIR}"

log_main() {
  local line="[request_upload_flow_scenario] $*"
  echo "${line}" | tee -a "${SCENARIO_LOG}"
}

# 失敗段階をファイルに記録してrun.sh側へ伝える
fail_phase() {
  local phase="$1"
  local detail_file="$2"
  mkdir -p "${DETAILS_DIR}"
  cp "${detail_file}" "${DETAILS_DIR}/${phase}.log"
  echo "${phase}" >"${FAILURE_FILE}"
  log_main "FAILED phase=${phase} (details: ${DETAILS_DIR}/${phase}.log)"
  exit 1
}

# 毎回一意なファイル名でテストデータを作成
request_name="test-upload-$(date +%Y%m%d%H%M%S).txt"
upload_file="${LOG_DIR}/${request_name}"
post_body_file="$(mktemp)"
post_detail_file="$(mktemp)"
put_detail_file="$(mktemp)"
put_body_file="$(mktemp)"
get_detail_file="$(mktemp)"

printf "lt-core-api-upload\n" >"${upload_file}"
log_main "scenario start request_file=${request_name}"

# 1) 受付作成APIを呼び出し、レスポンスとHTTPコードを取得
post_http_code="$(curl -sS -o "${post_body_file}" -w "%{http_code}" -X POST "${API_BASE_URL}/requests" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"u001\",\"fileName\":\"${request_name}\"}" \
  2>"${post_detail_file}")" || fail_phase "post" "${post_detail_file}"

response_json="$(cat "${post_body_file}")"
log_main "POST_HTTP_CODE=${post_http_code}"

request_id="$(echo "${response_json}" | jq -r '.requestId // empty')" || fail_phase "post" "${post_detail_file}"
upload_url="$(echo "${response_json}" | jq -r '.uploadUrl // empty')" || fail_phase "post" "${post_detail_file}"
status="$(echo "${response_json}" | jq -r '.status // empty')" || fail_phase "post" "${post_detail_file}"

# POST結果の最小契約を検証
if [[ "${post_http_code}" != "201" || -z "${request_id}" || -z "${upload_url}" || "${status}" != "RECEIVED" ]]; then
  {
    echo "Invalid POST response"
    echo "${response_json}"
  } >>"${post_detail_file}"
  fail_phase "post" "${post_detail_file}"
fi
log_main "POST contract validated request_id=${request_id} status=${status}"

put_http_code="000"
# 2) 署名付きURLへPUT
put_http_code="$(curl -sS -o "${put_body_file}" -w "%{http_code}" -X PUT \
  "${upload_url}" \
  --upload-file "${upload_file}" \
  2>"${put_detail_file}")" || fail_phase "put" "${put_detail_file}"

# PUT成功(200)を検証
log_main "PUT_HTTP_CODE=${put_http_code}"
if [[ "${put_http_code}" != "200" ]]; then
  {
    echo "PUT failed"
    echo "PUT_HTTP_CODE=${put_http_code}"
    echo "upload_url=${upload_url}"
    echo "PUT_RESPONSE_BODY_BEGIN"
    cat "${put_body_file}"
    echo "PUT_RESPONSE_BODY_END"
  } >>"${put_detail_file}"
  fail_phase "put" "${put_detail_file}"
fi

# 3) 単票取得APIでrequestId一致を検証
get_one_body_file="$(mktemp)"
get_list_body_file="$(mktemp)"

get_one_http_code="$(curl -sS -o "${get_one_body_file}" -w "%{http_code}" \
  "${API_BASE_URL}/requests/${request_id}" \
  2>"${get_detail_file}")" || fail_phase "get" "${get_detail_file}"
log_main "GET_ONE_HTTP_CODE=${get_one_http_code}"
if [[ "${get_one_http_code}" != "200" ]]; then
  {
    echo "GET one failed"
    echo "GET_ONE_HTTP_CODE=${get_one_http_code}"
  } >>"${get_detail_file}"
  fail_phase "get" "${get_detail_file}"
fi

if ! jq -e --arg id "${request_id}" '.requestId == $id' "${get_one_body_file}" >/dev/null 2>&1; then
  {
    echo "GET one body validation failed"
    cat "${get_one_body_file}"
  } >>"${get_detail_file}"
  fail_phase "get" "${get_detail_file}"
fi

# 4) 一覧取得APIでitemsが返ることを検証
get_list_http_code="$(curl -sS -o "${get_list_body_file}" -w "%{http_code}" \
  "${API_BASE_URL}/requests" \
  2>>"${get_detail_file}")" || fail_phase "get" "${get_detail_file}"
log_main "GET_LIST_HTTP_CODE=${get_list_http_code}"
if [[ "${get_list_http_code}" != "200" ]]; then
  {
    echo "GET list failed"
    echo "GET_LIST_HTTP_CODE=${get_list_http_code}"
  } >>"${get_detail_file}"
  fail_phase "get" "${get_detail_file}"
fi

if ! jq -e '.items != null' "${get_list_body_file}" >/dev/null 2>&1; then
  {
    echo "GET list body validation failed"
    cat "${get_list_body_file}"
  } >>"${get_detail_file}"
  fail_phase "get" "${get_detail_file}"
fi

# シナリオ成功マーカー
rm -f "${post_body_file}" "${post_detail_file}" "${put_detail_file}" "${put_body_file}" "${get_detail_file}" "${get_one_body_file}" "${get_list_body_file}"
log_main "REQUEST_ID=${request_id}"
log_main "SCENARIO=request_upload_flow_scenario success"
