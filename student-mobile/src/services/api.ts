import { NativeModules, Platform } from 'react-native'

export interface AiAnalysis {
  intent: string
  title: string
  summary: string
  confidence: number
  reasoningSteps: string[]
  affectedObjects: string[]
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
