#!/bin/sh
# Builds the API and migration images of the current commit and pushes them to ECR, tagged
# with the short commit hash. Run in backend/ from Git Bash (or any POSIX shell):
#   sh deploy/push-images.sh
# Set AWS to the CLI path if `aws` is not on PATH, e.g.
#   AWS="/c/Program Files/Amazon/AWSCLIV2/aws.exe" sh deploy/push-images.sh
set -eu
AWS=${AWS:-aws}
# The region saved with `aws configure set region …` (or AWS_REGION).
REGION=${AWS_REGION:-$("$AWS" configure get region)}
if [ -z "$REGION" ]; then
  echo "No AWS region: run aws configure set region <region> first" >&2
  exit 1
fi
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

# Large layers sometimes time out on slow links ("timeout awaiting response headers");
# each try uploads the layers that are still missing. Tags are immutable, so an image
# already in ECR (from an earlier, partly failed run) is skipped instead of pushed again.
push() {
  if "$AWS" ecr describe-images --region "$REGION" --repository-name "$1" \
    --image-ids imageTag="$TAG" >/dev/null 2>&1; then
    echo "$1:$TAG is already in ECR"
    return 0
  fi
  for attempt in 1 2 3 4 5; do
    if docker --config "$CFG" push "$REG/$1:$TAG"; then
      return 0
    fi
    echo "Push of $1 failed (try $attempt of 5), retrying" >&2
    sleep 10
  done
  return 1
}
push tripfair-api
push tripfair-migrate
echo "Pushed tripfair-api:$TAG and tripfair-migrate:$TAG"
