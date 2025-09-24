#!/usr/bin/env bash
set -euo pipefail
con=".buildtoflip/consensus/discovery-consensus.v5.json"
[ -f "$con" ] || { echo "❌ consenso não encontrado ($con)"; exit 1; }
lvl=$(jq -r '.consensus.foundation_level' "$con")
mkdir -p .buildtoflip/consensus
if [ "$lvl" = "lite" ]; then
  must='["core","security_basic","docker","openapi","tests"]'
  skip='["oauth2","multi_tenant","kafka","k8s"]'
  release="14d"
elif [ "$lvl" = "standard" ]; then
  must='["core","security_jwt","observability_basic","docker","ci_cd","tests"]'
  skip='["multi_tenant","kafka"]'
  release="60-90d"
else
  must='["core","security_oauth2","observability_full","multi_tenant","docker","k8s"]'
  skip='["none"]'
  release="180d"
fi
cat > .buildtoflip/consensus/decision-tree-pro.v5.json <<EOF
{
  "version":"5.0-pro",
  "timestamp":"$(date -Iseconds)",
  "recommended_level":"$lvl",
  "must_have":$must,
  "can_skip":$skip,
  "first_release":"$release"
}
EOF
echo "✅ Decision Tree gerada para nível: $lvl"
