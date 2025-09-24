#!/usr/bin/env bash
set -euo pipefail
PROFILE=${1:-mvp}
DOMAIN=${2:-saas}
pass=0; warn=0; fail=0
ok(){ echo -e "✅ $1"; pass=$((pass+1)); }
wa(){ echo -e "⚠️  $1"; warn=$((warn+1)); }
no(){ echo -e "❌ $1"; fail=$((fail+1)); }

echo "== BuildToFlip v5 :: gates ($PROFILE | $DOMAIN) =="

# README
[ -f README.md ] && ok "readme present" || wa "readme missing"

# OpenAPI file present
if [ -f docs/API/openapi.yaml ] || [ -f docs/API/openapi.yml ]; then ok "openapi present"; else wa "openapi missing (advised)"; fi

# RFC7807 presence (heuristic)
if grep -R "ProblemDetail" -n src >/dev/null 2>&1; then ok "RFC7807 handling (ProblemDetail) detected"; else
  if [ "$PROFILE" = "mvp" ]; then wa "RFC7807 not detected (ok for MVP)"; else no "RFC7807 not detected"; fi
fi

# Coverage (if Jacoco generated report)
if [ -f target/site/jacoco/index.html ]; then ok "coverage report found"; else
  if [ "$PROFILE" = "mvp" ]; then wa "coverage report not found (ok for MVP)"; else no "coverage report not found"; fi
fi

# k6 script presence
[ -f k6/load-test.js ] && ok "k6 script present" || wa "k6 script missing (advised)"

echo ""
echo "== RESULT =="
echo "Pass: $pass  Warn: $warn  Fail: $fail"
[ $fail -gt 0 ] && exit 1 || exit 0
