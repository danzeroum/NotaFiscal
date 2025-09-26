# ADR-001: Meta de Performance para Processamento NFe

## Status
Aceita (2025-01-20)

## Contexto
O processamento de NFes em lote precisa ser eficiente para volumes de até 1000 documentos.

## Decisão
- P95 < 800ms por NFe individual
- Processamento assíncrono com @Async para lotes
- Batch de 100 NFes por thread
- Cache de validações CFOP/CSOSN

## Consequências
### Positivas
- Resposta rápida para o usuário
- Escalabilidade horizontal simples
- Uso eficiente de recursos

### Negativas  
- Complexidade no tratamento de erros assíncronos
- Necessidade de status polling

### Mitigações
- WebSocket para notificações real-time
- Retry policy com backoff exponencial

## Validação
- [x] Arquiteto aprovou
- [x] Dev team concordou
- [x] Testes k6 validaram
