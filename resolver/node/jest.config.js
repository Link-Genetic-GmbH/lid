module.exports = {
  testEnvironment: 'node',
  rootDir: '.',
  testMatch: ['**/test/**/*.test.js'],
  setupFilesAfterEnv: ['<rootDir>/test/jest.setup.js'],
  coveragePathIgnorePatterns: ['/node_modules/', '/test/'],
  verbose: false,
};


