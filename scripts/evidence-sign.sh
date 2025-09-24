#!/usr/bin/env bash
set -euo pipefail
FILE=${1:-.buildtoflip/evidence/ledger.jsonl}
KEY=${2:-}
[ -f "$FILE" ] || { echo "❌ arquivo não encontrado: $FILE"; exit 1; }
if command -v gpg >/dev/null 2>&1 && [ -n "$KEY" ]; then
  gpg --yes --output "$FILE.sig" --local-user "$KEY" --detach-sign "$FILE" && echo "✅ assinado: $FILE.sig"
else
  echo "⚠️ gpg ou KEY ausente — assinatura não realizada."
fi
