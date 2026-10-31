import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  outputDir: '../backend/target/playwright',
  workers: 1,
  timeout: 90000,
  expect: { timeout: 15000 },
  reporter: 'list',
  use: {
    channel: process.env.PLAYWRIGHT_CHANNEL,
    baseURL: 'http://localhost:5173',
    viewport: { width: 1280, height: 900 },
    trace: 'off',
    screenshot: 'off',
    video: 'off'
  }
});
