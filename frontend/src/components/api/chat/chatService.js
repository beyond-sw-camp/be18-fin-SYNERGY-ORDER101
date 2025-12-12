// src/components/api/chat/chatService.js

import apiClient from '..'

// 1:1 채팅방 생성 or 기존 방 ID 반환
export async function createOrGetPrivateRoom(otherNickname) {
  const res = await apiClient.post('/api/v1/chatrooms/private/create', null, {
    params: { otherNickname },
  })

  const body = res.data ?? {}
  const items = Array.isArray(body.items) ? body.items : []

  // BaseResponseDto(HttpStatus, roomId) → items[0] = roomId
  return items.length > 0 ? items[0] : null
}

// 채팅 히스토리 조회
export async function getChatHistory(roomId) {
  const res = await apiClient.get(`/api/v1/chatrooms/history/${roomId}`)

  const body = res.data ?? {}
  const items = Array.isArray(body.items) ? body.items : []

  // items: [{ senderName, message }, ...]
  return items
}

// 읽음 처리
export async function markChatRead(roomId) {
  await apiClient.post(`/api/v1/chatrooms/${roomId}/read`)
}
