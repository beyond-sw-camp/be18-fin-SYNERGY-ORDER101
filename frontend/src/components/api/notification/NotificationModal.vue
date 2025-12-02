<template>
  <div class="noti-modal">
    <div class="noti-modal-header">
      <div class="title">알림</div>

      <button v-if="items.length > 0" class="btn-clear-all" @click="emit('clear-all')">
        모두 삭제
      </button>
    </div>

    <div ref="scrollBox" class="noti-scroll" @scroll="onScroll">
      <div v-if="items.length === 0" class="noti-empty">알림이 없습니다.</div>

      <ul v-else class="noti-list">
        <li v-for="n in items" :key="n.notificationId ?? n.id">
          <div class="noti-item" :class="{ unread: !(n.readAt || n.isRead) }">
            <div class="dot" v-if="!(n.readAt || n.isRead)"></div>

            <div class="body">
              <div class="row">
                <div class="noti-title">
                  {{ n.title || n.type || '알림' }}
                </div>
                <div class="noti-time">
                  {{ formatTime(n.createdAt || n.createdDate) }}
                </div>
              </div>
              <div class="noti-content">
                {{ n.message || n.content || n.body }}
              </div>
            </div>

            <button
              class="delete"
              @click="emit('delete', n.notificationId ?? n.id)"
              aria-label="알림 삭제"
            >
              ×
            </button>
          </div>
        </li>
      </ul>
      <div v-if="loading" class="noti-loading">더 불러오는 중...</div>
      <div v-else-if="!hasMore && items.length > 0" class="noti-end">모든 알림을 불러왔습니다.</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  hasMore: { type: Boolean, default: false },
})

const emit = defineEmits(['delete', 'load-more', 'clear-all'])

const scrollBox = ref(null)

const formatTime = (v) => {
  if (!v) return ''
  const d = new Date(v)
  if (isNaN(d.getTime())) return v
  return d.toLocaleString()
}

const onScroll = () => {
  const el = scrollBox.value
  if (!el || !props.hasMore || props.loading) return

  const threshold = 40 // 바닥에서 40px 이내면 다음 페이지 요청
  const reachedBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - threshold

  if (reachedBottom) {
    emit('load-more')
  }
}
</script>

<style scoped>
.noti-scroll {
  max-height: 360px; /* 모달 높이 제한 */
  overflow-y: auto;
  padding: 8px 0;
}

.noti-loading,
.noti-end {
  text-align: center;
  padding: 8px 0 12px;
  font-size: 13px;
  color: #9ca3af;
}
.noti-modal {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  width: 360px;
  max-height: 520px;
  background: #fff;
  border: 1px solid #e6e9f2;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
  overflow: hidden;
  z-index: 1200;
}
.noti-modal-header {
  padding: 12px 14px;
  border-bottom: 1px solid #f1f3f7;
  font-weight: 800;
}
.noti-empty {
  padding: 40px 0;
  text-align: center;
  color: #6b7280;
}
.noti-list {
  list-style: none;
  margin: 0;
  padding: 6px;
}
.noti-item {
  position: relative;
  display: flex;
  gap: 8px;
  padding: 12px;
  border-radius: 10px;
}
.noti-item:hover {
  background: #f7f8fb;
}
.noti-item.unread {
  background: #f5f7ff;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  margin-top: 7px;
  flex: 0 0 auto;
}
.body {
  flex: 1;
  min-width: 0;
}
.row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}
.noti-title {
  font-weight: 700;
  font-size: 14px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.noti-time {
  font-size: 12px;
  color: #6b7280;
  flex: 0 0 auto;
}
.noti-content {
  margin-top: 4px;
  font-size: 13px;
  color: #374151;
  line-height: 1.35;
  word-break: break-word;
}
.delete {
  border: none;
  background: transparent;
  font-size: 18px;
  color: #9ca3af;
  cursor: pointer;
  padding: 0 4px;
}
.delete:hover {
  color: #ef4444;
}
.btn-clear-all {
  border: none;
  background: transparent;
  color: #9ca3af;
  font-size: 12px;
  padding: 4px 6px;
  cursor: pointer;
}
.btn-clear-all:hover {
  color: #6b7280;
  text-decoration: underline;
}
</style>
