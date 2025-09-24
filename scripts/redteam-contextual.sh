#!/usr/bin/env bash
set -euo pipefail
DOMAIN=${1:-saas}
STACK=${2:-java-spring}
out=".buildtoflip/evidence/redteam-$DOMAIN-$STACK.md"
mkdir -p .buildtoflip/evidence
cat > "$out" <<EOF
# Red Team (contextual)
Domínio: $DOMAIN
Stack: $STACK

Possíveis vetores de ataque:
- Auth bypass / session fixation
- Rate limiting insuficiente
- Injeção (SQL/NoSQL) em endpoints críticos
- Exposição de dados sensíveis em logs
- Falta de validação de input (OWASP)
EOF
echo "{"ts":"$(date -Iseconds)","type":"REDTEAM_CONTEXT","file":"$out"}" >> .buildtoflip/evidence/ledger.jsonl
echo "✅ Red Team contextual salvo em $out"
