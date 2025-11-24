<script setup>
import { RouterLink } from 'vue-router'
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

// use the provided flaticon image URL
const notificationIcon = 'https://cdn-icons-png.flaticon.com/512/3119/3119338.png'
import logoUrl from '../assets/logo.png'
const props = defineProps({
  currentRole: { type: String, required: true },
  roleOptions: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:currentRole'])

const updateRole = (e) => emit('update:currentRole', e.target.value)

const router = useRouter()

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
}

onMounted(() => document.addEventListener('click', onDocumentClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocumentClick))
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <RouterLink to="/" class="brand">
        <div><img :src="logoUrl" alt="ORDER 101" class="logo-image" /></div>
      </RouterLink>
      <div class="header-tools">
        <div class="header-actions">
          <button type="button" class="notification-button" aria-label="알림">
            <span class="notification-content">
              <img :src="notificationIcon" alt="Notification" class="notification-icon" />

              <span class="notification-label">알림</span>
            </span>
            <span class="badge">3</span>
          </button>
          <div class="avatar-wrap">
            <button
              ref="avatarBtnRef"
              type="button"
              class="avatar"
              @click="toggleProfileMenu"
              aria-haspopup="true"
              :aria-expanded="showProfileMenu"
            >
              SY
            </button>
            <div v-if="showProfileMenu" ref="profileMenuRef" class="profile-menu" role="menu">
              <button type="button" class="profile-action" @click="goToMyPage">내 정보</button>
              <button type="button" class="profile-action logout" @click="handleLogoutClick">
                로그아웃
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
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
</style>
