import { chromium } from '@playwright/test'
import { mkdir } from 'node:fs/promises'

// Read-only visual smoke test against the running app, without seeding or mutating data.
const browser = await chromium.launch({ channel: 'msedge', headless: true })
const page = await browser.newPage({ viewport: { width: 1672, height: 1000 }, deviceScaleFactor: 1, reducedMotion: 'reduce' })
const errors = []
page.on('pageerror', error => errors.push(error.message))
const output = 'output/emerald-ui'
await mkdir(output, { recursive: true })
const routes = ['/', '/twin', '/places', '/network', '/tasks', '/knowledge', '/agents', '/media', '/analytics', '/settings']
const results = []
let visitNumber = 0
async function visit(route) {
  // A unique query forces a fresh document: no stale popovers, toast or hash-navigation timing.
  await page.goto(`http://127.0.0.1:5173/?visual-check=${++visitNumber}#${route}`)
  const ready = { '/':'.overview-page .stat-row', '/knowledge':'.kw-tabs', '/agents':'.kw-tabs', '/settings':'.setting-section', '/twin':'.model-stage', '/network':'.network-canvas' }[route] || '.page-root .stat-row'
  await page.locator(ready).first().waitFor({state:'visible'})
  await page.waitForLoadState('networkidle')
}
try {
  for (const route of routes) {
    await visit(route)
    await page.waitForLoadState('networkidle')
    await page.waitForFunction(() => ![...document.querySelectorAll('.page-root *')].some(el => el.children.length === 0 && /正在读取后台数据|正在从 MySQL|正在连接工作台/.test(el.textContent)))
    await page.waitForTimeout(350)
    const metrics = await page.evaluate(() => ({
      title: document.querySelector('h1')?.textContent,
      navigation: document.querySelectorAll('.main-nav a').length,
      active: document.querySelectorAll('.main-nav a.active').length,
      overflow: document.documentElement.scrollWidth > window.innerWidth,
      sidebar: getComputedStyle(document.querySelector('.sidebar')).backgroundImage,
    }))
    if (metrics.active !== 1 || metrics.overflow || metrics.navigation !== 10) throw new Error(`${route}: ${JSON.stringify(metrics)}`)
    if (route === '/') {
      const fonts = await page.evaluate(() => Object.fromEntries(['.nav-item','.stat-copy small','.chat-answer p','.compact-card h3'].map(selector => [selector, parseFloat(getComputedStyle(document.querySelector(selector)).fontSize)])))
      if (fonts['.nav-item'] !== 15 || fonts['.stat-copy small'] < 13 || fonts['.chat-answer p'] < 15 || fonts['.compact-card h3'] < 18) throw new Error(`Typography regression: ${JSON.stringify(fonts)}`)
    }
    await page.screenshot({ path: `${output}/${route === '/' ? 'overview' : route.slice(1)}.png`, fullPage: true, animations: 'disabled' })
    results.push({ route, ...metrics })
  }
  await page.goto('http://127.0.0.1:5173/#/network')
  await page.waitForLoadState('networkidle')
  await page.getByRole('button', { name: '路线测试', exact: true }).click()
  await page.getByRole('heading', { name: '规划条件设置' }).waitFor()
  await page.screenshot({ path: `${output}/route-planner.png`, fullPage: true, animations: 'disabled' })
  const selects = page.locator('.planner-form select')
  const origin = await selects.nth(0).inputValue()
  const destination = await selects.nth(1).inputValue()
  await page.getByRole('button', { name: '交换起点和终点' }).click()
  if (await selects.nth(0).inputValue() !== destination || await selects.nth(1).inputValue() !== origin) throw new Error('Swap endpoints failed')
  // A browser-only response fixture validates result/error rendering without writing planning logs to MySQL.
  await page.route('**/api/navigation/plan', async route => {
    const body = route.request().postDataJSON()
    await route.fulfill({ json: { ...body, strategy: 'DISTANCE', confidence: 1, distanceMeters: 120, estimatedMinutes: 2, evidence: ['UI 自动化测试响应，不写入数据库'], blockedRoads: [], steps: [{roadId:-1,roadName:'UI测试路段',fromNode:body.origin,toNode:body.destination,lengthMeters:120,status:'OPEN',accessible:true}] } })
  })
  await page.getByRole('button', { name: '生成路线', exact: true }).click()
  await page.getByText('UI测试路段', { exact: true }).waitFor()
  await page.getByLabel('仅选择无障碍道路').check()
  await page.getByRole('heading', { name: '等待规划出行路线' }).waitFor()
  await page.unroute('**/api/navigation/plan')
  await page.route('**/api/navigation/plan', route => route.fulfill({status:400,json:{message:'测试：没有符合条件的路线'}}))
  await page.getByRole('button', { name: '生成路线', exact: true }).click()
  await page.getByRole('alert').filter({hasText:'测试：没有符合条件的路线'}).waitFor()
  await page.unroute('**/api/navigation/plan')
  await visit('/places')
  await page.waitForLoadState('networkidle')
  await page.getByRole('button', { name: /新增地点/ }).click()
  await page.locator('.modal').waitFor({ state: 'visible' })
  await page.screenshot({ path: `${output}/place-modal.png` })
  await page.locator('.modal-close').click()
  await page.goto('http://127.0.0.1:5173/#/settings')
  await page.getByRole('button', { name: '用户与角色', exact: true }).click()
  await page.getByRole('heading', { name: '当前管理账户' }).waitFor()
  await page.getByRole('button', { name: '通知', exact: true }).click()
  await page.locator('.notice-menu').waitFor()
  for (const width of [1366, 1024]) {
    await page.setViewportSize({ width, height: 900 })
    for (const route of ['/', '/network', '/knowledge', '/agents', '/settings']) {
      await visit(route)
      await page.waitForLoadState('networkidle')
      if (route === '/network') await page.getByRole('button', {name:'路线测试',exact:true}).click()
      const overflowing = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)
      if (overflowing) throw new Error(`Horizontal page overflow at ${width}px: ${route}`)
    }
    await visit('/')
    await page.waitForLoadState('networkidle')
    await page.screenshot({ path: `${output}/overview-${width}.png`, fullPage: true })
  }
  if (errors.length) throw new Error(errors.join('\n'))
  console.log(JSON.stringify({ passed: true, pages: results, interactions: ['new place dialog', 'settings tabs', 'notifications', 'route planner swap, result, invalidation and error with isolated response fixture'], widths: [1672, 1366, 1024], pageErrors: errors }, null, 2))
} finally {
  await browser.close()
}
