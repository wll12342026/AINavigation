import { NativeModules, Platform } from 'react-native'

export interface AiAnalysis {
  intent: string
  title: string
  summary: string
  confidence: number
  reasoningSteps: string[]
  affectedObjects: string[]
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
}

function resolveDevHost() {
  const scriptUrl = NativeModules.SourceCode?.scriptURL as string | undefined
  const match = scriptUrl?.match(/^https?:\/\/([^/:]+)/)
  if (match?.[1]) return match[1]
  return Platform.OS === 'android' ? '10.0.2.2' : '127.0.0.1'
}

export const apiBaseUrl = `http://${resolveDevHost()}:8088/api`

export async function analyzeOperation(prompt: string): Promise<AiAnalysis> {
  const response = await fetch(`${apiBaseUrl}/ai/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt }),
  })
  if (!response.ok) throw new Error(`AI 接口请求失败：${response.status}`)
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
