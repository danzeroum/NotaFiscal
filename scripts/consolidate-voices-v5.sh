#!/usr/bin/env bash
set -euo pipefail
mkdir -p .buildtoflip/consensus .buildtoflip/evidence
need=(.buildtoflip/responses/01-human.json .buildtoflip/responses/01-architect.json .buildtoflip/responses/01-developer.json .buildtoflip/responses/01-auditor.json)
for f in "${need[@]}"; do
  [ -f "$f" ] || { echo "❌ faltando $f"; exit 1; }
done
levels=$(jq -r '.level' .buildtoflip/responses/01-*.json | tr -d '\r')
lite=$(echo "$levels" | grep -c '^lite$' || true)
std=$(echo "$levels" | grep -c '^standard$' || true)
ent=$(echo "$levels" | grep -c '^enterprise$' || true)
cons="lite"; 
if [ $std -ge $lite ] && [ $std -ge $ent ]; then cons="standard"; fi
if [ $ent -ge $lite ] && [ $ent -ge $std ]; then cons="enterprise"; fi
conf=$(jq -s 'map(.confidence) | add/length' .buildtoflip/responses/01-*.json 2>/dev/null || echo "0.8")

cat > .buildtoflip/consensus/discovery-consensus.v5.json <<EOF
{
  "version":"5.0",
  "timestamp":"$(date -Iseconds)",
  "vote_summary":{"lite":$lite,"standard":$std,"enterprise":$ent,"total":4},
  "consensus":{"foundation_level":"$cons","confidence_score":$conf,"decision_method":"majority_vote"}
}
EOF

echo "{"ts":"$(date -Iseconds)","type":"CONSENSUS_V5","file":".buildtoflip/consensus/discovery-consensus.v5.json"}" >> .buildtoflip/evidence/ledger.jsonl
echo "✅ Consenso: $cons (conf ~$conf)"
