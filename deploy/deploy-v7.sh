#!/usr/bin/env bash
set -euo pipefail

release="$(date +%Y%m%d%H%M%S)"
new_frontend="/opt/personal-site/frontend-v7-${release}"

mkdir -p "$new_frontend"
tar -xzf /tmp/personal-site-frontend-v7.tar.gz -C "$new_frontend"
chown -R personalsite:personalsite "$new_frontend"
cp -a /opt/personal-site/backend/app.jar "/opt/personal-site/backend/app.jar.${release}.bak"
install -o personalsite -g personalsite -m 0644 /tmp/personal-site-backend-v7.jar /opt/personal-site/backend/app.jar

systemctl stop personal-site-web
mv /opt/personal-site/frontend "/opt/personal-site/frontend.${release}.bak"
mv "$new_frontend" /opt/personal-site/frontend
systemctl restart personal-site-api
systemctl start personal-site-web

for attempt in $(seq 1 30); do
  if curl --fail --silent http://127.0.0.1:8080/actuator/health >/dev/null \
      && curl --fail --silent http://127.0.0.1:3000/now >/dev/null; then
    systemctl is-active --quiet personal-site-api
    systemctl is-active --quiet personal-site-web
    exit 0
  fi
  sleep 1
done

exit 1
