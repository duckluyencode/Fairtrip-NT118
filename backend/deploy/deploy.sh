#!/bin/sh
# Deploys the TripFair API on the EC2 host. Runs as root through SSM Run Command:
#   sh /opt/tripfair/deploy.sh <image-tag>
# Every setting and secret comes from SSM Parameter Store under /tripfair/prod. The script
# applies migrations with the migrate image, then (re)starts the API and Caddy.
set -eu

TAG="${1:?usage: deploy.sh <image-tag>}"
# The region the host runs in, from the instance metadata service (IMDSv2).
IMDS_TOKEN=$(curl -fsS -X PUT http://169.254.169.254/latest/api/token \
  -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')
REGION=$(curl -fsS -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" \
  http://169.254.169.254/latest/meta-data/placement/region)
PREFIX=/tripfair/prod
DIR=/opt/tripfair
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
REGISTRY="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"

mkdir -p "$DIR/certs"
# CA bundle that signs the RDS server certificates, for verified TLS to the database.
curl -fsSL -o "$DIR/certs/rds-ca.pem" \
  "https://truststore.pki.rds.amazonaws.com/$REGION/$REGION-bundle.pem"

# Settings and secrets go to an env file only root can read.
umask 077
aws ssm get-parameters-by-path --region "$REGION" --path "$PREFIX" --with-decryption \
  --query 'Parameters[].[Name,Value]' --output text |
  while IFS="$(printf '\t')" read -r name value; do
    printf '%s=%s\n' "${name##*/}" "$value"
  done >"$DIR/app.env.new"
mv "$DIR/app.env.new" "$DIR/app.env"
SITE=$(sed -n 's/^SITE_ADDRESS=//p' "$DIR/app.env")

aws ecr get-login-password --region "$REGION" |
  docker login --username AWS --password-stdin "$REGISTRY"
docker pull "$REGISTRY/tripfair-migrate:$TAG"
docker pull "$REGISTRY/tripfair-api:$TAG"
docker network inspect tripfair >/dev/null 2>&1 || docker network create tripfair

# Migrations first, so the API never starts on a schema older than its code.
docker run --rm --env-file "$DIR/app.env" -v "$DIR/certs:/certs:ro" \
  "$REGISTRY/tripfair-migrate:$TAG"

docker rm -f tripfair-api >/dev/null 2>&1 || true
docker run -d --name tripfair-api --network tripfair --restart unless-stopped \
  --env-file "$DIR/app.env" -v "$DIR/certs:/certs:ro" \
  --log-opt max-size=10m --log-opt max-file=3 \
  "$REGISTRY/tripfair-api:$TAG"

# Caddy keeps its certificates in a named volume, so restarts do not request new ones.
docker rm -f tripfair-caddy >/dev/null 2>&1 || true
docker run -d --name tripfair-caddy --network tripfair --restart unless-stopped \
  -p 80:80 -p 443:443 -e SITE_ADDRESS="$SITE" \
  -v "$DIR/Caddyfile:/etc/caddy/Caddyfile:ro" -v caddy-data:/data -v caddy-config:/config \
  --log-opt max-size=10m --log-opt max-file=3 \
  caddy:2.10-alpine

docker image prune -f >/dev/null
echo "Deployed $TAG at https://$SITE"
