const navItems = [
  { id: 'overview', label: '运行总览', icon: 'dashboard', title: '校园空间智能管理中心', subtitle: '实时掌握空间、路网与新生任务运行状态' },
  { id: 'twin', label: '数字孪生', icon: 'cube', title: '数字孪生场景中心', subtitle: '管理校园模型、空间对象与语义标注' },
  { id: 'places', label: '校园地点', icon: 'pin', title: '校园地点管理', subtitle: '维护建筑、办事点、入口与导航语义信息' },
  { id: 'network', label: '导航路网', icon: 'route', title: '校园导航路网', subtitle: '可视化编辑节点、道路与动态通行状态' },
  { id: 'tasks', label: '新生任务', icon: 'task', title: '新生任务编排', subtitle: '配置报到、校园卡、寝室入住等任务流程' },
  { id: 'knowledge', label: 'AI知识库', icon: 'book', title: 'AI 校园知识库', subtitle: '建设可追溯、可审核的校园办事与地点知识' },
  { id: 'media', label: '实景素材', icon: 'image', title: '校园实景素材', subtitle: '管理建筑外观、入口指引与地标识别素材' },
  { id: 'analytics', label: '运营分析', icon: 'chart', title: '智能运营分析', subtitle: '分析新生问询、导航行为与任务完成情况' },
  { id: 'settings', label: '系统设置', icon: 'settings', title: '系统设置', subtitle: '配置 AI 模型、数字孪生、权限与系统参数' }
];

const svgPaths = {
  dashboard: '<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',
  cube: '<path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z"/><path d="m4.5 7.8 7.5 4.3 7.5-4.3M12 12v9"/>',
  pin: '<path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.4"/>',
  route: '<circle cx="6" cy="18" r="2"/><circle cx="18" cy="6" r="2"/><path d="M8 18h3a3 3 0 0 0 3-3v-1a3 3 0 0 0-3-3H9a3 3 0 0 1-3-3V6M16 6H9"/>',
  task: '<rect x="4" y="3" width="16" height="18" rx="2"/><path d="M9 3v3h6V3M8 11h8M8 15h5"/>',
  book: '<path d="M4 4.5A2.5 2.5 0 0 1 6.5 2H11v18H6.5A2.5 2.5 0 0 0 4 22V4.5ZM20 4.5A2.5 2.5 0 0 0 17.5 2H13v18h4.5A2.5 2.5 0 0 1 20 22V4.5Z"/>',
  image: '<rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><path d="m21 15-5-5L5 20"/>',
  chart: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-4V21a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 0 0 .3-1.9A1.7 1.7 0 0 0 3 14H2.8v-4H3a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9L4.2 7 7 4.2l.1.1a1.7 1.7 0 0 0 1.9.3A1.7 1.7 0 0 0 10 3V2.8h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v4H21a1.7 1.7 0 0 0-1.6 1Z"/>'
};

const icon = (name) => `<svg viewBox="0 0 24 24" aria-hidden="true">${svgPaths[name] || svgPaths.dashboard}</svg>`;
const statCard = (name, value, sub, tone, tint, trend = '') => `
  <article class="stat-card" style="--tone:${tone};--tint:${tint}">
    <div class="stat-icon">${icon(name)}</div>
    <div class="stat-copy"><span>${sub}</span><strong>${value}</strong><small>较昨日数据已同步</small></div>
    ${trend ? `<div class="stat-trend">${trend}</div>` : ''}
  </article>`;

const campusImage = () => `<div class="map-image-crop"><img src="./assets/dashboard-reference.png" alt="校园场景演示底图" /></div><div class="map-shade"></div>`;

function overviewPage() {
  return `
    <div class="page-enter">
      <div class="grid cols-4 stat-row">
        ${statCard('pin','128','校园地点','#3478f5','#ebf3ff','+8')}
        ${statCard('route','6.8km','路网里程','#2f7af2','#e8f5ff','+0.4km')}
        ${statCard('dashboard','96%','开放道路','#13ba8e','#e8faf5','良好')}
        ${statCard('book','12','待审核知识','#7057ee','#f0edff','-3')}
      </div>
      <div class="dashboard-grid">
        <div class="map-card">
          ${campusImage()}
          <div class="map-toolbar"><button class="active">地点</button><button>路网</button><button>任务</button><button>热力</button></div>
          <div class="map-tools"><button data-action="locate">◎</button><button data-action="zoom-in">+</button><button data-action="zoom-out">−</button><button data-action="layers">◈</button></div>
          <div class="map-legend"><span><i class="legend-dot" style="background:#29d7aa"></i>正常路线</span><span><i class="legend-dot" style="background:#ff5b61"></i>施工路段</span><span><i class="legend-dot" style="background:#5da0ff"></i>校园地点</span></div>
          <div class="mock-badge">● 演示场景 · 等待真实孪生数据</div>
        </div>
        <article class="panel ai-panel">
          <div class="ai-head"><div class="ai-brand"><div class="ai-orb">AI</div><div><h2>AI空间运营助手</h2><p>理解校园空间，辅助决策与任务调度</p></div></div></div>
          <div class="ai-body">
            <div class="chat-user">关闭图书馆东侧道路，并检查受影响的新生路线</div>
            <div class="chat-answer"><strong>影响 3 条任务路线，已生成绕行方案</strong><p>施工将影响新生报到、校园卡领取和寝舍入住 3 条路线。基于当前路网生成最优绕行方案，预计增加步行 280 米。</p><div class="chat-actions"><button class="btn" data-action="preview-plan">查看方案</button><button class="btn primary" data-action="apply-plan">应用调整</button></div></div>
            <div class="event-list"><div class="event"><i>✓</i><span><b>路网检查完成</b>已识别封闭路段及周边道路</span><time>14:23</time></div><div class="event"><i>✓</i><span><b>任务影响已分析</b>共影响 3 条新生任务路线</span><time>14:23</time></div><div class="event"><i>✓</i><span><b>新路线已验证</b>绕行方案通过路径仿真</span><time>14:24</time></div></div>
          </div>
        </article>
      </div>
      <div class="bottom-grid">
        <article class="panel panel-pad"><div class="panel-title"><span class="title-bar"></span><h3>AI咨询趋势</h3></div><div class="line-chart"><svg viewBox="0 0 400 130" preserveAspectRatio="none"><defs><linearGradient id="area" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#3778f5" stop-opacity=".28"/><stop offset="1" stop-color="#3778f5" stop-opacity="0"/></linearGradient></defs><path d="M0 115 C35 108 45 92 72 95 S112 70 138 73 S170 30 195 54 S220 18 245 44 S272 52 291 43 S321 77 343 57 S370 72 400 62 L400 130 L0 130Z" fill="url(#area)"/><path d="M0 115 C35 108 45 92 72 95 S112 70 138 73 S170 30 195 54 S220 18 245 44 S272 52 291 43 S321 77 343 57 S370 72 400 62" fill="none" stroke="#3275f3" stroke-width="3"/></svg></div><div class="chart-axis"><span>00:00</span><span>04:00</span><span>08:00</span><span>12:00</span><span>16:00</span><span>20:00</span></div></article>
        <article class="panel panel-pad"><div class="panel-title" style="margin-bottom:16px"><span class="title-bar"></span><h3>新生任务完成情况</h3></div><div class="progress-list"><div class="progress-item"><b>学院报到</b><div class="progress-track"><i style="width:89%;--bar:#1cc89b"></i></div><span class="progress-num">428/480　<b style="color:#14a77f">89%</b></span></div><div class="progress-item"><b>领取校园卡</b><div class="progress-track"><i style="width:82%;--bar:#3c83f7"></i></div><span class="progress-num">392/480　<b style="color:#2f72ee">82%</b></span></div><div class="progress-item"><b>宿舍入住</b><div class="progress-track"><i style="width:74%;--bar:#7655ec"></i></div><span class="progress-num">356/480　<b style="color:#6b50dc">74%</b></span></div><div class="progress-item"><b>校园导览</b><div class="progress-track"><i style="width:65%;--bar:#ff8b45"></i></div><span class="progress-num">310/480　<b style="color:#e87332">65%</b></span></div></div></article>
        <article class="panel panel-pad"><div class="panel-title" style="margin-bottom:14px"><span class="title-bar"></span><h3>知识库健康度</h3></div><div class="health"><div class="health-ring"><strong>92%</strong></div><div class="health-info"><div class="health-alert">⚠ 7 条内容即将过期</div><span class="metric-sub">最近更新</span><div class="thumbs"><i class="thumb"></i><i class="thumb"></i><i class="thumb"></i><i class="thumb"></i></div></div></div></article>
      </div>
    </div>`;
}

function twinPage() {
  return `<div class="page-enter"><div class="page-tools"><div class="page-tools-left"><div class="segmented"><button class="active">场景编辑</button><button>模型版本</button><button>语义标注</button></div><span class="tag orange">● 真实模型未接入</span></div><div class="page-tools-right"><button class="btn" data-action="scene-check">AI 场景检查</button><button class="btn primary" data-action="upload-model">上传孪生模型</button></div></div>
  <div class="content-split"><section class="panel scene-workspace">${campusImage()}<div class="scene-top"><div class="glass-control"><button class="active">选择</button><button>标注 POI</button><button>测量</button><button>设置原点</button></div><div class="glass-control"><button>俯视</button><button>默认视角</button><button>全屏</button></div></div><div class="scene-status"><span>模型：演示场景 v0.1</span><span>坐标系：待标定</span><span>POI：128</span></div></section>
  <aside class="grid"><article class="panel"><div class="panel-head"><div><h3>场景对象</h3><p>当前选中的空间语义对象</p></div><span class="tag green">已启用</span></div><div class="panel-pad property-list"><div class="property"><label>对象名称</label><strong>图书馆</strong></div><div class="property"><label>对象 ID</label><span>BLD-LIB-001</span></div><div class="property"><label>场景坐标</label><span>126.50, 0, 82.30</span></div><div class="property"><label>类型</label><span class="tag">公共建筑</span></div><div class="property"><label>入口数</label><span>3 个</span></div><div class="property"><label>数据状态</label><span class="tag orange">等待真实坐标</span></div></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>✦ AI 场景建议</b><p>当前建筑已绑定 3 个入口，但东侧入口缺少朝向和实景照片。建议在真实模型接入后进行二次标定。</p></div><div style="height:14px"></div><button class="btn primary" style="width:100%" data-action="semantic-mark">开始语义标注</button></article><article class="panel panel-pad"><div class="panel-title"><span class="title-bar"></span><h3>接入进度</h3></div><div class="progress-list" style="margin-top:15px"><div class="progress-item" style="grid-template-columns:90px 1fr 38px"><b>POI数据</b><div class="progress-track"><i style="width:78%;--bar:#3479f4"></i></div><span>78%</span></div><div class="progress-item" style="grid-template-columns:90px 1fr 38px"><b>路网数据</b><div class="progress-track"><i style="width:66%;--bar:#18c79a"></i></div><span>66%</span></div><div class="progress-item" style="grid-template-columns:90px 1fr 38px"><b>3D模型</b><div class="progress-track"><i style="width:8%;--bar:#7657ed"></i></div><span>8%</span></div></div></article></aside></div></div>`;
}

const placeRows = [
  ['图书馆','公共建筑','3 个','08:00—22:00','已发布','自习、借阅、报到'],
  ['校园卡中心','办事服务','2 个','08:30—17:30','已发布','校园卡、充值、挂失'],
  ['第一学院报到点','新生服务','1 个','08:00—18:00','已发布','报到、资料审核'],
  ['学生公寓 A 区','宿舍','4 个','全天','已发布','寝室、入住、宿管'],
  ['第一食堂','生活服务','5 个','06:30—21:00','已发布','餐饮、清真窗口'],
  ['校医院','医疗服务','2 个','08:00—22:00','待审核','体检、就诊、急救']
];

function placesPage() {
  return `<div class="page-enter"><div class="grid cols-4 stat-row">${statCard('pin','128','地点总数','#3478f5','#ebf3ff')}${statCard('dashboard','36','建筑地点','#18b68a','#e8faf5')}${statCard('task','21','新生服务点','#7057ee','#f0edff')}${statCard('book','7','待完善地点','#ef7d3f','#fff1e8')}</div><div class="page-tools"><div class="page-tools-left"><input class="filter-input" id="placeFilter" placeholder="搜索地点名称或标签"/><select class="select-input"><option>全部类型</option><option>公共建筑</option><option>新生服务</option></select><select class="select-input"><option>全部状态</option><option>已发布</option><option>待审核</option></select></div><div class="page-tools-right"><button class="btn" data-action="ai-tag">AI补全语义标签</button><button class="btn primary" data-action="add-place">+新增地点</button></div></div><div class="content-split"><section class="panel"><table class="data-table"><thead><tr><th>地点名称</th><th>类型</th><th>入口</th><th>开放时间</th><th>状态</th><th>操作</th></tr></thead><tbody id="placeTable">${placeRows.map((r,i)=>`<tr data-search="${r.join(' ')}"><td><div class="name-cell"><span class="poi-icon">${['▦','◉','◎','▤','♨','+'][i]}</span>${r[0]}</div></td><td>${r[1]}</td><td>${r[2]}</td><td>${r[3]}</td><td><span class="tag ${r[4]==='待审核'?'orange':'green'}">${r[4]}</span></td><td><button class="table-action" data-action="edit-place">编辑</button><button class="table-action" data-action="view-place">查看</button></td></tr>`).join('')}</tbody></table><div class="pagination"><span class="metric-sub" style="margin-right:auto">共 128 条地点</span><button>‹</button><button class="active">1</button><button>2</button><button>3</button><button>›</button></div></section><aside class="grid"><article class="panel"><div class="panel-head"><div><h3>图书馆</h3><p>BLD-LIB-001 · 已发布</p></div><span class="tag green">导航可用</span></div><div class="panel-pad property-list"><div class="property"><label>建筑坐标</label><span>126.50, 82.30</span></div><div class="property"><label>推荐入口</label><strong>南门主入口</strong></div><div class="property"><label>办事事项</label><span>新生报到、借阅开通</span></div><div class="property"><label>语义标签</label><span><i class="tag">自习</i> <i class="tag purple">学习</i></span></div><div class="property"><label>实景素材</label><span>12 张·入口 3 张</span></div></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>✦ AI 语义完善建议</b><p>根据最近 86 次新生问询，建议补充“借阅证开通”和“东侧自助服务机”两项语义。</p></div></article></aside></div></div>`;
}

function networkPage() {
  return `<div class="page-enter"><div class="page-tools"><div class="page-tools-left"><div class="segmented"><button class="active">路网编辑</button><button>路线测试</button><button>变更记录</button></div><span class="tag green">96% 道路可通行</span></div><div class="page-tools-right"><button class="btn" data-action="network-check">AI路网质检</button><button class="btn primary" data-action="add-road">+新增路段</button></div></div><div class="content-split"><section class="panel network-canvas"><svg viewBox="0 0 900 640" preserveAspectRatio="none"><g fill="none" stroke="#79a7ef" stroke-width="4"><path d="M120 510 C210 460 190 360 310 330 S460 250 560 300 S690 245 790 160"/><path d="M310 330 C390 400 500 470 690 470"/><path d="M560 300 C590 200 520 150 420 110"/><path d="M560 300 C660 330 735 370 790 410"/></g><path d="M310 330 C390 305 470 275 560 300" fill="none" stroke="#ff626c" stroke-width="7" stroke-dasharray="12 8"/><g fill="#fff" stroke="#4d8bf4" stroke-width="3"><circle cx="120" cy="510" r="7"/><circle cx="310" cy="330" r="7"/><circle cx="560" cy="300" r="7"/><circle cx="790" cy="160" r="7"/><circle cx="690" cy="470" r="7"/><circle cx="420" cy="110" r="7"/><circle cx="790" cy="410" r="7"/></g></svg><div class="node major" style="left:8%;top:78%">东门入口<br/>N-001</div><div class="node" style="left:28%;top:47%">学院路口<br/>N-016</div><div class="node closed" style="left:50%;top:43%">图书馆东路<br/>施工中</div><div class="node major" style="left:80%;top:20%">校园卡中心<br/>N-042</div><div class="node" style="left:68%;top:72%">学生公寓<br/>N-063</div><div class="node" style="left:38%;top:12%">图书馆西口<br/>N-028</div><div class="scene-status"><span>节点 186</span><span>路段 244</span><span>闭合路段 2</span></div></section><aside class="grid"><article class="panel"><div class="panel-head"><div><h3>图书馆东路</h3><p>ROAD-056 · 已关闭</p></div><span class="tag red">施工</span></div><div class="panel-pad property-list"><div class="property"><label>路段长度</label><strong>126 米</strong></div><div class="property"><label>通行方向</label><span>双向步行</span></div><div class="property"><label>影响路线</label><span class="tag red">3 条</span></div><div class="property"><label>关闭时间</label><span>10月15日—10月22日</span></div></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>✦ AI 路网质检</b><p>发现 2 个路网断点与 1 条疑似重复路段。关闭图书馆东路后，建议将西侧林荫路提高为首选绕行道路。</p></div><div style="height:12px"></div><button class="btn primary" style="width:100%" data-action="route-test">运行路径仿真</button></article></aside></div></div>`;
}

function tasksPage() {
  return `<div class="page-enter"><div class="grid cols-4 stat-row">${statCard('task','12','任务模板','#3478f5','#ebf3ff')}${statCard('dashboard','46','任务节点','#18b68a','#e8faf5')}${statCard('route','4.6','平均步骤','#7057ee','#f0edff')}${statCard('book','3','待审核变更','#ef7d3f','#fff1e8')}</div><div class="content-split left-narrow"><aside class="panel"><div class="panel-head"><div><h3>任务模板</h3><p>拖拽节点编排新生流程</p></div><button class="panel-link" data-action="add-task">+新建</button></div><div class="panel-pad doc-list"><div class="doc-item active"><span class="doc-ico">1</span><span><b>新生报到一日通</b><small>6 个节点 · 已发布</small></span><span class="tag green">运行中</span></div><div class="doc-item"><span class="doc-ico">2</span><span><b>校园卡办理</b><small>3 个节点 · 已发布</small></span><span>⋮</span></div><div class="doc-item"><span class="doc-ico">3</span><span><b>宿舍入住</b><small>4 个节点 · 已发布</small></span><span>⋮</span></div><div class="doc-item"><span class="doc-ico">4</span><span><b>校医院体检</b><small>5 个节点 · 待审核</small></span><span>⋮</span></div></div><div class="panel-pad"><div class="ai-suggest"><b>✦ AI 发现新通知</b><p>最新入学通知将校园卡领取地点更改为综合服务中心，可一键同步到任务流。</p><button class="btn primary" style="width:100%;margin-top:10px" data-action="sync-notice">生成变更草稿</button></div></div></aside><section class="panel"><div class="panel-head"><div><h3>新生报到一日通</h3><p>任务 ID：TASK-2026-001 · 预计 2 小时 35 分钟</p></div><div><button class="btn" data-action="task-preview">预览学生端</button> <button class="btn primary" data-action="save-task">保存发布</button></div></div><div class="workflow"><div class="flow-node"><span class="step">1</span><div><b>到达学院报到点</b><p>身份核验 · 录取通知书、身份证</p></div><span class="flow-meta">预计 20 分钟</span></div><div class="flow-line"></div><div class="flow-node"><span class="step">2</span><div><b>完成入学资料审核</b><p>学院报到点 · 前置任务：身份核验</p></div><span class="flow-meta">预计 15 分钟</span></div><div class="flow-line"></div><div class="flow-node"><span class="step">3</span><div><b>领取校园卡</b><p>校园卡中心 · 需携带身份证原件</p></div><span class="flow-meta">预计 25 分钟</span></div><div class="flow-line"></div><div class="flow-node"><span class="step">4</span><div><b>宿舍入住登记</b><p>学生公寓 A 区 · 完成钥匙和床品领取</p></div><span class="flow-meta">预计 35 分钟</span></div><div class="flow-line"></div><div class="flow-node"><span class="step">✓</span><div><b>完成报到</b><p>AI 将根据开放时间和实时路况动态调整顺序</p></div><span class="tag green">自动触发</span></div></div></section></div></div>`;
}

function knowledgePage() {
  return `<div class="page-enter"><div class="grid cols-4 stat-row">${statCard('book','2,486','知识条目','#3478f5','#ebf3ff')}${statCard('task','96.2%','可追溯率','#18b68a','#e8faf5')}${statCard('dashboard','92%','知识健康度','#7057ee','#f0edff')}${statCard('book','7','即将过期','#ef7d3f','#fff1e8')}</div><div class="content-split left-narrow"><aside class="panel"><div class="panel-head"><div><h3>知识来源</h3><p>文档解析、审核与版本追踪</p></div><button class="panel-link" data-action="upload-doc">+上传</button></div><div class="panel-pad doc-list"><div class="doc-item active"><span class="doc-ico">文</span><span><b>2026级新生入学指南</b><small>286 条知识 · 2小时前</small></span><span class="tag green">有效</span></div><div class="doc-item"><span class="doc-ico">通</span><span><b>校园卡办理通知</b><small>42 条知识 · 昨天</small></span><span class="tag orange">待审核</span></div><div class="doc-item"><span class="doc-ico">图</span><span><b>校园地点与开放时间</b><small>196 条知识 · 3天前</small></span><span class="tag green">有效</span></div><div class="doc-item"><span class="doc-ico">问</span><span><b>新生常见问题</b><small>363 条知识 · 5天前</small></span><span class="tag green">有效</span></div></div></aside><section class="grid"><article class="panel"><div class="panel-head"><div><h3>语义关系图谱</h3><p>任务—地点—部门—材料—时间关联</p></div><div class="segmented"><button class="active">图谱</button><button>条目</button></div></div><div class="semantic-graph"><svg viewBox="0 0 800 390"><g stroke="#bdcff0" stroke-width="1.5"><path d="M400 195 210 90M400 195 600 75M400 195 660 235M400 195 485 330M400 195 180 280M210 90 105 155M600 75 700 120M660 235 735 315M180 280 90 335"/></g></svg><span class="graph-label core" style="left:50%;top:50%">新生报到</span><span class="graph-label" style="left:26%;top:23%">学院报到点</span><span class="graph-label" style="left:75%;top:19%">学生工作处</span><span class="graph-label" style="left:82%;top:60%">录取通知书</span><span class="graph-label" style="left:61%;top:84%">新生报到日</span><span class="graph-label" style="left:22%;top:72%">身份证</span><span class="graph-label" style="left:13%;top:40%">综合楼</span><span class="graph-label" style="left:88%;top:31%">招生办</span><span class="graph-label" style="left:92%;top:81%">必需材料</span><span class="graph-label" style="left:11%;top:86%">身份验证</span></div></article><div class="grid cols-2"><article class="panel panel-pad"><div class="panel-title"><span class="title-bar"></span><h3>AI问答测试</h3></div><div class="chat-user" style="margin:15px 0 10px">校园卡在哪里领？需要带什么？</div><div class="chat-answer"><strong>请前往综合服务中心 1 楼</strong><p>需携带本人身份证原件与录取通知书。服务时间为 08:30—17:30。</p><span class="tag green">置信度 96%</span> <span class="tag">来源 2 条</span></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>✦ 质量审计结果</b><p>7 条关于迎新摆渡车的知识将在 3 天后过期；2 个办事时间与最新通知存在冲突。</p><button class="btn primary" style="width:100%;margin-top:10px" data-action="review-knowledge">进入审核队列</button></div></article></div></section></div></div>`;
}

const mediaNames = ['东门入口全景','图书馆南门','校园卡中心入口','学生公寓 A 区','学院报到点','第一食堂东门','校医院正门','体育场西入口','综合服务中心'];
function mediaPage() {
  return `<div class="page-enter"><div class="grid cols-4 stat-row">${statCard('image','1,286','实景图片','#3478f5','#ebf3ff')}${statCard('pin','128','已绑定地点','#18b68a','#e8faf5')}${statCard('dashboard','94%','AI质检通过率','#7057ee','#f0edff')}${statCard('image','23','待标注素材','#ef7d3f','#fff1e8')}</div><div class="page-tools"><div class="page-tools-left"><input class="filter-input" placeholder="搜索素材或地点"/><select class="select-input"><option>全部地点</option><option>图书馆</option><option>学生公寓</option></select><select class="select-input"><option>全部类型</option><option>建筑外观</option><option>入口指引</option></select></div><div class="page-tools-right"><button class="btn" data-action="ai-media-check">AI批量质检</button><button class="btn primary" data-action="upload-media">+上传素材</button></div></div><div class="content-split"><section class="panel"><div class="media-grid">${mediaNames.map((n,i)=>`<article class="media-card"><div class="media-photo"><span class="media-check"></span><span class="media-score">AI ${[98,96,93,97,91,95,89,94,92][i]}分</span></div><div class="media-info"><b>${n}</b><p>${i%3===0?'入口指引':'建筑外观'} · ${i+2} 天前更新</p></div></article>`).join('')}</div><div class="pagination"><span class="metric-sub" style="margin-right:auto">已显示 9 / 1,286</span><button>‹</button><button class="active">1</button><button>2</button><button>3</button><button>›</button></div></section><aside class="grid"><article class="panel"><div class="panel-head"><div><h3>AI图片质检</h3><p>自动检查清晰度、方向与地点一致性</p></div><span class="tag green">模型已就绪</span></div><div class="panel-pad property-list"><div class="property"><label>待处理</label><strong>23 张</strong></div><div class="property"><label>低清晰度</label><span class="tag orange">6 张</span></div><div class="property"><label>方向错误</label><span>3 张</span></div><div class="property"><label>疑似重复</label><span>8 张</span></div></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>✦ 采集建议</b><p>图书馆东侧入口在夜间和雨天场景下缺少对照图片，可能影响拍照识别效果。</p></div></article></aside></div></div>`;
}

function analyticsPage() {
  const bars = [42,55,48,68,61,78,73,88,82,95,74,67];
  return `<div class="page-enter"><div class="page-tools"><div class="page-tools-left"><div class="segmented"><button>今日</button><button class="active">近7天</button><button>近30天</button></div><select class="select-input"><option>全部新生</option><option>2026级本科生</option></select></div><div class="page-tools-right"><button class="btn" data-action="export-report">导出分析报告</button><button class="btn primary" data-action="ai-report">AI生成周报</button></div></div><div class="grid cols-4 stat-row">${statCard('book','8,642','AI问询量','#3478f5','#ebf3ff','+18.6%')}${statCard('route','3,286','导航规划次数','#18b68a','#e8faf5','+12.4%')}${statCard('task','81.6%','任务完成率','#7057ee','#f0edff','+6.2%')}${statCard('dashboard','93.8%','AI有效回答率','#ef7d3f','#fff1e8','+2.1%')}</div><div class="grid cols-2"><article class="panel panel-pad"><div class="panel-title"><span class="title-bar"></span><h3>AI咨询与导航趋势</h3></div><div class="bar-chart">${bars.map((v,i)=>`<div class="bar-col" style="--h:${v}%"><span>${10+i}:00</span></div>`).join('')}</div></article><article class="panel panel-pad"><div class="panel-title"><span class="title-bar"></span><h3>新生问题分类</h3></div><div class="donut-row"><div class="donut"><strong>8,642</strong></div><div class="legend-list"><span><i style="background:#367af4"></i>办事任务 38%</span><span><i style="background:#6b58ec"></i>地点导航 24%</span><span><i style="background:#18c79a"></i>校园生活 20%</span><span><i style="background:#ff9b55"></i>其他问题 18%</span></div></div></article></div><div class="grid cols-3" style="margin-top:12px"><article class="panel panel-pad"><div class="panel-title"><span class="title-bar"></span><h3>热门地点</h3></div><div class="progress-list" style="margin-top:18px"><div class="progress-item"><b>1 图书馆</b><div class="progress-track"><i style="width:92%;--bar:#3479f4"></i></div><span class="progress-num">1,682</span></div><div class="progress-item"><b>2 校园卡中心</b><div class="progress-track"><i style="width:78%;--bar:#18c79a"></i></div><span class="progress-num">1,426</span></div><div class="progress-item"><b>3 学生公寓</b><div class="progress-track"><i style="width:64%;--bar:#7655ec"></i></div><span class="progress-num">1,108</span></div><div class="progress-item"><b>4 学院报到点</b><div class="progress-track"><i style="width:53%;--bar:#ff8b45"></i></div><span class="progress-num">926</span></div></div></article><article class="panel panel-pad"><div class="panel-title"><span class="title-bar"></span><h3>任务完成率</h3></div><div class="health" style="margin-top:22px"><div class="health-ring" style="background:conic-gradient(#3479f4 0 82%,#e7edf4 82%)"><strong>82%</strong></div><div class="health-info"><span><b>392</b> 人已完成</span><span class="metric-sub">480 人已开始报到</span><span class="tag green">+较上周 6.2%</span></div></div></article><article class="panel panel-pad"><div class="panel-title" style="margin-bottom:14px"><span class="title-bar"></span><h3>AI运营洞察</h3></div><div class="insight-list"><div class="insight"><span class="insight-num">1</span><div><b>校园卡问询显著上升</b><p>近 24 小时上升 36%，建议将领取入口提至首页。</p></div></div><div class="insight"><span class="insight-num">2</span><div><b>东门导航误差较高</b><p>17 位用户在同一路口重新规划，建议补充实景指引。</p></div></div></div></article></div></div>`;
}

function settingsPage() {
  return `<div class="page-enter"><section class="panel settings-layout"><aside class="settings-tabs"><button class="active">AI模型与智能体</button><button>知识库参数</button><button>数字孪生配置</button><button>导航引擎</button><button>学生端配置</button><button>用户与角色</button><button>日志与安全</button></aside><div class="settings-content"><div class="setting-section"><h3>AI模型配置</h3><p>配置校园智能体使用的语言与视觉模型</p><div class="setting-row"><label><b>主语言模型</b><small>用于任务理解、工具调用和回答生成</small></label><div class="setting-control"><select class="select-input" style="width:280px"><option>校园智能体模型 v1</option><option>备用通用模型</option></select><span class="tag green">连接正常</span></div></div><div class="setting-row"><label><b>多模态图像识别</b><small>用于建筑照片识别与实景质检</small></label><div class="setting-control"><button class="switch on" data-action="toggle"></button><span class="metric-sub">已开启</span></div></div><div class="setting-row"><label><b>工具调用</b><small>允许 AI 调用地点、任务与导航服务</small></label><div class="setting-control"><button class="switch on" data-action="toggle"></button><span class="metric-sub">已开启</span></div></div></div><div class="setting-section"><h3>可信回答</h3><p>约束 AI 回答的来源、置信度和审核机制</p><div class="setting-row"><label><b>显示信息来源</b><small>学生端回答附带通知或手册来源</small></label><div class="setting-control"><button class="switch on" data-action="toggle"></button><span class="metric-sub">已开启</span></div></div><div class="setting-row"><label><b>最低置信度</b><small>低于阈值的回答转人工确认</small></label><div class="range-wrap"><input type="range" min="50" max="100" value="82" id="confidenceRange"><output id="confidenceOutput">82%</output></div></div><div class="setting-row"><label><b>未知信息拒答</b><small>知识库无证据时禁止模型自行编造</small></label><div class="setting-control"><button class="switch on" data-action="toggle"></button><span class="metric-sub">已开启</span></div></div></div><div class="setting-section"><h3>对话与任务策略</h3><p>配置智能体的记忆和主动提醒能力</p><div class="setting-row"><label><b>上下文记忆轮数</b><small>保留当前会话中最近对话信息</small></label><div class="setting-control"><select class="select-input" style="width:160px"><option>10 轮</option><option>20 轮</option><option>30 轮</option></select></div></div></div><div style="display:flex;justify-content:flex-end;gap:9px"><button class="btn">恢复默认</button><button class="btn primary" data-action="save-settings">保存配置</button></div></div></section></div>`;
}

const pages = { overview: overviewPage, twin: twinPage, places: placesPage, network: networkPage, tasks: tasksPage, knowledge: knowledgePage, media: mediaPage, analytics: analyticsPage, settings: settingsPage };

let currentPage = localStorage.getItem('campus-admin-page') || 'overview';
const navRoot = document.querySelector('#mainNav');
const pageRoot = document.querySelector('#pageRoot');

function renderNav() {
  navRoot.innerHTML = navItems.map(n => `<button class="nav-item ${n.id===currentPage?'active':''}" data-page="${n.id}"><span class="nav-icon">${icon(n.icon)}</span><span>${n.label}</span></button>`).join('');
}

function renderPage() {
  const meta = navItems.find(n => n.id === currentPage) || navItems[0];
  document.querySelector('#pageTitle').textContent = meta.title;
  document.querySelector('#pageSubtitle').textContent = meta.subtitle;
  pageRoot.innerHTML = pages[currentPage]();
  bindPageEvents();
}

function navigate(page) {
  if (!pages[page]) return;
  currentPage = page;
  localStorage.setItem('campus-admin-page', page);
  location.hash = page;
  renderNav();
  renderPage();
}

function toast(message, type='success') {
  const el = document.createElement('div');
  el.className = 'toast';
  el.innerHTML = `<i>${type==='success'?'✓':'!'}</i><span>${message}</span>`;
  document.querySelector('#toastStack').appendChild(el);
  setTimeout(() => el.remove(), 3000);
}

function openModal(title, body, confirm='确认') {
  const root = document.querySelector('#modalRoot');
  root.innerHTML = `<div class="modal-backdrop"><div class="modal"><div class="modal-head"><h3>${title}</h3><button class="modal-close">×</button></div><div class="modal-body">${body}</div><div class="modal-foot"><button class="btn modal-close">取消</button><button class="btn primary modal-confirm">${confirm}</button></div></div></div>`;
  root.querySelectorAll('.modal-close').forEach(b=>b.addEventListener('click',()=>root.innerHTML=''));
  root.querySelector('.modal-confirm').addEventListener('click',()=>{root.innerHTML='';toast(`${title}已保存`);});
}

function bindPageEvents() {
  document.querySelectorAll('.segmented button, .map-toolbar button, .glass-control button').forEach(btn => btn.addEventListener('click', () => {
    btn.parentElement.querySelectorAll('button').forEach(x=>x.classList.remove('active'));
    btn.classList.add('active');
  }));
  document.querySelectorAll('[data-action]').forEach(el=>el.addEventListener('click', () => handleAction(el.dataset.action, el)));
  const filter = document.querySelector('#placeFilter');
  if (filter) filter.addEventListener('input', e => document.querySelectorAll('#placeTable tr').forEach(row => row.style.display = row.dataset.search.toLowerCase().includes(e.target.value.trim().toLowerCase()) ? '' : 'none'));
  const range = document.querySelector('#confidenceRange');
  if (range) range.addEventListener('input',()=>document.querySelector('#confidenceOutput').value = `${range.value}%`);
}

function handleAction(action, sourceElement) {
  const messages = {
    locate:'已定位到校园中心', 'zoom-in':'场景已放大', 'zoom-out':'场景已缩小', layers:'图层面板已开启',
    'apply-plan':'绕行方案已应用，学生端将同步更新', 'scene-check':'AI 场景检查已启动', 'semantic-mark':'已进入语义标注模式',
    'ai-tag':'AI 正在为 7 个地点补全语义标签', 'network-check':'路网质检完成，发现 3 项建议', 'route-test':'路径仿真完成，绕行方案可行',
    'sync-notice':'已根据最新通知生成任务变更草稿', 'task-preview':'学生端预览已生成', 'save-task':'任务流程已保存并发布',
    'review-knowledge':'已进入知识审核队列', 'ai-media-check':'AI 已开始批量检查 23 张素材', 'export-report':'分析报告已生成', 'ai-report':'AI 运营周报已生成', 'save-settings':'系统配置已保存'
  };
  if (action === 'toggle') {
    const target = sourceElement; target.classList.toggle('on');
    target.nextElementSibling.textContent = target.classList.contains('on') ? '已开启' : '已关闭'; return;
  }
  if (action === 'add-place' || action === 'edit-place') return openModal(action==='add-place'?'新增校园地点':'编辑校园地点', `<div class="form-grid"><div class="form-field"><label>地点名称</label><input value="${action==='edit-place'?'图书馆':''}" placeholder="请输入名称"></div><div class="form-field"><label>地点类型</label><select><option>公共建筑</option><option>新生服务</option></select></div><div class="form-field"><label>开放时间</label><input value="08:00—22:00"></div><div class="form-field"><label>推荐入口</label><input placeholder="例：南门主入口"></div><div class="form-field full"><label>语义标签</label><input placeholder="例：自习、借阅、报到"></div></div>`);
  if (['upload-model','upload-doc','upload-media'].includes(action)) return openModal(action==='upload-model'?'上传数字孪生模型':action==='upload-doc'?'上传知识文档':'上传实景素材', `<div class="form-field full"><label>选择文件</label><div style="height:140px;display:grid;place-items:center;border:1px dashed #9bb8e8;border-radius:10px;background:#f7faff;color:#71819b">点击或拖拽文件到此处<br><small>${action==='upload-model'?'支持 glTF / GLB / 3D Tiles':'支持批量上传与自动解析'}</small></div></div>`, '开始上传');
  if (['add-road','add-task'].includes(action)) return openModal(action==='add-road'?'新增导航路段':'新建任务模板', `<div class="form-grid"><div class="form-field"><label>名称</label><input placeholder="请输入名称"></div><div class="form-field"><label>类型</label><select><option>步行道路</option><option>新生任务</option></select></div><div class="form-field full"><label>说明</label><textarea placeholder="填写基本说明"></textarea></div></div>`);
  toast(messages[action] || '功能已响应');
}

navRoot.addEventListener('click', e => { const btn = e.target.closest('[data-page]'); if (btn) navigate(btn.dataset.page); });
document.querySelector('#globalSearch').addEventListener('keydown', e => { if (e.key === 'Enter' && e.target.value.trim()) toast(`正在全局搜索“${e.target.value.trim()}”`); });
document.addEventListener('keydown', e => { if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase()==='k') { e.preventDefault(); document.querySelector('#globalSearch').focus(); } });
window.addEventListener('hashchange',()=>{ const p=location.hash.slice(1); if (pages[p] && p!==currentPage) navigate(p); });
if (location.hash.slice(1) && pages[location.hash.slice(1)]) currentPage = location.hash.slice(1);
renderNav(); renderPage();
