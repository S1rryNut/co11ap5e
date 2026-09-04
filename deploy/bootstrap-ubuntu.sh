#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script as root." >&2
  exit 1
fi

if [[ -z "${SITE_HOST:-}" ]]; then
  read -r -p "Public domain or server IP: " SITE_HOST
fi
if [[ -z "${SITE_ADMIN_USERNAME:-}" ]]; then
  read -r -p "Administrator username [admin]: " SITE_ADMIN_USERNAME
fi
SITE_ADMIN_USERNAME="${SITE_ADMIN_USERNAME:-admin}"
if [[ -z "${SITE_ADMIN_PASSWORD:-}" ]]; then
  read -r -s -p "Administrator password (minimum 14 characters): " SITE_ADMIN_PASSWORD
  echo
fi
if [[ ${#SITE_ADMIN_PASSWORD} -lt 14 ]]; then
  echo "Administrator password is too short." >&2
  exit 1
fi

SITE_DB_PASSWORD="$(openssl rand -hex 24)"

# Recover cleanly if an earlier run stopped after creating a NodeSource file
# under a restrictive caller umask.
if [[ -f /etc/apt/keyrings/nodesource.gpg ]]; then
  chmod 0644 /etc/apt/keyrings/nodesource.gpg
fi
if [[ -f /etc/apt/sources.list.d/nodesource.list ]]; then
  chmod 0644 /etc/apt/sources.list.d/nodesource.list
fi

apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y nginx postgresql postgresql-contrib openjdk-21-jre-headless curl ca-certificates gnupg rsync ufw fail2ban openssl

install -d -m 0755 /etc/apt/keyrings
curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor --yes -o /etc/apt/keyrings/nodesource.gpg
echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_22.x nodistro main" > /etc/apt/sources.list.d/nodesource.list
chmod 0644 /etc/apt/keyrings/nodesource.gpg /etc/apt/sources.list.d/nodesource.list
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs

if ! id personalsite >/dev/null 2>&1; then
  useradd --system --home /opt/personal-site --shell /usr/sbin/nologin personalsite
fi
install -d -o personalsite -g personalsite /opt/personal-site/backend /opt/personal-site/frontend /opt/personal-site/data /opt/personal-site/uploads
install -d -m 0750 -o root -g personalsite /etc/personal-site

runuser -u postgres -- psql --set=site_db_password="${SITE_DB_PASSWORD}" <<'SQL'
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'personal_site') THEN
    CREATE ROLE personal_site LOGIN;
  END IF;
END $$;
ALTER ROLE personal_site PASSWORD :'site_db_password';
SELECT 'CREATE DATABASE personal_site OWNER personal_site'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'personal_site')\gexec
SQL

runuser -u postgres -- psql -c "ALTER SYSTEM SET max_connections = '30';"
runuser -u postgres -- psql -c "ALTER SYSTEM SET shared_buffers = '128MB';"
runuser -u postgres -- psql -c "ALTER SYSTEM SET work_mem = '4MB';"
systemctl restart postgresql

cat > /etc/personal-site/backend.env <<EOF
DB_URL=jdbc:postgresql://127.0.0.1:5432/personal_site
DB_USERNAME=personal_site
DB_PASSWORD=${SITE_DB_PASSWORD}
APP_ADMIN_USERNAME=${SITE_ADMIN_USERNAME}
APP_ADMIN_PASSWORD=${SITE_ADMIN_PASSWORD}
APP_COOKIE_SECURE=false
APP_CORS_ORIGIN=http://${SITE_HOST}
AI_SYNC_ENABLED=true
EOF
chmod 0640 /etc/personal-site/backend.env
chown root:personalsite /etc/personal-site/backend.env

cat > /etc/personal-site/frontend.env <<EOF
NODE_ENV=production
NITRO_HOST=127.0.0.1
NITRO_PORT=3000
NUXT_API_BASE=http://127.0.0.1:8080/api
NUXT_PUBLIC_API_BASE=/api
NUXT_SITE_URL=http://${SITE_HOST}
NODE_OPTIONS=--max-old-space-size=256
EOF
chmod 0640 /etc/personal-site/frontend.env
chown root:personalsite /etc/personal-site/frontend.env

if [[ ! -f /swapfile ]]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi
sysctl vm.swappiness=15
cat > /etc/sysctl.d/99-personal-site.conf <<'EOF'
vm.swappiness=15
EOF

ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw --force enable
systemctl enable --now nginx postgresql fail2ban

echo "Server bootstrap complete. Upload application artifacts, install service files, then start both services."
