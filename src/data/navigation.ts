import type { NavItem } from '@/types'

export const navigation: NavItem[] = [
  { id: 'overview', path: '/', label: '运行总览', icon: '▦', title: '校园空间智能管理中心', subtitle: '实时掌握空间、路网与新生任务运行状态' },
  { id: 'twin', path: '/twin', label: '数字孪生', icon: '◇', title: '数字孪生场景中心', subtitle: '管理校园模型、空间对象与语义标注' },
  { id: 'places', path: '/places', label: '校园地点', icon: '⌖', title: '校园地点管理', subtitle: '维护建筑、办事点、入口与导航语义信息' },
  { id: 'network', path: '/network', label: '导航路网', icon: '⌘', title: '校园导航路网', subtitle: '可视化编辑节点、道路与动态通行状态' },
  { id: 'tasks', path: '/tasks', label: '新生任务', icon: '☑', title: '新生任务编排', subtitle: '配置报到、校园卡、寝室入住等任务流程' },
  { id: 'knowledge', path: '/knowledge', label: 'AI知识库', icon: '◫', title: 'AI 校园知识库', subtitle: '建设可追溯、可审核的校园办事与地点知识' },
  { id: 'media', path: '/media', label: '实景素材', icon: '▣', title: '校园实景素材', subtitle: '管理建筑外观、入口指引与地标识别素材' },
  { id: 'analytics', path: '/analytics', label: '运营分析', icon: '◩', title: '智能运营分析', subtitle: '分析新生问询、导航行为与任务完成情况' },
  { id: 'settings', path: '/settings', label: '系统设置', icon: '⚙', title: '系统设置', subtitle: '配置 AI 模型、数字孪生、权限与系统参数' },
]
