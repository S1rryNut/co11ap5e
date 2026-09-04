#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1}"
CREDENTIALS_FILE="${CREDENTIALS_FILE:-/root/personal-site-admin.txt}"
COOKIE_JAR="$(mktemp)"
RESPONSE_FILE="$(mktemp)"
stage="startup"
trap 'rm -f "${COOKIE_JAR}" "${RESPONSE_FILE}"' EXIT

source "${CREDENTIALS_FILE}"

expect_status() {
  local expected="$1"
  shift
  local actual
  actual="$(curl -sS -o "${RESPONSE_FILE}" -w '%{http_code}' "$@")"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "${stage}: expected HTTP ${expected}, received ${actual}: $(cat "${RESPONSE_FILE}")" >&2
    exit 1
  fi
}

stage="login without CSRF"
expect_status 403 -H 'Content-Type: application/json' \
  --data '{"username":"admin","password":"invalid"}' \
  "${BASE_URL}/api/admin/auth/login"

refresh_csrf() {
  expect_status 200 -b "${COOKIE_JAR}" -c "${COOKIE_JAR}" "${BASE_URL}/api/public/csrf"
  csrf_token="$(sed -nE 's/.*"token":"([^"]+)".*/\1/p' "${RESPONSE_FILE}")"
  if [[ -z "${csrf_token}" ]]; then
    echo "CSRF token was not returned." >&2
    exit 1
  fi
}

refresh_csrf
csrf_token="$(awk '$6 == "XSRF-TOKEN" { value = $7 } END { print value }' "${COOKIE_JAR}")"
if [[ -z "${csrf_token}" ]]; then
  echo "XSRF-TOKEN cookie was not returned." >&2
  exit 1
fi

login_body="$(printf '{"username":"%s","password":"%s"}' "${username}" "${password}")"
stage="login"
expect_status 200 -b "${COOKIE_JAR}" -c "${COOKIE_JAR}" \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: ${csrf_token}" \
  --data "${login_body}" "${BASE_URL}/api/admin/auth/login"

stage="authenticated session"
expect_status 200 -b "${COOKIE_JAR}" "${BASE_URL}/api/admin/auth/me"

refresh_csrf
stage="admin article list"
expect_status 200 -b "${COOKIE_JAR}" "${BASE_URL}/api/admin/articles"
csrf_token="$(awk '$6 == "XSRF-TOKEN" { value = $7 } END { print value }' "${COOKIE_JAR}")"
if [[ -z "${csrf_token}" ]]; then
  echo "XSRF-TOKEN cookie was not returned." >&2
  exit 1
fi
article_slug="deployment-smoke-test-$(date +%s)"
article_body="$(printf '{\"slug\":\"%s\",\"title\":\"Deployment smoke test\",\"excerpt\":\"Temporary deployment verification article.\",\"category\":\"Test\",\"tags\":[\"smoke-test\"],\"content\":\"This temporary draft verifies the authenticated content API.\",\"published\":false,\"readingMinutes\":1}' "${article_slug}")"
stage="article creation"
expect_status 201 -b "${COOKIE_JAR}" -c "${COOKIE_JAR}" \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: ${csrf_token}" \
  --data "${article_body}" "${BASE_URL}/api/admin/articles"
article_id="$(sed -nE 's/.*"id":([0-9]+).*/\1/p' "${RESPONSE_FILE}")"
if [[ -z "${article_id}" ]]; then
  echo "Created article did not return an ID." >&2
  exit 1
fi

refresh_csrf
csrf_token="$(awk '$6 == "XSRF-TOKEN" { value = $7 } END { print value }' "${COOKIE_JAR}")"
stage="article deletion"
expect_status 204 -b "${COOKIE_JAR}" \
  -H "X-XSRF-TOKEN: ${csrf_token}" -X DELETE \
  "${BASE_URL}/api/admin/articles/${article_id}"

refresh_csrf
csrf_token="$(awk '$6 == "XSRF-TOKEN" { value = $7 } END { print value }' "${COOKIE_JAR}")"
stage="logout"
expect_status 200 -b "${COOKIE_JAR}" -c "${COOKIE_JAR}" \
  -H "X-XSRF-TOKEN: ${csrf_token}" -X POST \
  "${BASE_URL}/api/admin/auth/logout"
stage="revoked session"
expect_status 403 -b "${COOKIE_JAR}" "${BASE_URL}/api/admin/auth/me"

echo "Smoke test passed: CSRF, login, session, article create/delete, and logout."
