<script setup>
import apiClient from '@/components/api'
import { ref, onMounted, computed } from 'vue'

const stores = ref([])
const loading = ref(false)
const error = ref('')

// Pagination / filter state
const page = ref(1)
const size = ref(15)
const keyword = ref('')
const totalCount = ref(0)

// address filter
const addresses = ref([])
const selectedAddress = ref('')
const searchName = ref('')

const localStatus = new Map()

function fmtDate(iso) {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toISOString().slice(0, 10)
}

async function loadStores(requestPage = page.value) {
  loading.value = true
  error.value = ''
  try {
    const params = { page: requestPage, size: size.value }
    if (searchName.value) params.store_name = searchName.value
    if (selectedAddress.value) params.address = selectedAddress.value

    const res = await apiClient.get('/api/v1/stores/search', { params })
    const data = res.data || {}
    const list = Array.isArray(data.items) ? data.items : Array.isArray(data) ? data : []

    // update reactive state from server response
    stores.value = list
    page.value = typeof data.page === 'number' ? data.page : requestPage
    totalCount.value = typeof data.totalCount === 'number' ? data.totalCount : list.length
    size.value = typeof data.numOfRows === 'number' ? data.numOfRows : size.value

    stores.value.forEach((s) => {
      localStatus.set(s.storeId, s.isActive ? '활성' : '비활성화')
    })
  } catch (err) {
    console.error('loadStores error', err)
    error.value = err.response?.data?.message || err.message || '가맹점 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / size.value)))

function goToPage(n) {
  if (n < 1) n = 1
  if (n > totalPages.value) n = totalPages.value
  if (n === page.value) return
  loadStores(n)
}

async function loadAddresses() {
  try {
    const res = await apiClient.get('/api/v1/stores/addresses')
    const data = res.data || {}
    addresses.value = Array.isArray(data.items) ? data.items : []
  } catch (err) {
    console.error('loadAddresses error', err)
  }
}

onMounted(async () => {
  await loadAddresses()
  await loadStores(0)
})

function onSearch() {
  page.value = 1
  loadStores(0)
}

function clearFilters() {
  selectedAddress.value = ''
  searchName.value = ''
  page.value = 1
  loadStores(0)
}

function getStatus(store) {
  return localStatus.get(store.storeId) ?? (store.isActive ? '활성' : '비활성화')
}

async function toggleStatus(store) {
  const cur = getStatus(store)
  const next = cur === '활성' ? '비활성화' : '활성'

  const confirmMsg =
    next === '활성' ? '이 가맹점을 활성화 하시겠습니까?' : '이 가맹점을 비활성화 하시겠습니까?'
  if (!window.confirm(confirmMsg)) return

  const prev = cur
  localStatus.set(store.storeId, next)

  try {
    const url = `/api/v1/stores/${store.storeId}/toggle-active`
    const res = await apiClient.patch(url)
    const data = res.data || {}
    const updated = Array.isArray(data.items) && data.items.length > 0 ? data.items[0] : data
    if (updated && typeof updated.isActive === 'boolean') {
      localStatus.set(store.storeId, updated.isActive ? '활성' : '비활성화')
      const idx = stores.value.findIndex((s) => s.storeId === store.storeId)
      if (idx !== -1) stores.value[idx].isActive = updated.isActive
    }
    await loadStores()
  } catch (err) {
    localStatus.set(store.storeId, prev)
    const msg = err.response?.data?.message || err.message || '상태 변경에 실패했습니다.'
    window.alert(msg)
    console.error('toggleStatus error', err)
  }
}
</script>

<template>
  <div class="stores-list">
    <header class="page-header">
      <h1>가맹점 목록</h1>
    </header>

    <section class="table-card">
      <div class="table-wrap">
        <div
          class="filters"
          style="display: flex; gap: 12px; align-items: center; margin-bottom: 12px"
        >
          <label style="display: flex; gap: 8px; align-items: center">
            <span style="color: #6b7280">주소</span>
            <select v-model="selectedAddress">
              <option value="">전체</option>
              <option v-for="addr in addresses" :key="addr" :value="addr">{{ addr }}</option>
            </select>
          </label>
          <label style="display: flex; gap: 8px; align-items: center">
            <span style="color: #6b7280">가맹점명</span>
            <input
              v-model="searchName"
              @keyup.enter="onSearch"
              type="text"
              placeholder="검색..."
              style="
                padding: 6px 10px;
                border: 1px solid #e5e7eb;
                border-radius: 6px;
                outline: none;
              "
            />
          </label>
          <button
            @click="onSearch"
            style="
              background: #4f46e5;
              color: white;
              border: none;
              padding: 6px 12px;
              border-radius: 6px;
              cursor: pointer;
              font-weight: 500;
            "
          >
            검색
          </button>
          <button
            @click="clearFilters"
            style="
              background: #f3f4f6;
              border: 1px solid #e5e7eb;
              padding: 6px 12px;
              border-radius: 6px;
              cursor: pointer;
            "
          >
            초기화
          </button>
        </div>
        <div v-if="loading" style="padding: 16px">로딩 중...</div>
        <div v-else-if="error" style="padding: 16px; color: #b91c1c">{{ error }}</div>

        <table class="stores-table">
          <thead>
            <tr>
              <th>코드</th>
              <th>가맹점명</th>
              <th>주소</th>
              <th>연락처</th>
              <th>상태</th>
              <th>생성일</th>
              <th>동작</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!loading && stores.length === 0">
              <td colspan="7" style="text-align: center; padding: 20px; color: #6b7280">
                가맹점이 없습니다.
              </td>
            </tr>
            <tr v-for="store in stores" :key="store.storeId">
              <td class="mono">{{ store.storeCode }}</td>
              <td>{{ store.storeName }}</td>
              <td>{{ store.address }}</td>
              <td class="mono">{{ store.contactNumber }}</td>
              <td>
                <span
                  :class="['status-badge', getStatus(store) === '활성' ? 'active' : 'inactive']"
                >
                  {{ getStatus(store) === '활성' ? '활성' : '비활성화' }}</span
                >
              </td>
              <td>{{ fmtDate(store.createdAt) }}</td>
              <td class="actions">
                <button class="more" @click="toggleStatus(store)">⋯</button>
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
.stores-table {
  width: 100%;
  border-collapse: collapse;
}
.stores-table thead th {
  text-align: left;
  padding: 18px;
  background: #fbfbfd;
  color: #6b7280;
  font-weight: 600;
}
.stores-table tbody td {
  padding: 28px 18px;
  border-top: 1px solid #f3f4f6;
  vertical-align: middle;
}
.stores-table tbody tr:nth-child(even) td {
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
  .stores-table thead {
    display: none;
  }
  .stores-table tbody td {
    display: block;
    width: 100%;
  }
  .stores-table tbody tr {
    margin-bottom: 12px;
    display: block;
    border: 1px solid #eef0f3;
    border-radius: 8px;
    overflow: hidden;
  }
}
</style>
