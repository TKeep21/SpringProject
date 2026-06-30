#!/usr/bin/env bash
set -euo pipefail

AUTH_SERVICE_URL="${AUTH_SERVICE_URL:-http://localhost:8081}"
FINANCE_SERVICE_URL="${FINANCE_SERVICE_URL:-http://localhost:8082}"
REPORT_SERVICE_URL="${REPORT_SERVICE_URL:-http://localhost:8083}"

timestamp="$(date +%s)"
email="e2e-${timestamp}@example.com"
password="password123"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

request_json() {
  local method="$1"
  local url="$2"
  local body="${3:-}"
  local token="${4:-}"
  local output="$5"
  local status_file="${output}.status"

  if [[ -n "${body}" && -n "${token}" ]]; then
    curl --show-error -s -o "${output}" -w "%{http_code}" -X "${method}" "${url}" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer ${token}" \
      -d "${body}" > "${status_file}"
  elif [[ -n "${body}" ]]; then
    curl --show-error -s -o "${output}" -w "%{http_code}" -X "${method}" "${url}" \
      -H "Content-Type: application/json" \
      -d "${body}" > "${status_file}"
  elif [[ -n "${token}" ]]; then
    curl --show-error -s -o "${output}" -w "%{http_code}" -X "${method}" "${url}" \
      -H "Authorization: Bearer ${token}" > "${status_file}"
  else
    curl --show-error -s -o "${output}" -w "%{http_code}" -X "${method}" "${url}" > "${status_file}"
  fi
}

assert_status() {
  local expected="$1"
  local output="$2"
  local actual
  actual="$(cat "${output}.status")"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "Expected HTTP ${expected}, got ${actual}" >&2
    cat "${output}" >&2
    exit 1
  fi
}

json_field() {
  local file="$1"
  local path="$2"
  python3 - "$file" "$path" <<'PY'
import json
import sys

data = json.load(open(sys.argv[1]))
value = data
for part in sys.argv[2].split("."):
    if part.isdigit():
        value = value[int(part)]
    else:
        value = value[part]
print(value)
PY
}

echo "Registering ${email}"
register_body="$(printf '{"email":"%s","password":"%s","firstName":"E2E","lastName":"User"}' "${email}" "${password}")"
request_json POST "${AUTH_SERVICE_URL}/api/v1/auth/register" "${register_body}" "" "${tmp_dir}/register.json"
assert_status 201 "${tmp_dir}/register.json"
token="$(json_field "${tmp_dir}/register.json" "accessToken")"
user_id="$(json_field "${tmp_dir}/register.json" "user.id")"

echo "Logging in"
login_body="$(printf '{"email":"%s","password":"%s"}' "${email}" "${password}")"
request_json POST "${AUTH_SERVICE_URL}/api/v1/auth/login" "${login_body}" "" "${tmp_dir}/login.json"
assert_status 200 "${tmp_dir}/login.json"

echo "Checking current user"
request_json GET "${AUTH_SERVICE_URL}/api/v1/auth/me" "" "${token}" "${tmp_dir}/me.json"
assert_status 200 "${tmp_dir}/me.json"

echo "Creating group"
request_json POST "${FINANCE_SERVICE_URL}/api/v1/groups" '{"name":"E2E Group","description":"Smoke"}' "${token}" "${tmp_dir}/group.json"
assert_status 201 "${tmp_dir}/group.json"
group_id="$(json_field "${tmp_dir}/group.json" "id")"

echo "Adding current user as duplicate member should fail with conflict"
add_member_body="$(printf '{"userId":"%s","role":"MEMBER"}' "${user_id}")"
request_json POST "${FINANCE_SERVICE_URL}/api/v1/groups/${group_id}/members" "${add_member_body}" "${token}" "${tmp_dir}/add-member.json"
assert_status 409 "${tmp_dir}/add-member.json"

echo "Creating category"
request_json POST "${FINANCE_SERVICE_URL}/api/v1/categories" '{"name":"E2E Food","type":"EXPENSE"}' "${token}" "${tmp_dir}/category.json"
assert_status 201 "${tmp_dir}/category.json"
category_id="$(json_field "${tmp_dir}/category.json" "id")"

echo "Creating operation"
operation_body="$(printf '{"categoryId":"%s","type":"EXPENSE","amount":12.34,"currency":"USD","operationDate":"2026-06-28","description":"E2E lunch"}' "${category_id}")"
request_json POST "${FINANCE_SERVICE_URL}/api/v1/operations" "${operation_body}" "${token}" "${tmp_dir}/operation.json"
assert_status 201 "${tmp_dir}/operation.json"

echo "Getting operations"
request_json GET "${FINANCE_SERVICE_URL}/api/v1/operations?fromDate=2026-06-01&toDate=2026-06-30" "" "${token}" "${tmp_dir}/operations.json"
assert_status 200 "${tmp_dir}/operations.json"

echo "Getting summary report"
request_json GET "${REPORT_SERVICE_URL}/api/v1/reports/summary?from=2026-06-01&to=2026-06-30&currency=USD" "" "${token}" "${tmp_dir}/summary.json"
assert_status 200 "${tmp_dir}/summary.json"

echo "Getting by-category report"
request_json GET "${REPORT_SERVICE_URL}/api/v1/reports/by-category?from=2026-06-01&to=2026-06-30&currency=USD" "" "${token}" "${tmp_dir}/by-category.json"
assert_status 200 "${tmp_dir}/by-category.json"

echo "Getting by-member report"
request_json GET "${REPORT_SERVICE_URL}/api/v1/reports/by-member?from=2026-06-01&to=2026-06-30&groupId=${group_id}&currency=USD" "" "${token}" "${tmp_dir}/by-member.json"
assert_status 200 "${tmp_dir}/by-member.json"

echo "Exporting CSV"
curl --show-error -s -o "${tmp_dir}/report.csv" -w "%{http_code}" \
  "${REPORT_SERVICE_URL}/api/v1/reports/export?from=2026-06-01&to=2026-06-30&currency=USD&format=csv" \
  -H "Authorization: Bearer ${token}" > "${tmp_dir}/report.csv.status"
assert_status 200 "${tmp_dir}/report.csv"
grep -q "date,type,amount,currency,category,userId,groupId,description" "${tmp_dir}/report.csv"

echo "E2E smoke passed"
