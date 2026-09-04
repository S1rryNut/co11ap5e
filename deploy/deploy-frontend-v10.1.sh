#!/usr/bin/env bash
set -euo pipefail

release="$(date +%Y%m%d%H%M%S)"
new_frontend="/opt/personal-site/frontend-v10.1-${release}"
old_frontend="/opt/personal-site/frontend.${release}.bak"

mkdir -p "$new_frontend"
tar -xzf /tmp/personal-site-frontend-v10.1.tar.gz -C "$new_frontend"
chown -R personalsite:personalsite "$new_frontend"

systemctl stop personal-site-web
mv /opt/personal-site/frontend "$old_frontend"
mv "$new_frontend" /opt/personal-site/frontend
systemctl start personal-site-web

for attempt in $(seq 1 30); do
  if curl --fail --silent http://127.0.0.1:3000/admin >/dev/null \
      && curl --fail --silent http://127.0.0.1:3000/games >/dev/null; then
    systemctl is-active --quiet personal-site-web
    echo "deployed=frontend-v10.1 release=${release}"
    exit 0
  fi
  sleep 1
done

systemctl stop personal-site-web
mv /opt/personal-site/frontend "/opt/personal-site/frontend.failed.${release}"
mv "$old_frontend" /opt/personal-site/frontend
systemctl start personal-site-web
exit 1
