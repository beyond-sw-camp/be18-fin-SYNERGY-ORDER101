<script setup>
import apiClient from '@/components/api'
import { ref, onMounted } from 'vue'

const users = ref([])
const loading = ref(false)
const error = ref('')

// localStatus for optimistic UI updates before server API is wired
const localStatus = new Map()

function fmtDate(iso) {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toISOString().slice(0, 10)
}

const ALLOWED_ROLES = ['HQ', 'STORE_ADMIN']

async function loadUsers(page = 0, size = 20) {
  loading.value = true
  error.value = ''
  try {
    const res = await apiClient.get('/api/v1/users', { params: { page, size } })
    const data = res.data || {}
    // According to your API, users are in data.content
    const rawList = Array.isArray(data.content) ? data.content : Array.isArray(data) ? data : []

    const list = rawList.filter((u) => {
      const role = u.role || (Array.isArray(u.roles) && u.roles[0]) || ''
      return ALLOWED_ROLES.includes(role)
    })

    users.value = list

    users.value.forEach((u) => {
      localStatus.set(u.userId, u.isActive ? '활성' : '비활성화')
    })
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '사용자 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(() => loadUsers())

function getStatus(user) {
  return localStatus.get(user.userId) ?? (user.isActive ? '활성' : '비활성화')
}

async function toggleStatus(user) {
  const cur = getStatus(user)
  const next = cur === '활성' ? '비활성화' : '활성'

  const confirmMsg = next === '활성' ? '활성화 하시겠습니까?' : '비활성화 하시겠습니까?'
  if (!window.confirm(confirmMsg)) return

  // optimistic update
  const prev = cur
  localStatus.set(user.userId, next)

  try {
    const url = `/api/v1/users/${user.userId}/toggle-active`
    // server expects PATCH without body in your spec; adjust if body required
    const res = await apiClient.patch(url)

    // server returns: { code, message, items: [ user ] }
    const data = res.data || {}
    const updatedUser =
      Array.isArray(data.items) && data.items.length > 0
        ? data.items[0]
        : Array.isArray(data) && data.length > 0
          ? data[0]
          : data

    if (updatedUser && typeof updatedUser.isActive === 'boolean') {
      localStatus.set(user.userId, updatedUser.isActive ? '활성' : '비활성화')
      const idx = users.value.findIndex((u) => u.userId === user.userId)
      if (idx !== -1) users.value[idx].isActive = updatedUser.isActive
    }

    // server record updated — refresh list to keep local data consistent
    await loadUsers()
  } catch (err) {
    // rollback optimistic update
    localStatus.set(user.userId, prev)
    const msg = err.response?.data?.message || err.message || '상태 변경에 실패했습니다.'
    // show a simple alert for now
    window.alert(msg)
  }
}
</script>

<template>
  <div class="users-list">
    <header class="page-header">
      <h1>사용자 목록</h1>
    </header>

    <section class="table-card">
      <div class="table-wrap">
        <div v-if="loading" style="padding: 16px">로딩 중...</div>
        <div v-else-if="error" style="padding: 16px; color: #b91c1c">{{ error }}</div>

        <table class="users-table">
          <thead>
            <tr>
              <th>이메일</th>
              <th>이름</th>
              <th>역할</th>
              <th>상태</th>
              <th>가입 날짜</th>

              <th>동작</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!loading && users.length === 0">
              <td colspan="7" style="text-align: center; padding: 20px; color: #6b7280">
                사용자가 없습니다.
              </td>
            </tr>
            <tr v-for="user in users" :key="user.userId">
              <td class="mono">{{ user.email }}</td>
              <td>{{ user.name }}</td>
              <td>{{ user.role }}</td>
              <td>
                <span :class="['status-badge', getStatus(user) === '활성' ? 'active' : 'inactive']">
                  {{ getStatus(user) === '활성' ? '활성' : '비활성화' }}
                </span>
              </td>
              <td>{{ fmtDate(user.createdAt) }}</td>

              <td class="actions">
                <button class="more" @click="toggleStatus(user)">⋯</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 20px;
}
.page-header h1 {
  margin: 0;
  font-size: 28px;
}
.table-card {
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 12px;
  padding: 16px;
}
.table-wrap {
  overflow: auto;
}
.users-table {
  width: 100%;
  border-collapse: collapse;
}
.users-table thead th {
  text-align: left;
  padding: 18px;
  background: #fbfbfd;
  color: #6b7280;
  font-weight: 600;
}
.users-table tbody td {
  padding: 28px 18px;
  border-top: 1px solid #f3f4f6;
  vertical-align: middle;
}
.users-table tbody tr:nth-child(even) td {
  background: #fbfbfd;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, 'Roboto Mono', monospace;
}
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 12px;
}
.status-badge.active {
  background: #eef2ff;
  color: #4f46e5;
}
.status-badge.inactive {
  background: #f3f4f6;
  color: #7c3aed;
  position: relative;
}
.status-badge.inactive::before {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff7ab6;
  margin-right: 6px;
}
.actions {
  text-align: center;
}
.more {
  background: transparent;
  border: none;
  font-size: 20px;
  cursor: pointer;
}

@media (max-width: 800px) {
  .users-table thead {
    display: none;
  }
  .users-table tbody td {
    display: block;
    width: 100%;
  }
  .users-table tbody tr {
    margin-bottom: 12px;
    display: block;
    border: 1px solid #eef0f3;
    border-radius: 8px;
    overflow: hidden;
  }
}
</style>
