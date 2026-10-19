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
  buildingCount: number
  servicePointCount: number
  roadKilometers: number
  roadNodeCount: number
  roadCount: number
  openRoadRate: number
  pendingKnowledge: number
  knowledgeHealth: number
  expiringKnowledge: number
  excludedDemoRecords: number
  consultationsToday: number
  consultationTrend: { label: string; value: number }[]
  tasks: TaskProgress[]
  events: OperationEvent[]
  recentKnowledge: { title: string; updatedAt: string; status: string }[]
}

export interface AiAnalysis {
  intent: string
  title: string
  summary: string
  confidence: number
  reasoningSteps: string[]
  affectedObjects: string[]
  mode?: string
  sources?: { id: number; title: string; content: string; source?: string; updatedAt?: string; expiresAt?: string }[]
  route?: RoutePlan
}

export interface RouteStep {
  roadId: number
  roadName: string
  fromNode: string
  toNode: string
  lengthMeters: number
  status: string
  accessible: boolean
  restrictionReason?: string
}

export interface RoutePlan {
  origin: string
  destination: string
  distanceMeters: number
  estimatedMinutes: number
  accessibleOnly: boolean
  avoidRestricted: boolean
  strategy: string
  confidence: number
  steps: RouteStep[]
  evidence: string[]
  blockedRoads: string[]
  optimization?: 'DISTANCE' | 'TIME'
}

export interface BaseRecord {
  id: number
  createdAt?: string
  updatedAt?: string
}

export interface Place extends BaseRecord {
  name: string
  category: string
  building: string
  floor: string
  status: string
  longitude: number | null
  latitude: number | null
  entranceHint: string
  description: string
  aliases?: string
  openingHours?: string
}

export interface RoadSegment extends BaseRecord {
  name: string
  fromNode: string
  toNode: string
  lengthMeters: number
  status: string
  restrictionReason: string | null
  accessible: boolean
  bidirectional?: boolean
  travelSeconds?: number | null
}

export interface StudentTask extends BaseRecord {
  name: string
  description: string
  location: string
  status: string
  progress: number
  completedCount: number
  totalCount: number
  estimatedMinutes: number
  sortOrder: number
}

export interface KnowledgeItem extends BaseRecord {
  title: string
  category: string
  content: string
  status: string
  source: string
  expiresAt: string | null
}

export interface MediaAsset extends BaseRecord {
  title: string
  placeId: number | null
  placeName: string
  type: string
  url: string
  qualityScore: number | null
  reviewStatus: string
  issueType: string | null
  fileName?: string
  fileSize?: number
  imageWidth?: number
  imageHeight?: number
  fileHash?: string
  visualFingerprint?: string
  brightnessScore?: number
  sharpnessScore?: number
  inspectionEvidence?: string
}

export interface RecognitionResult {
  sufficientEvidence: boolean
  message: string
  method: string
  candidates: { placeId: number; placeName: string; referenceUrl: string; confidence: number; evidence: string }[]
}

export interface MediaSummary {
  total: number
  boundPlaces: number
  passRate: number
  pending: number
  lowQuality: number
  wrongDirection: number
  duplicate: number
}

export interface AnalyticsOverview {
  aiQueries: number
  aiQueryChangeRate: number
  navigationPlans: number
  taskCompletionRate: number
  aiAnswerRate: number
  completedTasks: number
  totalTasks: number
  trend: { label: string; value: number }[]
  categories: { category: string; count: number }[]
  popularPlaces: { name: string; count: number }[]
  insights: string[]
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  })
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    const details = payload?.details ? Object.values(payload.details).join('；') : ''
    throw new Error(details || payload?.message || `接口请求失败：${response.status}`)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

async function multipart<T>(url: string, form: FormData): Promise<T> {
  const response = await fetch(url, { method: 'POST', body: form })
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.message || `接口请求失败：${response.status}`)
  }
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

export function planRoute(origin: string, destination: string, accessibleOnly = false, avoidRestricted = true, optimization = 'DISTANCE') {
  return request<RoutePlan>('/api/navigation/plan', {
    method: 'POST',
    body: JSON.stringify({ origin, destination, accessibleOnly, avoidRestricted, optimization }),
  })
}

export function planRouteFromPrompt(prompt: string, origin?: string) {
  return request<RoutePlan>('/api/navigation/ai-plan', {
    method: 'POST',
    body: JSON.stringify({ prompt, origin }),
  })
}

export function getMediaSummary() {
  return request<MediaSummary>('/api/media/summary')
}

export function getAnalyticsOverview(days = 7) {
  return request<AnalyticsOverview>(`/api/analytics/overview?days=${days}`)
}

export function getSettings() {
  return request<Record<string, string>>('/api/settings')
}

export function saveSettings(values: Record<string, string>) {
  return request<Record<string, string>>('/api/settings', { method: 'PUT', body: JSON.stringify(values) })
}

function crud<T extends BaseRecord>(path: string) {
  return {
    list: () => request<T[]>(path),
    create: (data: Omit<T, keyof BaseRecord>) => request<T>(path, { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: Omit<T, keyof BaseRecord>) => request<T>(`${path}/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    remove: (id: number) => request<void>(`${path}/${id}`, { method: 'DELETE' }),
  }
}

export const placesApi = crud<Place>('/api/places')
export const roadsApi = crud<RoadSegment>('/api/roads')
export const tasksApi = crud<StudentTask>('/api/tasks')
export const knowledgeApi = crud<KnowledgeItem>('/api/knowledge')
export const mediaApi = crud<MediaAsset>('/api/media')

export function uploadMedia(file: File, title: string, placeId: number | null, type = 'PHOTO') {
  const form = new FormData()
  form.append('file', file)
  form.append('title', title)
  form.append('type', type)
  if (placeId != null) form.append('placeId', String(placeId))
  return multipart<MediaAsset>('/api/media/upload', form)
}

export function recognizeLandmark(file: File) {
  const form = new FormData()
  form.append('file', file)
  return multipart<RecognitionResult>('/api/media/recognize', form)
}
