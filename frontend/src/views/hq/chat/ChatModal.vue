<!-- src/components/chat/ChatModal.vue -->
<script setup>
import { ref, computed, watch, onBeforeUnmount, nextTick } from 'vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { useAuthStore } from '@/stores/authStore'
import {
  createOrGetPrivateRoom,
  getChatHistory,
  markChatRead,
} from '@/components/api/chat/chatService'

const props = defineProps({
  visible: { type: Boolean, default: false },
  otherNickname: { type: String, required: true },
  title: { type: String, default: '1:1 채팅' },
})

const emit = defineEmits(['close'])

const authStore = useAuthStore()

const myName = computed(() => authStore.userInfo?.name || '')
const token = computed(() => authStore.userInfo?.accessToken || '')

const chatRoomId = ref(null)
const messages = ref([]) // { senderName, message, createdAt }
const inputMessage = ref('')

const loading = ref(false)
const errorMsg = ref('')
const connecting = ref(false)
const isConnected = ref(false)

let stompClient = null
const messageEndRef = ref(null)

// ----------------- 유틸: 시간 포맷 -----------------

function formatDateTime(dateLike) {
  if (!dateLike) return ''
  const d = typeof dateLike === 'string' ? new Date(dateLike) : dateLike
  if (Number.isNaN(d.getTime())) return ''

  // 2025-12-11 15:32 이런 형식
  // const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
}

// ----------------- 웹소켓 연결 -----------------

function connectWebSocket() {
  if (!chatRoomId.value || !token.value) {
    return
  }

  // const socket = new SockJS('http://localhost:8080/ws/chat')
  const socket = new SockJS('https://order101.link/ws/chat')

  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    connectHeaders: {
      Authorization: `Bearer ${token.value}`,
    },
    debug: (str) => {
      // console.log('[STOMP debug]', str)
    },
    onConnect() {

      isConnected.value = true
      connecting.value = false

      stompClient.subscribe(
        `/sub/chat/${chatRoomId.value}`,
        (frame) => {
          const payload = JSON.parse(frame.body)
          messages.value.push({
            senderName: payload.senderName,
            message: payload.message,
            // 서버에서 보낸 시간이 있으면 사용, 없으면 현재 시간
            createdAt: payload.createdAt ?? payload.sentAt ?? payload.time ?? null,
          })
          scrollToBottom()
        },
        {
          Authorization: `Bearer ${token.value}`,
        },
      )
    },
    onStompError(frame) {
      console.error('[STOMP] error', frame.headers['message'], frame.body)
      isConnected.value = false
      connecting.value = false
      errorMsg.value = frame.headers['message'] || '채팅 서버 연결 중 오류가 발생했습니다.'
    },
  })

  stompClient.onWebSocketClose = (evt) => {
    isConnected.value = false
  }

  connecting.value = true
  stompClient.activate()
}

function disconnectWebSocket() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
    isConnected.value = false
  }
}

// ----------------- 초기 로딩 -----------------

async function initChat() {
  loading.value = true
  errorMsg.value = ''
  messages.value = []
  chatRoomId.value = null
  isConnected.value = false

  try {
    if (!token.value) {
      throw new Error('로그인이 필요합니다.')
    }

    const roomId = await createOrGetPrivateRoom(props.otherNickname)
    if (!roomId) {
      throw new Error('채팅방 정보를 찾을 수 없습니다.')
    }
    chatRoomId.value = roomId

    const history = await getChatHistory(roomId)

    messages.value = history.map((m) => ({
      senderName: m.senderName,
      message: m.message,
      createdAt: m.createdAt ?? m.sentAt ?? m.time ?? null,
    }))

    await nextTick()
    scrollToBottom()

    await markChatRead(roomId)

    connectWebSocket()
  } catch (e) {
    console.error('[CHAT] initChat error', e)
    errorMsg.value =
      e.response?.data?.message ||
      e.response?.data?.data ||
      e.message ||
      '채팅방을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

// ----------------- 메시지 전송 -----------------

function sendMessage() {
  const text = inputMessage.value.trim()
  if (!text) return

  if (!stompClient || !isConnected.value || !stompClient.connected) {
    console.warn('[CHAT] STOMP not connected, message not sent')
    errorMsg.value = '채팅 서버에 연결되지 않았습니다. 잠시 후 다시 시도해주세요.'
    return
  }

  const payload = {
    senderName: myName.value,
    message: text,
  }

  stompClient.publish({
    destination: `/pub/chat/${chatRoomId.value}`,
    body: JSON.stringify(payload),
  })

  inputMessage.value = ''
}

// ----------------- 스크롤 -----------------

async function scrollToBottom() {
  await nextTick()
  const el = messageEndRef.value
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'end' })
}

// ----------------- 모달 열림/닫힘 -----------------

watch(
  () => props.visible,
  (val) => {
    if (val) {
      initChat()
    } else {
      disconnectWebSocket()
      messages.value = []
      inputMessage.value = ''
      chatRoomId.value = null
      errorMsg.value = ''
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  disconnectWebSocket()
})
</script>

<template>
  <teleport to="body">
    <div v-if="visible" class="chat-overlay">
      <div class="chat-modal">
        <header class="chat-header">
          <h2>{{ title }}</h2>
          <button class="chat-close" type="button" @click="$emit('close')">✕</button>
        </header>

        <section class="chat-body">
          <div v-if="loading" class="chat-status">채팅방을 불러오는 중입니다...</div>
          <div v-else-if="errorMsg" class="chat-status chat-error">
            {{ errorMsg }}
          </div>
          <div v-else class="chat-messages">
            <div
              v-for="(msg, idx) in messages"
              :key="idx"
              :class="['chat-message', msg.senderName === myName ? 'me' : 'other']"
            >
              <div class="chat-meta">
                <span class="chat-sender">
                  {{ msg.senderName === myName ? '나' : msg.senderName }}
                </span>
              </div>

              <!-- 말풍선 + 시간 한 줄 -->
              <div class="chat-row" :class="msg.senderName === myName ? 'me' : 'other'">
                <div class="chat-bubble">
                  {{ msg.message }}
                </div>
                <span class="chat-time">
                  {{ formatDateTime(msg.createdAt) }}
                </span>
              </div>
            </div>
            <div ref="messageEndRef" />
          </div>
        </section>

        <footer class="chat-footer">
          <textarea
            v-model="inputMessage"
            class="chat-input"
            placeholder="메시지를 입력하세요. Enter로 전송, Shift+Enter로 줄바꿈"
            rows="2"
            :disabled="!!errorMsg || loading || connecting"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <button
            type="button"
            class="chat-send-btn"
            @click="sendMessage"
            :disabled="!!errorMsg || loading || connecting || !isConnected"
          >
            전송
          </button>
        </footer>
      </div>
    </div>
  </teleport>
</template>

<style scoped>
.chat-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}

.chat-modal {
  width: 420px;
  max-width: 95vw;
  max-height: 80vh;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.4);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: linear-gradient(135deg, #7c3aed 0%, #06b6d4 100%);
  color: #fff;
}

.chat-header h2 {
  font-size: 15px;
  font-weight: 600;
}

.chat-close {
  border: none;
  background: transparent;
  color: inherit;
  font-size: 16px;
  cursor: pointer;
}

.chat-body {
  padding: 8px 12px;
  flex: 1;
  min-height: 200px;
  max-height: 380px;
  overflow-y: auto;
  background: #f9fafb;
}

.chat-status {
  padding: 12px;
  font-size: 13px;
  color: #4b5563;
}

.chat-error {
  color: #b91c1c;
}

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-message {
  max-width: 85%;
  display: flex;
  flex-direction: column;
}

.chat-message.me {
  margin-left: auto;
  align-items: flex-end;
}

.chat-message.other {
  align-items: flex-start;
}

.chat-meta {
  font-size: 11px;
  color: #6b7280;
  margin-bottom: 2px;
}

/* 말풍선 + 시간 한 줄 */
.chat-row {
  display: inline-flex;
  align-items: flex-end;
  gap: 4px;
  max-width: 100%;
}

.chat-row.me {
  flex-direction: row-reverse; /* 내 말풍선은 오른쪽, 시간은 왼쪽 */
}

/* 말풍선 자체는 내용 길이에 맞게 */
.chat-bubble {
  padding: 8px 10px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.4;
  background: #e5e7eb;
  color: #111827;
  word-break: break-word;
  display: inline-block;
  max-width: 260px; /* 너무 길어지지 않게 */
}

.chat-message.me .chat-bubble {
  background: #4f46e5;
  color: #fff;
}

.chat-time {
  font-size: 11px;
  color: #9ca3af;
  white-space: nowrap;
}

.chat-footer {
  display: flex;
  gap: 8px;
  padding: 8px 12px 12px;
  border-top: 1px solid #e5e7eb;
  background: #f9fafb;
}

.chat-input {
  flex: 1;
  resize: none;
  font-size: 13px;
  padding: 6px 8px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  outline: none;
}

.chat-input:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 1px rgba(79, 70, 229, 0.2);
}

/* 예쁜 전송 버튼 */
.chat-send-btn {
  padding: 0 18px;
  border-radius: 999px;
  border: none;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%);
  color: #fff;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.35);
  transition:
    transform 0.08s ease,
    box-shadow 0.08s ease,
    opacity 0.08s ease;
}

.chat-send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(59, 130, 246, 0.45);
}

.chat-send-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 3px 8px rgba(59, 130, 246, 0.35);
}

.chat-send-btn:disabled {
  opacity: 0.6;
  cursor: default;
  box-shadow: none;
}

.chat-input {
  flex: 1;
  resize: none;
  font-size: 13px;
  padding: 6px 8px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  outline: none;
  font-family:
    'Pretendard',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    sans-serif; /* ← 전체 인풋 폰트 */
}

/* placeholder 전용 스타일 */
.chat-input::placeholder {
  font-family:
    'Pretendard',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    sans-serif;
  font-size: 12px;
  font-weight: 400;
  color: #9ca3af; /* 살짝 연한 회색 */
  letter-spacing: 0.01em;
}
</style>
