#!/usr/bin/env bash
set -euo pipefail

echo "=================="
echo "NFe Processor - BuildToFlip v6 Gates"
echo "=================="

PASS=0
FAIL=0
WARN=0

pass() { echo "✅ $1"; ((PASS++)); }
fail() { echo "❌ $1"; ((FAIL++)); }
warn() { echo "⚠️  $1"; ((WARN++)); }

# Gate 1: RFC 7807
if grep -r "ProblemDetail\|problem\+json" src/main/java >/dev/null 2>&1; then
    pass "RFC 7807 implementado"
else
    fail "RFC 7807 ausente"
fi

# Gate 2: Testes por must_have (6 must_have * 2 = 12 testes mínimos)
MUST_HAVE_COUNT=6
TEST_COUNT=$(find src/test -name "*Test.java" | wc -l)
MIN_TESTS=$((MUST_HAVE_COUNT * 2))

if [ "$TEST_COUNT" -ge "$MIN_TESTS" ]; then
    pass "Testes mínimos OK ($TEST_COUNT/$MIN_TESTS)"
else
    fail "Testes insuficientes ($TEST_COUNT/$MIN_TESTS - adicionar testes negativos)"
fi

# Gate 3: Logs estruturados
if grep -r "traceId" src/main/resources/application.yml >/dev/null 2>&1; then
    pass "Logs estruturados configurados"
else
    fail "Logs estruturados não detectados"
fi

# Gate 4: OpenAPI
if [ -f "docs/API/openapi.yaml" ]; then
    pass "OpenAPI presente"
else
    fail "OpenAPI ausente"
fi

# Gate 5: Healthcheck
if grep -r "actuator/health" src/main/resources/application.yml >/dev/null 2>&1; then
    pass "Health endpoints configurados"
else
    warn "Health endpoints não encontrados"
fi

# Gate 6: Validação NFe
if grep -r "FiscalValidationService\|validateTotals" src/main/java >/dev/null 2>&1; then
    pass "Validação fiscal implementada"
else
    fail "Validação fiscal ausente"
fi

# Gate 7: Ledger v6
if [ -f ".buildtoflip/ledger/decisions.log" ]; then
    pass "Ledger v6 presente"
else
    fail "Ledger v6 não inicializado"
fi

# Gate 8: Fast Consensus
if grep -r "fast-consensus" .buildtoflip/consensus >/dev/null 2>&1; then
    pass "Fast Consensus configurado"
else
    warn "Fast Consensus não configurado"
fi

# Gate 9: Mock Data Header
if grep -r "X-BTF-Mock" src/main/java >/dev/null 2>&1; then
    pass "Mock data marking implementado"
else
    warn "Mock data header não encontrado"
fi

# Resultado
echo ""
echo "=================="
echo "RESULTADO"
echo "=================="
echo "✅ Passed: $PASS"
echo "⚠️  Warnings: $WARN"  
echo "❌ Failed: $FAIL"

# Registrar no ledger
TIMESTAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)
echo "{\"timestamp\":\"$TIMESTAMP\",\"event\":\"gates_v6_run\",\"passed\":$PASS,\"failed\":$FAIL,\"warnings\":$WARN}" >> .buildtoflip/ledger/decisions.log

if [ $FAIL -gt 0 ]; then
    echo ""
    echo "❌ QUALITY GATES FAILED - Correções necessárias"
    exit 1
else
    echo ""
    echo "✅ QUALITY GATES PASSED - Pronto para v6!"
    exit 0
fi
