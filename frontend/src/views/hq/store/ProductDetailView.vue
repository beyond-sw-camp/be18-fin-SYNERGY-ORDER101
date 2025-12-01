<template>
  <div class="product-detail">
    <header class="page-header">
      <h1>상품 상세 정보</h1>
      <div class="actions">
        <!-- 보기 모드 -->
        <template v-if="!isEditMode">
          <button class="btn-edit" @click="startEdit">편집</button>
        </template>

        <!-- 수정 모드 -->
        <template v-else>
          <button class="btn-edit" @click="saveEdit">저장</button>
          <button class="btn-delete" @click="cancelEdit">취소</button>
        </template>
      </div>
    </header>

    <!-- 로딩 -->
    <div v-if="isLoading" class="card loading">불러오는 중...</div>

    <!-- 에러 -->
    <div v-else-if="errorMsg" class="card error">
      {{ errorMsg }}
    </div>

    <div v-else-if="product">
      <div class="tabs">
        <button
          :class="['tab', activeTab === 'basic' ? 'active' : '']"
          @click="activeTab = 'basic'"
        >
          기본 정보
        </button>
        <button
          :class="['tab', activeTab === 'stock' ? 'active' : '']"
          @click="activeTab = 'stock'"
        >
          재고 현황
        </button>
      </div>

      <section v-if="activeTab === 'basic'" class="card basic-card">
        <div class="content-grid">
          <!-- LEFT -->
          <div class="basic-left">
            <!-- 제품명 -->
            <div class="field">
              <label>제품명</label>
              <div class="name-row">
                <template v-if="!isEditMode">
                  <div class="value name">{{ product.productName }}</div>
                </template>
                <template v-else>
                  <input class="value name" v-model="editForm.productName" />
                </template>

                <span class="chip" v-if="product.status">활성화</span>
                <span class="chip2" v-else>비활성화</span>
              </div>
            </div>

            <!-- 상품코드 -->
            <!-- 상품코드: 항상 읽기 전용 -->
            <div class="field">
              <label>상품 코드</label>
              <div class="value readonly">
                {{ product.productCode }}
              </div>
            </div>

            <!-- 카테고리 3단 셀렉트 -->
            <div class="category-row">
              <div class="field">
                <label>대분류</label>
                <template v-if="!isEditMode">
                  <div class="value">{{ product.categoryLargeName }}</div>
                </template>
                <template v-else>
                  <select
                    class="value select-value"
                    v-model="selectedLargeId"
                    @change="onLargeChange"
                  >
                    <option :value="null">선택</option>
                    <option v-for="c in largeCategories" :key="c.id" :value="c.id">
                      {{ c.name }}
                    </option>
                  </select>
                </template>
              </div>

              <div class="field">
                <label>중분류</label>
                <template v-if="!isEditMode">
                  <div class="value">{{ product.categoryMediumName }}</div>
                </template>
                <template v-else>
                  <select
                    class="value select-value"
                    v-model="selectedMediumId"
                    @change="onMediumChange"
                    :disabled="!selectedLargeId"
                  >
                    <option :value="null">선택</option>
                    <option v-for="c in mediumCategories" :key="c.id" :value="c.id">
                      {{ c.name }}
                    </option>
                  </select>
                </template>
              </div>

              <div class="field">
                <label>소분류</label>
                <template v-if="!isEditMode">
                  <div class="value">{{ product.categorySmallName }}</div>
                </template>
                <template v-else>
                  <select
                    class="value select-value"
                    v-model="selectedSmallId"
                    :disabled="!selectedMediumId"
                  >
                    <option :value="null">선택</option>
                    <option v-for="c in smallCategories" :key="c.id" :value="c.id">
                      {{ c.name }}
                    </option>
                  </select>
                </template>
              </div>
            </div>

            <!-- 공급가 -->
            <div class="field">
              <label>공급가</label>
              <template v-if="!isEditMode">
                <div class="value"><Money :value="product.price" /></div>
              </template>
              <template v-else>
                <input class="value" type="number" v-model.number="editForm.price" />
              </template>
            </div>

            <!-- 설명 -->
            <div class="field">
              <label>제품 설명</label>
              <template v-if="!isEditMode">
                <div class="value description">{{ product.description }}</div>
              </template>
              <template v-else>
                <textarea class="value description" v-model="editForm.description"></textarea>
              </template>
            </div>

            <!-- 상태 -->
            <div class="field" v-if="isEditMode">
              <label>상태</label>
              <select class="value" v-model="editForm.status">
                <option :value="true">활성</option>
                <option :value="false">비활성</option>
              </select>
            </div>
          </div>

          <!-- RIGHT (이미지) -->
          <div class="image-col">
            <div class="image-box">
              <template v-if="!isEditMode">
                <img v-if="imageSrc" :src="imageSrc" alt="product image" />
                <div v-else class="no-image">이미지 없음</div>
              </template>

              <template v-else>
                <img
                  v-if="newImagePreview || imageSrc"
                  :src="newImagePreview || imageSrc"
                  alt="product image"
                />
                <div v-else class="no-image">이미지 없음</div>
              </template>
            </div>

            <!-- ✅ 박스 밖에 컨트롤을 둔다 -->
            <div v-if="isEditMode" class="image-actions">
              <input type="file" accept="image/*" @change="onPickImage" />
              <button type="button" class="btn-edit" @click="removeImage">이미지 삭제</button>
            </div>
          </div>
        </div>
      </section>

      <section v-if="activeTab === 'stock'" class="card stock-card">
        <!-- 상단 요약 박스 -->
        <div class="stock-header">
          <div class="stock-title">
            <div class="code">{{ inventorySummary?.productCode || product.productCode }}</div>
            <div class="name-lg">{{ inventorySummary?.productName || product.productName }}</div>
          </div>

          <!-- ✅ 카드 2개만 (입고 예정 삭제) -->
          <div class="stock-cards">
            <div class="stat">
              현재 재고
              <div class="stat-value">{{ inventorySummary?.currentQty ?? 0 }}개</div>
            </div>
            <div class="stat">
              안전 재고
              <div class="stat-value">{{ inventorySummary?.safetyQty ?? 0 }}개</div>
            </div>
          </div>
        </div>

        <!-- 입출고 목록 -->
        <div class="table-wrap">
          <table class="stock-table">
            <thead>
              <tr>
                <th>입출고 번호</th>
                <th>타입</th>
                <th class="numeric">수량</th>
                <th>입출고일</th>
              </tr>
            </thead>

            <tbody>
              <tr v-if="invLoading">
                <td colspan="4" class="empty-data">불러오는 중...</td>
              </tr>

              <tr v-else-if="invError">
                <td colspan="4" class="empty-data">{{ invError }}</td>
              </tr>

              <tr v-else-if="movements.length === 0">
                <td colspan="4" class="empty-data">입출고 내역이 없습니다.</td>
              </tr>

              <tr v-else v-for="m in movements" :key="m.movementNo">
                <td>{{ m.movementNo }}</td>
                <td>{{ m.type }}</td>
                <td class="numeric">{{ m.qty }}</td>
                <td>{{ formatDateTime(m.occurredAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- ✅ 입출고 페이지네이션 -->
        <div class="pagination" v-if="invTotalPages > 1">
          <button
            class="pager"
            :disabled="invCurrentPage === 1 || invLoading"
            @click="changeInvPage(invCurrentPage - 1)"
          >
            ‹ Previous
          </button>

          <button
            v-for="page in invPageNumbers"
            :key="page"
            class="page"
            :class="{ active: page === invCurrentPage }"
            :disabled="invLoading"
            @click="changeInvPage(page)"
          >
            {{ page }}
          </button>

          <button
            class="pager"
            :disabled="invCurrentPage === invTotalPages || invLoading"
            @click="changeInvPage(invCurrentPage + 1)"
          >
            Next ›
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Money from '@/components/global/Money.vue'
import {
  getProductDetail,
  getProductInventory,
  updateProduct,
  deleteProduct,
} from '@/components/api/product/productService'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const isDeleting = ref(false)
const productId = Number(route.params.id)
const errorMsg = ref('')
const isLoading = ref(false)
const isEditMode = ref(false)

const editForm = ref({
  productCode: '',
  productName: '',
  description: '',
  price: 0,
  status: true,
  categoryLargeId: null,
  categoryMediumId: null,
  categorySmallId: null,
  removeImage: false,
})

const newImageFile = ref(null)
const newImagePreview = ref(null)

const startEdit = () => {
  if (!product.value) return
  isEditMode.value = true

  editForm.value = {
    productCode: product.value.productCode,
    productName: product.value.productName,
    description: product.value.description,
    price: product.value.price,
    status: product.value.status,
    categoryLargeId: selectedLargeId.value,
    categoryMediumId: selectedMediumId.value,
    categorySmallId: selectedSmallId.value,
    removeImage: false,
  }
  newImageFile.value = null
  newImagePreview.value = null
}

const cancelEdit = () => {
  isEditMode.value = false
  newImageFile.value = null
  newImagePreview.value = null
}

const largeCategories = ref([])
const mediumCategories = ref([])
const smallCategories = ref([])

const selectedLargeId = ref(null)
const selectedMediumId = ref(null)
const selectedSmallId = ref(null)

const fetchLarge = async () => {
  const res = await axios.get('/api/v1/categories/top')
  largeCategories.value = res.data
}

const fetchChildren = async (parentId) => {
  if (!parentId) return []
  const res = await axios.get(`/api/v1/categories/${parentId}/children`)
  return res.data
}

const onLargeChange = async () => {
  selectedMediumId.value = null
  selectedSmallId.value = null
  mediumCategories.value = await fetchChildren(selectedLargeId.value)
  smallCategories.value = []
}

const onMediumChange = async () => {
  selectedSmallId.value = null
  smallCategories.value = await fetchChildren(selectedMediumId.value)
}

const onPickImage = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  newImageFile.value = file
  editForm.value.removeImage = false

  const reader = new FileReader()
  reader.onload = () => (newImagePreview.value = reader.result)
  reader.readAsDataURL(file)
}

const removeImage = () => {
  newImageFile.value = null
  newImagePreview.value = null
  editForm.value.removeImage = true
}

const saveEdit = async () => {
  try {
    isLoading.value = true

    editForm.value.categoryLargeId = selectedLargeId.value
    editForm.value.categoryMediumId = selectedMediumId.value
    editForm.value.categorySmallId = selectedSmallId.value

    const updated = await updateProduct(productId, editForm.value, newImageFile.value)
    product.value = updated
    isEditMode.value = false
  } catch (e) {
    console.error(e)
    alert('상품 수정에 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

const activeTab = ref('basic')

// sample product data, in real app fetch by code
const product = ref(null)

const API_BASE = axios.defaults.baseURL || 'http://localhost:8080'

const imageSrc = computed(() => {
  const url = product.value?.imageUrl
  if (!url) return null

  // S3 같이 절대 경로(https://...)면 그대로 사용
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  // 예전처럼 "/product-images/..." 처럼 상대 경로면 API_BASE 붙여서 사용
  return `${API_BASE}${url}`
})

const inventorySummary = ref(null)
const movements = ref([])

const invLoading = ref(false)
const invError = ref('')

const invCurrentPage = ref(1)
const invPageSize = ref(10)
const invTotalCount = ref(0)

const MAX_VISIBLE_PAGES = 5

const invTotalPages = computed(() =>
  invTotalCount.value > 0 ? Math.ceil(invTotalCount.value / invPageSize.value) : 1,
)

const invPageNumbers = computed(() => {
  const pages = []
  const total = invTotalPages.value
  const current = invCurrentPage.value
  const half = Math.floor(MAX_VISIBLE_PAGES / 2)

  let start = Math.max(1, current - half)
  let end = Math.min(total, start + MAX_VISIBLE_PAGES - 1)

  if (end - start + 1 < MAX_VISIBLE_PAGES) {
    start = Math.max(1, end - MAX_VISIBLE_PAGES + 1)
  }

  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

const fetchInventory = async (page = 1) => {
  invLoading.value = true
  invError.value = ''
  try {
    const data = await getProductInventory(productId, page, invPageSize.value)

    inventorySummary.value = data.summary
    movements.value = data.items

    invCurrentPage.value = data.page
    invPageSize.value = data.numOfRows
    invTotalCount.value = data.totalCount
  } catch (e) {
    console.error(e)
    invError.value = '재고 현황 조회에 실패했습니다.'
  } finally {
    invLoading.value = false
  }
}

const changeInvPage = (page) => {
  if (page < 1 || page > invTotalPages.value || page === invCurrentPage.value) return
  fetchInventory(page)
}

watch(activeTab, (tab) => {
  if (tab === 'stock') {
    fetchInventory(invCurrentPage.value)
  }
})

const formatDateTime = (iso) => {
  if (!iso) return ''
  return iso.toString().replace('T', ' ').slice(0, 16) // yyyy-MM-dd HH:mm
}

const fetchProduct = async () => {
  if (!productId) {
    errorMsg.value = '잘못된 상품 ID입니다.'
    return
  }
  isLoading.value = true
  errorMsg.value = ''
  try {
    product.value = await getProductDetail(productId)
  } catch (e) {
    console.error(e)
    errorMsg.value = '상품 상세 조회에 실패했습니다.'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchLarge()
  fetchProduct()
})
</script>

<style scoped>
/* 드롭다운 전용 스타일 */

.product-detail {
  padding: 24px;
}

/* 헤더 버튼 사진 느낌으로 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

/* 버튼 두 개를 가로로 */
.page-header .actions {
  display: flex;
  gap: 8px;
  margin-left: auto; /* 오른쪽 끝으로 */
}

/* 버튼 공통 스타일 */
.page-header .actions button {
  height: 36px;
  font-size: 13px;
  border-radius: 6px;
  padding: 0 12px;
  cursor: pointer;
  display: inline-flex; /* ✅ 가로 유지 + 내부 정렬만 flex */
  align-items: center;
  justify-content: center;
}
.btn-edit {
  background: #fff;
  border: 1px solid #e5e7eb;
  padding: 8px 10px;
  border-radius: 8px;
}
.btn-delete {
  background: #ef4444;
  color: #fff;
  border: none;
  padding: 8px 10px;
  border-radius: 8px;
}

/* 탭도 사진처럼 납작한 캡슐 */
.tabs {
  display: inline-flex;
  background: #f3f4f6;
  border-radius: 10px;
  padding: 4px;
  gap: 4px;
  margin-bottom: 16px;
}
.tab {
  min-width: 160px;
  height: 36px;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  color: #6b7280;
}
.tab.active {
  background: #fff;
  color: #111827;
  border: 1px solid #e5e7eb;
}

/* 카드 기본 */
.card {
  background: #fff;
  border: 1px solid #eef2f7;
  padding: 22px;
  border-radius: 12px;
}

/* ✅ 사진처럼 좌(폼) / 우(이미지) */
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 36px;
  align-items: start;
}

.basic-left {
  max-width: 640px; /* 왼쪽 카드 폭이 너무 커지지 않게 */
}

/* 폼 필드 */
.field {
  margin-bottom: 14px;
}
.field label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 6px;
}

/* 입력창 같은 value */
.value {
  background: #eef0f3;
  border: 1px solid #e5e7eb;
  padding: 10px 12px;
  border-radius: 8px;
  min-height: 40px;
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #111827;
}
/* 기존 .value는 그대로 두고, 아래에 추가 */
.value.select-value {
  /* 기본 인풋 스타일에서 덮어쓰기 */
  display: block;
  width: 100%;
  cursor: pointer;

  /* 브라우저 기본 화살표 제거 */
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;

  background-color: #f9fafb;

  /* 오른쪽에 커스텀 화살표 아이콘 */
  background-image: url("data:image/svg+xml,%3Csvg width='12' height='12' viewBox='0 0 20 20' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M5 7l5 5 5-5' stroke='%239ca3af' stroke-width='1.5' fill='none' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 12px 12px;
  padding-right: 36px; /* 화살표 들어갈 공간 */
}

/* 비활성화일 때 */
.value.select-value:disabled {
  cursor: not-allowed;
  color: #9ca3af;
  background-color: #f3f4f6;
  opacity: 0.8;
}

.value.name {
  font-weight: 700;
  font-size: 15px;
  min-height: 44px;
}

/* 제품명 + 칩 한줄 */
.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.chip {
  background: #e7e9ff;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid #c7ccff;
  white-space: nowrap;
}
.chip2 {
  background: #64656f;
  color: #c9c9d7;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid #1d1e29;
  white-space: nowrap;
}

/* 카테고리 3칸 */
.category-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 14px;
}

/* 설명 박스는 더 크게 */
.description {
  min-height: 110px;
  align-items: flex-start;
  line-height: 1.6;
  padding-top: 12px;
}

/* 오른쪽 이미지 박스 */
.image-box {
  width: 300px;
  height: 300px;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  overflow: hidden;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
}
.image-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.no-image {
  color: #9ca3af;
  font-size: 14px;
}

/* 반응형 */
@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  .image-box {
    width: 100%;
    height: 280px;
  }
}

/* =========================
    ✅ 재고 현황(stock) 탭 스타일
    ========================= */
.stock-card {
  padding: 22px;
}

/* 상단 요약 영역(코드/이름 + 카드들) */
.stock-header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 24px;
  margin-bottom: 16px;
}

.stock-title .code {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
  font-weight: 600;
  flex: 1;
}
.stock-title .name-lg {
  font-size: 16px;
  font-weight: 800;
  color: #111827;
}

/* 요약 카드들 */
.stock-cards {
  display: flex;
  gap: 12px;
  margin-left: 24px;
}

.stat {
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  padding: 12px 16px;
  min-width: 140px;
  font-size: 13px;
  color: #6b7280;
  font-weight: 600;
}

.stat-value {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}

/* 입출고 테이블 */
.table-wrap {
  margin-top: 8px;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  overflow: hidden;
}

.stock-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.stock-table thead th {
  background: #f9fafb;
  text-align: left;
  padding: 12px;
  border-bottom: 1px solid #eef2f7;
  color: #111827;
  font-weight: 700;
}

.stock-table tbody td {
  padding: 12px;
  border-top: 1px solid #f3f4f6;
}

.stock-table th.numeric,
.stock-table td.numeric {
  text-align: right;
}

.empty-data {
  text-align: center;
  padding: 18px;
  color: #9ca3af;
}

/* 페이지네이션 */
.pagination {
  margin-top: 16px;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
}

.pager {
  background: transparent;
  border: none;
  color: #666;
  cursor: pointer;
  padding: 6px 8px;
}
.pager:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.page {
  min-width: 32px;
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #eee;
  background: #fff;
  cursor: pointer;
}
.page.active {
  background: #6b46ff;
  color: #fff;
  border-color: #6b46ff;
  font-weight: 700;
}
.page:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 반응형 */
@media (max-width: 1024px) {
  .stock-header {
    flex-direction: column;
  }
  .stock-cards {
    width: 100%;
  }
  .stat {
    flex: 1;
    min-width: 0;
  }
}
/* ✅ 편집 모드에서 form control이 div처럼 꽉 차게 */
input.value,
select.value,
textarea.value {
  display: block; /* flex 해제 */
  width: 100%; /* 칸을 꽉 채움 */
  box-sizing: border-box; /* padding 포함 */
  background: #eef0f3;
  border: 1px solid #e5e7eb;
  padding: 10px 12px;
  border-radius: 8px;
  min-height: 40px;
  font-size: 14px;
  color: #111827;
}

/* textarea는 높이/정렬만 따로 */
textarea.value {
  min-height: 110px;
  line-height: 1.6;
  resize: vertical;
}

/* select도 높이/커서 */
select.value {
  cursor: pointer;
}
.image-col {
  width: 300px; /* 오른쪽 컬럼 폭 고정 */
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.image-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.image-actions input[type='file'] {
  width: 100%;
  font-size: 12px;
}
</style>
