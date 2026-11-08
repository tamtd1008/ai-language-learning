export type ApiEnvelope<T> = { success: boolean; message?: string; data?: T; timestamp?: string }

export type User = {
  id: number
  username: string
  email: string
  firstName?: string
  lastName?: string
  role: string
}

export type Topic = { id: number; name: string; description?: string; level?: string; active: boolean }
export type AiConfig = { id: number; name: string; provider: string; model: string; systemPrompt?: string; temperature: number; maxTokens: number; active: boolean }
export type Message = { id: number; role: 'USER' | 'ASSISTANT'; content: string; createdAt: string }
export type Session = { id: number; status: 'ACTIVE' | 'ENDED'; topicId?: number; topicName?: string; startedAt: string; aiConfigName?: string }
export type Summary = {
  totalSessions: number
  totalMessagesSent: number
  averageOverallScore: number | null
  currentLevel: string | null
  vocabularyLearnedCount: number
  vocabularyLearningCount: number
  scoreHistory: { sessionId: number; overallScore: number; startedAt: string }[]
}
export type VocabularyProgress = { vocabularyId: number; word: string; meaning: string; status: 'LEARNING' | 'LEARNED' }

const tokenKey = 'lingua-token'
export const getToken = () => sessionStorage.getItem(tokenKey)
export const setToken = (token: string) => sessionStorage.setItem(tokenKey, token)
export const clearToken = () => sessionStorage.removeItem(tokenKey)

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  let response: Response
  try {
    response = await fetch(path, { ...options, headers })
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error('Không kết nối được API. Hãy kiểm tra backend ở cổng 8080 và cấu hình VITE_BACKEND_URL, sau đó tải lại trang.')
    }
    throw error
  }
  if (!response.ok) {
    let message = `Yêu cầu thất bại (${response.status})`
    try {
      const body = await response.json() as ApiEnvelope<unknown>
      message = body.message || message
    } catch { /* response may not contain JSON */ }
    if (response.status === 401) {
      clearToken()
      window.dispatchEvent(new Event('lingua:unauthorized'))
    }
    throw new Error(message)
  }
  if (response.status === 204) return undefined as T
  const type = response.headers.get('content-type') || ''
  if (type.includes('application/json')) {
    const body = await response.json() as ApiEnvelope<T>
    return body.data as T
  }
  return await response.blob() as T
}

