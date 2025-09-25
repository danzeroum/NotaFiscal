# Phase 2 – Implementação

## Integração Real com a SEFAZ
- Cliente SOAP construído com Spring Web Services.
- Certificado A1 carregado via `SEFAZ_CERT_PATH` e `SEFAZ_CERT_PASSWORD` (apenas variáveis de ambiente).
- Circuit breaker configurado via Resilience4j (`sefazClient`) com janela curta e timeouts de 3 segundos no HTTP client.
- `SefazStatus` padroniza os cenários retornados: `AUTORIZADA`, `CANCELADA`, `DENEGADA`, `INEXISTENTE`, `INDISPONIVEL`.
- Adapter real (`SefazAdapter`) ativa quando `sefaz.stub.enabled=false`. Fallback mantém disponibilidade.
- Testes de integração simulam todos os códigos de resposta, timeouts e credenciais incorretas.

## Observabilidade
- `Phase2HealthIndicator` agrega sinal de OCR (`OcrAdapter.isAvailable`) e validade do certificado SEFAZ.
- Health endpoint indica `DOWN` sempre que OCR estiver desligado/indisponível ou o certificado expirar.

## Contingência
- Circuit breaker retorna `INDISPONIVEL` para o domínio, permitindo degradar o fluxo sem interromper o processamento dos lotes.
- O stub permanece disponível via flag para ambientes sem certificado.
