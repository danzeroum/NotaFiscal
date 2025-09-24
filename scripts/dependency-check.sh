#!/usr/bin/env bash
set -euo pipefail
echo "== BuildToFlip v5 :: dependency-check =="
REQ=(jq curl git)
OPT=(gh k6 docker yq gpg pandoc)
miss=0
for c in "${REQ[@]}"; do
  if command -v "$c" >/dev/null 2>&1; then
    echo "✅ required: $c"
  else
    echo "❌ required missing: $c"
    miss=$((miss+1))
  fi
done
for c in "${OPT[@]}"; do
  if command -v "$c" >/dev/null 2>&1; then
    echo "ℹ️ optional: $c"
  else
    echo "⚠️ optional missing: $c"
  fi
done
if [ $miss -gt 0 ]; then
  echo "== RESULT: missing $miss required dependency(ies). Please install them and re-run."
  exit 1
fi
echo "== RESULT: all required dependencies present."
