import { test, expect } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  await page.route('**/api/settings', route => route.fulfill({ json: { twinModelName: '', twinModelUrl: '' } }))
})

test('没有模型时不创建查看器，不宣称在线', async ({ page }) => {
  await page.goto('/#/twin')
  await expect(page.getByRole('heading', { name: '校园三维场景待接入' })).toBeVisible()
  await expect(page.locator('.splat-viewer iframe')).toHaveCount(0)
  await expect(page.getByText('等待模型', { exact: true })).toBeVisible()
  await expect(page.locator('.twin-page')).toHaveCSS('opacity', '1')
  await page.screenshot({ path: 'output/twin-model-empty.png', fullPage: true, animations: 'disabled' })
})

test('错误文件格式和本地磁盘路径不能加载', async ({ page }) => {
  await page.goto('/#/twin')
  const address = page.getByPlaceholder('/models/campus.sog 或 https://…/campus.sog')
  await address.fill('C:\\models\\campus.sog')
  await page.getByRole('button', { name: '临时预览' }).click()
  await expect(page.locator('.config-error')).toContainText('磁盘路径')
  await address.fill('/models/campus.glb')
  await page.getByRole('button', { name: '临时预览' }).click()
  await expect(page.locator('.config-error')).toContainText('仅支持 .sog')
  await expect(page.locator('.splat-viewer iframe')).toHaveCount(0)
})

test('真实查看器遇到不存在模型显示失败，支持重试', async ({ page }) => {
  let requests = 0
  await page.route('**/models/missing.sog', async route => { requests++; await route.fulfill({ status: 404, body: 'missing' }) })
  await page.goto('/#/twin')
  await page.getByPlaceholder('/models/campus.sog 或 https://…/campus.sog').fill('/models/missing.sog')
  await page.getByRole('button', { name: '临时预览' }).click()
  await expect(page.getByRole('heading', { name: '场景暂时无法显示' })).toBeVisible()
  await expect(page.getByText('模型首帧已显示', { exact: true })).toHaveCount(0)
  await page.screenshot({ path: 'output/twin-model-error.png', fullPage: true, animations: 'disabled' })
  const firstAttempt = requests
  await page.getByRole('button', { name: '重新加载', exact: true }).click()
  await expect(page.getByRole('heading', { name: '场景暂时无法显示' })).toBeVisible()
  expect(requests).toBeGreaterThan(firstAttempt)
})

test('iframe文档加载不等于模型就绪，首帧消息必须来自当前查看器', async ({ page }) => {
  // 模拟消息只检验父子页面状态协议，不验证真实模型渲染。
  await page.route('**/twin-viewer/index.html?**', route => route.fulfill({ contentType: 'text/html', body: '<html><body>Viewer bridge test</body></html>' }))
  await page.goto('/#/twin')
  await page.getByPlaceholder('/models/campus.sog 或 https://…/campus.sog').fill('/models/bridge-test.sog')
  await page.getByRole('button', { name: '临时预览' }).click()
  await expect(page.locator('.viewer-state.loading')).toBeVisible()
  await page.evaluate(() => window.postMessage({ source: 'campus-twin', type: 'ready' }, location.origin))
  await expect(page.locator('.viewer-state.loading')).toBeVisible()
  const child = page.frames().find(frame => frame.url().includes('/twin-viewer/'))!
  await child.waitForLoadState()
  await child.evaluate(() => parent.postMessage({ source: 'campus-twin', channel: new URL(location.href).searchParams.get('channel'), type: 'ready' }, location.origin))
  await expect(page.getByText('模型首帧已显示', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '复位视角' })).toBeVisible()
  const oldSource = await page.locator('.splat-viewer iframe').getAttribute('src')
  await page.getByRole('button', { name: '复位视角' }).click()
  await expect(page.locator('.splat-viewer iframe')).not.toHaveAttribute('src', oldSource!)
  await expect(page.locator('.viewer-state.loading')).toBeVisible()
})

test('临时预览不写配置，保存使用现有设置接口并能重新读取', async ({ page }) => {
  let saved = { twinModelName: '', twinModelUrl: '' }
  let writes = 0
  await page.route('**/api/settings', async route => {
    if (route.request().method() === 'PUT') {
      writes++
      const body = route.request().postDataJSON()
      expect(Object.keys(body).sort()).toEqual(['twinModelName', 'twinModelUrl'])
      saved = body
    }
    await route.fulfill({ json: saved })
  })
  await page.route('**/twin-viewer/index.html?**', route => route.fulfill({ contentType: 'text/html', body: '<html><body>Viewer bridge test</body></html>' }))
  await page.goto('/#/twin')
  await page.getByPlaceholder('例如：主校区航拍场景').fill('校园场景验收')
  await page.getByPlaceholder('/models/campus.sog 或 https://…/campus.sog').fill('/models/test.sog')
  await page.getByRole('button', { name: '临时预览' }).click()
  expect(writes).toBe(0)
  await page.getByRole('button', { name: '保存并加载' }).click()
  await expect(page.locator('.toast')).toContainText('模型地址已保存')
  expect(writes).toBe(1)
  await page.reload()
  await expect(page.getByPlaceholder('/models/campus.sog 或 https://…/campus.sog')).toHaveValue('/models/test.sog')
})
