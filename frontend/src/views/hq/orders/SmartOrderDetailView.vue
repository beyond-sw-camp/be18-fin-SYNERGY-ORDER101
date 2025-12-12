<template>
  <div class="order-approval">
    <div class="page-inner" v-if="!loading"> 
      <div class="left-col">
        <h2 class="title">스마트 발주 상세</h2>

        <section class="card">
          <h3 class="card-title">발주 정보</h3>

          <div class="form-row">
            <label>공급사</label>
            <input class="input" :value="detail.supplierName || '-'" readonly />
          </div>

          <div class="form-row">
            <label>생성일</label>
            <input class="input" :value="detail.targetWeek" readonly />
          </div>

          <div class="form-row">
            <label>요청 담당자</label>
            <input class="input" :value="detail.requesterName || '시스템'" readonly />
          </div>

          <div class="form-row">
            <label>상태</label>
            <input class="input" :value="statusLabel(overallStatus)" readonly />
          </div>
        </section>

        <section class="card">
          <h3 class="card-title">품목 세부 정보</h3>

          <table class="order-table">
            <thead>
              <tr>
                <th>상품 코드</th>
                <th>상품명</th>
                <th class="numeric">단가</th>
                <th class="numeric">예측량</th>
                <th class="numeric">추천 발주량</th>
                <th class="numeric">금액</th> 
                <th class="center">작성자</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in detail.items" :key="row.smartOrderId + '-' + idx">
                <td>{{ row.productCode }}</td>
                <td>{{ row.productName }}</td>
                <td class="numeric">{{ formatCurrency(row.unitPrice) }}</td>
                <td class="numeric">{{ row.forecastQty?.toLocaleString() ?? '-' }}</td>
                <td class="numeric">
                  <input
                    type="number"
                    min="0"
                    class="qty-input"
                    v-model.number="row.recommendedOrderQty"
                    :disabled="isSubmitted"
                  />
                </td>

                <td class="numeric">
                  {{ formatCurrency(row.unitPrice * (row.recommendedOrderQty || 0)) }}
                </td>

                <td class="center">
                  <span :class="['chip-mini', isEdited(row) ? 'edited' : 'not-edited']">
                    {{ isEdited(row) ? 'USER' : 'SYSTEM' }}
                  </span>
                </td>
              </tr>

              <tr v-if="detail.items.length === 0">
                <td colspan="5" class="empty">품목이 없습니다.</td>
              </tr>
            </tbody>
          </table>

          <div class="actions-bottom" v-if="detail.items.length > 0">
            <button
              class="btn-primary"
              :disabled="saving || isSubmitted"
              @click="submitAll"
            >
              {{ isSubmitted ? '이미 제출된 발주입니다' : (saving ? '저장 중...' : '수정 및 제출') }}
            </button>
          </div>
        </section>
      </div>

      <aside class="right-col">
        <div class="summary card">
          <h4>발주 금액 요약</h4>

          <div class="summary-row">
            <span>총 예측량</span>
            <span class="numeric">{{ totalForecastQty.toLocaleString() }} 개</span>
          </div>

          <div class="summary-row">
            <span>총 추천 발주 수량</span>
            <span class="numeric">{{ totalRecommendedQty.toLocaleString() }} 개</span>
          </div>

          <div class="summary-row">
            <span>총 추천 발주 금액</span>
            <span class="numeric">{{ formatCurrency(totalAmount) }}</span>
          </div>
        </div>
      </aside>
    </div>

    <div v-else class="loading-state">
      데이터를 가져오는 중입니다...
    </div>
  </div>
</template>


<script setup>
import { reactive, computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import apiClient from '@/components/api'

const route = useRoute()
const router = useRouter()

const supplierId = route.params.supplierId
const targetWeek = route.params.targetWeek

const detail = reactive({
  supplierId: null,
  supplierName: '',
  targetWeek: '',
  requesterName: '',
  poNumber: '',
  status: 'DRAFT_AUTO',
  items: [],
})

const saving = ref(false)
const loading = ref(false)

const totalForecastQty = computed(() =>
  detail.items.reduce((sum, r) => sum + (r.forecastQty || 0), 0)
)

const totalRecommendedQty = computed(() =>
  detail.items.reduce((sum, r) => sum + (r.recommendedOrderQty || 0), 0)
)

const totalAmount = computed(() =>
  detail.items.reduce((sum, r) => sum + Number(r.unitPrice) * Number(r.recommendedOrderQty), 0)
)

const overallStatus = computed(() => detail.status)
const isSubmitted = computed(() =>
  ['SUBMITTED', 'CONFIRMED'].includes(overallStatus.value)
)

onMounted(() => {
  fetchDetail()
})

async function fetchDetail() {
  loading.value = true
  try {
    const res = await apiClient.get('/api/v1/smart-orders/detail', {
      params: { supplierId, targetWeek }
    })

    const data = res.data

    detail.supplierId = data.supplierId
    detail.supplierName = data.supplierName
    detail.targetWeek = data.targetWeek
    detail.requesterName = data.requesterName
    detail.poNumber = data.poNumber

    detail.items = (data.items || []).map(item => ({
      smartOrderId: item.smartOrderId,
      productCode: item.productCode,
      productName: item.productName,
      forecastQty: item.forecastQty,
      recommendedOrderQty: item.recommendedOrderQty,
      unitPrice: Number(item.unitPrice || 0),
      originalRecommendedQty: item.recommendedOrderQty,
    }))

    await fetchStatusFromList()

  } catch (e) {
    console.error('상세 오류:', e)
    alert('상세 정보를 불러오는 중 오류가 발생했습니다.')
  } finally {
    loading.value = false
  }
}

async function fetchStatusFromList() {
  const listRes = await apiClient.get('/api/v1/smart-orders', {
    params: { supplierId: detail.supplierId }
  })

  const rows = listRes.data || []

  const matched = rows.find(r =>
    r.supplierId === detail.supplierId &&
    r.targetWeek === detail.targetWeek
  )

  detail.status = matched?.smartOrderStatus || 'DRAFT_AUTO'
}

function formatCurrency(value) {
  return Number(value || 0).toLocaleString('ko-KR') + '원'
}

function isEdited(row) {
  return row.recommendedOrderQty !== row.originalRecommendedQty
}

async function submitAll() {
  if (isSubmitted.value) return alert('이미 제출된 발주입니다.')

  const changed = detail.items.filter(isEdited)

  if (!confirm(`총 ${changed.length}개 품목을 수정하셨습니다. 제출하시겠습니까?`)) return

  try {
    saving.value = true

    for (const item of detail.items) {
      await apiClient.patch(`/api/v1/smart-orders/${item.smartOrderId}`, {
        recommendedOrderQty: item.recommendedOrderQty,
      })
    }

    alert('스마트 발주가 수정 및 제출되었습니다.')
    router.push({ name: 'hq-smart-orders' })

  } catch (e) {
    console.error('제출 실패:', e)
    alert('제출 중 오류가 발생했습니다.')
  } finally {
    saving.value = false
  }
}

function statusLabel(s) {
  if (s === 'CONFIRMED') return '승인'
  if (s === 'SUBMITTED') return '제출'
  if (s === 'DRAFT_AUTO') return '초안'
  return s || '-'
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

.input {
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #f9fafb;
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
  font-size: 13px;
}

/* 기본은 왼쪽 정렬 */
.order-table th,
.order-table td {
  text-align: left;
}

/* 숫자 컬럼만 오른쪽 정렬 */
.order-table th.numeric,
.order-table td.numeric {
  text-align: right;
}

/* 수정 여부는 가운데 정렬 */
.order-table th.center,
.order-table td.center {
  text-align: center;
}

.numeric {
  text-align: right;
}

.qty-input {
  width: 100px;
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
  text-align: right;
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

.chip-mini {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: #fff;
}

.chip-mini.edited {
  background: #6b46ff;
}

.chip-mini.not-edited {
  background: #9ca3af;
}

.actions-bottom {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.btn-primary {
  background: #6b46ff;
  color: #fff;
  border: none;
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 로딩 상태 */
.loading-state {
  padding: 40px;
  text-align: center;
  color: #6b7280;
  font-size: 15px;
}
</style>
