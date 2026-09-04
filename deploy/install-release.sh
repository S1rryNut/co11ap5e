#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script as root." >&2
  exit 1
fi
RELEASE_DIR="${RELEASE_DIR:-/tmp/personal-site-release}"
if [[ ! -f "${RELEASE_DIR}/app.jar" || ! -d "${RELEASE_DIR}/frontend" ]]; then
  echo "Release artifacts are missing from ${RELEASE_DIR}." >&2
  exit 1
fi

install -o personalsite -g personalsite -m 0644 "${RELEASE_DIR}/app.jar" /opt/personal-site/backend/app.jar
rsync -a --delete --chown=personalsite:personalsite "${RELEASE_DIR}/frontend/" /opt/personal-site/frontend/
install -o root -g root -m 0644 "${RELEASE_DIR}/personal-site-api.service" /etc/systemd/system/personal-site-api.service
install -o root -g root -m 0644 "${RELEASE_DIR}/personal-site-web.service" /etc/systemd/system/personal-site-web.service
install -o root -g root -m 0644 "${RELEASE_DIR}/nginx.conf" /etc/nginx/sites-available/personal-site
ln -sfn /etc/nginx/sites-available/personal-site /etc/nginx/sites-enabled/personal-site
if [[ -L /etc/nginx/sites-enabled/default ]]; then unlink /etc/nginx/sites-enabled/default; fi

systemctl daemon-reload
systemctl enable personal-site-api personal-site-web
nginx -t
systemctl restart personal-site-api personal-site-web nginx
