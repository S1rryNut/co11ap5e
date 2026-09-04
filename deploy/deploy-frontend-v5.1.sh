#!/usr/bin/env bash
set -euo pipefail

release="$(date +%Y%m%d%H%M%S)"
new_frontend="/opt/personal-site/frontend-v5.1-${release}"

mkdir -p "$new_frontend"
tar -xzf /tmp/personal-site-frontend-v5.1.tar.gz -C "$new_frontend"
chown -R personalsite:personalsite "$new_frontend"

systemctl stop personal-site-web
mv /opt/personal-site/frontend "/opt/personal-site/frontend.${release}.bak"
mv "$new_frontend" /opt/personal-site/frontend
systemctl start personal-site-web

for attempt in $(seq 1 20); do
  if curl --fail --silent http://127.0.0.1:3000/admin >/dev/null; then
    systemctl is-active --quiet personal-site-web
    exit 0
  fi
  sleep 1
done

exit 1
