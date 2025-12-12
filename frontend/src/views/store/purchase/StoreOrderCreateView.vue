<template>
  <div class="order-create">
    <div class="page-inner">
      <!-- 1. 좌측 칼럼 (상세 정보 및 품목 목록) -->
      <div class="left-col">
        <h2 class="title">매장 발주 생성</h2>

        <!-- 발주 세부 정보 카드 -->
        <!-- <section class="card">
          <h3 class="card-title">발주 세부 정보</h3>
        </section> -->

        <!-- 품목 세부 정보 카드 -->
        <section class="card">
          <div class="card-head">
            <h3 class="card-title">품목 세부 정보</h3>
            <button class="btn" @click="openItemModal">+ 품목 추가</button>
          </div>

          <!-- 품목 테이블 -->
          <table class="order-table">
            <thead>
              <tr>
                <th>SKU</th>
                <th>이름</th>
                <th>현재 재고</th>
                <th>금액</th>
                <th>주문 수량</th>
                <th>작업</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in rows" :key="row.sku + idx">
                <td>{{ row.sku }}</td>
                <td>{{ row.name }}</td>
                <td class="numeric">{{ row.stock ?? 0 }}</td>
                <td class="numeric">
                  <Money :value="row.price" />
                </td>
                <td>
                  <input type="number" v-model.number="row.qty" class="qty" min="1" />
                </td>
                <td><button class="btn-delete" @click="removeRow(idx)">삭제</button></td>
              </tr>
              <tr v-if="rows.length === 0">
                <td colspan="6" class="empty">
                  품목이 없습니다. '품목 추가' 버튼을 눌러 추가하세요.
                </td>
              </tr>
            </tbody>
          </table>

          <!-- 발주 요청 버튼 -->
          <div class="actions">
            <button class="btn-primary" @click="onSubmitOrder" :disabled="!canSubmitOrder">발주 요청 제출</button>
          </div>
        </section>
      </div>

      <!-- 2. 우측 칼럼 (요약 정보) -->
      <aside class="right-col">
        <div class="summary card">
          <h4>발주 요약</h4>
          <div class="summary-row">
            <span>총 품목 수:</span>
            <span class="numeric">{{ rows.length }}개</span>
          </div>
          <div class="summary-row">
            <span>총 주문 수량:</span>
            <span class="numeric">{{ totalQty.toLocaleString() }}개</span>
          </div>
          <hr />
          <div class="summary-row total">
            <span>총 금액:</span>
            <span class="numeric">
              <Money :value="totalAmount" />
            </span>
          </div>
        </div>
      </aside>
    </div>

    <!-- 품목 추가 모달 (OrderItemModal - 전체 품목 조회) -->
    <OrderItemModal v-if="showItemModal" @close="showItemModal = false" @add="onAddItems" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import OrderItemModal from '@/components/modal/OrderItemModal.vue'
import apiClient from '@/components/api'
import { useAuthStore } from '@/stores/authStore'
import Money from '@/components/global/Money.vue'

// --- 상수 정의 ---
const authStore = useAuthStore()

// --- 상태 (State) 정의 ---
const showItemModal = ref(false)
const rows = ref([])
const productIdSet = ref(new Set())

// --- 계산된 속성 (Computed) ---
const totalQty = computed(() =>
  rows.value.reduce((sum, r) => sum + (r.qty || 0), 0)
)

const totalAmount = computed(() =>
  rows.value.reduce((sum, r) => sum + (r.price || 0) * (r.qty || 0), 0)
)

const canSubmitOrder = computed(() => rows.value.length > 0)

// --- 이벤트 핸들러 ---

/**
 * OrderItemModal에서 선택된 품목들을 받아 발주 목록에 추가합니다.
 */
function onAddItems(products) {
  if (!Array.isArray(products)) {
    return
  }
  products.forEach(p => {
    if (productIdSet.value.has(p.productId)) return

    productIdSet.value.add(p.productId)
    rows.value.push({
      productId: p.productId,
      sku: p.sku,
      name: p.name,
      price: p.price || 0,
      stock: p.stock ?? 0,
      qty: 1
    })
  })
}

/**
 * 발주 목록에서 품목을 제거합니다.
 */
function removeRow(idx) {
  const removed = rows.value.splice(idx, 1)[0]
  if (removed) productIdSet.value.delete(removed.productId)
}

/**
 * 품목 추가 모달을 엽니다.
 */
function openItemModal() {
  showItemModal.value = true
}

/**
 * 폼 초기화
 */
function resetForm() {
  rows.value = []
  productIdSet.value.clear()
  showItemModal.value = false
}

/**
 * 발주 생성 요청(POST)을 서버로 전송합니다.
 */
async function onSubmitOrder() {
  if (!canSubmitOrder.value) {
    alert('최소 1개 이상의 품목을 선택해야 합니다.')
    return
  }

  // authStore에서 storeId와 userId 가져오기
  const storeId = authStore.userInfo.storeId
  const userId = authStore.userInfo.userId

  if (!storeId) {
    alert('매장 정보가 없습니다. 다시 로그인해주세요.')
    return
  }

  // 발주 DTO 구성 (StoreOrderCreateRequest)
  const payload = {
    storeId: Number(storeId),
    userId: Number(userId),
    items: rows.value.map(r => ({
      productId: r.productId,
      orderQty: r.qty
    })),
    remark: ''
  }
  try {
    const res = await apiClient.post('/api/v1/store-orders', payload)

    if (res.status === 201 || res.status === 200) {
      const orderId = res.data?.storeOrderId || res.data?.id || ''
      alert(`발주 요청이 완료되었습니다.`);
      resetForm()
    }
  } catch (e) {
    const errorMessage = e.response?.data?.message || '발주 생성 중 오류가 발생했습니다.'
    alert(`발주 생성 실패: ${errorMessage}`)
  }
}
</script>

<style scoped>
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

.form-row label {
  margin-bottom: 6px;
  font-weight: 500;
  color: #374151;
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
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-primary:hover:not(:disabled) {
  background: #5a38d6;
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
