<script setup>
import { RouterLink } from 'vue-router'
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { useNotificationStore } from './api/notification/notification'
import NotificationModal from './api/notification/NotificationModal.vue'
import { computed } from 'vue'
import ChatModal from '@/views/hq/chat/ChatModal.vue'

// use the provided flaticon image URL
const notificationIcon = 'https://cdn-icons-png.flaticon.com/512/3119/3119338.png'
const notiStore = useNotificationStore()

import logoUrl from '../assets/logo.png'

const loading = computed(() => notiStore.loadingPage)
const hasMore = computed(() => notiStore.hasMore)

const props = defineProps({
  currentRole: { type: String, required: true },
  roleOptions: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:currentRole'])

const updateRole = (e) => emit('update:currentRole', e.target.value)

const router = useRouter()
const authStore = useAuthStore()

const avatarLabel = computed(() => {
  const name = authStore.userInfo && authStore.userInfo.name ? authStore.userInfo.name : ''
  if (!name) return 'SY'
  return name.length > 2 ? name.slice(-2) : name
})

const avatarStyle = computed(() => {
  const gradient = 'linear-gradient(135deg, #7c3aed 0%, #06b6d4 100%)'
  return {
    background: gradient,
    color: '#fff',
  }
})

const showNotiMenu = ref(false)
const notiBtnRef = ref(null)
const notiMenuRef = ref(null)

// avatar dropdown state
const showProfileMenu = ref(false)
const avatarBtnRef = ref(null)
const profileMenuRef = ref(null)

const toggleProfileMenu = () => {
  showProfileMenu.value = !showProfileMenu.value
}

const goToMyPage = () => {
  showProfileMenu.value = false
  router.push('/mypage')
}

const handleLogoutClick = () => {
  showProfileMenu.value = false
  showNotiMenu.value = false
  notiStore.reset()
  const authStore = useAuthStore()
  authStore.logout()
  router.push('/login')
}

const onDocumentClick = (e) => {
  const btn = avatarBtnRef.value
  const menu = profileMenuRef.value
  if (!btn || !menu) return
  if (btn.contains(e.target) || menu.contains(e.target)) return
  showProfileMenu.value = false

  const nbtn = notiBtnRef.value
  const nmenu = notiMenuRef.value
  if (nbtn && nmenu && !nbtn.contains(e.target) && !nmenu.contains(e.target)) {
    showNotiMenu.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  
  //  새로고침/창닫기 시 SSE 연결 정리
  const handleBeforeUnload = () => {
    notiStore.disconnectSSE()
  }
  window.addEventListener('beforeunload', handleBeforeUnload)
  
  notiStore.init()
})

const toggleNotiMenu = async () => {
  showNotiMenu.value = !showNotiMenu.value

  // 모달 열 때 읽지 않은게 있으면 "전체 읽음" 처리 → 뱃지 사라짐

  await notiStore.markAllRead()
}

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  notiStore.disconnectSSE()
})

const handleLoadMore = () => {
  notiStore.loadMore()
}
const handleClearAll = async () => {
  if (!confirm('모든 알림을 삭제하시겠습니까?')) return
  await notiStore.clearAll()
}

// ===== 채팅 모달 상태 =====
const showChatModal = ref(false)
const chatTargetNickname = ref('')

// 역할 플래그 (헤더는 currentRole prop으로 전달받음)
const isHQ = computed(() => props.currentRole === 'HQ')
const isHqAdmin = computed(() => props.currentRole === 'HQ_ADMIN')
const isStoreAdmin = computed(() => props.currentRole === 'STORE_ADMIN')

// 나중에 백엔드에서 store_id 기준으로 상대 찾아서 userInfo에 심어둔다고 가정
// 예: STORE_ADMIN → 담당 HQ 이름, HQ → 담당 점주 이름
const assignedHqName = computed(() => authStore.userInfo?.hqName || '')
const assignedStoreOwnerName = computed(() => authStore.userInfo?.storeOwnerName || '')
// 위 둘은 필요에 따라 필드명 맞춰서 사용하면 됨. (지금은 예시)

// 가맹점주: 본사 문의하기
// 가맹점주 → HQ(조상원)에게 1:1 채팅
const openChatToHq = () => {
  chatTargetNickname.value = '조상원' // DB의 name 컬럼 그대로!
  showChatModal.value = true
}

// HQ → 점주(윤석현)에게 1:1 채팅
const openChatToStoreOwner = () => {
  chatTargetNickname.value = '윤석현' // DB의 name 컬럼 그대로!
  showChatModal.value = true
}

const closeChatModal = () => {
  showChatModal.value = false
}
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <RouterLink to="/" class="brand">
        <div><img :src="logoUrl" alt="ORDER 101" class="logo-image" /></div>
      </RouterLink>
      <div class="header-tools">
        <div class="header-actions">
          <!--  가맹점주: 본사 문의하기 버튼 -->
          <button v-if="isStoreAdmin" type="button" class="chat-button" @click="openChatToHq">
            본사 문의하기
          </button>

          <!--  HQ/HQ_ADMIN: 점주랑 대화하기 버튼 -->
          <button
            v-if="isHQ || isHqAdmin"
            type="button"
            class="chat-button"
            @click="openChatToStoreOwner"
          >
            점주랑 대화하기
          </button>
          <div class="noti-wrap">
            <button
              ref="notiBtnRef"
              type="button"
              class="notification-button"
              aria-label="알림"
              @click.stop="toggleNotiMenu"
            >
              <span class="notification-content">
                <img :src="notificationIcon" alt="Notification" class="notification-icon" />
                <span class="notification-label">알림</span>
              </span>

              <!-- 알림 배지 -->
              <span v-if="notiStore.unreadCount > 0" class="badge">
                {{ notiStore.unreadCount }}
              </span>
            </button>

            <!-- 드롭다운 모달 -->
            <div v-if="showNotiMenu" ref="notiMenuRef">
              <NotificationModal
                :items="notiStore.notifications"
                :loading="loading"
                :hasMore="hasMore"
                @delete="notiStore.deleteNotification"
                @load-more="handleLoadMore"
                @clear-all="handleClearAll"
              />
            </div>
          </div>
          <div class="avatar-wrap">
            <button
              ref="avatarBtnRef"
              type="button"
              class="avatar"
              @click="toggleProfileMenu"
              aria-haspopup="true"
              :aria-expanded="showProfileMenu"
              :style="avatarStyle"
            >
              {{ avatarLabel }}
            </button>
            <div v-if="showProfileMenu" ref="profileMenuRef" class="profile-menu" role="menu">
              <button type="button" class="profile-action logout" @click="handleLogoutClick">
                로그아웃
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    <ChatModal
      v-if="chatTargetNickname"
      :visible="showChatModal"
      :otherNickname="chatTargetNickname"
      :title="isStoreAdmin ? '본사 문의하기' : '점주와의 1:1 채팅'"
      @close="closeChatModal"
    />
  </header>
</template>

<style scoped>
.logo-image {
  height: 36px;
  object-fit: cover;
}
.app-header {
  position: sticky;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  padding: 0; /* header itself has no padding so it sits flush to viewport */
}

.header-inner {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  background-color: #fff;
  border-bottom: 1px solid #ebeef5;

  align-items: center;
  padding: 16px 24px; /* internal padding preserved for content spacing */
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
}

.logo-mark {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4c6fff, #8a7dff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 20px;
}

.brand-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 2px;
}
.brand-subtitle {
  font-size: 13px;
  color: #6b7280;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: flex-end;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.notification-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  color: #374151;
  font-weight: 600;
}
.notification-button:hover {
  background: #f5f7ff;
  border-color: #dbe2ff;
}
.notification-content {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.notification-icon {
  width: 18px;
  height: 18px;
  color: #1f2933;
}
.notification-label {
  font-size: 14px;
  color: #111827;
}
.badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: #ef4444;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.15);
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #1f2933;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}
.role-select {
  display: flex;
  flex-direction: column;
  font-size: 12px;
  color: #6b7280;
}
.role-select select {
  margin-top: 4px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  padding: 6px 12px;
  background: #fff;
}

/* profile dropdown */
.profile-menu {
  position: absolute;
  right: 0; /* position relative to avatar-wrap */
  top: calc(100% + 8px);
  background: #fff;
  border: 1px solid #e6e9f2;
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.08);
  padding: 8px;
  display: flex;
  flex-direction: column;
  min-width: 140px;
  z-index: 1100;
}
.avatar-wrap {
  position: relative;
  display: inline-block;
}
.profile-action {
  background: transparent;
  border: none;
  text-align: left;
  padding: 10px 12px;
  font-size: 14px;
  color: #111827;
  cursor: pointer;
}
.profile-action:hover {
  background: #f5f7ff;
}
.profile-action.logout {
  color: #ef4444;
  font-weight: 700;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #1f2933;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  cursor: pointer;
  border: none;
}

.notification-img {
  width: 18px;
  height: 18px;
  display: inline-block;
}
.noti-wrap {
  position: relative;
  display: inline-block;
}

.chat-button {
  margin-right: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  font-size: 13px;
  cursor: pointer;
  transition:
    background 0.15s ease,
    box-shadow 0.15s ease;
}

.chat-button:hover {
  background: #f3f4f6;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1);
}
</style>
