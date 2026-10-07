const http = require('http');
const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const config = require('./config');
const { checkLlamaHealth, scanDiscoveredModels } = require('./llama');

const app = express();
app.use(cors({ origin: '*' }));
app.use(express.json());

// Log incoming requests
app.use((req, res, next) => {
  const ts = new Date().toISOString();
  console.log(`[${ts}] ${req.method} ${req.url}`);
  next();
});

// GET /health - Lightweight Bridge Process Health Endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'diya-bridge',
    timestamp: Date.now()
  });
});

// GET /api/status - Comprehensive System Health & Diagnostics
app.get('/api/status', async (req, res) => {
  const llamaHealth = await checkLlamaHealth();
  const models = scanDiscoveredModels();
  const selectedModel = path.basename(config.modelPath);
  const modelFileExists = fs.existsSync(config.modelPath);

  // Overall status is 'ready' ONLY when bridge is up, llama-server is healthy, and model is available
  const isReady = llamaHealth.ok;

  res.json({
    bridge: {
      status: 'ok',
      service: 'diya-bridge',
      port: config.port,
      host: config.host
    },
    llama: {
      status: llamaHealth.status,
      runtime: llamaHealth.runtime || 'llama.cpp',
      serverUrl: config.llamaServerUrl,
      reachable: llamaHealth.ok,
      error: llamaHealth.ok ? null : llamaHealth.message
    },
    status: isReady ? 'ready' : (llamaHealth.status === 'loading' ? 'loading' : 'offline'),
    runtime: llamaHealth.runtime || 'llama.cpp',
    model: selectedModel,
    modelPath: config.modelPath,
    modelExists: modelFileExists,
    modelLoaded: isReady,
    modelsDiscovered: models,
    error: isReady ? null : llamaHealth.message
  });
});

// GET /api/models - List discovered GGUF models
app.get('/api/models', (req, res) => {
  const models = scanDiscoveredModels();
  res.json({ models });
});

// POST /api/chat - SSE token stream proxy with active socket termination on abort
app.post('/api/chat', async (req, res) => {
  const { messages, temperature = 0.7, max_tokens = 1024, model = 'diya.gguf' } = req.body;

  if (!messages || !Array.isArray(messages)) {
    return res.status(400).json({ error: 'Missing or invalid messages array' });
  }

  // Health check before initiating generation
  const health = await checkLlamaHealth();
  if (!health.ok && health.status === 'offline') {
    return res.status(503).json({
      error: 'Local AI is offline.',
      reason: health.message,
      suggestion: `Ensure llama-server is started on ${config.llamaServerUrl}`
    });
  }

  // Set up SSE headers
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');

  const llamaReqBody = JSON.stringify({
    messages,
    temperature,
    max_tokens,
    stream: true,
    model
  });

  const targetUrl = new URL('/v1/chat/completions', config.llamaServerUrl);
  const options = {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream'
    }
  };

  const llamaReq = http.request(targetUrl, options, (llamaRes) => {
    // Pipe llama tokens directly to Android client
    llamaRes.on('data', (chunk) => {
      res.write(chunk);
    });

    llamaRes.on('end', () => {
      res.end();
    });

    llamaRes.on('error', (err) => {
      console.error('[Bridge] llama stream error:', err.message);
      res.write(`data: {"error": "${err.message}"}\n\n`);
      res.end();
    });
  });

  llamaReq.on('error', (err) => {
    console.error('[Bridge] Failed to connect to llama-server:', err.message);
    if (!res.headersSent) {
      res.status(502).json({ error: 'llama-server connection failure', details: err.message });
    } else {
      res.write(`data: {"error": "${err.message}"}\n\n`);
      res.end();
    }
  });

  // CRITICAL REQUIREMENT 10: When client aborts or stops generation, terminate upstream request to llama-server
  req.on('close', () => {
    if (!llamaReq.destroyed) {
      console.log('[Bridge] Client disconnected / aborted generation. Destroying upstream llama request.');
      llamaReq.destroy();
    }
  });

  llamaReq.write(llamaReqBody);
  llamaReq.end();
});

// Bind to host (configurable, default 127.0.0.1)
app.listen(config.port, config.host, () => {
  console.log(`===============================================`);
  console.log(`Diya Local Bridge running at http://${config.host}:${config.port}`);
  console.log(`Connecting to llama-server at: ${config.llamaServerUrl}`);
  console.log(`Bridge Health: http://${config.host}:${config.port}/health`);
  console.log(`Status Diagnostics: http://${config.host}:${config.port}/api/status`);
  console.log(`===============================================`);
});
