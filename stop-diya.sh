#!/usr/bin/env bash

# ==============================================================================
# DIYA STOP SCRIPT
# Gracefully stops Diya Bridge and llama-server started by Diya
# ==============================================================================

echo "Stopping Diya..."

# 1. Stop Diya Bridge
if [ -f logs/diya-bridge.pid ]; then
    PID=$(cat logs/diya-bridge.pid)
    if kill -0 "$PID" >/dev/null 2>&1; then
        echo "Stopping Diya bridge (PID: $PID)..."
        kill "$PID" 2>/dev/null || true
    fi
    rm -f logs/diya-bridge.pid
fi

# 2. Stop llama-server
if [ -f logs/llama-server.pid ]; then
    PID=$(cat logs/llama-server.pid)
    if kill -0 "$PID" >/dev/null 2>&1; then
        echo "Stopping llama-server (PID: $PID)..."
        kill "$PID" 2>/dev/null || true
    fi
    rm -f logs/llama-server.pid
fi

echo "✓ Diya processes stopped."
