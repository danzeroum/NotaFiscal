#!/usr/bin/env bash
set -euo pipefail
bar(){ cur=$1; max=$2; width=20; filled=$((cur*width/max)); printf "["; printf "█%.0s" $(seq 1 $filled); printf "░%.0s" $(seq $((filled+1)) $width); printf "] "; }
cons=0; [ -f .buildtoflip/consensus/discovery-consensus.v5.json ] && cons=1
tree=0; [ -f .buildtoflip/consensus/decision-tree-pro.v5.json ] && tree=1
evi=0; [ -f .buildtoflip/evidence/ledger.jsonl ] && evi=1
echo "BuildToFlip v5 — Dashboard"
printf "Consenso     : "; bar $cons 1; echo $((cons*100))"%"
printf "DecisionTree : "; bar $tree 1; echo $((tree*100))"%"
printf "Evidências   : "; bar $evi 1; echo $((evi*100))"%"
