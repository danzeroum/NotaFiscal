# Review Brief — NFe Processor (MVP)

## Escopo do MVP
Ingestão ZIP (XML/PDF) → Extração XML (OCR opcional por flag) → Validação fiscal básica (ICMS/IPI/ISS) → Detecção de inconsistências simples → Export Excel.

## Fora do escopo (nesta rodada)
SEFAZ real (apenas stub), RAG de legislação, ML de categorização, multi-tenant, OpenTelemetry completo.

## Feature Flags
- `ocr.enabled=false` (default) — usar tess4j apenas quando ligado
- `sefaz.stub.enabled=true` — verificação simulada

## Quality Gates (aceitação)
- OpenAPI em `docs/API/openapi.yaml` (em linha com endpoints)
- RFC 7807 (ProblemDetail com `traceId` + `instance`)
- Testes: para cada must_have, **1 positivo + 1 negativo**
- `/actuator/health` operacional
- Excel reconcilia somatórios (itens, impostos, total)

## Branch alvo
`feature/mvp-nfe`

## Objetivo da revisão externa
Apontar **correções cirúrgicas** (pequenos diffs) que aumentem robustez/clareza sem reescrever arquitetura. Priorizar:
1) Contratos/erros (OpenAPI + RFC7807)  
2) Regras fiscais básicas e somatórios  
3) Testes negativos ausentes  
4) Observabilidade mínima e logs sem PII  
5) Micro-performance (I/O redundante, N+1, loops custosos)

## Referências do projeto (no repo)
- `.buildtoflip/responses/*.json`
- `.buildtoflip/consensus/*.json`
- `docs/API/openapi.yaml`
