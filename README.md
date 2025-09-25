# NFe Processor — MVP BuildToFlip v5

Processador mínimo de lotes de NFe seguindo o handoff Crisp Pragmatist.

## ✅ Requisitos
- Java 21
- Maven Wrapper (`./mvnw`)

## ⚙️ Setup rápido (< 5 min)
```bash
./mvnw clean package
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

## 🔌 Endpoints principais
| Método | Caminho | Descrição |
| ------ | ------- | --------- |
| `POST` | `/batches` | Faz upload de um ZIP com XML/PDF e inicia processamento |
| `GET` | `/batches` | Lista lotes paginados com status e alertas resumidos |
| `GET` | `/batches/{id}` | Retorna status, estatísticas e amostra de validações |
| `GET` | `/batches/{id}/export.xlsx` | Exporta planilha padrão ERP |
| `GET` | `/actuator/health` | Healthcheck Spring Boot |

### Exemplos `curl`
```bash
# Gerar amostras (caso não existam)
./scripts/create-samples.sh

# Criar lote com o ZIP gerado
curl -F "file=@samples/ok-two-xml.zip" http://localhost:8080/batches

# Consultar resumo do lote
curl http://localhost:8080/batches/{id}

# Exportar Excel
curl -L -o export.xlsx http://localhost:8080/batches/{id}/export.xlsx
```

## 🧪 Testes
```bash
./mvnw test
```

Inclui testes unitários para cada serviço obrigatório e testes de integração cobrindo o fluxo do endpoint `POST /batches` (casos positivos e negativos).

## 🗂️ Amostras
Os artefatos binários são gerados sob demanda pelo script `./scripts/create-samples.sh` e não são versionados.
Após executar o script, os seguintes pacotes ficam disponíveis em `samples/`:
- `ok-two-xml.zip` — duas NF-e válidas
- `bad-totals.zip` — divergência de totais detectada
- `pdf-no-xml.zip` — contém apenas PDF (falha quando OCR está desabilitado)

## 🔐 Feature Flags e Integrações
- `ocr.enabled=false` (padrão) — ativa integração experimental via Tess4J
- `sefaz.stub.enabled=true` — mantém o modo demo que não realiza chamadas reais à SEFAZ
- Para habilitar a consulta real configure as variáveis de ambiente:
  - `SEFAZ_CERT_PATH` — caminho absoluto do certificado digital A1 (formato `.p12`)
  - `SEFAZ_CERT_PASSWORD` — senha do certificado digital
  - `SEFAZ_ENV` — ambiente da SEFAZ (`homolog` ou `prod`)
  - Opcional: `sefaz.endpoint` caso precise apontar para uma URL customizada de webservice

A integração real utiliza cliente SOAP (Spring Web Services) com certificado A1, timeouts de 3 segundos e circuit breaker (`resilience4j`) para degradar em caso de indisponibilidade.

## 🧠 Configuração do Tesseract
- O OCR real exige os arquivos `.traineddata` disponíveis no contêiner (copiados no build).
- A aplicação valida o diretório informado em `ocr.tessdata-path` (ou na variável de ambiente `TESSDATA_PREFIX`).
- Caminhos aceitos por padrão: `/usr/share/tessdata`, `/usr/share/tesseract-ocr`, `/opt/tessdata`.
- Para apontar para um local customizado, ajuste a propriedade/variável para ficar dentro de um desses diretórios ou adicione um *bind mount* que respeite o caminho permitido.

## 🔎 Observabilidade
- Healthcheck: `GET /actuator/health` — inclui verificação do OCR e validade do certificado SEFAZ
- Todos os erros seguem RFC 7807 com `traceId` correlacionado via MDC (`X-Trace-Id`).

## 📄 Contrato
O contrato oficial está em `docs/API/openapi.yaml` e reflete os endpoints implementados.

## 📦 Export Excel
A planilha gerada contém as colunas:
`chave_acesso, emitente, destinatario, qtde_itens, total, icms, ipi, iss, erros, avisos`.

## 🖥️ Dashboard mínimo
- Interface SPA servida em `http://localhost:8080/index.html`.
- Upload de lotes via formulário (limite configurado em 10MB por arquivo).
- Lista paginada (5 itens por página) com status, métricas e resumo dos principais alertas.
- Botão "Atualizar" para sincronizar manualmente os lotes exibidos.

## 📝 Notas
- Injeção por construtor em todos os componentes Spring
- Dados persistidos em H2 em memória (`jdbc:h2:mem:nfeprocessor`)
- OCR permanece stubado até habilitação explícita da flag
