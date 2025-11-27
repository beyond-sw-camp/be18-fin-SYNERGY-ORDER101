<template>
  <div class="noti-modal">
    <div class="noti-modal-header">
      <div class="title">알림</div>
    </div>

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
  </div>
</template>

<script setup>
const props = defineProps({
  items: { type: Array, default: () => [] },
})

const emit = defineEmits(['delete'])

const formatTime = (v) => {
  if (!v) return ''
  const d = new Date(v)
  if (isNaN(d.getTime())) return v
  return d.toLocaleString()
}
</script>

<style scoped>
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
  overflow-y: auto;
  max-height: 470px;
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
</style>
