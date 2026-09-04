#!/usr/bin/env bash
set -euo pipefail

release="$(date +%Y%m%d%H%M%S)"
cp -a /opt/personal-site/backend/app.jar "/opt/personal-site/backend/app.jar.${release}.bak"
install -o personalsite -g personalsite -m 0644 /tmp/personal-site-backend-v7.1.jar /opt/personal-site/backend/app.jar
systemctl restart personal-site-api

for attempt in $(seq 1 30); do
  if curl --fail --silent http://127.0.0.1:8080/actuator/health >/dev/null; then
    systemctl is-active --quiet personal-site-api
    exit 0
  fi
  sleep 1
done

exit 1
