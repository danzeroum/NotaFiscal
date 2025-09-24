#!/usr/bin/env bash
set -euo pipefail
MSG=${1:-"feat: codex delivery"}
BR=${2:-"feature/codex"}
echo "== BuildToFlip v5 :: codex commit/pr =="
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo "❌ não é um repo git"; exit 1; }
git checkout -b "$BR" || git checkout "$BR"
git add -A
git commit -m "$MSG" || echo "ℹ️ nada para commitar"
git push -u origin "$BR" || echo "⚠️ push falhou (verifique remote/permissões)"
if command -v gh >/dev/null 2>&1; then
  gh pr create --fill --label codex || echo "⚠️ gh pr create falhou (auth/permissões?)"
  echo "✅ PR solicitado (gh)."
else
  echo "⚠️ gh CLI não encontrado — abra o PR manualmente no GitHub."
fi
