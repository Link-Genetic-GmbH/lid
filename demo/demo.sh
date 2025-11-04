#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080}
TOKEN=${TOKEN:-demo}

echo "# Health" && curl -s ${BASE_URL}/health | jq . || true
echo
echo "# Well-known" && curl -s ${BASE_URL}/.well-known/linkid-resolver | jq . || true
echo

echo "# Register" 
REG=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"targetUri":"https://example.org/resource","mediaType":"text/html","language":"en"}' \
  ${BASE_URL}/register)
echo "$REG" | jq . || echo "$REG"
LINK_ID=$(echo "$REG" | jq -r .id 2>/dev/null || echo "")

if [[ -z "$LINK_ID" || "$LINK_ID" == "null" ]]; then
  echo "Failed to obtain link ID from registration" >&2
  exit 1
fi

echo
echo "# Resolve redirect" && curl -s -I ${BASE_URL}/resolve/${LINK_ID} | sed -n '1,10p'
echo
echo "# Resolve metadata" && curl -s ${BASE_URL}/resolve/${LINK_ID}?metadata=true | jq . || true
echo
echo "# Update" && curl -s -X PUT -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
  -d '{"targetUri":"https://example.org/new"}' ${BASE_URL}/resolve/${LINK_ID} | jq . || true
echo
echo "# Withdraw" && curl -s -X DELETE -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
  -d '{"reason":"owner request"}' ${BASE_URL}/resolve/${LINK_ID} | jq . || true


