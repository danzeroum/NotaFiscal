Perfeito! Aqui está o **handoff** ajustado para o **NFe Processor**.

> Salve exatamente em: `handoff-codex.md`

```markdown
# Prompt de Handoff ao Codex — NFe Processor (MVP)

Você é o **Codex**, implementador oficial do BuildToFlip v5 para o projeto **NFe Processor**.

Implemente **exatamente** conforme estes artefatos do repositório:
- `.buildtoflip/responses/01-human.json`
- `.buildtoflip/responses/01-architect.json`
- `.buildtoflip/responses/01-developer.json`
- `.buildtoflip/responses/01-auditor.json`
- `.buildtoflip/consensus/discovery-consensus.v5.json`
- `.buildtoflip/consensus/decision-tree-pro.v5.json`
- `docs/API/openapi.yaml`

---

## Perfil / Stack
- Tipo: **MVP (lite)**
- Stack: **Java 21 + Spring Boot 3 + Maven + H2**
- Estilo: **Crisp Pragmatist** (erros **RFC 7807**, injeção por construtor, testes negativos, observabilidade mínima via Actuator)

## Escopo desta rodada (MVP)
**Pipeline**: Ingestão → Extração (XML; OCR opcional) → Validação Fiscal Básica → Inconsistências → Export Excel

### Endpoints (contrato em `docs/API/openapi.yaml`)
1. `POST /batches` — upload de ZIP (XML/PDF) e criação de lote  
2. `GET /batches/{id}` — status + sumário de validações e issues  
3. `GET /batches/{id}/export.xlsx` — export Excel padrão ERP  
4. `GET /actuator/health` — healthcheck

### Feature flags (properties)
- `ocr.enabled=false` (default; use **tess4j** apenas quando ativado)
- `sefaz.stub.enabled=true` (stub de verificação de chave NFe para demo, sem integração real)

---

## Entregáveis obrigatórios
1. **Controllers REST** dos 3 endpoints principais + health (contrato OpenAPI)
2. **Serviços**:
   - `ZipIngestionService` (descompactar, validar extensões, criar lote)
   - `XmlParserService` (parse de NFe XML → `Invoice`)
   - `OcrAdapter` (habilitado por flag, mínimo viável)
   - `FiscalValidationService` (ICMS/IPI/ISS básicos + conferência de totais)
   - `AnomalyService` (heurísticas: soma itens ≠ total, CFOP básico inválido, etc.)
   - `ExcelExportService` (Apache POI; colunas chave/emitente/destinatario/qtde_itens/total/icms/ipi/iss/erros/avisos)
3. **Persistência (H2)** para `Batch`, `Invoice`, `ValidationReport`, `Issue`
4. **GlobalExceptionHandler** com **ProblemDetail** (RFC 7807) incluindo `traceId` (MDC) e `instance`
5. **OpenAPI** fiel aos endpoints
6. **Testes**:
   - Para cada *must_have*, **1 positivo e 1 negativo** (mínimo)
   - 1 teste de integração para `POST /batches` (ZIP com 2 XML válidos + 1 PDF sem XML → com `ocr.enabled=false` deve falhar com 422)
7. **README** com setup < 5min e exemplos de uso (curl)
8. **Exemplos de entrada** em `samples/`:
   - `ok-two-xml.zip`
   - `bad-totals.zip` (soma divergente)
   - `pdf-no-xml.zip` (para negativo com OCR desligado)

---

## Estrutura (sugestão, Clean/Hexagonal)
```

nfe-processor/
├─ src/main/java/br/com/nfe/processor/
│  ├─ core/domain/{model,repository,service}
│  ├─ adapter/in/web/       # Controllers
│  ├─ adapter/out/{persistence,ocr,excel,sefaz}
│  ├─ config/
│  └─ exception/GlobalExceptionHandler.java
├─ src/test/java/br/com/nfe/processor/{unit,integration}
├─ docs/API/openapi.yaml
├─ .buildtoflip/{responses,consensus}
└─ samples/{ok-two-xml.zip,bad-totals.zip,pdf-no-xml.zip}

````

---

## Critérios de aceitação (Gates v5 - MVP)
- ✅ **README presente** com setup em < 5min (`./mvnw spring-boot:run`)  
- ✅ **OpenAPI mínima** em `docs/API/openapi.yaml` (alinha com endpoints)  
- ✅ **RFC 7807** implementado com `traceId` + `instance`  
- ✅ **Testes**: para cada *must_have*, **1 positivo e 1 negativo**  
- ✅ **Performance**: alvo MVP `P95 < 800ms` por NFe em lote de 100 (dev)  
- ✅ **Excel** reconcilia totais e impostos (somatórios consistentes)

**NO-GO** (falha bloqueante):
- OpenAPI ausente ou divergente dos endpoints
- Ausência de RFC 7807
- Export Excel não reconcilia totais/impostos
- Falha em teste negativo crítico (ex.: soma itens ≠ total não detectada)

---

## Padrões e decisões (respeitar os artefatos)
- **Injeção via construtor** (proibido `@Autowired` em campo)
- **Logs estruturados** (incluir `traceId` via MDC)
- **Sem segredos hardcoded**  
- **Sem concatenação de SQL** (quando houver persistência manual)  
- **OCR** sob flag e marcado como experimental no README  
- **Escopo fiscal**: regras **básicas** e **explícitas** (documentadas nas ADRs)

---

## Workflow Git
- Branch: `feature/mvp-nfe`
- Abra PR para `main` com o título:  
  **`feat: MVP NFe Processor (batches + validation + export)`**
- Marque a PR com os labels: `mvp`, `api`, `crisp-pragmatist`

---

## Comandos úteis (README deve conter)
```bash
# Rodar local
./mvnw spring-boot:run

# Testes
./mvnw test

# Exemplo - criar lote (ZIP)
curl -F "file=@samples/ok-two-xml.zip" http://localhost:8080/batches

# Exemplo - status
curl http://localhost:8080/batches/{id}

# Exemplo - export
curl -L -o export.xlsx http://localhost:8080/batches/{id}/export.xlsx
````

---

## Observabilidade mínima

* **Actuator**: `GET /actuator/health`
* **@Timed** opcional em `POST /batches` (recomendado)
* Sanitização de logs (sem PII sensível da NFe)

---

## Entrega final esperada nesta rodada

* Código compilando e rodando
* Testes passando (incluindo negativos exigidos)
* OpenAPI atualizada
* README atualizado (com exemplos)
* ZIPs de amostra em `samples/`
* Planilha gerada validando somatórios

> Ao concluir, responda na PR com um **resumo objetivo**:
>
> * O que foi implementado
> * Como rodar em 1 bloco de comandos
> * Evidências dos critérios (prints/trechos de logs/saída de testes)

—
**BuildToFlip v5 — Crisp Pragmatist.**
Disciplina mínima, valor máximo.

```
```
