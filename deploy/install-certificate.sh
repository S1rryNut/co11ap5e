#!/usr/bin/env bash
set -euo pipefail

install -d -m 0700 /etc/ssl/personal-site
install -o root -g root -m 0644 /tmp/personal-site.crt /etc/ssl/personal-site/site.crt
install -o root -g root -m 0600 /tmp/personal-site.key /etc/ssl/personal-site/site.key

site=/etc/nginx/sites-enabled/personal-site
if ! grep -q 'listen 443 ssl' "$site"; then
  cat >> "$site" <<'NGINX'

server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name co11ap5e.site www.co11ap5e.site;
    ssl_certificate /etc/ssl/personal-site/site.crt;
    ssl_certificate_key /etc/ssl/personal-site/site.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    add_header Strict-Transport-Security "max-age=31536000" always;
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
    }
}
NGINX
fi
sed -i '/server_name co11ap5e.site www.co11ap5e.site;/!b;n' "$site" || true
nginx -t
systemctl reload nginx
curl --fail --silent --show-error --head https://co11ap5e.site/
systemctl is-active --quiet nginx
echo "certificate installed"
