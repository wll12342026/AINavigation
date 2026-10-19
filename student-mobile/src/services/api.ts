import { NativeModules, Platform } from 'react-native'

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

export interface StudentTask {
  id: number
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
  tasks: { name: string; progress: number; completed: number; total: number }[]
  recentKnowledge: { title: string; updatedAt: string; status: string }[]
}

export interface Place {
  id: number
  name: string
  category: string
  building: string
  floor: string
  status: string
  entranceHint: string
  description: string
  aliases?: string
  openingHours?: string
}

export interface RoadSegment {
  id: number
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

export interface MediaAsset {
  id: number
  title: string
  placeId: number | null
  placeName: string
  type: string
  url: string
  qualityScore: number | null
  reviewStatus: string
  inspectionEvidence?: string
}

export interface RecognitionResult {
  sufficientEvidence: boolean
  message: string
  method: string
  candidates: { placeId: number; placeName: string; referenceUrl: string; confidence: number; evidence: string }[]
}

function resolveDevHost() {
  const scriptUrl = NativeModules.SourceCode?.scriptURL as string | undefined
  const match = scriptUrl?.match(/^https?:\/\/([^/:]+)/)
  if (match?.[1]) return match[1]
  return Platform.OS === 'android' ? '10.0.2.2' : '127.0.0.1'
}

export const apiBaseUrl = `http://${resolveDevHost()}:8088/api`
export const resolveMediaUrl = (value?: string) => value?.startsWith('/') ? `http://${resolveDevHost()}:8088${value}` : value ?? ''

export async function analyzeOperation(prompt: string): Promise<AiAnalysis> {
  const response = await fetch(`${apiBaseUrl}/ai/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt }),
  })
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.message || `AI 接口请求失败：${response.status}`)
  }
  return response.json() as Promise<AiAnalysis>
}

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`)
  if (!response.ok) throw new Error(`接口请求失败：${response.status}`)
  return response.json() as Promise<T>
}

export function getTasks() {
  return getJson<StudentTask[]>('/tasks')
}

export function getDashboardOverview() {
  return getJson<DashboardOverview>('/dashboard/overview')
}

export function getPlaces() {
  return getJson<Place[]>('/places')
}

export function getRoads() {
  return getJson<RoadSegment[]>('/roads')
}

export function getMedia() {
  return getJson<MediaAsset[]>('/media')
}

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.message || `接口请求失败：${response.status}`)
  }
  return response.json() as Promise<T>
}

export function planRoute(origin: string, destination: string, accessibleOnly = false, avoidRestricted = true, optimization = 'DISTANCE') {
  return postJson<RoutePlan>('/navigation/plan', { origin, destination, accessibleOnly, avoidRestricted, optimization })
}

export function planAiRoute(prompt: string, origin?: string) {
  return postJson<RoutePlan>('/navigation/ai-plan', { prompt, origin })
}

async function postImage<T>(path: string, image: { uri: string; fileName?: string | null; mimeType?: string | null }, fields: Record<string, string> = {}) {
  const form = new FormData()
  form.append('file', { uri: image.uri, name: image.fileName || 'campus-photo.jpg', type: image.mimeType || 'image/jpeg' } as unknown as Blob)
  Object.entries(fields).forEach(([key, value]) => form.append(key, value))
  const response = await fetch(`${apiBaseUrl}${path}`, { method: 'POST', body: form })
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.message || `上传失败：${response.status}`)
  }
  return response.json() as Promise<T>
}

export function uploadCampusPhoto(image: { uri: string; fileName?: string | null; mimeType?: string | null }, title: string, placeId?: number) {
  const fields: Record<string, string> = { title, type: 'PHOTO' }
  if (placeId != null) fields.placeId = String(placeId)
  return postImage<MediaAsset>('/media/upload', image, fields)
}

export function recognizeCampusPhoto(image: { uri: string; fileName?: string | null; mimeType?: string | null }) {
  return postImage<RecognitionResult>('/media/recognize', image)
}
