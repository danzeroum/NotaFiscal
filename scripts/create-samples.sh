#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"
SAMPLES_DIR="${ROOT_DIR}/samples"
XML_DIR="${SAMPLES_DIR}/xml"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

mkdir -p "${SAMPLES_DIR}"

function make_zip() {
  local zip_name="$1"
  shift
  local zip_path="${SAMPLES_DIR}/${zip_name}"
  rm -f "${zip_path}"
  (cd "${TMP_DIR}" && zip -q "${zip_path}" "$@")
  echo "Generated ${zip_path}"
}

# Prepare files
cp "${XML_DIR}/ok-01.xml" "${TMP_DIR}/ok-01.xml"
cp "${XML_DIR}/ok-02.xml" "${TMP_DIR}/ok-02.xml"
cp "${XML_DIR}/bad-totals.xml" "${TMP_DIR}/bad-totals.xml"

# Minimal PDF placeholder for demo purposes
cat <<'PDF' > "${TMP_DIR}/nota.pdf"
%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Count 1 /Kids [3 0 R] >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] /Contents 4 0 R >>
endobj
4 0 obj
<< /Length 44 >>
stream
BT /F1 24 Tf 10 100 Td (NF-e placeholder PDF) Tj ET
endstream
endobj
xref
0 5
0000000000 65535 f 
0000000010 00000 n 
0000000061 00000 n 
0000000114 00000 n 
0000000203 00000 n 
trailer
<< /Size 5 /Root 1 0 R >>
startxref
292
%%EOF
PDF

# Generate archives
make_zip "ok-two-xml.zip" "ok-01.xml" "ok-02.xml"
make_zip "bad-totals.zip" "bad-totals.xml"
make_zip "pdf-no-xml.zip" "nota.pdf"
