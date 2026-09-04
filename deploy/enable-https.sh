#!/usr/bin/env bash
set -euo pipefail

domain="co11ap5e.site"
www_domain="www.co11ap5e.site"
webroot="/var/www/letsencrypt"
nginx_site="/etc/nginx/sites-enabled/personal-site"

if ! getent ahostsv4 "$domain" | awk '{print $1}' | grep -qx '203.0.113.1'; then
  echo "DNS is not pointing to this server; aborting."
  exit 2
fi

mkdir -p "$webroot/.well-known/acme-challenge"
chown -R www-data:www-data "$webroot"
if ! grep -q "server_name ${domain} ${www_domain};" "$nginx_site"; then
  sed -i "s/server_name _;/server_name ${domain} ${www_domain};/" "$nginx_site"
fi
if ! grep -q "acme-challenge" "$nginx_site"; then
  sed -i "/server_name ${domain}/a\\    location ^~ /.well-known/acme-challenge/ { root ${webroot}; default_type text/plain; }" "$nginx_site"
fi
nginx -t
systemctl reload nginx

certbot --nginx -d "$domain" -d "$www_domain" --redirect \
  --non-interactive --agree-tos --register-unsafely-without-email
nginx -t
systemctl reload nginx
systemctl enable --now certbot.timer
certbot renew --dry-run
curl --fail --silent --show-error --head "https://${domain}/"
echo "HTTPS enabled for ${domain}"
