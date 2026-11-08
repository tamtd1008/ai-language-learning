#!/bin/sh
set -eu

VOICE="${PIPER_VOICE:-en_US-lessac-medium}"
DATA_DIR="/data"

if [ ! -f "${DATA_DIR}/${VOICE}.onnx" ]; then
  python -m piper.download_voices "${VOICE}" --data-dir "${DATA_DIR}"
fi

exec python -m piper.http_server \
  --host 0.0.0.0 \
  --port 5000 \
  --data-dir "${DATA_DIR}" \
  --model "${DATA_DIR}/${VOICE}.onnx"
