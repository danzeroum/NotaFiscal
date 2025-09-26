# UI Kit - NFe Processor v6

## 🎨 Identidade Visual
- **Fonte primária:** Inter, system-ui, sans-serif
- **Cores (WCAG AA)**:
  - Primária: #0D6EFD (Azul)
  - Sucesso: #16A34A (Verde)
  - Erro: #DC2626 (Vermelho)
  - Warning: #D97706 (Laranja)
  - Background: #FFFFFF
  - Texto: #111827

## 🧩 Componentes NFe
- **Upload Area**: Drag & drop para ZIP
- **Batch Table**: Lista com status, progresso, ações
- **Validation Cards**: ICMS, IPI, ISS com badges
- **Issue List**: Tabela de problemas com severidade
- **Export Button**: Download Excel com ícone

## 📱 Layouts
- **Dashboard**: Grid 3 colunas (desktop), stack (mobile)
- **Upload**: Centered modal com preview
- **Results**: Split view (lista + detalhe)

## ♿ Acessibilidade
- Contraste mínimo: 4.5:1
- Focus visible em todos elementos
- ARIA labels em ações
- Keyboard navigation completa

## 🔄 Fluxos
1. Upload ZIP → Processing → Results
2. Select Batch → View Issues → Export
3. Error → Show ProblemDetail → Retry
