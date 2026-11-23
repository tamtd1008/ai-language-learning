const byId = id => document.getElementById(id);
const login = byId('login');
const turn = byId('turn');
const record = byId('record');
const listen = byId('listen');
const newSession = byId('new-session');
const topicSelect = byId('topic');
const player = byId('player');
const grade = byId('grade');
const listenScore = byId('listen-score');
let token, session, robot, reply, recorder, recordingTimer, audioUrl;
let score, evaluation;
const scoreFields = [
  ['grammarScore', 'Ngữ pháp', 'Grammar'],
  ['vocabularyScore', 'Từ vựng', 'Vocabulary'],
  ['conversationScore', 'Hội thoại', 'Conversation'],
  ['overallScore', 'Điểm tổng', 'Overall score'],
];
let robotUpdate = Promise.resolve();
let busy = false;
const topicExamples = new Map([
  ['Daily Life', { label: 'Đời sống hằng ngày', prompt: 'What do you usually do on weekends?' }],
  ['Food & Dining', { label: 'Ăn uống', prompt: 'Imagine we are in a café. What would you like to order?' }],
]);

function updateControls() {
  for (const button of document.querySelectorAll('button')) button.disabled = busy;
  const active = session?.status === 'ACTIVE';
  turn.querySelector('button').disabled = busy || !active || !!recorder || !player.paused;
  record.disabled = busy || !active || !player.paused;
  listen.disabled = busy || !reply || !!recorder || !player.paused;
  newSession.disabled = busy || !!recorder || !player.paused;
  topicSelect.disabled = newSession.disabled;
  grade.disabled = newSession.disabled || !session || !reply || !!evaluation;
  listenScore.disabled = newSession.disabled || !score;
  player.controls = !busy && !recorder;
}

async function api(path, method = 'GET', body, binary = false) {
  const headers = token ? { Authorization: 'Bearer ' + token } : {};
  if (body && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(body);
  }
  const response = await fetch('/api' + path, { method, headers, body });
  if (response.ok && binary) return response.blob();
  const result = await response.json().catch(() => null);
  if (!response.ok || !result?.success) {
    throw new Error((result?.message || 'Yêu cầu thất bại') + ' (HTTP ' + response.status + ')');
  }
  return result.data;
}

function robotState(status) {
  if (!robot) return Promise.resolve();
  const sessionId = session?.id ?? null;
  robotUpdate = robotUpdate.then(async () => {
    try {
      await api('/robots/' + robot.id + '/status', 'PUT', { status, sessionId });
      byId('robot-status').textContent = 'Trạng thái gửi tới robot: ' + status;
    } catch (error) {
      byId('robot-status').textContent = 'Không cập nhật được trạng thái robot: ' + error.message;
    }
  });
  return robotUpdate;
}

async function refreshMessages() {
  const messages = await api('/sessions/' + session.id + '/messages');
  byId('messages').replaceChildren(...messages.map(message => {
    const item = document.createElement('li');
    item.textContent = (message.role === 'USER' ? 'Bạn: ' : 'Robot: ') + message.content;
    return item;
  }));
}

async function run(message, action) {
  if (busy) return;
  busy = true;
  byId('status').textContent = message;
  updateControls();
  try {
    byId('status').textContent = (await action()) || '';
  } catch (error) {
    byId('status').textContent = error.message;
  } finally {
    busy = false;
    updateControls();
  }
}

async function loadTopics() {
  const status = byId('topic-status');
  try {
    const topics = await api('/topics');
    topicSelect.replaceChildren(
      new Option('Tự do', ''),
      ...topics.filter(topic => topic.active !== false).map(topic =>
        new Option(topicExamples.get(topic.name)?.label ?? topic.name, topic.id)),
    );
    status.hidden = true;
  } catch {
    status.textContent = 'Chưa tải được danh sách chủ đề. Bạn vẫn có thể luyện hội thoại tự do.';
    status.hidden = false;
  }
}

async function openSession(topicId) {
  const next = await api('/sessions', 'POST', topicId ? { topicId: Number(topicId) } : {});
  const example = topicExamples.get(next.topicName);
  byId('session-topic').textContent = example?.label ?? next.topicName ?? 'Hội thoại tự do';
  byId('topic-prompt').textContent = example?.prompt ?? (next.topicName
    ? 'What would you like to talk about in this topic?'
    : 'Hi! Tell me your name and one thing you like.');
  return next;
}

login.onsubmit = event => {
  event.preventDefault();
  void run('Đang đăng nhập…', async () => {
    token = (await api('/auth/login', 'POST', Object.fromEntries(new FormData(login)))).token;
    session = await openSession('');
    await loadTopics();
    try {
      const deviceId = 'gazebo-language-robot';
      robot = (await api('/robots')).find(device => device.deviceId === deviceId)
        || await api('/robots/register', 'POST', { deviceId });
      await robotState('IDLE');
    } catch (error) {
      byId('robot-status').textContent = 'Không đăng ký được robot: ' + error.message;
    }
    login.reset();
    login.hidden = true;
    byId('lesson').hidden = false;
  });
};

newSession.onclick = () => {
  if (recorder || !player.paused) return;
  const topicId = topicSelect.value;
  void run('Đang mở hội thoại mới…', async () => {
    if (session) {
      if (session.status === 'ACTIVE') await api('/sessions/' + session.id + '/end', 'PUT');
      session = null;
      reply = null;
      player.removeAttribute('src');
      player.load();
      player.hidden = true;
      if (audioUrl) {
        URL.revokeObjectURL(audioUrl);
        audioUrl = null;
      }
      await robotState('IDLE');
    }
    score = evaluation = null;
    byId('assessment').hidden = true;
    byId('scores').replaceChildren();
    byId('feedback').textContent = '';
    grade.textContent = 'Kết thúc và chấm điểm';
    try {
      session = await openSession(topicId);
    } catch (error) {
      throw new Error('Chưa mở được hội thoại mới. Hãy bấm nút để thử lại. ' + error.message);
    }
    turn.reset();
    byId('messages').replaceChildren();
    for (const id of ['transcript', 'audio-text']) {
      byId(id).textContent = '';
      byId(id).hidden = true;
    }
    await robotState('IDLE');
    turn.elements.content.focus();
    return 'Đã bắt đầu hội thoại mới.';
  });
};

async function sendTurn(content) {
  try {
    reply = await api('/sessions/' + session.id + '/messages', 'POST', { content });
  } catch (error) {
    throw new Error('Không lấy được câu trả lời của robot: ' + error.message);
  }
  turn.reset();
  await refreshMessages();
}

turn.onsubmit = event => {
  event.preventDefault();
  if (session?.status !== 'ACTIVE' || recorder || !player.paused) return;
  const content = turn.elements.content.value.trim();
  if (!content) return;
  void run('Đang chờ AI trả lời…', () => sendTurn(content));
};

record.onclick = async () => {
  if (session?.status !== 'ACTIVE') return;
  if (recorder) {
    if (recorder.state === 'recording') recorder.stop();
    return;
  }
  if (!player.paused) return;
  let stream;
  await run('Đang mở microphone…', async () => {
    stream = await navigator.mediaDevices.getUserMedia({ audio: { channelCount: 1 } });
    try {
      const chunks = [];
      recorder = new MediaRecorder(stream);
      recorder.ondataavailable = ({ data }) => { if (data.size) chunks.push(data); };
      recorder.onstop = () => {
        clearTimeout(recordingTimer);
        stream.getTracks().forEach(track => track.stop());
        const blob = new Blob(chunks, { type: recorder.mimeType });
        recorder = null;
        record.textContent = 'Thu âm';
        void run('Whisper đang chép lời…', async () => {
          await robotState('IDLE');
          const form = new FormData();
          form.append('audio', await asWav(blob), 'voice.wav');
          form.append('languageCode', 'en-US');
          let transcription;
          try {
            transcription = await api('/speech/transcribe', 'POST', form);
          } catch (error) {
            throw new Error('Không chép lời được bằng Whisper: ' + error.message);
          }
          turn.elements.content.value = transcription.text;
          const transcript = byId('transcript');
          transcript.textContent = 'Whisper nghe được: “' + transcription.text + '”';
          transcript.hidden = false;
          return 'Bạn có thể sửa bản chép lời rồi bấm Gửi để robot trả lời.';
        });
      };
      await robotState('LISTENING');
      recorder.start();
      record.textContent = 'Dừng và gửi';
      recordingTimer = setTimeout(() => {
        if (recorder?.state === 'recording') recorder.stop();
      }, 30000);
    } catch (error) {
      stream.getTracks().forEach(track => track.stop());
      recorder = null;
      await robotState('IDLE');
      throw error;
    }
  });
};

grade.onclick = () => {
  if (grade.disabled) return;
  void run('Đang đánh giá hội thoại…', async () => {
    if (session.status === 'ACTIVE') {
      session = await api('/sessions/' + session.id + '/end', 'PUT');
      grade.textContent = 'Thử chấm lại';
    }
    if (!score) {
      score = await api('/sessions/' + session.id + '/score', 'POST');
      byId('scores').replaceChildren(...scoreFields.map(([field, label]) => {
        const item = document.createElement('li');
        item.textContent = label + ': ' + score[field].toLocaleString('vi-VN') + '/100';
        return item;
      }));
      byId('assessment').hidden = false;
      grade.textContent = 'Thử lấy lại nhận xét';
    }
    evaluation = await api('/sessions/' + session.id + '/evaluate', 'POST');
    byId('feedback').textContent = [
      evaluation.summary,
      'Điểm mạnh: ' + evaluation.strengths,
      'Cần cải thiện: ' + evaluation.areasToImprove,
      'CEFR tham khảo: ' + evaluation.estimatedLevel,
    ].join('\n');
    grade.textContent = 'Đã chấm điểm';
    return 'Đã có kết quả. Bấm Nghe kết quả bằng tiếng Anh để phát.';
  });
};

listenScore.onclick = () => {
  if (listenScore.disabled) return;
  void run('Piper đang tạo âm thanh…', async () => {
    const text = [
      ...scoreFields.map(([field, , label]) => label + ': ' + score[field] + ' out of 100.'),
      evaluation && 'Estimated level: ' + evaluation.estimatedLevel + '.',
      evaluation?.spokenFeedback?.trim().slice(0, 500),
    ].filter(Boolean).join(' ');
    const audio = await api('/speech/synthesize', 'POST', { text, languageCode: 'en-US' }, true);
    return playAudio(audio, text);
  });
};

listen.onclick = () => {
  if (recorder || !player.paused) return;
  void run('Piper đang tạo âm thanh…', async () => {
    const audio = await api('/sessions/' + session.id + '/messages/' + reply.id + '/audio', 'GET', undefined, true);
    return playAudio(audio, reply.content);
  });
};

async function playAudio(audio, text) {
    if (audioUrl) URL.revokeObjectURL(audioUrl);
    audioUrl = URL.createObjectURL(audio);
    player.src = audioUrl;
    player.hidden = false;
    const audioText = byId('audio-text');
    audioText.textContent = 'Piper đọc câu này: “' + text + '”';
    audioText.hidden = false;
    return player.play().then(() => '').catch(error => {
      if (error.name !== 'NotAllowedError') throw error;
      return 'Bấm nút phát trên thanh âm thanh để nghe.';
    });
}

player.onplay = () => { updateControls(); void robotState('SPEAKING'); };
player.onpause = player.onended = () => { updateControls(); void robotState('IDLE'); };

async function asWav(blob) {
  const context = new AudioContext();
  try {
    const decoded = await context.decodeAudioData(await blob.arrayBuffer());
    const rate = 16000;
    const offline = new OfflineAudioContext(1, Math.ceil(decoded.duration * rate), rate);
    const source = offline.createBufferSource();
    source.buffer = decoded;
    source.connect(offline.destination);
    source.start();
    const samples = (await offline.startRendering()).getChannelData(0);
    const view = new DataView(new ArrayBuffer(44 + samples.length * 2));
    function tag(offset, value) {
      for (let i = 0; i < value.length; i++) view.setUint8(offset + i, value.charCodeAt(i));
    }
    tag(0, 'RIFF'); view.setUint32(4, view.byteLength - 8, true);
    tag(8, 'WAVE'); tag(12, 'fmt '); view.setUint32(16, 16, true);
    view.setUint16(20, 1, true); view.setUint16(22, 1, true);
    view.setUint32(24, rate, true); view.setUint32(28, rate * 2, true);
    view.setUint16(32, 2, true); view.setUint16(34, 16, true);
    tag(36, 'data'); view.setUint32(40, samples.length * 2, true);
    samples.forEach((sample, index) => view.setInt16(44 + index * 2,
      Math.round(Math.max(-1, Math.min(1, sample)) * 32767), true));
    return new Blob([view.buffer], { type: 'audio/wav' });
  } finally {
    await context.close();
  }
}
