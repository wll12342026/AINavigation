import { ref } from 'vue'

export const workspaceToken = ref(sessionStorage.getItem('campus-workspace-token') || '')
export const workspaceRequiresLogin = ref(true)
export const workspaceAccessReady = ref(false)
let accessRequest: Promise<void> | undefined
export async function loadWorkspaceAccess() {
  if (workspaceAccessReady.value) return
  if (!accessRequest) accessRequest = (async () => {
    const response = await fetch('/api/workspace-access')
    if (!response.ok) throw new Error('无法读取工作台访问模式，请检查后端服务')
    const data = await response.json()
    workspaceRequiresLogin.value = data.requiresLogin !== false
    workspaceAccessReady.value = true
  })().finally(() => { accessRequest = undefined })
  await accessRequest
}
export async function workspaceLogin(username: string, password: string) {
  const response = await fetch('/api/auth/login', { method: 'POST', headers: {'Content-Type':'application/json'}, body:JSON.stringify({username,password}) })
  const data = await response.json()
  if (!response.ok) throw new Error(data.message || '登录失败')
  workspaceToken.value = data.token; sessionStorage.setItem('campus-workspace-token', data.token)
}
export function workspaceLogout() { workspaceToken.value=''; sessionStorage.removeItem('campus-workspace-token') }
async function request<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  const multipart = body instanceof FormData
  const response = await fetch(`/api/workspace${path}`, {method, headers: {Authorization:`Bearer ${workspaceToken.value}`, ...(!multipart?{'Content-Type':'application/json'}:{})}, body: body == null ? undefined : multipart ? body : JSON.stringify(body)})
  const text = await response.text(); const data = text ? JSON.parse(text) : undefined
  if (response.status===401) workspaceLogout()
  if (!response.ok) throw new Error(data?.message || `请求失败 ${response.status}`)
  return data as T
}
export interface KnowledgeBase {id:number;name:string;description:string;campus:string;audience:string;chunkSize:number;overlap:number;chunkMode:string}
export interface KnowledgeDocument {id:number;baseId:number;rootId?:number;legacyId?:number;versionNumber:number;title:string;source?:string;originalName?:string;format:string;fileSize:number;content?:string;processing:string;status:string;error?:string;warning?:string;effectiveAt?:string;expiresAt?:string;updatedAt?:string;attempts:number}
export interface KnowledgeChunk {id:number;documentId:number;parentId?:number;position:number;page?:number;section:string;kind:string;content:string;enabled:boolean;indexType:string}
export interface StandardFaq {id:number;baseId:number;question:string;aliases?:string;answer:string;category?:string;source?:string;campus?:string;audience?:string;conditions?:string;answerMode:string;status:string;effectiveAt?:string|null;expiresAt?:string|null;updatedAt?:string}
export interface ModelConnection {id:number;name:string;kind:string;baseUrl:string;modelId:string;apiKeyEnv?:string;keyConfigured:boolean;testStatus:string;testMessage?:string}
export interface CampusAgent {id:number;name:string;baseId:number;modelId?:number|null;systemPrompt:string;publishedRevisionId?:number}
export interface AgentRelease {id:number;agentId:number;baseId:number;systemPrompt:string;releaseNote?:string;createdAt?:string;providerModelId?:string}
export interface Evidence {id:string;kind:string;recordId:number;documentId?:number;version?:number;title:string;content:string;source?:string;page?:number;section?:string;score:number}
export interface SearchResult {query:string;mode:string;vectorAvailable:boolean;directFaqId?:number;directAnswer?:string;message:string;hits:Evidence[]}
export interface AgentAnswer {runId:number;answer:string;mode:string;status:string;elapsedMs:number;inputTokens?:number;outputTokens?:number;evidence:Evidence[];route?:{origin:string;destination:string;distanceMeters:number;estimatedMinutes:number}}
export interface WorkspaceRun {id:number;agentId:number;mode:string;status:string;traffic:string;question:string;answer:string;sourceIds:string;elapsedMs:number;inputTokens?:number;outputTokens?:number;createdAt:string}
export interface ImportPreview {rows:Omit<StandardFaq,'id'|'baseId'>[];errors:string[]}
export const workspaceApi = {
  bases:()=>request<KnowledgeBase[]>('/bases'),
  saveBase:(body:unknown,id?:number)=>request<KnowledgeBase>(id?`/bases/${id}`:'/bases',id?'PUT':'POST',body),
  documents:(id:number)=>request<KnowledgeDocument[]>(`/bases/${id}/documents`),
  text:(id:number,body:unknown)=>request<KnowledgeDocument>(`/bases/${id}/text`,'POST',body),
  upload:(id:number,file:File,title:string,source:string,previousId?:number)=>{const data=new FormData();data.append('file',file);data.append('title',title);data.append('source',source);if(previousId)data.append('previousId',String(previousId));return request<KnowledgeDocument>(`/bases/${id}/upload`,'POST',data)},
  migrate:(id:number)=>request<{imported:number}>(`/bases/${id}/migrate-legacy`,'POST'),
  action:(id:number,action:string)=>request<void>(`/documents/${id}/action`,'POST',{action}),
  chunks:(id:number)=>request<KnowledgeChunk[]>(`/documents/${id}/chunks`),
  preview:(id:number,size:number,overlap:number)=>request<{content:string;page?:number;section:string}[]>(`/documents/${id}/preview`,'POST',{size,overlap}),
  split:(id:number,size:number,overlap:number)=>request<KnowledgeChunk[]>(`/documents/${id}/split`,'POST',{size,overlap}),
  editChunk:(id:number,content:string,enabled:boolean)=>request<KnowledgeChunk>(`/chunks/${id}`,'PUT',{content,enabled}),
  faqs:(id:number)=>request<StandardFaq[]>(`/bases/${id}/faqs`),
  saveFaq:(id:number,body:unknown,faqId?:number)=>request<StandardFaq>(faqId?`/bases/${id}/faqs/${faqId}`:`/bases/${id}/faqs`,faqId?'PUT':'POST',body),
  importPreview:(id:number,file:File)=>{const data=new FormData();data.append('file',file);return request<ImportPreview>(`/bases/${id}/faqs/import-preview`,'POST',data)},
  importFaqs:(id:number,rows:unknown[])=>request<{imported:number}>(`/bases/${id}/faqs/import`,'POST',rows),
  exportFaqs:async(id:number)=>{const response=await fetch(`/api/workspace/bases/${id}/faqs/export`,{headers:{Authorization:`Bearer ${workspaceToken.value}`}});if(!response.ok)throw new Error('导出失败');const url=URL.createObjectURL(await response.blob());const link=document.createElement('a');link.href=url;link.download='标准问答.csv';link.click();window.setTimeout(()=>URL.revokeObjectURL(url),1000)},
  search:(id:number,query:string,campus?:string,audience?:string)=>request<SearchResult>(`/bases/${id}/search`,'POST',{query,campus,audience,topK:6}),
  models:()=>request<ModelConnection[]>('/models'),
  saveModel:(body:unknown,id?:number)=>request<ModelConnection>(id?`/models/${id}`:'/models',id?'PUT':'POST',body),
  testModel:(id:number)=>request<ModelConnection>(`/models/${id}/test`,'POST'),
  agents:()=>request<CampusAgent[]>('/agents'),
  saveAgent:(body:unknown,id?:number)=>request<CampusAgent>(id?`/agents/${id}`:'/agents',id?'PUT':'POST',body),
  releases:(id:number)=>request<AgentRelease[]>(`/agents/${id}/releases`),
  publish:(id:number,note:string)=>request<AgentRelease>(`/agents/${id}/publish`,'POST',{note}),
  rollback:(id:number,releaseId:number)=>request<CampusAgent>(`/agents/${id}/rollback/${releaseId}`,'POST'),
  chat:(id:number,query:string,published=false,campus?:string,audience?:string)=>request<AgentAnswer>(`/agents/${id}/chat`,'POST',{query,published,campus,audience}),
  runs:()=>request<WorkspaceRun[]>('/runs'),
}
