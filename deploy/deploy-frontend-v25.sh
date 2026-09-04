#!/usr/bin/env bash
set -euo pipefail
release="$(date +%Y%m%d%H%M%S)"
frontend_current="/opt/personal-site/frontend"
frontend_backup="/opt/personal-site/frontend.${release}.bak"
frontend_new="/opt/personal-site/frontend-v25-${release}"

mkdir -p "$frontend_new"
tar -xzf /tmp/personal-site-frontend-v25.tar.gz -C "$frontend_new"
chown -R personalsite:personalsite "$frontend_new"

systemctl stop personal-site-web
mv "$frontend_current" "$frontend_backup"
mv "$frontend_new" "$frontend_current"
systemctl start personal-site-web

web_ready=false
for attempt in $(seq 1 30); do
  if curl --fail --silent http://127.0.0.1:3000/learn >/dev/null \
      && curl --fail --silent http://127.0.0.1:3000/learn/part-1-llm-basics >/dev/null \
      && curl --fail --silent http://127.0.0.1:3000/learn/part-3-llm-principles-deep >/dev/null; then
    web_ready=true
    break
  fi
  sleep 1
done
if [[ "$web_ready" != true ]]; then
  systemctl stop personal-site-web
  mv "$frontend_current" "/opt/personal-site/frontend.failed.${release}"
  mv "$frontend_backup" "$frontend_current"
  systemctl start personal-site-web
  echo "WEB_FAILED"
  exit 1
fi

echo "deployed=v25 release=${release}"
