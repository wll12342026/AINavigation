import { test } from 'node:test'
import assert from 'node:assert/strict'
import { normalizeModelUrl, viewerBudget } from '../../src/twin/viewer.ts'

test('模型地址必须是支持格式的 HTTP URL，保留签名查询参数', () => {
  assert.equal(normalizeModelUrl('/models/campus.sog', 'http://localhost:5173/#/twin'), 'http://localhost:5173/models/campus.sog')
  assert.equal(normalizeModelUrl('https://models.example/campus.sog?token=abc', 'https://campus.example/'), 'https://models.example/campus.sog?token=abc')
  assert.equal(normalizeModelUrl('/models/scene/lod-meta.json', 'http://localhost:5173'), 'http://localhost:5173/models/scene/lod-meta.json')
})
test('拒绝磁盘路径、脚本、账号密码、错误格式与混合内容', () => {
  for (const value of ['C:\\data\\campus.sog', 'file:///campus.sog', 'javascript:alert(1)', '//host/campus.sog', '/scene.glb', '/scene.ply', 'https://name:password@host/campus.sog']) {
    assert.throws(() => normalizeModelUrl(value, 'https://campus.example/'))
  }
  assert.throws(() => normalizeModelUrl('http://host/campus.sog', 'https://campus.example/'))
})
test('低内存和移动设备限载，预算参数使用百万点单位', () => {
  assert.equal(viewerBudget(true, 16), 1)
  assert.equal(viewerBudget(false, 4), 1)
  assert.equal(viewerBudget(false, 8), 3)
  assert.equal(viewerBudget(false, 16), 6)
})
