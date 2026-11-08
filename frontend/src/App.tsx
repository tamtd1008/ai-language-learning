import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import type { ChangeEvent, ReactNode } from 'react'
import {
  Activity, ArrowDownRight, ArrowRight, AudioLines, BookOpen, Bot, Check, ChevronDown,
  CircleHelp, Compass, GraduationCap, Headphones, House, LogOut, MessageCircle, Mic,
  Pause, Plus, Send, Settings2, ShieldCheck, Sparkles, Star, Volume2, X,
} from 'lucide-react'
import {
  clearToken, getToken, request, setToken,
  type AiConfig, type Message, type Session, type Summary, type Topic, type User, type VocabularyProgress,
} from './api'

type Page = 'home' | 'conversation' | 'practice' | 'progress' | 'vocabulary' | 'admin'
const levels = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Có lỗi xảy ra. Vui lòng thử lại.'
}

export default function App() {
  const [user, setUser] = useState<User | null>(null)
  const [page, setPage] = useState<Page>('home')
  const [summary, setSummary] = useState<Summary | null>(null)
  const [topics, setTopics] = useState<Topic[]>([])
  const [level, setLevel] = useState<string>('A2')
  const [notice, setNotice] = useState('')
  const [booting, setBooting] = useState(Boolean(getToken()))

  const refresh = useCallback(async () => {
    const [profile, stats, topicList, learnerLevel] = await Promise.allSettled([
      request<User>('/api/users/me'),
      request<Summary>('/api/progress/summary'),
      request<Topic[]>('/api/topics'),
      request<{ level: string | null }>('/api/learner-level/me'),
    ])
    if (profile.status === 'fulfilled') setUser(profile.value)
    if (stats.status === 'fulfilled') setSummary(stats.value)
    if (topicList.status === 'fulfilled') setTopics(topicList.value || [])
    if (learnerLevel.status === 'fulfilled' && learnerLevel.value.level) setLevel(learnerLevel.value.level)
  }, [])

  useEffect(() => {
    if (!getToken()) return
    refresh().finally(() => setBooting(false))
  }, [refresh])

  useEffect(() => {
    const onUnauthorized = () => { setUser(null); setNotice('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.') }
    window.addEventListener('lingua:unauthorized', onUnauthorized)
    return () => window.removeEventListener('lingua:unauthorized', onUnauthorized)
  }, [])

  const login = (nextUser: User) => { setUser(nextUser); setNotice(''); refresh() }
  const logout = async () => {
    try { await request('/api/auth/logout', { method: 'POST' }) } catch { /* local logout still clears the session */ }
    clearToken(); setUser(null); setPage('home')
  }

  if (booting) return <div className="boot-screen"><div className="brand-mark"><AudioLines size={22} /></div><span>Đang mở lớp học của bạn...</span></div>
  if (!user) return <LoginScreen onLogin={login} initialMessage={notice} />

  const isAdmin = user.role?.toUpperCase().includes('ADMIN')
  const displayName = user.firstName || user.username

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><div className="brand-mark"><AudioLines size={22} /></div><span>lingua<span className="brand-dot">.</span></span></div>
        <div className="workspace-label">KHÔNG GIAN HỌC</div>
        <nav className="nav-list" aria-label="Điều hướng chính">
          <NavButton icon={<House />} label="Tổng quan" active={page === 'home'} onClick={() => setPage('home')} />
          <NavButton icon={<MessageCircle />} label="Hội thoại AI" active={page === 'conversation'} onClick={() => setPage('conversation')} />
          <NavButton icon={<BookOpen />} label="Luyện tập" active={page === 'practice'} onClick={() => setPage('practice')} />
          <NavButton icon={<GraduationCap />} label="Từ vựng" active={page === 'vocabulary'} onClick={() => setPage('vocabulary')} />
          <NavButton icon={<Activity />} label="Tiến độ" active={page === 'progress'} onClick={() => setPage('progress')} />
        </nav>
        {isAdmin && <><div className="workspace-label nav-gap">QUẢN TRỊ</div><NavButton icon={<Settings2 />} label="Quản lý hệ thống" active={page === 'admin'} onClick={() => setPage('admin')} /></>}
        <div className="sidebar-spacer" />
        <div className="sidebar-tip"><div className="tip-icon"><Sparkles size={17} /></div><strong>Mỗi ngày một chút</strong><p>10 phút luyện tập đều đặn tạo nên khác biệt.</p><div className="tip-progress"><span /></div><small>Hôm nay · 0/10 phút</small></div>
        <button className="profile-button" onClick={logout}><div className="avatar">{displayName.slice(0, 1).toUpperCase()}</div><div className="profile-copy"><strong>{displayName}</strong><span>{isAdmin ? 'Quản trị viên' : `Trình độ ${summary?.currentLevel || level}`}</span></div><LogOut size={17} className="logout-icon" /></button>
      </aside>

      <main className="main-area">
        <header className="topbar"><div className="breadcrumb">Học tập <span>/</span> <b>{pageTitle(page)}</b></div><div className="topbar-right"><span className="online-status"><i /> Hệ thống sẵn sàng</span><button className="help-button" aria-label="Trợ giúp"><CircleHelp size={19} /></button><div className="mini-avatar">{displayName.slice(0, 1).toUpperCase()}</div></div></header>
        {notice && <div className="notice-bar">{notice}<button onClick={() => setNotice('')}><X size={16} /></button></div>}
        <div className="page-content">
          {page === 'home' && <Dashboard user={user} summary={summary} level={level} topics={topics} setPage={setPage} setLevel={setLevel} refresh={refresh} onError={setNotice} />}
          {page === 'conversation' && <Conversation topics={topics} level={level} onRefresh={refresh} onError={setNotice} />}
          {page === 'practice' && <Practice level={level} topics={topics} onError={setNotice} />}
          {page === 'vocabulary' && <Vocabulary onError={setNotice} />}
          {page === 'progress' && <Progress summary={summary} onRefresh={refresh} />}
          {page === 'admin' && isAdmin && <Admin topics={topics} onRefresh={refresh} onError={setNotice} />}
        </div>
      </main>
    </div>
  )
}

function pageTitle(page: Page) {
  return ({ home: 'Tổng quan', conversation: 'Hội thoại AI', practice: 'Luyện tập', progress: 'Tiến độ học', vocabulary: 'Từ vựng của tôi', admin: 'Quản lý hệ thống' })[page]
}

function NavButton({ icon, label, active, onClick }: { icon: ReactNode; label: string; active: boolean; onClick: () => void }) {
  return <button className={`nav-button ${active ? 'active' : ''}`} onClick={onClick}>{icon}<span>{label}</span>{active && <span className="nav-indicator" />}</button>
}

function LoginScreen({ onLogin, initialMessage }: { onLogin: (user: User) => void; initialMessage: string }) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(initialMessage)
  const [form, setForm] = useState({ username: '', password: '', email: '', firstName: '', lastName: '' })
  const update = (key: keyof typeof form) => (event: ChangeEvent<HTMLInputElement>) => setForm({ ...form, [key]: event.target.value })

  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('')
    let authenticated = false
    try {
      if (mode === 'register') {
        await request('/api/auth/register', { method: 'POST', body: JSON.stringify(form) })
      }
      const data = await request<{ token: string; username: string; role: string }>('/api/auth/login', { method: 'POST', body: JSON.stringify({ username: form.username, password: form.password }) })
      setToken(data.token)
      authenticated = true
      const profile = await request<User>('/api/users/me')
      onLogin({ ...profile, username: data.username, role: data.role })
    } catch (e) {
      if (authenticated) clearToken()
      setError(authenticated
        ? `Đăng nhập thành công nhưng không tải được hồ sơ: ${errorMessage(e)}`
        : errorMessage(e))
    } finally { setBusy(false) }
  }

  return <div className="login-layout"><div className="login-art"><div className="login-art-inner"><div className="brand brand-light"><div className="brand-mark"><AudioLines size={22} /></div><span>lingua<span className="brand-dot">.</span></span></div><div className="art-copy"><span className="eyebrow light-eyebrow"><Sparkles size={14} /> HỌC THEO CÁCH CỦA BẠN</span><h1>Ngôn ngữ mở ra<br /><em>những thế giới mới.</em></h1><p>Luyện nói tự nhiên, từng cuộc trò chuyện một.</p></div><div className="orbit-card"><div className="orbit-spark">✦</div><div><small>GỢI Ý HÔM NAY</small><strong>Tell me about a place<br />you want to visit.</strong></div><div className="sound-wave"><i /><i /><i /><i /><i /><i /><i /><i /><i /><i /></div></div><div className="art-footer">Học ngoại ngữ cùng AI · Một hành trình của riêng bạn</div></div></div><div className="login-side"><form className="login-form" onSubmit={submit}><div className="mobile-brand brand"><div className="brand-mark"><AudioLines size={22} /></div><span>lingua<span className="brand-dot">.</span></span></div><div className="eyebrow">BẮT ĐẦU HÀNH TRÌNH</div><h2>{mode === 'login' ? 'Chào mừng bạn trở lại' : 'Tạo tài khoản học tập'}</h2><p className="muted">{mode === 'login' ? 'Đăng nhập để tiếp tục luyện tập mỗi ngày.' : 'Chỉ mất một chút thời gian để bắt đầu.'}</p>
      {mode === 'register' && <div className="form-row"><label>Họ<input value={form.firstName} onChange={update('firstName')} placeholder="Nguyễn" /></label><label>Tên<input value={form.lastName} onChange={update('lastName')} placeholder="Minh" /></label></div>}
      {mode === 'register' && <label>Email<input type="email" value={form.email} onChange={update('email')} placeholder="ban@email.com" required /></label>}
      <label>Tên đăng nhập<input value={form.username} onChange={update('username')} placeholder="Tên đăng nhập" autoComplete="username" required /></label>
      <label>Mật khẩu<input type="password" value={form.password} onChange={update('password')} placeholder="••••••••" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required /></label>
      {error && <div className="form-error">{error}</div>}
      <button className="primary-button login-submit" disabled={busy}>{busy ? 'Đang xử lý...' : mode === 'login' ? 'Đăng nhập' : 'Tạo tài khoản'} <ArrowRight size={17} /></button>
      <div className="switch-auth">{mode === 'login' ? 'Chưa có tài khoản?' : 'Đã có tài khoản?'} <button type="button" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError('') }}>{mode === 'login' ? 'Đăng ký' : 'Đăng nhập'}</button></div>
      <div className="login-privacy"><ShieldCheck size={15} /> Dữ liệu học tập của bạn được bảo vệ</div>
    </form></div></div>
}

function Dashboard({ user, summary, level, topics, setPage, setLevel, refresh, onError }: {
  user: User; summary: Summary | null; level: string; topics: Topic[]; setPage: (page: Page) => void; setLevel: (level: string) => void; refresh: () => Promise<void>; onError: (message: string) => void
}) {
  const [savingLevel, setSavingLevel] = useState(false)
  const firstName = user.firstName || user.username
  const today = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date())
  async function saveLevel(value: string) {
    setLevel(value); setSavingLevel(true)
    try { await request('/api/learner-level/me', { method: 'PUT', body: JSON.stringify({ level: value }) }); await refresh() }
    catch (e) { onError(errorMessage(e)) } finally { setSavingLevel(false) }
  }
  return <>
    <div className="welcome-line"><span>{today}</span><span className="streak"><span>✦</span> Bắt đầu chuỗi học hôm nay</span></div>
    <section className="welcome-hero"><div className="hero-copy"><span className="eyebrow hero-eyebrow"><Sparkles size={14} /> KHÔNG GIAN HỌC CỦA BẠN</span><h1>Chào {firstName},<br />mình cùng <em>luyện nói nhé.</em></h1><p>Một cuộc trò chuyện ngắn hôm nay sẽ giúp bạn tự tin hơn ngày mai.</p><button className="hero-button" onClick={() => setPage('conversation')}>Bắt đầu hội thoại <ArrowRight size={17} /></button></div><div className="hero-illustration"><div className="hero-orb orb-one" /><div className="hero-orb orb-two" /><div className="hero-bubble bubble-a">Hello! <span>✦</span></div><div className="hero-bubble bubble-b"><AudioLines size={17} /> <div className="bubble-bars">{Array.from({ length: 17 }, (_, i) => <i key={i} style={{ height: `${9 + ((i * 13) % 23)}px` }} />)}</div></div><div className="hero-robot"><div className="robot-antenna" /><div className="robot-head"><span /><span /></div><div className="robot-body"><i /><i /><i /></div></div><div className="floating-star star-a">✦</div><div className="floating-star star-b">✳</div><div className="hero-label">AI conversation<br /><small>Practice at your pace</small></div></div></section>
    <div className="section-heading"><div><h2>Hành trình của bạn</h2><p>Mỗi bước nhỏ đều đáng ghi nhận.</p></div><button className="text-button" onClick={() => setPage('progress')}>Xem tiến độ <ArrowRight size={15} /></button></div>
    <section className="stats-grid"><StatCard label="Phiên học" value={summary?.totalSessions ?? '—'} suffix="phiên" icon={<MessageCircle />} tone="lavender" sub="Mọi cuộc hội thoại đều có ích" /><StatCard label="Tin nhắn đã gửi" value={summary?.totalMessagesSent ?? '—'} suffix="tin" icon={<Send />} tone="peach" sub="Bạn đang luyện tập rất tốt" /><StatCard label="Điểm trung bình" value={summary?.averageOverallScore == null ? '—' : Math.round(summary.averageOverallScore)} suffix={summary?.averageOverallScore == null ? '' : '/ 100'} icon={<Star />} tone="mint" sub="Sau các phiên được đánh giá" /><div className="level-card"><div className="level-card-top"><span className="stat-icon level-icon"><GraduationCap size={18} /></span><span className="level-tag">CEFR</span></div><div className="level-value">{summary?.currentLevel || level}</div><div className="level-label">Trình độ hiện tại</div><div className="level-select-wrap"><span>Cập nhật trình độ</span><select value={level} disabled={savingLevel} onChange={e => saveLevel(e.target.value)}>{levels.map(item => <option key={item}>{item}</option>)}</select><ChevronDown size={14} /></div></div></section>
    <section className="home-lower"><div className="quick-card"><div className="section-heading compact"><div><h2>Chọn cách luyện tập</h2><p>Bắt đầu từ điều bạn muốn hôm nay.</p></div></div><div className="quick-actions"><button onClick={() => setPage('conversation')} className="quick-action"><span className="quick-icon violet"><MessageCircle /></span><span><b>Trò chuyện với AI</b><small>Luyện phản xạ giao tiếp</small></span><ArrowRight /></button><button onClick={() => setPage('practice')} className="quick-action"><span className="quick-icon coral"><BookOpen /></span><span><b>Bài luyện tập</b><small>Ngữ pháp, nghe và nói</small></span><ArrowRight /></button></div></div><div className="topic-card"><div className="section-heading compact"><div><h2>Chủ đề gợi ý</h2><p>{topics.length ? 'Chọn một chủ đề để bắt đầu.' : 'Chủ đề sẽ xuất hiện ở đây.'}</p></div><Compass size={19} className="topic-compass" /></div><div className="topic-chips">{topics.slice(0, 4).map((topic, i) => <button key={topic.id} className={`topic-chip chip-${i % 4}`} onClick={() => setPage('conversation')}>{topic.name}<ArrowDownRight size={14} /></button>)}</div></div></section>
  </>
}

function StatCard({ label, value, suffix, icon, tone, sub }: { label: string; value: string | number; suffix: string; icon: ReactNode; tone: string; sub: string }) {
  return <div className="stat-card"><div className="stat-card-top"><span className={`stat-icon ${tone}`}>{icon}</span><span className="stat-dots">•••</span></div><div className="stat-label">{label}</div><div className="stat-value">{value}<small>{suffix}</small></div><div className="stat-sub">{sub}</div></div>
}

function Conversation({ topics, level, onRefresh, onError }: { topics: Topic[]; level: string; onRefresh: () => Promise<void>; onError: (message: string) => void }) {
  const [session, setSession] = useState<Session | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [selectedTopic, setSelectedTopic] = useState('')
  const [draft, setDraft] = useState('')
  const [busy, setBusy] = useState(false)
  const [aiWaitingLong, setAiWaitingLong] = useState(false)
  const [recording, setRecording] = useState(false)
  const [recordBusy, setRecordBusy] = useState(false)
  const [feedback, setFeedback] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)
  const recorderRef = useRef<{ recorder: MediaRecorder; stream: MediaStream; chunks: Blob[] } | null>(null)
  const availableTopics = useMemo(() => topics.filter(t => t.active), [topics])

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages])
  useEffect(() => () => recorderRef.current?.stream.getTracks().forEach(track => track.stop()), [])

  async function startSession() {
    setBusy(true); setFeedback('')
    try {
      const next = await request<Session>('/api/sessions', { method: 'POST', body: JSON.stringify(selectedTopic ? { topicId: Number(selectedTopic) } : {}) })
      setSession(next); setMessages([])
    } catch (e) { onError(errorMessage(e)) } finally { setBusy(false) }
  }
  async function loadMessages(id = session?.id) {
    if (!id) return
    try { setMessages(await request<Message[]>(`/api/sessions/${id}/messages`)) } catch (e) { onError(errorMessage(e)) }
  }
  async function sendText(event?: FormEvent) {
    event?.preventDefault()
    const content = draft.trim()
    if (!session || !content || busy) return
    setBusy(true); setDraft(''); setFeedback('')
    setAiWaitingLong(false)
    const slowReplyTimer = window.setTimeout(() => setAiWaitingLong(true), 6000)
    try {
      await request<Message>(`/api/sessions/${session.id}/messages`, { method: 'POST', body: JSON.stringify({ content }) })
      await loadMessages()
    } catch (e) { setDraft(content); onError(errorMessage(e)) } finally { window.clearTimeout(slowReplyTimer); setAiWaitingLong(false); setBusy(false) }
  }
  async function endSession() {
    if (!session) return
    setBusy(true)
    try {
      const ended = await request<Session>(`/api/sessions/${session.id}/end`, { method: 'PUT' })
      setSession(ended)
      const [review, points] = await Promise.allSettled([
        request<{ summary: string; strengths: string; areasToImprove: string; estimatedLevel: string }>(`/api/sessions/${session.id}/evaluate`, { method: 'POST' }),
        request<{ grammarScore: number; vocabularyScore: number; conversationScore: number; overallScore: number }>(`/api/sessions/${session.id}/score`, { method: 'POST' }),
      ])
      const reviewText = review.status === 'fulfilled'
        ? `${review.value.summary} Điểm mạnh: ${review.value.strengths} Cần cải thiện: ${review.value.areasToImprove} Trình độ ước tính: ${review.value.estimatedLevel}.`
        : ''
      const scoreText = points.status === 'fulfilled'
        ? `Điểm tổng: ${Math.round(points.value.overallScore)}/100 (ngữ pháp ${Math.round(points.value.grammarScore)}, từ vựng ${Math.round(points.value.vocabularyScore)}, hội thoại ${Math.round(points.value.conversationScore)}).`
        : ''
      setFeedback([reviewText, scoreText].filter(Boolean).join(' ')
        || 'Phiên đã kết thúc. Một phần đánh giá chưa sẵn sàng; bạn vẫn có thể xem phiên trong tiến độ.')
      await onRefresh()
    }
    catch (e) { onError(errorMessage(e)) } finally { setBusy(false) }
  }
  async function startRecording() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true } })
      const recorder = new MediaRecorder(stream)
      const chunks: Blob[] = []
      recorder.ondataavailable = event => { if (event.data.size) chunks.push(event.data) }
      recorderRef.current = { recorder, stream, chunks }
      recorder.start(); setRecording(true)
      recorder.onstop = async () => {
        setRecordBusy(true)
        try {
          const blob = new Blob(chunks, { type: recorder.mimeType || 'audio/webm' })
          const wav = await convertToWav(blob)
          const form = new FormData(); form.append('audio', wav, 'voice.wav')
          const result = await request<{ transcript: string; reply: string }>(`/api/sessions/${session!.id}/voice-messages`, { method: 'POST', body: form })
          setFeedback(`Bạn đã nói: “${result.transcript}”`)
          await loadMessages()
        } catch (e) { onError(errorMessage(e)) }
        finally { setRecordBusy(false); stream.getTracks().forEach(track => track.stop()) }
      }
    } catch { onError('Không mở được microphone. Hãy cho phép trình duyệt sử dụng micro và thử lại.') }
  }
  function stopRecording() { recorderRef.current?.recorder.stop(); setRecording(false) }
  async function playAudio(messageId: number) {
    try {
      const blob = await request<Blob>(`/api/sessions/${session!.id}/messages/${messageId}/audio`)
      const url = URL.createObjectURL(blob); const audio = new Audio(url)
      audio.onended = () => URL.revokeObjectURL(url); await audio.play()
    } catch (e) { onError(errorMessage(e)) }
  }

  return <div className="conversation-page"><div className="page-heading"><div><span className="eyebrow"><MessageCircle size={14} /> LUYỆN GIAO TIẾP</span><h1>Hội thoại cùng AI</h1><p>Trò chuyện tự nhiên, luyện nghe và nói theo trình độ của bạn.</p></div>{session && <span className={`session-pill ${session.status === 'ACTIVE' ? 'live' : ''}`}><i /> {session.status === 'ACTIVE' ? 'Đang diễn ra' : 'Đã kết thúc'}</span>}</div>
    {!session ? <div className="start-conversation-card"><div className="start-art"><div className="start-art-orb" /><div className="mini-bot"><div className="mini-bot-face"><i /><i /></div><div className="mini-bot-mouth" /></div><div className="mini-chat chat-left">Hi there! <Sparkles size={13} /></div><div className="mini-chat chat-right"><AudioLines size={14} /> Let's talk</div></div><div className="start-copy"><span className="eyebrow">SẴN SÀNG CHƯA?</span><h2>Một cuộc trò chuyện<br />mới đang chờ bạn.</h2><p>Chọn một chủ đề yêu thích hoặc bắt đầu trò chuyện tự do. AI sẽ điều chỉnh cách nói theo trình độ <b>{level}</b> của bạn.</p><label className="field-label">Chủ đề trò chuyện<select value={selectedTopic} onChange={e => setSelectedTopic(e.target.value)}><option value="">Tự do — không chọn chủ đề</option>{availableTopics.map(topic => <option key={topic.id} value={topic.id}>{topic.name}{topic.level ? ` · ${topic.level}` : ''}</option>)}</select></label><button className="primary-button" onClick={startSession} disabled={busy}>{busy ? 'Đang tạo phiên...' : 'Bắt đầu trò chuyện'} <ArrowRight size={17} /></button></div></div> : <div className="chat-layout"><div className="chat-card"><div className="chat-header"><div className="assistant-avatar"><Bot size={20} /></div><div><b>Lingua AI</b><span>{session.topicName || 'Trò chuyện tự do'} · Trình độ {level}</span></div><div className="chat-header-tools"><span className="online-status"><i /> Trực tuyến</span><button className="end-button" disabled={busy || session.status === 'ENDED'} onClick={endSession}>Kết thúc phiên</button></div></div><div className="message-list">{messages.length === 0 && <div className="chat-welcome"><div className="welcome-spark">✦</div><h3>Xin chào! Rất vui được gặp bạn.</h3><p>Hãy bắt đầu bằng một lời chào hoặc câu hỏi bất kỳ.</p><div className="suggestions"><button onClick={() => setDraft('Hi! Can you introduce yourself?')}>Introduce yourself <ArrowRight size={13} /></button><button onClick={() => setDraft('What do you like to do in your free time?')}>Talk about hobbies <ArrowRight size={13} /></button></div></div>}{messages.map(message => <div key={message.id} className={`message-row ${message.role === 'USER' ? 'mine' : ''}`}>{message.role === 'ASSISTANT' && <div className="message-avatar"><Bot size={15} /></div>}<div className="message-bubble"><p>{message.content}</p><div className="message-meta"><time>{new Date(message.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</time>{message.role === 'ASSISTANT' && <button onClick={() => playAudio(message.id)} aria-label="Phát câu trả lời"><Volume2 size={14} /></button>}</div></div>{message.role === 'USER' && <div className="user-message-avatar">{message.role[0]}</div>}</div>)}{busy && <div className="typing-row"><div className="message-avatar"><Bot size={15} /></div><div className="typing-bubble"><i /><i /><i /></div><span>{aiWaitingLong ? "AI đang xử lý lâu hơn bình thường…" : "AI đang suy nghĩ"}</span></div>}<div ref={bottomRef} /></div>{feedback && <div className="transcript-note"><AudioLines size={14} />{feedback}</div>}{session.status === 'ACTIVE' ? <form className="composer" onSubmit={sendText}><button type="button" className={`mic-button ${recording ? 'recording' : ''}`} disabled={busy || recordBusy} onClick={recording ? stopRecording : startRecording} title={recording ? 'Dừng và gửi ghi âm' : 'Gửi tin nhắn thoại'}>{recording ? <Pause size={18} /> : <Mic size={18} />}</button><input value={draft} onChange={e => setDraft(e.target.value)} placeholder={recording ? 'Đang ghi âm... nhấn nút micro để gửi' : recordBusy ? 'Đang xử lý giọng nói...' : 'Nhập tin nhắn bằng tiếng Anh...'} disabled={recording || recordBusy} /><span className="composer-hint">↵</span><button className="send-button" disabled={!draft.trim() || busy} aria-label="Gửi"><Send size={17} /></button></form> : <div className="session-ended"><Check size={17} /> Phiên học đã kết thúc. Bạn có thể xem lại hội thoại trong phần tiến độ.</div>}</div><aside className="chat-aside"><div className="aside-card session-info"><span className="eyebrow">PHIÊN HỌC</span><h3>{session.topicName || 'Trò chuyện tự do'}</h3><div className="aside-detail"><span>Trình độ</span><b>{level}</b></div><div className="aside-detail"><span>Tin nhắn</span><b>{messages.length}</b></div><div className="aside-detail"><span>Bắt đầu lúc</span><b>{new Date(session.startedAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</b></div></div><div className="aside-card tip-card"><div className="tip-icon"><Sparkles size={17} /></div><span className="eyebrow">MẸO NHỎ</span><h3>Đừng sợ mắc lỗi.</h3><p>Hãy nói tự nhiên nhất có thể. Mỗi lần thực hành là một bước tiến.</p></div><button className="subtle-button" onClick={() => loadMessages()}>Làm mới tin nhắn</button></aside></div>}</div>
}

async function convertToWav(blob: Blob): Promise<Blob> {
  const AudioContextCtor = window.AudioContext || (window as typeof window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
  if (!AudioContextCtor) throw new Error('Trình duyệt này chưa hỗ trợ xử lý âm thanh.')
  const context = new AudioContextCtor()
  try {
    const decoded = await context.decodeAudioData(await blob.arrayBuffer())
    const targetRate = 16000
    const source = decoded.getChannelData(0)
    const ratio = decoded.sampleRate / targetRate
    const length = Math.floor(source.length / ratio)
    const samples = new Float32Array(length)
    for (let i = 0; i < length; i++) {
      const start = Math.floor(i * ratio)
      const end = Math.min(Math.floor((i + 1) * ratio), source.length)
      let sum = 0
      for (let j = start; j < end; j++) sum += source[j]
      samples[i] = sum / Math.max(1, end - start)
    }
    const buffer = new ArrayBuffer(44 + samples.length * 2)
    const view = new DataView(buffer)
    const write = (offset: number, text: string) => [...text].forEach((char, index) => view.setUint8(offset + index, char.charCodeAt(0)))
    write(0, 'RIFF'); view.setUint32(4, 36 + samples.length * 2, true); write(8, 'WAVE'); write(12, 'fmt ')
    view.setUint32(16, 16, true); view.setUint16(20, 1, true); view.setUint16(22, 1, true)
    view.setUint32(24, targetRate, true); view.setUint32(28, targetRate * 2, true); view.setUint16(32, 2, true); view.setUint16(34, 16, true)
    write(36, 'data'); view.setUint32(40, samples.length * 2, true)
    for (let i = 0; i < samples.length; i++) {
      const value = Math.max(-1, Math.min(1, samples[i]))
      view.setInt16(44 + i * 2, value < 0 ? value * 0x8000 : value * 0x7fff, true)
    }
    return new Blob([buffer], { type: 'audio/wav' })
  } finally { await context.close() }
}

function Practice({ level, topics, onError }: { level: string; topics: Topic[]; onError: (message: string) => void }) {
  const [type, setType] = useState<'grammar' | 'listening'>('grammar')
  const [topicId, setTopicId] = useState('')
  const [question, setQuestion] = useState('')
  const [passage, setPassage] = useState('')
  const [listeningAudio, setListeningAudio] = useState('')
  const [answer, setAnswer] = useState('')
  const [result, setResult] = useState<{ correct: boolean; correctAnswer?: string; explanation?: string } | null>(null)
  const [busy, setBusy] = useState(false)
  const [prompt, setPrompt] = useState('')
  useEffect(() => () => { if (listeningAudio) URL.revokeObjectURL(listeningAudio) }, [listeningAudio])
  async function generate() {
    setBusy(true); setResult(null); setAnswer(''); setListeningAudio('')
    try {
      const data = await request<{ question: string; passage?: string }>(`/api/learn/${type}/exercise`, { method: 'POST', body: JSON.stringify({ level, ...(topicId ? { topicId: Number(topicId) } : {}) }) })
      setQuestion(data.question); setPassage(data.passage || '')
      if (type === 'listening' && data.passage) {
        const audio = await request<Blob>('/api/speech/synthesize', { method: 'POST', body: JSON.stringify({ text: data.passage, languageCode: 'en-US' }) })
        setListeningAudio(URL.createObjectURL(audio))
      }
    } catch (e) { onError(errorMessage(e)) } finally { setBusy(false) }
  }
  async function check(event: FormEvent) {
    event.preventDefault(); setBusy(true)
    const body = type === 'grammar' ? { question, learnerAnswer: answer } : { passage, question, learnerAnswer: answer }
    try { setResult(await request('/api/learn/' + type + '/exercise/check', { method: 'POST', body: JSON.stringify(body) })) }
    catch (e) { onError(errorMessage(e)) } finally { setBusy(false) }
  }
  async function generatePrompt(path: 'speaking' | 'pronunciation') {
    setBusy(true)
    try {
      const data = await request<Record<string, string>>(`/api/learn/${path}/prompt`, { method: 'POST', body: JSON.stringify({ level }) })
      setPrompt(data.prompt || data.sentence || '')
    } catch (e) { onError(errorMessage(e)) } finally { setBusy(false) }
  }
  return <div className="content-page"><PageIntro eyebrow="LUYỆN TẬP CÁ NHÂN" title="Học bằng cách thực hành" description="Bài tập được tạo theo trình độ, giúp bạn tiến bộ từng chút một." /><div className="practice-tabs"><button className={type === 'grammar' ? 'selected' : ''} onClick={() => { setType('grammar'); setQuestion(''); setPassage(''); setListeningAudio(''); setResult(null) }}><BookOpen size={17} /> Ngữ pháp</button><button className={type === 'listening' ? 'selected' : ''} onClick={() => { setType('listening'); setQuestion(''); setPassage(''); setListeningAudio(''); setResult(null) }}><Headphones size={17} /> Nghe hiểu</button></div><div className="exercise-card"><div className="exercise-toolbar"><div><span className="eyebrow">BÀI TẬP {type === 'grammar' ? 'NGỮ PHÁP' : 'NGHE HIỂU'}</span><h2>Sẵn sàng thử sức?</h2></div><div className="exercise-filters"><span className="level-pill">{level}</span><select value={topicId} onChange={e => setTopicId(e.target.value)}><option value="">Mọi chủ đề</option>{topics.map(topic => <option value={topic.id} key={topic.id}>{topic.name}</option>)}</select></div></div>{question ? <><div className="question-box">{passage && <div className="passage"><Volume2 size={16} /><span>Nghe đoạn văn rồi trả lời câu hỏi.</span>{listeningAudio && <audio controls src={listeningAudio} aria-label="Đoạn nghe" />}</div>}<span className="question-label">CÂU HỎI</span><p>{question}</p></div><form onSubmit={check} className="answer-form"><label className="field-label">Câu trả lời của bạn<input value={answer} onChange={e => setAnswer(e.target.value)} placeholder="Nhập câu trả lời..." required /></label><button className="primary-button" disabled={busy}>{busy ? 'Đang chấm...' : 'Kiểm tra đáp án'} <ArrowRight size={16} /></button></form>{result && <div className={`answer-result ${result.correct ? 'correct' : 'incorrect'}`}><div className="result-mark">{result.correct ? <Check size={17} /> : <X size={17} />}</div><div><b>{result.correct ? 'Chính xác!' : 'Chưa chính xác'}</b>{result.correctAnswer && <p>Đáp án: {result.correctAnswer}</p>}{result.explanation && <p>{result.explanation}</p>}</div></div>}</> : <div className="empty-exercise"><div className="exercise-orb"><Sparkles size={25} /></div><p>Bài tập sẽ phù hợp với trình độ và chủ đề bạn chọn.</p><button className="primary-button" onClick={generate} disabled={busy}>{busy ? 'Đang tạo bài...' : 'Tạo bài tập'} <ArrowRight size={16} /></button></div>}</div><div className="prompt-grid"><PromptCard icon={<MessageCircle />} title="Luyện nói" description="Nhận một câu hỏi mở để luyện nói tự do." action="Tạo đề nói" onClick={() => generatePrompt('speaking')} /><PromptCard icon={<AudioLines />} title="Luyện phát âm" description="Luyện đọc một câu phù hợp trình độ." action="Tạo câu luyện đọc" onClick={() => generatePrompt('pronunciation')} /></div>{prompt && <div className="prompt-result"><div><span className="eyebrow">ĐỀ BÀI CỦA BẠN</span><p>{prompt}</p></div><button onClick={() => setPrompt('')} aria-label="Đóng"><X size={17} /></button></div>}</div>
}

function PromptCard({ icon, title, description, action, onClick }: { icon: ReactNode; title: string; description: string; action: string; onClick: () => void }) {
  return <div className="prompt-card"><span className="prompt-icon">{icon}</span><div><h3>{title}</h3><p>{description}</p><button className="text-button" onClick={onClick}>{action} <ArrowRight size={14} /></button></div></div>
}

function Vocabulary({ onError }: { onError: (message: string) => void }) {
  const [items, setItems] = useState<VocabularyProgress[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('')
  const fetchItems = useCallback(async () => {
    setLoading(true)
    try { setItems(await request<VocabularyProgress[]>(`/api/progress/vocabulary${filter ? `?status=${filter}` : ''}`)) }
    catch (e) { onError(errorMessage(e)) } finally { setLoading(false) }
  }, [filter, onError])
  useEffect(() => { fetchItems() }, [fetchItems])
  async function update(item: VocabularyProgress, status: string) {
    try { await request(`/api/progress/vocabulary/${item.vocabularyId}`, { method: 'PUT', body: JSON.stringify({ status }) }); await fetchItems() }
    catch (e) { onError(errorMessage(e)) }
  }
  return <div className="content-page"><PageIntro eyebrow="SỔ TAY CỦA BẠN" title="Từ vựng đang học" description="Theo dõi những từ bạn muốn ghi nhớ và đánh dấu khi đã thành thạo." /><div className="list-toolbar"><div className="filter-pills"><button className={!filter ? 'active' : ''} onClick={() => setFilter('')}>Tất cả</button><button className={filter === 'LEARNING' ? 'active' : ''} onClick={() => setFilter('LEARNING')}>Đang học</button><button className={filter === 'LEARNED' ? 'active' : ''} onClick={() => setFilter('LEARNED')}>Đã thuộc</button></div><span className="muted-small">{items.length} từ được theo dõi</span></div><div className="word-list">{loading ? <LoadingLine /> : items.length ? items.map((item, index) => <div className="word-row" key={item.vocabularyId}><div className={`word-number tone-${index % 4}`}>{String(index + 1).padStart(2, '0')}</div><div className="word-main"><b>{item.word}</b><span>{item.meaning}</span></div><span className={`word-status ${item.status === 'LEARNED' ? 'done' : ''}`}>{item.status === 'LEARNED' ? 'Đã thuộc' : 'Đang học'}</span><button className="word-action" onClick={() => update(item, item.status === 'LEARNED' ? 'LEARNING' : 'LEARNED')}>{item.status === 'LEARNED' ? 'Ôn lại' : 'Đánh dấu đã thuộc'}</button></div>) : <EmptyState icon={<BookOpen />} title="Chưa có từ vựng được theo dõi" body="Từ vựng sẽ xuất hiện tại đây khi bạn bắt đầu đánh dấu tiến độ học." />}</div></div>
}

function Progress({ summary, onRefresh }: { summary: Summary | null; onRefresh: () => Promise<void> }) {
  useEffect(() => { onRefresh() }, [onRefresh])
  const points = summary?.scoreHistory || []
  const max = Math.max(100, ...points.map(p => p.overallScore || 0))
  return <div className="content-page"><PageIntro eyebrow="NHÌN LẠI HÀNH TRÌNH" title="Tiến độ của bạn" description="Những con số nhỏ kể câu chuyện về sự cố gắng của bạn." /><div className="progress-grid"><div className="progress-feature"><div className="feature-top"><span className="eyebrow">ĐIỂM TRUNG BÌNH</span><span className="feature-icon"><Activity size={18} /></span></div><div className="big-score">{summary?.averageOverallScore == null ? '—' : Math.round(summary.averageOverallScore)}<small>{summary?.averageOverallScore == null ? '' : ' / 100'}</small></div><p>{summary?.averageOverallScore == null ? 'Hoàn thành và chấm điểm một phiên để bắt đầu theo dõi.' : 'Điểm số được tổng hợp từ các phiên đã hoàn thành.'}</p><div className="feature-sparkline">{Array.from({ length: 30 }, (_, i) => <i key={i} style={{ height: `${14 + ((i * 17 + 9) % 32)}px` }} />)}</div></div><div className="progress-metrics"><Metric label="Phiên hội thoại" value={summary?.totalSessions ?? 0} icon={<MessageCircle />} /><Metric label="Tin nhắn của bạn" value={summary?.totalMessagesSent ?? 0} icon={<Send />} /><Metric label="Từ đã thuộc" value={summary?.vocabularyLearnedCount ?? 0} icon={<BookOpen />} /><Metric label="Đang học" value={summary?.vocabularyLearningCount ?? 0} icon={<GraduationCap />} /></div></div><div className="history-card"><div className="section-heading compact"><div><h2>Lịch sử điểm số</h2><p>Điểm qua các phiên luyện tập đã chấm.</p></div><span className="level-tag">GẦN ĐÂY</span></div>{points.length ? <div className="score-history">{points.map((item, index) => <div className="score-point" key={item.sessionId}><span>{item.startedAt ? new Date(item.startedAt).toLocaleDateString('vi-VN') : `Phiên ${index + 1}`}</span><div className="score-track"><i style={{ width: `${Math.min(100, item.overallScore / max * 100)}%` }} /></div><b>{Math.round(item.overallScore)}</b></div>)}</div> : <EmptyState icon={<Activity />} title="Chưa có điểm số" body="Kết thúc một phiên hội thoại và chấm điểm để xem tiến trình tại đây." />}</div></div>
}
function Metric({ label, value, icon }: { label: string; value: number; icon: ReactNode }) { return <div className="metric-card"><span>{icon}</span><div><b>{value}</b><small>{label}</small></div></div> }

function Admin({ topics, onRefresh, onError }: { topics: Topic[]; onRefresh: () => Promise<void>; onError: (message: string) => void }) {
  const [name, setName] = useState(''); const [description, setDescription] = useState(''); const [level, setLevel] = useState('A2'); const [chat, setChat] = useState(''); const [reply, setReply] = useState(''); const [busy, setBusy] = useState(false)
  const [configs, setConfigs] = useState<AiConfig[]>([])
  const [configForm, setConfigForm] = useState({ name: 'english-tutor', provider: 'ollama', model: 'qwen3:4b', systemPrompt: 'You are a friendly English tutor. Adapt your language to the learner’s CEFR level.', temperature: '0.7', maxTokens: '1024' })
  const loadConfigs = useCallback(async () => {
    try { setConfigs(await request<AiConfig[]>('/api/admin/ai-configs')) } catch (e) { onError(errorMessage(e)) }
  }, [onError])
  useEffect(() => { loadConfigs() }, [loadConfigs])
  async function createTopic(event: FormEvent) {
    event.preventDefault(); setBusy(true)
    try { await request('/api/topics', { method: 'POST', body: JSON.stringify({ name, description, level }) }); setName(''); setDescription(''); await onRefresh() }
    catch (e) { onError(errorMessage(e)) } finally { setBusy(false) }
  }
  async function testAI(event: FormEvent) {
    event.preventDefault(); setBusy(true)
    try { const result = await request<{ provider: string; reply: string }>('/api/admin/ai/test-chat', { method: 'POST', body: JSON.stringify({ message: chat }) }); setReply(`[${result.provider}] ${result.reply}`) }
    catch (e) { onError(errorMessage(e)) } finally { setBusy(false) }
  }
  async function createConfig(event: FormEvent) {
    event.preventDefault(); setBusy(true)
    try {
      await request('/api/admin/ai-configs', { method: 'POST', body: JSON.stringify({ ...configForm, temperature: Number(configForm.temperature), maxTokens: Number(configForm.maxTokens) }) })
      await loadConfigs()
    } catch (e) { onError(errorMessage(e)) } finally { setBusy(false) }
  }
  async function activateConfig(configId: number) {
    try { await request(`/api/admin/ai-configs/${configId}/activate`, { method: 'PUT' }); await loadConfigs() }
    catch (e) { onError(errorMessage(e)) }
  }
  async function deactivate(topic: Topic) {
    try { await request(`/api/topics/${topic.id}`, { method: 'DELETE' }); await onRefresh() }
    catch (e) { onError(errorMessage(e)) }
  }
  return <div className="content-page"><PageIntro eyebrow="KHU VỰC QUẢN TRỊ" title="Quản lý hệ thống" description="Quản lý chủ đề học, cấu hình AI và kiểm tra kết nối với provider." /><div className="admin-grid"><form className="admin-card" onSubmit={createTopic}><div className="admin-card-title"><span className="prompt-icon"><Plus /></span><div><h2>Tạo chủ đề học</h2><p>Chủ đề mới sẽ hiển thị cho học viên.</p></div></div><label className="field-label">Tên chủ đề<input value={name} onChange={e => setName(e.target.value)} required maxLength={100} placeholder="Ví dụ: Food & cooking" /></label><label className="field-label">Mô tả<input value={description} onChange={e => setDescription(e.target.value)} maxLength={500} placeholder="Mô tả ngắn về chủ đề" /></label><label className="field-label">Trình độ<select value={level} onChange={e => setLevel(e.target.value)}>{levels.map(l => <option key={l}>{l}</option>)}</select></label><button className="primary-button" disabled={busy}>Tạo chủ đề <Plus size={16} /></button></form><form className="admin-card" onSubmit={testAI}><div className="admin-card-title"><span className="prompt-icon coral-icon"><Bot /></span><div><h2>Kiểm tra AI</h2><p>Gửi một tin nhắn đến provider đang hoạt động.</p></div></div><label className="field-label">Tin nhắn thử<input value={chat} onChange={e => setChat(e.target.value)} required placeholder="Say hello in English" /></label><button className="secondary-button" disabled={busy}>Gửi thử <ArrowRight size={16} /></button>{reply && <div className="ai-test-reply"><span className="eyebrow">PHẢN HỒI</span><p>{reply}</p></div>}</form></div><form className="admin-card config-create" onSubmit={createConfig}><div className="admin-card-title"><span className="prompt-icon"><Settings2 /></span><div><h2>Tạo cấu hình AI</h2><p>Cấu hình cần được kích hoạt để dùng cho phiên hội thoại mới.</p></div></div><div className="admin-grid"><label className="field-label">Tên cấu hình<input value={configForm.name} onChange={e => setConfigForm({ ...configForm, name: e.target.value })} required /></label><label className="field-label">Provider<select value={configForm.provider} onChange={e => setConfigForm({ ...configForm, provider: e.target.value, model: e.target.value === 'ollama' ? 'qwen3:4b' : e.target.value === 'gemini' ? 'gemini-2.5-flash' : 'mock' })}><option value="ollama">Ollama</option><option value="gemini">Gemini</option><option value="mock">Mock</option></select></label><label className="field-label">Model<input value={configForm.model} onChange={e => setConfigForm({ ...configForm, model: e.target.value })} required /></label><label className="field-label">Temperature<input type="number" min="0" max="2" step="0.1" value={configForm.temperature} onChange={e => setConfigForm({ ...configForm, temperature: e.target.value })} required /></label></div><label className="field-label">System prompt<input value={configForm.systemPrompt} onChange={e => setConfigForm({ ...configForm, systemPrompt: e.target.value })} /></label><button className="primary-button" disabled={busy}>Lưu cấu hình <Plus size={16} /></button></form><div className="history-card admin-topics"><div className="section-heading compact"><div><h2>Cấu hình AI</h2><p>{configs.length} cấu hình</p></div></div>{configs.map(config => <div className="topic-admin-row" key={config.id}><span className="topic-swatch"><Bot size={16} /></span><div><b>{config.name} {config.active && <span className="active-config-label">ĐANG DÙNG</span>}</b><small>{config.provider} · {config.model} · {config.maxTokens} tokens</small></div>{!config.active && <button className="text-button" onClick={() => activateConfig(config.id)}>Kích hoạt <ArrowRight size={13} /></button>}</div>)}</div><div className="history-card admin-topics"><div className="section-heading compact"><div><h2>Chủ đề hiện có</h2><p>{topics.length} chủ đề đang hoạt động</p></div></div>{topics.map(topic => <div className="topic-admin-row" key={topic.id}><span className="topic-swatch"><Compass size={16} /></span><div><b>{topic.name}</b><small>{topic.description || 'Chưa có mô tả'} · {topic.level || 'Chưa phân cấp'}</small></div><button className="text-danger" onClick={() => deactivate(topic)}>Ẩn chủ đề</button></div>)}</div></div>
}

function PageIntro({ eyebrow, title, description }: { eyebrow: string; title: string; description: string }) { return <div className="page-heading content-intro"><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{description}</p></div> }
function EmptyState({ icon, title, body }: { icon: ReactNode; title: string; body: string }) { return <div className="empty-state"><div>{icon}</div><b>{title}</b><p>{body}</p></div> }
function LoadingLine() { return <div className="loading-line"><i /><span>Đang tải dữ liệu...</span></div> }

