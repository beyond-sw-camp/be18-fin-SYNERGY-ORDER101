<template>
  <!-- 
    DEPRECATED: 이 컴포넌트는 더 이상 사용되지 않습니다.
    대신 PurchaseCreateView.vue를 사용하세요.
    BaseOrderCreateView를 상속받아 재사용 가능하게 리팩토링되었습니다.
  -->
  <div class="order-create">
    <div class="page-inner">
      <!-- 1. 좌측 칼럼 (상세 정보 및 품목 목록) -->
      <div class="left-col">
        <h2 class="title">발주 생성 (DEPRECATED)</h2>

        <!-- 발주 세부 정보 카드 -->
        <section class="card">
          <h3 class="card-title">발주 세부 정보</h3>
          <div class="form-row">
            <label>공급업체 선택</label>
            <SupplierSelectDisplay :supplier="selectedSupplier" @open-modal="isModalOpen = true" />
          </div>

          <div class="actions">
            <button class="btn-primary" @click="OnCreatedPurchase" :disabled="!canSubmitOrder">
              구매 주문 제출
            </button>
          </div>
        </section>

        <!-- 품목 세부 정보 카드 -->
        <section class="card">
          <div class="card-head">
            <h3 class="card-title">품목 세부 정보</h3>
            <button class="btn" @click="openModal" :disabled="!selectedSupplier.supplierId">
              + 품목 추가
            </button>
          </div>

          <!-- 품목 테이블 -->
          <table class="order-table">
            <thead>
              <tr>
                <th class="center">SKU</th>
                <th class="center">이름</th>
                <th class="center">주문 수량</th>
                <th class="center">공급가</th>
                <th class="center">판매가</th>
                <th class="center">작업</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in rows" :key="row.sku + idx">
                <td class="center">{{ row.sku }}</td>
                <td class="center">{{ row.name }}</td>
                <td class="center">
                  <!-- 수량이 1 미만으로 내려가지 않도록 min 설정 -->
                  <input type="number" v-model.number="row.qty" class="qty" min="1" />
                </td>
                <td class="numeric">
                  <Money :value="row.purchasePrice" />
                </td>
                <td class="numeric">
                  <Money :value="row.price" />
                </td>
                <td class="center">
                  <button class="btn-delete" @click="removeRow(idx)">삭제</button>
                </td>
              </tr>
              <tr v-if="rows.length === 0">
                <td colspan="6" class="empty">
                  품목이 없습니다. '품목 추가' 버튼을 눌러 추가하세요.
                </td>
              </tr>
            </tbody>
          </table>
        </section>
      </div>

      <!-- 2. 우측 칼럼 (요약 정보) -->
      <aside class="right-col">
        <div class="summary card">
          <h4>발주 금액 요약</h4>
          <div class="summary-row">
            <span>소계:</span
            ><span class="numeric">
              <Money :value="subtotal" /> </span
            >1
          </div>
          <div class="summary-row">
            <!-- <span>배송:</span><span class="numeric">
              <Money :value="shipping" />
            </span> -->
          </div>
          <hr />
          <div class="summary-row total">
            <span>총액:</span
            ><span class="numeric">
              <Money :value="total" />
            </span>
          </div>
        </div>
      </aside>
    </div>

    <!-- 품목 추가 모달 (OrderItemModal) -->
    <OrderItemModal
      v-if="showModal"
      @close="showModal = false"
      :initial-supplier-id="selectedSupplier.supplierId"
      :use-purchase-price="true"
      @add="onAddItems"
    />

    <!-- 공급업체 검색 모달 (SupplierSearchModal) -->
    <SupplierSearchModal
      :is-open="isModalOpen"
      :selected-supplier="selectedSupplier"
      @update:is-open="isModalOpen = $event"
      @select="handleSupplierSelect"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import OrderItemModal from '../../../components/modal/OrderItemModal.vue'
import SupplierSearchModal from '../../../components/modal/SupplierSearchModal.vue'
import SupplierSelectDisplay from './SupplierSelectDisplay.vue'
import Money from '@/components/global/Money.vue'
import { useAuthStore } from '@/stores/authStore'
import apiClient from '@/components/api'

// --- 1. 상수 정의 (DTO 및 기본 설정) ---
const authStore = useAuthStore()

const DEFAULT_ORDER_CONFIG = {
  WAREHOUSE_ID: 1,
  ORDER_TYPE: 'MANUAL',
  ORDER_STATUS: 'SUBMITTED',
}

// --- 2. 상태 (State) 정의 ---
const showModal = ref(false) // 품목 추가 모달 상태
const isModalOpen = ref(false) // 공급업체 선택 모달 상태
const rows = ref([]) // 발주 품목 목록
const selectedSupplier = ref({ supplierId: null, name: '공급업체 선택' }) // 선택된 공급업체 정보
const productIdSet = ref(new Set()) // 품목 중복 방지용 Set (productId 기준)

// --- 3. 계산된 속성 (Computed) ---
const shipping = 0 // 배송비 (임시 고정값)

const subtotal = computed(() =>
  rows.value.reduce((s, r) => s + Number(r.purchasePrice || 0) * (r.qty || 0), 0),
)

const total = computed(() => subtotal.value + shipping)

// 발주 제출 가능 여부 검사
const canSubmitOrder = computed(() => selectedSupplier.value.supplierId && rows.value.length > 0)

// --- 4. 이벤트 핸들러 (Event Handlers) ---

/**
 * 공급업체 검색 모달에서 최종 선택 값을 처리합니다.
 * @param {object} supplier - 선택된 공급업체 객체
 */
function handleSupplierSelect(supplier) {
  selectedSupplier.value = supplier

  // 공급업체 변경 시 기존 품목 초기화
  if (rows.value.length > 0) {
    rows.value = []
    productIdSet.value.clear()
  }
  isModalOpen.value = false
}

/**
 * OrderItemModal에서 선택된 품목들을 받아 발주 목록에 추가합니다.
 * @param {Array<object>} products - 추가할 품목 배열 ({productId, sku, name, price, ...})
 */
function onAddItems(products) {
  // products가 배열인지 확인
  if (!Array.isArray(products)) {
    return
  }

  products.forEach((p) => {
    // 중복된 productId가 있으면 추가하지 않음
    if (productIdSet.value.has(p.productId)) return

    productIdSet.value.add(p.productId)
    rows.value.push({
      productId: p.productId,
      sku: p.sku,
      name: p.name,
      purchasePrice: p.purchasePrice || p.supplyPrice || 0, // 공급가
      price: p.price || 0, // 납품가
      qty: 1, // 기본 수량 1
    })
  })
}

/**
 * 발주 목록에서 품목을 제거합니다.
 * @param {number} idx - rows 배열 내의 인덱스
 */
function removeRow(idx) {
  const removed = rows.value.splice(idx, 1)[0]
  if (removed) productIdSet.value.delete(removed.productId)
}

/**
 * 품목 추가 모달을 엽니다.
 */
function openModal() {
  // 공급업체가 선택되지 않았으면 알림
  if (!selectedSupplier.value.supplierId) {
    alert('품목을 추가하려면 먼저 공급업체를 선택해야 합니다.')
    return
  }
  showModal.value = true
}
//폼 초기화
const resetForm = () => {
  rows.value = []
  productIdSet.value.clear()
  selectedSupplier.value = { supplierId: null, name: '공급업체 선택' }
  showModal.value = false
  isModalOpen.value = false
}

/**
 * 발주 생성 요청(POST)을 서버로 전송합니다.
 */
async function OnCreatedPurchase() {
  if (!canSubmitOrder.value) {
    alert('공급업체와 최소 1개 이상의 품목을 선택해야 합니다.')
    return
  }

  // 발주 DTO 구성
  const payload = {
    userId: authStore.userInfo?.userId,
    supplierId: selectedSupplier.value.supplierId,
    warehouseId: DEFAULT_ORDER_CONFIG.WAREHOUSE_ID,
    orderType: DEFAULT_ORDER_CONFIG.ORDER_TYPE,
    orderStatus: DEFAULT_ORDER_CONFIG.ORDER_STATUS,
    // requiredDate.value는 현재 정의되지 않았으므로 주석 처리
    // deadline: requiredDate.value + 'T00:00:00',
    items: rows.value.map((r) => ({
      productId: r.productId,
      orderQty: r.qty,
    })),
  }

  try {
    const res = await apiClient.post('/api/v1/purchase-orders', payload)

    if (res.status === 201) {
      alert('발주 생성 완료!')
      resetForm() // 폼 초기화
    }
  } catch (e) {
    const errorMessage = e.response?.data?.message || '발주 생성 중 알 수 없는 오류가 발생했습니다.'
    // 경고: 실제 운영 환경에서는 alert 대신 커스텀 모달 UI를 사용해야 합니다.
    alert(`발주 생성 실패: ${errorMessage}`)
  }
}
</script>

<style scoped>
/* 스타일은 변경 없이 유지 */
.page-inner {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.left-col {
  flex: 1;
}

.right-col {
  width: 320px;
}

.title {
  margin: 8px 0 16px;
}

.card {
  background: #fff;
  border: 1px solid #eef2f7;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  margin-bottom: 12px;
}

.form-row {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
}

.select,
.input {
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.btn-primary {
  background: #6b46ff;
  color: #fff;
  border: none;
  padding: 8px 12px;
  border-radius: 6px;
  transition: opacity 0.2s;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  transition: background 0.2s;
}

.btn:hover {
  background: #f7f7f9;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.order-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 12px;
}

.order-table th,
.order-table td {
  padding: 12px;
  border-top: 1px solid #f7f7f9;
}

.numeric {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.center {
  text-align: center;
}

.qty {
  width: 80px;
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
}

.btn-delete {
  background: transparent;
  border: none;
  color: #ef4444;
  cursor: pointer;
}

.empty {
  text-align: center;
  color: #9ca3af;
  padding: 18px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
}

.total {
  font-weight: 700;
  color: #4f46e5;
}
</style>
