import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './scripts/tests',
  testMatch: 'twin-page.spec.ts',
  workers: 1,
  timeout: 30000,
  use: {
    channel: 'msedge', headless: true,
    baseURL: process.env.TWIN_BASE_URL || 'http://127.0.0.1:5173',
    viewport: { width: 1600, height: 1000 },
    trace: 'retain-on-failure',
  },
})
