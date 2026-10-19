import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { css, html, js } from '@playcanvas/supersplat-viewer'

const output = new URL('../public/twin-viewer/', import.meta.url)
const hook = 'const viewer = await main(canvas, settingsJson, config);'
if (!html.includes(hook)) throw new Error('查看器初始化接口变更，请核对模型状态桥接')
const page = html
  .replace('<head>', '<head><script src="./campus-bridge.js"></script>')
  .replace('contents: fetch(contentUrl),', `contents: fetch(contentUrl).then(response => {
    if (!response.ok) throw new Error('模型请求失败：HTTP ' + response.status);
    return response;
  }),`)
  .replace(hook, `${hook}
    viewer.global.events.on('firstFrame', () => window.campusTwinReport('ready'));
    viewer.global.events.on('progress:changed', value => window.campusTwinReport('progress', { value }));
    if (viewer.global.state.loaded) window.campusTwinReport('ready');`)

const bridge = `(() => {
  const channel = new URL(location.href).searchParams.get('channel');
  window.campusTwinReport = (type, payload = {}) => {
    if (window.parent !== window) window.parent.postMessage({ source: 'campus-twin', channel, type, ...payload }, location.origin);
  };
  window.addEventListener('unhandledrejection', event => {
    window.campusTwinReport('error', { message: String(event.reason?.message || event.reason || '模型加载失败') });
  });
  window.addEventListener('error', event => {
    window.campusTwinReport('error', { message: event.message || '查看器资源未能加载' });
  }, true);
  window.campusTwinReport('boot');
})();`

await mkdir(output, { recursive: true })
await mkdir(new URL('../public/models/', import.meta.url), { recursive: true })
await Promise.all([
  writeFile(new URL('index.html', output), page),
  writeFile(new URL('index.css', output), css),
  writeFile(new URL('index.js', output), js),
  writeFile(new URL('campus-bridge.js', output), bridge),
  writeFile(new URL('LICENSE', output), await readFile(new URL('../LICENSE', import.meta.resolve('@playcanvas/supersplat-viewer')))),
  writeFile(new URL('settings.json', output), JSON.stringify({
    background: { color: [0.04, 0.08, 0.14] },
    camera: { fov: 60, startAnim: 'none' }, animTracks: [],
  })),
])
console.log(`Campus SuperSplat viewer generated: ${fileURLToPath(output)}`)
