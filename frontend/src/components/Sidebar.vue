<script setup>
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
// SVG icon components available in the project
import IconEcosystem from './icons/IconEcosystem.vue'
import IconDocumentation from './icons/IconDocumentation.vue'
import IconTooling from './icons/IconTooling.vue'
import IconSupport from './icons/IconSupport.vue'
import IconCommunity from './icons/IconCommunity.vue'
// lucide icons for top-level menu visuals
import {
  ShoppingCart,
  Box,
  Tag,
  CreditCard,
  Users,
  Home,
  Warehouse,
  ChartPie,
} from 'lucide-vue-next'

const props = defineProps({
  sections: { type: Array, default: () => [] },
  expanded: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['toggle'])
const route = useRoute()
const router = useRouter()

const toggleSection = (id) => emit('toggle', id)
const isSectionExpanded = (id) => !!props.expanded[id]

const handleLogout = () => {
  const authStore = useAuthStore()
  authStore.logout()
  router.push('/login')
}

const iconMap = {
  ecosystem: IconEcosystem,
  documentation: IconDocumentation,
  tooling: IconTooling,
  support: IconSupport,
  community: IconCommunity,
  // mappings for app top-level sections
  shoppingCart: ShoppingCart,
  box: Box,
  tag: Tag,
  creditCard: CreditCard,
  users: Users,
  home: Home,
  warehouse: Warehouse,
  chartpie: ChartPie,
}

const getIconComponent = (name) => iconMap[name] || null
</script>

<template>
  <aside class="app-sidebar">
    <nav class="sidebar-nav">
      <div v-for="section in props.sections" :key="section.id" class="sidebar-section">
        <button
          type="button"
          class="section-title"
          :aria-expanded="isSectionExpanded(section.id)"
          @click="toggleSection(section.id)"
        >
          <span class="section-left">
            <span class="section-icon" v-if="section.icon">
              <component :is="getIconComponent(section.icon)" />
            </span>
            <span class="section-label">{{ section.title }}</span>
          </span>
          <span class="chevron" :class="{ open: isSectionExpanded(section.id) }">⌄</span>
        </button>
        <ul v-if="isSectionExpanded(section.id)">
          <li v-for="item in section.children" :key="item.path">
            <RouterLink
              :to="item.path"
              class="menu-link"
              :class="{ active: route.path === item.path }"
            >
              <span class="menu-icon" v-if="item.icon">
                <component :is="getIconComponent(item.icon)" />
              </span>
              <span class="menu-title">{{ item.title }}</span>
            </RouterLink>
          </li>
        </ul>
      </div>
    </nav>
    <button type="button" class="logout-button" @click="handleLogout">로그아웃</button>
  </aside>
</template>

<style scoped>
.app-sidebar {
  width: 260px;
  background-color: #fff;
  border-radius: 16px;
  padding: 24px 20px;
  border: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
  /* make the sidebar flush to the left edge and sit under the header */
  border-radius: 0 0 0 0;
  position: fixed;
  left: 0;
  top: var(--header-height, 72px); /* aligns under header; adjust variable if needed */
  height: calc(100vh - var(--header-height, 72px));
  overflow: auto;
  z-index: 900;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sidebar-section {
  border-bottom: 1px solid #f2f4f7;
  padding-bottom: 12px;
}
.sidebar-section:last-of-type {
  border-bottom: none;
  padding-bottom: 0;
}
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #111827;
  background: transparent;
  border: none;
  width: 100%;
  padding: 0;
  margin-bottom: 8px;
  cursor: pointer;
  font-size: 15px;
}
.section-title:focus {
  outline: 2px solid #c7d2fe;
  border-radius: 6px;
  padding: 0 4px;
}
.chevron {
  font-size: 14px;
  transition: transform 0.2s ease;
}
.chevron.open {
  transform: rotate(180deg);
}
.section-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.section-icon {
  display: inline-flex;
  width: 24px;
  height: 24px;
  color: #6b7280;
}
.section-icon svg {
  width: 24px;
  height: 24px;
}
.section-label {
  font-size: 15px;
}
.sidebar-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.menu-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 10px;
  color: #4b5563;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}
.menu-link:hover {
  background-color: #f5f7ff;
  border-color: #dbe2ff;
}
.menu-link.active {
  background-color: #eef2ff;
  border-color: #c7d2fe;
  color: #1d4ed8;
  font-weight: 600;
}
.menu-title {
  flex: 1;
}
.menu-icon {
  display: inline-flex;
  width: 18px;
  height: 18px;
  color: #6b7280;
}
.menu-icon svg {
  width: 18px;
  height: 18px;
}
.logout-button {
  margin-top: 32px;
  border: none;
  border-radius: 12px;
  padding: 12px;
  background-color: #f43f5e;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

@media (max-width: 1024px) {
  .app-sidebar {
    width: 100%;
    position: static;
    height: auto;
    overflow: visible;
    border-radius: 16px;
    padding: 24px 20px;
  }
}
</style>
