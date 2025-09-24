#!/usr/bin/env bash
set -euo pipefail
mkdir -p .buildtoflip/evidence
md=".buildtoflip/evidence/report.md"
echo "# BuildToFlip v5 — Evidence Report" > "$md"
echo "- Generated: $(date -Iseconds)" >> "$md"
echo "- Consensus: .buildtoflip/consensus/discovery-consensus.v5.json" >> "$md"
echo "- Decision : .buildtoflip/consensus/decision-tree-pro.v5.json" >> "$md"
echo "" >> "$md"
if [ -f .buildtoflip/evidence/ledger.jsonl ]; then
  echo "## Ledger" >> "$md"
  cat .buildtoflip/evidence/ledger.jsonl >> "$md"
fi
if command -v pandoc >/dev/null 2>&1; then
  pandoc "$md" -o ".buildtoflip/evidence/report.pdf" && echo "✅ PDF gerado (pandoc)"
else
  echo "⚠️ pandoc não encontrado — gerado apenas report.md"
fi
echo "✅ Evidence report gerado em $md"
