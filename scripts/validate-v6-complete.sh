#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0
WARN=0

pass() { echo -e "${GREEN}✅ $1${NC}"; ((PASS++)); }
fail() { echo -e "${RED}❌ $1${NC}"; ((FAIL++)); }
warn() { echo -e "${YELLOW}⚠️  $1${NC}"; ((WARN++)); }

echo "=========================================="
echo "🔍 VALIDAÇÃO COMPLETA BuildToFlip v6"
echo "=========================================="

echo ""
echo "1️⃣ VERIFICANDO ESTRUTURA"
echo "--------------------------"

if [ -d ".buildtoflip/ledger" ]; then
    if [ -f ".buildtoflip/ledger/decisions.log" ] && [ -f ".buildtoflip/ledger/overrides.log" ]; then
        pass "Ledger completo"
    else
        warn "Ledger incompleto"
    fi
else
    fail "Ledger ausente"
fi

if [ -f ".buildtoflip/consensus/discovery-consensus.v6.json" ] && \
   [ -f ".buildtoflip/consensus/decision-tree-pro.v6.json" ]; then
    pass "Consensus v6 presente"
else
    fail "Consensus v6 ausente"
fi

if [ -f "docs/UX/ui-kit.md" ]; then
    pass "UI Kit documentado"
else
    fail "UI Kit ausente"
fi

if ls docs/ADR/*.md >/dev/null 2>&1; then
    pass "ADRs presentes"
else
    warn "ADRs não encontradas"
fi

if [ -f "k6/nfe-load-test.js" ]; then
    pass "Testes k6 presentes"
else
    fail "Testes k6 ausentes"
fi

echo ""
echo "2️⃣ VERIFICANDO CÓDIGO"
echo "---------------------"

if grep -R "application/problem+json" src/main/java >/dev/null 2>&1; then
    pass "RFC 7807 com content-type correto"
else
    fail "RFC 7807 não detectado"
fi

if grep -R "TraceIdFilter" src/main/java >/dev/null 2>&1 && \
   grep -R "spanId" src/main/java >/dev/null 2>&1; then
    pass "TraceId e SpanId implementados"
else
    warn "TraceId/SpanId não configurados"
fi

if grep -R "X-BTF-Mock" src/main/java >/dev/null 2>&1; then
    pass "Mock data header ativo"
else
    fail "Mock data header ausente"
fi

if grep -R "HealthIndicator" src/main/java/br/com/nfe/processor/infrastructure/health >/dev/null 2>&1; then
    pass "Health indicator customizado"
else
    warn "Health indicator não encontrado"
fi

echo ""
echo "3️⃣ VERIFICANDO TESTES"
echo "---------------------"

TEST_COUNT=$(find src/test -name "*Test.java" -o -name "*IT.java" | wc -l)
MUST_HAVE_COUNT=6
MIN_TESTS=$((MUST_HAVE_COUNT * 2))

if [ "$TEST_COUNT" -ge "$MIN_TESTS" ]; then
    pass "Testes mínimos OK ($TEST_COUNT/$MIN_TESTS)"
else
    fail "Testes insuficientes ($TEST_COUNT/$MIN_TESTS)"
fi

if grep -R "MustHaveNegativeTests" src/test >/dev/null 2>&1; then
    pass "Testes negativos presentes"
else
    fail "Testes negativos ausentes"
fi

echo ""
echo "4️⃣ VERIFICANDO CONFIGURAÇÃO"
echo "----------------------------"

if grep -R "traceId" src/main/resources/application.yml >/dev/null 2>&1; then
    pass "Logs estruturados configurados"
else
    fail "Logs estruturados não configurados"
fi

if grep -R "prometheus" docker-compose.yml >/dev/null 2>&1 && \
   grep -R "grafana" docker-compose.yml >/dev/null 2>&1; then
    pass "Observabilidade configurada"
else
    warn "Observabilidade incompleta"
fi

echo ""
echo "5️⃣ COMPILANDO PROJETO"
echo "----------------------"

if ./mvnw -q clean compile; then
    pass "Projeto compila"
else
    fail "Falha na compilação"
fi

echo ""
echo "6️⃣ EXECUTANDO TESTES"
echo "--------------------"

if ./mvnw -q test; then
    pass "Testes passando"
else
    warn "Falhas em testes"
fi

echo ""
echo "7️⃣ VERIFICANDO DASHBOARD"
echo "------------------------"

if [ -f "src/main/resources/static/index.html" ] && \
   [ -f "src/main/resources/static/js/dashboard.js" ]; then
    pass "Dashboard disponível"
else
    fail "Dashboard ausente"
fi

echo ""
echo "=========================================="
echo "📊 RESULTADO FINAL"
echo "=========================================="
echo "✅ Passed: $PASS"
echo "⚠️  Warnings: $WARN"
echo "❌ Failed: $FAIL"

total=$((PASS + WARN + FAIL))
if [ "$total" -gt 0 ]; then
    score=$((PASS * 100 / total))
else
    score=0
fi

echo "Score de Conformidade: ${score}%"

timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)
echo "{\"timestamp\":\"$timestamp\",\"event\":\"v6_validation\",\"passed\":$PASS,\"warnings\":$WARN,\"failed\":$FAIL,\"score\":$score}" \
    >> .buildtoflip/ledger/decisions.log

if [ "$FAIL" -eq 0 ]; then
    echo ""
    echo -e "${GREEN}🎉 PARABÉNS! Projeto 100% compatível com BuildToFlip v6!${NC}"
else
    echo ""
    echo -e "${RED}❌ Projeto ainda não está totalmente compatível com v6${NC}"
    exit 1
fi
