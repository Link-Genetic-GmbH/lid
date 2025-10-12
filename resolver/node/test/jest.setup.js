process.env.LOG_LEVEL = process.env.LOG_LEVEL || 'error';
process.env.REDIS_URL = '';
process.env.MONGODB_URL = 'mongodb://127.0.0.1:0/test';

// Silence console during tests (can be toggled via env)
const silent = process.env.TEST_SILENT_CONSOLE !== 'false';
if (silent) {
  for (const method of ['log', 'info', 'warn', 'error']) {
    // eslint-disable-next-line no-console
    console[method] = jest.fn();
  }
}


