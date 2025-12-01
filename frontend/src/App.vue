<script setup>
import { computed, ref, watch } from 'vue'
import { computed as vueComputed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import Header from './components/Header.vue'
import Sidebar from './components/Sidebar.vue'
import { useAuthStore } from './stores/authStore'

// instantiate auth store
const auth = useAuthStore()

const roleOptions = [
  { label: 'HQ', value: 'HQ' },
  { label: 'HQ Admin', value: 'HQ_ADMIN' },
  { label: 'Store Admin', value: 'STORE_ADMIN' },
]

const adminSidebar = [
  {
    id: 'orders',
    title: '발주 관리',
    children: [
      { title: '발주서 생성', path: '/hq/orders/create' },
      { title: '일반 발주 현황', path: '/hq/orders/status' },
      { title: '스마트 발주 현황', path: '/hq/smart-orders' },
      { title: '자동 발주 현황', path: '/hq/orders/auto/status' },
      { title: '발주 승인', path: '/hq/orders/approval' },
      { title: '공급사 관리', path: '/hq/orders/vendors' },
    ],
  },
  {
    id: 'warehouse',
    title: '창고 관리',
    children: [
      { title: '재고 상태', path: '/hq/inventory/stock' },
      { title: '입출고 조회', path: '/hq/inventory/movements' },
    ],
  },
  {
    id: 'product',
    title: '상품 관리',
    children: [
      { title: '상품 목록', path: '/hq/product/list' },
      { title: '상품 등록', path: '/hq/product/register' },
    ],
  },
  {
    id: 'settlement',
    title: '정산 관리',
    children: [
      { title: '정산 목록', path: '/hq/settlement/list' },
      //{ title: '일일 정산', path: '/hq/settlement/daily' },
      { title: '정산 리포트', path: '/hq/settlement/report' },
    ],
  },
  {
    id: 'users',
    title: '사용자 관리',
    children: [
      { title: '사용자 목록', path: '/hq/users/' },
      { title: '사용자 등록', path: '/hq/users/registration' },
    ],
  },
  {
    id: 'franchise',
    title: '가맹점 관리',
    children: [
      { title: '가맹점 등록', path: '/hq/franchise/registration' },
      { title: '가맹점 주문 승인', path: '/hq/franchise/approval' },
      { title: '가맹점 주문 조회', path: '/hq/franchise/orders' },
      { title: '배송 관리', path: '/hq/franchise/delivery' },
    ],
  },
  {
    id: 'dashboard',
    title: '대시보드',
    children: [
      { title: '수요 예측', path: '/hq/dashboard/forecast' },
      { title: '주문 대시보드', path: '/hq/dashboard/order' },
    ],
  },
]

const storeSidebar = [
  {
    id: 'store-orders',
    title: '발주 관리',
    children: [
      { title: '발주 생성', path: '/store/purchase/create' },
      { title: '발주 목록', path: '/store/purchase/list' },
    ],
  },
  {
    id: 'store-inventory',
    title: '재고 관리',
    children: [{ title: '재고 조회', path: '/store/inventory/stock' }],
  },
]

const router = useRouter()
const route = useRoute()
function setRole(v) {
  // store role in userInfo.type for now
  auth.userInfo.type = v
  // also keep role and roles[] in sync for other consumers
  auth.userInfo.role = v
  auth.userInfo.roles = v ? [v] : []
}

const currentRole = computed({
  get: () => {
    // prefer explicit role, then type, otherwise first role in roles array
    return (
      auth.userInfo.role ||
      auth.userInfo.type ||
      (auth.userInfo.roles && auth.userInfo.roles[0]) ||
      ''
    )
  },
  set: (v) => setRole(v),
})
const expandedSections = ref({})
const isAuthRoute = computed(() => {
  // hide header/sidebar for login or other auth-only pages
  return route.name === 'login' || route.path === '/login'
})

const isHQ = computed(() => currentRole.value === 'HQ')

const sidebarSections = computed(() => {
  if (currentRole.value === 'STORE_ADMIN') {
    return storeSidebar
  }

  if (isHQ.value) {
    return adminSidebar.filter((section) => section.id !== 'users')
  }

  return adminSidebar
})

const isStoreRole = computed(() => currentRole.value === 'STORE_ADMIN')
const isHqAdmin = computed(() => currentRole.value === 'HQ_ADMIN')

// HQ_ADMIN 전용 메뉴를 필터링한 사이드바
const filteredAdminSidebar = computed(() => {
  return adminSidebar.map(section => {
    if (section.id === 'orders') {
      return {
        ...section,
        children: section.children.filter(child => {
          // 발주 승인 메뉴는 HQ_ADMIN만 볼 수 있음
          if (child.path === '/hq/orders/approval') {
            return isHqAdmin.value
          }
          return true
        })
      }
    }
    return section
  })
})

const setDefaultExpanded = () => {
  const sections = sidebarSections.value
  if (!sections.length) return
  const defaults = {}
  sections.forEach((section, index) => {
    defaults[section.id] = index === 0
  })
  expandedSections.value = defaults
}

watch(
  currentRole,
  (role) => {
    const defaultPath = role === 'STORE_ADMIN' ? '/store/dashboard' : '/hq/dashboard'
    const targetPrefix = role === 'STORE_ADMIN' ? '/store' : '/hq'
    // if (!route.path.startsWith(targetPrefix)) {
    //   router.push(defaultPath)
    // }
  },
  { immediate: true },
)

watch(
  sidebarSections,
  () => {
    setDefaultExpanded()
  },
  { immediate: true },
)

watch(
  () => route.path,
  (path) => {
    const activeSection = sidebarSections.value.find((section) =>
      section.children.some((child) => child.path === path),
    )
    if (activeSection) {
      expandedSections.value = {
        ...expandedSections.value,
        [activeSection.id]: true,
      }
    }
  },
  { immediate: true },
)

const toggleSection = (sectionId) => {
  expandedSections.value = {
    ...expandedSections.value,
    [sectionId]: !expandedSections.value[sectionId],
  }
}

const isSectionExpanded = (sectionId) => !!expandedSections.value[sectionId]

// reactive loggedIn computed as a safety-net (mirrors router guard logic)
const loggedIn = vueComputed(() => {
  const token = auth.userInfo && auth.userInfo.accessToken
  const expiresAtRaw = auth.userInfo && auth.userInfo.expiresAt
  const expiresAt = expiresAtRaw ? Number(expiresAtRaw) : 0
  return !!(token && expiresAt > Date.now())
})

// Ensure UI redirects to login if unauthenticated, or redirects away from login when authenticated.
watch(
  loggedIn,
  (val) => {
    if (!val) {
      // if user is not logged in and not already on login, navigate to login
      if (route.name !== 'login') router.push({ name: 'login' })
    } else {
      // logged in: if currently on login, send to role dashboard
      if (route.name === 'login' || route.path === '/login') {
        const role =
          auth.userInfo &&
          (auth.userInfo.role ||
            auth.userInfo.type ||
            (auth.userInfo.roles && auth.userInfo.roles[0]))
        if (role === 'STORE_ADMIN') router.push({ name: 'store-dashboard' })
        else router.push({ name: 'hq-dashboard' })
      }
    }
  },
  { immediate: true },
)
</script>

<template>
  <div>
    <!-- If route is an auth page (login), render only the view without header/sidebar -->
    <Header
      v-if="!isAuthRoute"
      :currentRole="currentRole"
      :roleOptions="roleOptions"
      @update:currentRole="(val) => (currentRole.value = val)"
    />

    <div v-if="!isAuthRoute" class="app-shell">
      <div class="app-body" :class="sidebarPlacementClass">
        <!-- Sidebar moved into its own component -->
        <Sidebar :sections="sidebarSections" :expanded="expandedSections" @toggle="toggleSection" />

        <section class="app-content">
          <RouterView />
        </section>
      </div>
    </div>

    <!-- Auth routes (login) render here without header/sidebar -->
    <div v-else class="auth-route">
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  /* leave space on the left for the fixed sidebar (260px) + standard page padding (24px) */
  /* remove top padding so content sits flush under the sticky header */
  padding: 0 0 0 calc(260px);
  min-height: 100vh;
  background-color: #fff;
  color: #1f2933;
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
  flex-wrap: wrap;
  justify-content: flex-end;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid #e5e7eb;
  background-color: #fff;
  cursor: pointer;
  font-size: 16px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #1f2933;
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
  background-color: #fff;
}

.app-body {
  display: flex;
  gap: 24px;
}

.app-body.sidebar-right {
  flex-direction: row-reverse;
}

.app-content {
  flex: 1;
  padding: 24px;
  background-color: #fff;
}

/* Sidebar and header styles moved into their own components */

@media (max-width: 1024px) {
  .app-header {
    flex-direction: column;
    align-items: stretch;
  }

  .app-body,
  .app-body.sidebar-right {
    flex-direction: column;
  }

  /* On smaller screens the sidebar becomes part of the flow again and we should remove the left padding */
  .app-shell {
    padding: 24px;
  }

  .app-sidebar {
    width: 100%;
  }
}
</style>
