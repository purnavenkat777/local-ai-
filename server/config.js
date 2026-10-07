const path = require('path');
const os = require('os');

const homeDir = os.homedir();

module.exports = {
  port: parseInt(process.env.DIYA_PORT || '8787', 10),
  host: process.env.DIYA_HOST || '127.0.0.1',
  llamaServerUrl: process.env.DIYA_LLAMA_SERVER || 'http://127.0.0.1:8080',
  llamaCliPath: process.env.DIYA_LLAMA_CLI || path.join(homeDir, 'llama.cpp', 'build', 'bin', 'llama-cli'),
  modelPath: process.env.DIYA_MODEL_PATH || path.join(homeDir, 'models', 'diya.gguf'),
  modelCandidateDirs: [
    path.join(homeDir, 'models'),
    path.join(homeDir, 'llama.cpp', 'models')
  ],
  dbPath: process.env.DIYA_DB_PATH || path.join(__dirname, '..', 'data', 'diya.db'),
  contextSize: parseInt(process.env.DIYA_CONTEXT_SIZE || '4096', 10),
  threads: parseInt(process.env.DIYA_THREADS || '4', 10)
};
