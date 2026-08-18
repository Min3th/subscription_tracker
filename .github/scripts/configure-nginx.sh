#!/usr/bin/env bash
set -Eeuo pipefail
set +x

if [[ $# -ne 3 ]]; then
  echo "Usage: configure-nginx.sh <api-domain> <letsencrypt-email-or-empty> <enable-tls:true|false>" >&2
  exit 2
fi

if [[ $EUID -ne 0 ]]; then
  echo "Nginx configuration must run as root." >&2
  exit 1
fi

readonly api_domain="$1"
readonly letsencrypt_email="$2"
readonly enable_tls="$3"
readonly nginx_config="/etc/nginx/conf.d/subtrak.conf"
readonly acme_webroot="/var/www/certbot"
readonly certificate_directory="/etc/letsencrypt/live/$api_domain"

if [[ ! "$api_domain" =~ ^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$ ]]; then
  echo "API domain is invalid." >&2
  exit 1
fi
if [[ "$enable_tls" != "true" && "$enable_tls" != "false" ]]; then
  echo "TLS flag must be true or false." >&2
  exit 1
fi
if [[ "$enable_tls" == "true" \
  && ! "$letsencrypt_email" =~ ^[^[:space:]@]+@[^[:space:]@]+\.[A-Za-z]{2,}$ ]]; then
  echo "A valid Let's Encrypt email is required when TLS is enabled." >&2
  exit 1
fi

if ! command -v nginx >/dev/null 2>&1; then
  command -v dnf >/dev/null 2>&1
  dnf install -y nginx
fi

for command_name in curl grep install nginx sed systemctl; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is unavailable: $command_name" >&2
    exit 1
  fi
done

install -d -o root -g root -m 0755 "$acme_webroot/.well-known/acme-challenge"

temporary_config="$(mktemp /tmp/subtrak-nginx.XXXXXX)"
previous_config="$(mktemp /tmp/subtrak-nginx-previous.XXXXXX)"
had_previous=false

cleanup() {
  rm -f -- "$temporary_config" "$previous_config"
}
trap cleanup EXIT

if [[ -f "$nginx_config" ]]; then
  cp -p -- "$nginx_config" "$previous_config"
  had_previous=true
fi

write_http_config() {
  cat >"$temporary_config" <<'NGINX'
server {
    listen 80;
    listen [::]:80;
    server_name __API_DOMAIN__;
    server_tokens off;

    client_max_body_size 10m;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/certbot;
        default_type text/plain;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 5s;
        proxy_read_timeout 60s;
    }
}
NGINX
  sed -i "s/__API_DOMAIN__/$api_domain/g" "$temporary_config"
}

write_tls_config() {
  cat >"$temporary_config" <<'NGINX'
server {
    listen 80;
    listen [::]:80;
    server_name __API_DOMAIN__;
    server_tokens off;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/certbot;
        default_type text/plain;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name __API_DOMAIN__;
    server_tokens off;

    ssl_certificate /etc/letsencrypt/live/__API_DOMAIN__/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/__API_DOMAIN__/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;

    add_header Strict-Transport-Security "max-age=31536000" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    client_max_body_size 10m;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Proto https;
        proxy_connect_timeout 5s;
        proxy_read_timeout 60s;
    }
}
NGINX
  sed -i "s/__API_DOMAIN__/$api_domain/g" "$temporary_config"
}

install_nginx_config() {
  install -o root -g root -m 0644 "$temporary_config" "$nginx_config"

  if ! nginx -t; then
    if [[ "$had_previous" == "true" ]]; then
      install -o root -g root -m 0644 "$previous_config" "$nginx_config"
    else
      rm -f -- "$nginx_config"
    fi
    nginx -t || true
    echo "Nginx configuration validation failed; previous configuration restored." >&2
    exit 1
  fi
}

write_http_config
install_nginx_config
systemctl enable nginx
systemctl restart nginx

# If certificate issuance or the TLS configuration fails, retain the newly
# verified HTTP proxy so DNS and ACME troubleshooting remain possible.
cp -p -- "$nginx_config" "$previous_config"
had_previous=true

if [[ "$enable_tls" == "false" ]]; then
  curl --fail --silent --show-error \
    --header "Host: $api_domain" \
    --max-time 5 \
    http://127.0.0.1/v3/api-docs \
    >/dev/null
  echo "Nginx HTTP proxy configured for $api_domain; TLS remains disabled."
  exit 0
fi

if ! command -v certbot >/dev/null 2>&1; then
  command -v dnf >/dev/null 2>&1
  dnf install -y certbot
fi

if [[ ! -r "$certificate_directory/fullchain.pem" \
  || ! -r "$certificate_directory/privkey.pem" ]]; then
  certbot certonly \
    --non-interactive \
    --agree-tos \
    --email "$letsencrypt_email" \
    --webroot \
    --webroot-path "$acme_webroot" \
    --domain "$api_domain"
fi

write_tls_config
install_nginx_config

install -d -o root -g root -m 0755 /etc/letsencrypt/renewal-hooks/deploy
cat >/etc/letsencrypt/renewal-hooks/deploy/reload-nginx <<'HOOK'
#!/usr/bin/env bash
set -Eeuo pipefail
nginx -t
systemctl reload nginx
HOOK
chmod 0755 /etc/letsencrypt/renewal-hooks/deploy/reload-nginx

if systemctl list-unit-files certbot-renew.timer --no-legend 2>/dev/null \
  | grep -Fq certbot-renew.timer; then
  systemctl enable --now certbot-renew.timer
fi

systemctl reload nginx
curl --fail --silent --show-error \
  --resolve "$api_domain:443:127.0.0.1" \
  --max-time 10 \
  "https://$api_domain/v3/api-docs" \
  >/dev/null

echo "Nginx HTTPS proxy configured for $api_domain."
