#!/usr/bin/env bash
set -euo pipefail
PROFILE=${1:-mvp}
mkdir -p .buildtoflip/prompts .buildtoflip/responses

echo "== BuildToFlip v5 :: wizard (profile: $PROFILE) =="
read -r -p "Nome do projeto: " PROJECT
read -r -p "Domínio (fintech/healthtech/saas/erp): " DOMAIN
read -r -p "Buyer (startup/enterprise): " BUYER
read -r -p "Timeline (ex.: 2 semanas): " TIMELINE
read -r -p "Seu nome: " HUMAN

cat > .buildtoflip/prompts/01-architect.md <<EOF
# Discovery - Arquiteto
Projeto: $PROJECT
Domínio: $DOMAIN | Buyer: $BUYER | Timeline: $TIMELINE
Responda em JSON:
{"voter":"IA-Arquiteto","level":"lite|standard|enterprise","confidence":0.0,"reason":"..."}
EOF

cat > .buildtoflip/prompts/01-developer.md <<EOF
# Discovery - Desenvolvedor
Projeto: $PROJECT
Stack sugerida: Spring Boot + Postgres + Docker
Responda em JSON:
{"voter":"IA-Dev","level":"lite|standard|enterprise","confidence":0.0,"reason":"..."}
EOF

cat > .buildtoflip/prompts/01-auditor.md <<EOF
# Discovery - Auditor
Foco: vendabilidade + riscos + compliance mínimo.
Responda em JSON:
{"voter":"IA-Auditor","level":"lite|standard|enterprise","confidence":0.0,"reason":"..."}
EOF

read -r -p "Seu voto (lite/standard/enterprise): " HLEVEL
read -r -p "Sua confiança (0.0-1.0): " HCONF
read -r -p "Razão principal: " HREASON

cat > .buildtoflip/responses/01-human.json <<EOF
{"voter":"$HUMAN","level":"$HLEVEL","confidence":$HCONF,"reason":"$HREASON","profile":"$PROFILE","timestamp":"$(date -Iseconds)"}
EOF

echo "Prompts gerados em .buildtoflip/prompts/*.md"
echo "Salve as respostas das IAs em .buildtoflip/responses/ como 01-architect.json, 01-developer.json, 01-auditor.json"
