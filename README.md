# 🚀 BuildToFlip v5 — Pacote Completo

Inclui:
- Scripts de utilidade (wizard, consensus, decision, gates, redteam, override, evidence, codex commit/PR).
- Schemas JSON.
- OpenAPI de exemplo (`docs/API/openapi.yaml`).
- Projeto Java Spring Boot (`src/main/java/...`, `pom.xml` com Jacoco).
- Teste de carga com k6 (`k6/load-test.js`).

## Fluxo rápido
```bash
bash scripts/dependency-check.sh
bash scripts/wizard-4-voices-v5.sh mvp
bash scripts/consolidate-voices-v5.sh mvp
bash scripts/generate-decision-pro-v5.sh
bash scripts/codex-commit-pr.sh "feat: entrega Codex MVP" feature/mvp
bash scripts/gates-v5.sh mvp fintech
bash scripts/redteam-contextual.sh fintech "java-spring"
bash scripts/evidence-to-pdf.sh
bash scripts/progress-dashboard.sh mvp
```
