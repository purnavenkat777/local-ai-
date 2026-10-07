#!/usr/bin/env bash

# ==============================================================================
# DIYA STARTUP SCRIPT
# Starts local llama-server and Diya Local Bridge
# ==============================================================================

set -e

echo "Starting Diya..."

# 1. Check Node.js
if command -v node >/dev/null 2>&1; then
    echo "✓ Node.js detected: $(node -v)"
else
    echo "✗ Node.js is not installed."
    echo "Please install Node.js via pkg install nodejs (Termux) or apt install nodejs."
    exit 1
fi

# 2. Configurable Paths via environment variables or Termux defaults
LLAMA_SERVER="${DIYA_LLAMA_SERVER_BIN:-$HOME/llama.cpp/build/bin/llama-server}"
LLAMA_CLI="${DIYA_LLAMA_CLI_BIN:-$HOME/llama.cpp/build/bin/llama-cli}"
HOST="${DIYA_HOST:-127.0.0.1}"
PORT="${DIYA_PORT:-8787}"
LLAMA_PORT="${DIYA_LLAMA_PORT:-8080}"
CONTEXT_SIZE="${DIYA_CONTEXT_SIZE:-4096}"
THREADS="${DIYA_THREADS:-4}"

# Check for models in priority order:
# 1. DIYA_MODEL_PATH if specified
# 2. $HOME/models/diya.gguf
# 3. $HOME/models/qwen2.5-0.5b-q4.gguf
MODEL_PATH="${DIYA_MODEL_PATH:-}"
if [ -z "$MODEL_PATH" ] || [ ! -f "$MODEL_PATH" ]; then
    if [ -f "$HOME/models/diya.gguf" ]; then
        MODEL_PATH="$HOME/models/diya.gguf"
    elif [ -f "$HOME/models/qwen2.5-0.5b-q4.gguf" ]; then
        MODEL_PATH="$HOME/models/qwen2.5-0.5b-q4.gguf"
    else
        echo "✗ No GGUF model found."
        echo "Looked for:"
        echo "  - $HOME/models/diya.gguf"
        echo "  - $HOME/models/qwen2.5-0.5b-q4.gguf"
        echo "Please place a model in ~/models/ or export DIYA_MODEL_PATH=/path/to/model.gguf"
        exit 1
    fi
fi
echo "✓ Model detected: $MODEL_PATH"

mkdir -p logs

# 3. Process Management: Check if llama-server is already running
LLAMA_ALREADY_RUNNING=0
if curl -s -f "http://$HOST:$LLAMA_PORT/health" >/dev/null 2>&1; then
    echo "✓ llama-server already running on port $LLAMA_PORT"
    LLAMA_ALREADY_RUNNING=1
fi

if [ $LLAMA_ALREADY_RUNNING -eq 0 ]; then
    if [ -f "$LLAMA_SERVER" ]; then
        echo "✓ llama.cpp detected: $LLAMA_SERVER"
        echo "Starting llama-server on http://$HOST:$LLAMA_PORT..."
        "$LLAMA_SERVER" \
            -m "$MODEL_PATH" \
            --host "$HOST" \
            --port "$LLAMA_PORT" \
            -c "$CONTEXT_SIZE" \
            -t "$THREADS" > logs/llama-server.log 2>&1 &
        LLAMA_PID=$!
        echo "$LLAMA_PID" > logs/llama-server.pid
        echo "✓ llama-server process launched (PID: $LLAMA_PID)"

        # Poll health endpoint until actually ready or timeout
        echo -n "Waiting for llama-server to become healthy..."
        ATTEMPTS=0
        MAX_ATTEMPTS=30
        READY=0
        while [ $ATTEMPTS -lt $MAX_ATTEMPTS ]; do
            if curl -s -f "http://$HOST:$LLAMA_PORT/health" >/dev/null 2>&1; then
                READY=1
                break
            fi
            echo -n "."
            sleep 1
            ATTEMPTS=$((ATTEMPTS+1))
        done
        echo ""

        if [ $READY -eq 1 ]; then
            echo "✓ llama-server is healthy."
        else
            echo "✗ llama-server failed to report healthy within 30 seconds."
            echo "Check logs/llama-server.log for details."
            exit 1
        fi
    else
        echo "✗ llama-server binary not found at $LLAMA_SERVER"
        echo "Expected: ~/llama.cpp/build/bin/llama-server"
        echo "If installed elsewhere, export DIYA_LLAMA_SERVER_BIN=/path/to/llama-server"
        exit 1
    fi
fi

# 4. Process Management: Check if Diya Bridge is already running
BRIDGE_ALREADY_RUNNING=0
if curl -s -f "http://$HOST:$PORT/health" >/dev/null 2>&1; then
    echo "✓ Diya bridge already running on port $PORT"
    BRIDGE_ALREADY_RUNNING=1
fi

if [ $BRIDGE_ALREADY_RUNNING -eq 0 ]; then
    echo "Starting Diya backend bridge..."
    cd server
    if [ ! -d "node_modules" ]; then
        echo "Installing server dependencies (npm install)..."
        npm install --production >/dev/null 2>&1 || true
    fi

    DIYA_HOST="$HOST" \
    DIYA_PORT="$PORT" \
    DIYA_LLAMA_SERVER="http://$HOST:$LLAMA_PORT" \
    DIYA_MODEL_PATH="$MODEL_PATH" \
    node index.js > ../logs/diya.log 2>&1 &
    BRIDGE_PID=$!
    echo "$BRIDGE_PID" > ../logs/diya-bridge.pid
    cd ..

    echo -n "Waiting for Diya bridge to become healthy..."
    ATTEMPTS=0
    MAX_ATTEMPTS=15
    BRIDGE_READY=0
    while [ $ATTEMPTS -lt $MAX_ATTEMPTS ]; do
        if curl -s -f "http://$HOST:$PORT/health" >/dev/null 2>&1; then
            BRIDGE_READY=1
            break
        fi
        echo -n "."
        sleep 1
        ATTEMPTS=$((ATTEMPTS+1))
    done
    echo ""

    if [ $BRIDGE_READY -eq 1 ]; then
        echo "✓ Diya bridge is healthy."
    else
        echo "✗ Diya bridge failed to become healthy. Check logs/diya.log."
        exit 1
    fi
fi

echo ""
echo "=========================================================="
echo "✓ DIYA LOCAL AI STACK IS READY"
echo "=========================================================="
echo "Bridge endpoint:       http://$HOST:$PORT"
echo "llama-server endpoint: http://$HOST:$LLAMA_PORT"
echo "Active Model:          $MODEL_PATH"
echo "Logs:                  logs/diya.log and logs/llama-server.log"
echo "To shut down:          ./stop-diya.sh"
echo "=========================================================="
