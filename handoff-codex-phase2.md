# Prompt de Handoff ao Codex — NFe Processor (Fase 2)

Você é o **Codex**, implementador oficial do BuildToFlip v5. Após o sucesso do MVP, você está recebendo o escopo para a **Fase 2** do projeto **NFe Processor**, que evoluirá a aplicação de um MVP para um produto comercialmente viável.

Sua implementação deve seguir rigorosamente as decisões documentadas no artefato de consenso:
* **Fonte da Verdade:** `.buildtoflip/consensus/phase2-implementation-consensus.v5.json`

---

## Perfil / Stack
* **Evolução de:** MVP (lite) para **Standard**
* **Stack:** Java 21 + Spring Boot 3 + Maven + H2 (mantida)
* **Estilo:** Crisp Pragmatist (mantido)

## Ordem de Implementação Prioritária
O desenvolvimento deve seguir esta sequência para maximizar o valor e mitigar riscos de forma incremental. Crie um Pull Request separado para cada funcionalidade.

1.  **Dashboard Mínimo** (`feature/phase2-dashboard`)
2.  **OCR Real** (`feature/phase2-ocr-real`)
3.  **Integração SEFAZ** (`feature/phase2-sefaz-integration`)

---

## Escopo e Entregáveis Obrigatórios

### 1. Dashboard Mínimo
* **Objetivo:** Criar uma interface web simples para upload e visualização de lotes, tornando o produto demonstrável para clientes.
* **Entregáveis:**
    * **Backend:**
        * Implementar um novo endpoint paginado `GET /batches` no `BatchController` que retorna `Page<BatchSummaryResponse>`. Utilize `Pageable` do Spring Data para paginação e ordenação.
        * Configurar limites de tamanho de arquivo no `application.yml` (ex: `spring.servlet.multipart.max-file-size=10MB`).
    * **Frontend:**
        * Criar um SPA (Single Page Application) simples (`index.html`, `app.js`, `style.css`) na pasta `src/main/resources/static`.
        * A interface deve permitir o upload de um arquivo `.zip` (reutilizando o endpoint `POST /batches`) e listar os lotes processados, exibindo seu status e um resumo dos erros.

### 2. OCR Real
* **Objetivo:** Substituir o *stub* do OCR por uma implementação funcional para extrair dados de PDFs de imagem e outros formatos de imagem, aumentando a flexibilidade do produto.
* **Entregáveis:**
    * **Backend:**
        * Implementar a lógica de extração de texto no `Tess4JOcrAdapter` usando a biblioteca `tess4j`.
        * A chamada ao OCR no `ZipIngestionService` deve ser assíncrona (`@Async`).
        * Adicionar um campo `boolean ocrProcessed` na entidade `Invoice`.
        * **Regra de Negócio Crítica:** Toda nota processada via OCR deve, obrigatoriamente, gerar um `Issue` de severidade `MEDIUM` com a mensagem "Dados extraídos via OCR, verificação manual recomendada".
    * **Infraestrutura:**
        * Garantir que os arquivos de dados de treinamento do Tesseract (`.traineddata`) sejam incluídos na imagem Docker.

### 3. Integração SEFAZ Real
* **Objetivo:** Implementar a validação real de chaves de NFe na SEFAZ para garantir segurança fiscal total.
* **Entregáveis:**
    * **Backend:**
        * Criar uma nova interface `SefazClient` e sua implementação `SefazAdapter` no pacote `adapter/out/sefaz`.
        * Implementar um `enum SefazStatus { AUTORIZADA, CANCELADA, DENEGADA, INEXISTENTE, INDISPONIVEL }` para tratar os retornos.
        * A implementação deve utilizar um cliente SOAP (ex: `spring-ws`) e gerenciar um certificado digital A1.
        * A chamada ao `SefazAdapter` deve ser resiliente, com um padrão de Circuit Breaker e timeouts curtos (3 segundos).
    * **Configuração e Segurança:**
        * O caminho e a senha do certificado digital devem ser configurados via variáveis de ambiente (`SEFAZ_CERT_PATH`, `SEFAZ_CERT_PASSWORD`), nunca hardcoded.

---

## Critérios de Aceitação e Quality Gates da Fase 2
O "Definition of Done" para a Fase 2 inclui:

* **Testes:**
    * **Dashboard:** Teste de carga com 100 usuários e teste de upload de arquivo de 10MB.
    * **OCR:** Acurácia na extração de chaves NFe superior a 70% em um conjunto de 100 documentos reais.
    * **SEFAZ:** Testes de integração simulando todos os status de retorno (incluindo timeout e falha de certificado).
* **Observabilidade:**
    * Implementar um `Phase2HealthIndicator` que verifique a disponibilidade do OCR e a validade do certificado SEFAZ.
    * Adicionar métricas customizadas com Micrometer (`@Timed`, `@Counted`) para as operações de OCR e SEFAZ.
* **Documentação:**
    * Atualizar o `README.md` e o `openapi.yaml` com as novas funcionalidades e endpoints.
    * Criar um `docs/PHASE2.md` com as decisões de implementação e planos de contingência.

---

## Entrega Final Esperada
* Código-fonte das três funcionalidades em Pull Requests separados.
* Testes automatizados cobrindo os novos cenários, com cobertura mínima de 70%.
* Documentação atualizada.
* Evidência de que as métricas de sucesso (performance, acurácia, etc.) foram atingidas.