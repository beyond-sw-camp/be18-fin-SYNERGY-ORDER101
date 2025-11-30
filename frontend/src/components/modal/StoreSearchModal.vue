<script setup>
import { ref, computed, watch } from 'vue'
import { getFranchiseList } from '../api/store/StoreService'

const MAX_VISIBLE_PAGES = 5

const props = defineProps({
  isOpen: { type: Boolean, default: false },
  selectedStore: { type: Object, default: null }
})

const emit = defineEmits(['update:isOpen', 'select'])

const stores = ref([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const isLoading = ref(false)
const internalSelectedStore = ref(props.selectedStore)

const fetchStores = async (page = 1) => {
  isLoading.value = true
  try {
    const data = await getFranchiseList(page, pageSize.value, keyword.value)
    stores.value = data.franchises
    totalCount.value = data.totalCount
    currentPage.value = data.currentPage
  } catch (error) {
    console.error('가맹점 목록 조회 실패:', error)
    alert('가맹점 목록을 불러오는 데 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

const totalPages = computed(() =>
  totalCount.value > 0 ? Math.ceil(totalCount.value / pageSize.value) : 1
)

const pageNumbers = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value
  const half = Math.floor(MAX_VISIBLE_PAGES / 2)

  let start = Math.max(1, current - half)
  let end = Math.min(total, start + MAX_VISIBLE_PAGES - 1)

  if (end - start + 1 < MAX_VISIBLE_PAGES) {
    start = Math.max(1, end - MAX_VISIBLE_PAGES + 1)
  }

  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

const changePage = (newPage) => {
  if (newPage >= 1 && newPage <= totalPages.value) fetchStores(newPage)
}

const handleSearch = () => fetchStores(1)

const selectStore = (store) => {
  internalSelectedStore.value = store
}

const confirmSelection = () => {
  if (internalSelectedStore.value) emit('select', internalSelectedStore.value)
  closeModal()
}

const closeModal = () => emit('update:isOpen', false)

watch(
  () => props.isOpen,
  (newVal) => {
    if (newVal) {
      internalSelectedStore.value = props.selectedStore
      keyword.value = ''
      fetchStores(1)
    }
  }
)
</script>

<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <div class="modal-header">
        <h3>가맹점 선택</h3>
        <button @click="closeModal" class="modal-close-button">&times;</button>
      </div>

      <div class="search-area">
        <input
          v-model="keyword"
          @keyup.enter="handleSearch"
          placeholder="가맹점명 검색"
          class="search-input"
        />
        <button @click="handleSearch" class="btn-primary">조회</button>
      </div>

      <div class="store-list-container">
        <div v-if="isLoading" class="loading-spinner">목록 불러오는 중...</div>
        <table v-else class="store-table">
          <thead>
            <tr>
              <th>선택</th>
              <th>ID</th>
              <th>가맹점명</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="store in stores"
              :key="store.storeId"
              :class="{
                selected:
                  internalSelectedStore?.storeId === store.storeId
              }"
              @click="selectStore(store)"
            >
              <td>
                <input
                  type="radio"
                  :checked="
                    internalSelectedStore &&
                    internalSelectedStore.storeId === store.storeId
                  "
                  @change="selectStore(store)"
                />
              </td>
              <td>{{ store.storeId }}</td>
              <td>{{ store.storeName }}</td>
            </tr>

            <tr v-if="!isLoading && stores.length === 0">
              <td colspan="3" class="empty-data">조회된 가맹점이 없습니다.</td>
            </tr>
          </tbody>
        </table>

        <div class="pagination">
          <button class="pager" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
            ‹ Previous
          </button>

          <button
            class="page"
            v-for="page in pageNumbers"
            :key="page"
            :class="{ active: page === currentPage }"
            @click="changePage(page)"
          >
            {{ page }}
          </button>

          <button class="pager" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
            Next ›
          </button>
        </div>
      </div>

      <div class="modal-footer">
        <button @click="closeModal" class="btn-secondary">취소</button>
        <button @click="confirmSelection" class="btn-primary confirm">선택 완료</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  width: 900px; /* 더 넓은 테이블 */
  max-height: 90vh;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 20px;
  font-weight: 600;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.modal-close-button {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #6b7280;
}

.search-area {
  margin: 16px 0;
  display: flex;
  gap: 10px;
}

.search-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.btn-primary {
  background: #6366f1;
  color: white;
  border: none;
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
}

.btn-secondary {
  background: #fff;
  border: 1px solid #d1d5db;
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
}

.store-list-container {
  overflow-y: auto;
  max-height: 450px;
  border-top: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.store-table {
  width: 100%;
  border-collapse: collapse;
}

.store-table th {
  background: #f3f4f6;
  padding: 14px;
  text-align: left;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
}

.store-table td {
  padding: 14px;
  border-bottom: 1px solid #f3f4f6;
}

.store-table tr.selected {
  background: #eef2ff;
}

.loading-spinner,
.empty-data {
  text-align: center;
  padding: 24px;
  color: #9ca3af;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 18px;
}

.page,
.pager {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 6px 10px;
  cursor: pointer;
}

.page.active {
  background: #6366f1;
  color: white;
  border-color: #6366f1;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.confirm {
  background: #6366f1;
  color: white;
}
</style>