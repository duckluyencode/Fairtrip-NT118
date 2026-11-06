#!/bin/sh
# Builds the API and migration images of the current commit and pushes them to ECR, tagged
# with the short commit hash. Run in backend/ from Git Bash (or any POSIX shell):
#   sh deploy/push-images.sh
# Set AWS to the CLI path if `aws` is not on PATH, e.g.
#   AWS="/c/Program Files/Amazon/AWSCLIV2/aws.exe" sh deploy/push-images.sh
set -eu
AWS=${AWS:-aws}
REGION=ap-southeast-1
if [ -n "$(git status --porcelain -- .)" ]; then
  echo "backend/ has uncommitted changes: commit first so the tag matches the code" >&2
  exit 1
fi
TAG=$(git rev-parse --short HEAD)
ACCOUNT=$("$AWS" sts get-caller-identity --query Account --output text)
REG="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"

docker build --platform linux/amd64 -t "$REG/tripfair-api:$TAG" .
docker build --platform linux/amd64 --target migrate -t "$REG/tripfair-migrate:$TAG" .

# The ECR token goes into a throwaway Docker config: Docker Desktop's Windows credential
# store rejects tokens this long ("stub received bad data"), and nothing is left behind.
CFG=$(mktemp -d)
trap 'rm -rf "$CFG"' EXIT
AUTH=$(printf 'AWS:%s' "$("$AWS" ecr get-login-password --region "$REGION")" | base64 | tr -d '\n')
printf '{"auths":{"%s":{"auth":"%s"}}}' "$REG" "$AUTH" >"$CFG/config.json"
docker --config "$CFG" push "$REG/tripfair-api:$TAG"
docker --config "$CFG" push "$REG/tripfair-migrate:$TAG"
echo "Pushed tripfair-api:$TAG and tripfair-migrate:$TAG"
