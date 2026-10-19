export function viewerBudget(mobile: boolean, memory?: number): number {
  // SuperSplat 1.28.0 的 budget 查询参数单位是百万点。
  return mobile || (memory != null && memory <= 4) ? 1 : memory != null && memory >= 12 ? 6 : 3
}

export function normalizeModelUrl(value: string, base: string): string {
  const input = value.trim()
  if (!input) return ''
  if (!/^(https?:\/\/|\/)/i.test(input) || input.startsWith('//')) {
    throw new Error('请填写 HTTP(S) 模型地址或以 / 开头的同源路径，不能填写磁盘路径')
  }
  const url = new URL(input, base)
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) {
    throw new Error('模型地址不合法，请勿在地址中填写账号密码')
  }
  if (!/\.sog$/i.test(url.pathname) && !/\/lod-meta\.json$/i.test(url.pathname)) {
    throw new Error('当前仅支持 .sog 或完整 LOD 资源目录中的 lod-meta.json；PLY 转换尚未接入')
  }
  if (new URL(base).protocol === 'https:' && url.protocol === 'http:') {
    throw new Error('HTTPS 页面请使用 HTTPS 模型地址，避免混合内容被浏览器阻止')
  }
  return url.href
}
