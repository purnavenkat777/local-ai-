const http = require('http');
const https = require('https');
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const config = require('./config');

async function checkLlamaHealth() {
  return new Promise((resolve) => {
    try {
      const url = new URL('/health', config.llamaServerUrl);
      const req = http.get(url, { timeout: 3500 }, (res) => {
        let rawData = '';
        res.on('data', (chunk) => { rawData += chunk; });
        res.on('end', () => {
          if (res.statusCode === 200) {
            let json = {};
            try { json = JSON.parse(rawData); } catch (_) {}
            resolve({
              ok: true,
              status: 'ready',
              runtime: 'llama-server',
              details: json
            });
          } else if (res.statusCode === 503) {
            resolve({
              ok: false,
              status: 'loading',
              runtime: 'llama-server',
              message: 'Model is currently loading in llama.cpp'
            });
          } else {
            resolve({
              ok: false,
              status: 'error',
              runtime: 'llama-server',
              message: `HTTP ${res.statusCode} from llama-server`
            });
          }
        });
      });
      req.on('error', (err) => {
        resolve({
          ok: false,
          status: 'offline',
          runtime: 'llama.cpp',
          message: `llama-server unreachable at ${config.llamaServerUrl} (${err.message})`
        });
      });
      req.on('timeout', () => {
        req.destroy();
        resolve({
          ok: false,
          status: 'offline',
          runtime: 'llama.cpp',
          message: `Health check to ${config.llamaServerUrl} timed out.`
        });
      });
    } catch (e) {
      resolve({
        ok: false,
        status: 'offline',
        runtime: 'llama.cpp',
        message: e.message
      });
    }
  });
}

function scanDiscoveredModels() {
  const models = [];
  const candidateDirs = config.modelCandidateDirs || [];

  for (const dir of candidateDirs) {
    if (fs.existsSync(dir)) {
      try {
        const files = fs.readdirSync(dir);
        for (const file of files) {
          if (file.endsWith('.gguf')) {
            const fullPath = path.join(dir, file);
            try {
              const stat = fs.statSync(fullPath);
              models.push({
                name: file,
                path: fullPath,
                sizeBytes: stat.size,
                available: true,
                format: 'GGUF'
              });
            } catch (_) {}
          }
        }
      } catch (err) {
        console.error(`Error scanning ${dir}:`, err.message);
      }
    }
  }

  // Also check if configured modelPath exists specifically
  if (config.modelPath && fs.existsSync(config.modelPath)) {
    const base = path.basename(config.modelPath);
    if (!models.some(m => m.path === config.modelPath || m.name === base)) {
      try {
        const stat = fs.statSync(config.modelPath);
        models.unshift({
          name: base,
          path: config.modelPath,
          sizeBytes: stat.size,
          available: true,
          format: 'GGUF'
        });
      } catch (_) {}
    }
  }

  return models;
}

module.exports = {
  checkLlamaHealth,
  scanDiscoveredModels
};
