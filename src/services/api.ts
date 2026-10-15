export interface TaskProgress {
  name: string
  progress: number
  completed: number
  total: number
}

export interface OperationEvent {
  title: string
  detail: string
  time: string
}

export interface DashboardOverview {
  placeCount: number
  roadKilometers: number
  openRoadRate: number
  pendingKnowledge: number
  knowledgeHealth: number
  expiringKnowledge: number
  tasks: TaskProgress[]
  events: OperationEvent[]
}

export interface AiAnalysis {
  intent: string
  title: string
  summary: string
  confidence: number
  reasoningSteps: string[]
  affectedObjects: string[]
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  })
  if (!response.ok) throw new Error(`接口请求失败：${response.status}`)
  return response.json() as Promise<T>
}

export function getDashboardOverview() {
  return request<DashboardOverview>('/api/dashboard/overview')
}

export function analyzeOperation(prompt: string) {
  return request<AiAnalysis>('/api/ai/analyze', {
    method: 'POST',
    body: JSON.stringify({ prompt }),
  })
}
