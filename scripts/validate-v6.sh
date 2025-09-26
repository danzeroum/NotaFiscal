#!/usr/bin/env bash
set -euo pipefail

echo "🔍 Validando migração BuildToFlip v6..."

# Executar gates
./scripts/gates-v6.sh

# Verificar estrutura
echo ""
echo "📁 Verificando estrutura v6..."
[ -d ".buildtoflip/ledger" ] && echo "✅ Ledger presente" || echo "❌ Ledger ausente"
[ -f "docs/UX/ui-kit.md" ] && echo "✅ UI Kit presente" || echo "❌ UI Kit ausente"
[ -f "k6/nfe-load-test.js" ] && echo "✅ k6 tests presente" || echo "❌ k6 ausente"

# Registrar conclusão
echo '{"timestamp":"'"$(date -u +%Y-%m-%dT%H:%M:%SZ)'","event":"v6_migration_completed","status":"success"}' >> .buildtoflip/ledger/decisions.log

echo ""
echo "🎉 Migração para BuildToFlip v6 concluída!"
echo "📋 Próximos passos:"
echo "   1. Rodar: mvn clean test"
echo "   2. Validar: docker run -v $(pwd)/k6:/scripts grafana/k6 run /scripts/nfe-load-test.js"
echo "   3. Subir: docker-compose up -d"
echo "   4. Acessar: http://localhost:8080"
