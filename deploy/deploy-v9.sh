#!/usr/bin/env bash
set -euo pipefail

release="$(date +%Y%m%d%H%M%S)"
frontend_current="/opt/personal-site/frontend"
frontend_backup="/opt/personal-site/frontend.${release}.bak"
frontend_new="/opt/personal-site/frontend-v9-${release}"
backend_current="/opt/personal-site/backend/app.jar"
backend_backup="/opt/personal-site/backend/app.jar.${release}.bak"

mkdir -p "$frontend_new"
tar -xzf /tmp/personal-site-frontend-v9.tar.gz -C "$frontend_new"
chown -R personalsite:personalsite "$frontend_new"
cp -a "$backend_current" "$backend_backup"
install -o personalsite -g personalsite -m 0644 /tmp/personal-site-backend-v9.jar "$backend_current"
systemctl restart personal-site-api

api_ready=false
for attempt in $(seq 1 30); do
  if curl --fail --silent http://127.0.0.1:8080/actuator/health >/dev/null \
      && curl --fail --silent http://127.0.0.1:8080/api/public/projects/ningbo-citizen-card-rights-confirmation >/dev/null; then
    api_ready=true
    break
  fi
  sleep 1
done

if [[ "$api_ready" != true ]]; then
  install -o personalsite -g personalsite -m 0644 "$backend_backup" "$backend_current"
  systemctl restart personal-site-api
  exit 1
fi

systemctl stop personal-site-web
mv "$frontend_current" "$frontend_backup"
mv "$frontend_new" "$frontend_current"
systemctl start personal-site-web

web_ready=false
for attempt in $(seq 1 20); do
  if curl --fail --silent http://127.0.0.1:3000/projects/ningbo-citizen-card-rights-confirmation >/dev/null; then
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
  exit 1
fi

systemctl is-active --quiet personal-site-api
systemctl is-active --quiet personal-site-web
