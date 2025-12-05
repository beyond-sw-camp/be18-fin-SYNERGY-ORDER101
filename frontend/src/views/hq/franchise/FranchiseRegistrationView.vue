<script setup>
import { reactive, computed, ref } from 'vue'
import apiClient from '@/components/api'
import router from '@/router'
import WarehouseSearchModal from '@/components/modal/WarehouseSearchModal.vue'

const form = reactive({
  storeName: '',
  address: '',
  contactNumber: '',
  defaultWarehouseId: '',
})

const errors = reactive({
  storeName: '',
  address: '',
  contactNumber: '',
  defaultWarehouseId: '',
})

const showWareHouseModal = ref(false)
const selectedWareHouse = ref(null)

function onWareHouseSelect(result) {
  selectedWareHouse.value = result
  // Check for both possible property names
  form.defaultWarehouseId = result.warehouseId || result.id || ''
  showWareHouseModal.value = false
}

function openWareHouseModal() {
  showWareHouseModal.value = true
}

function clearWareHouseSelection() {
  form.defaultWarehouseId = ''
  selectedWareHouse.value = null
}

const canSubmit = computed(() => {
  if (!form.storeName) return false
  if (!form.address) return false
  if (!form.contactNumber) return false
  if (!form.defaultWarehouseId) return false
  return true
})

const handleSubmit = async () => {
  // basic validation
  errors.storeName = ''
  errors.address = ''
  errors.contactNumber = ''
  errors.defaultWarehouseId = ''

  if (!form.storeName) {
    errors.storeName = '이름을 입력하세요.'
    return
  }
  if (!form.address) {
    errors.address = '주소를 입력하세요.'
    return
  }
  if (!form.contactNumber) {
    errors.contactNumber = '연락처를 입력하세요.'
    return
  }
  if (!form.defaultWarehouseId) {
    errors.defaultWarehouseId = '기본 창고를 선택하세요.'
    return
  }

  const payload = {
    storeName: form.storeName,
    address: form.address,
    contactNumber: form.contactNumber,
    defaultWarehouseId: form.defaultWarehouseId,
  }

  try {
    const resp = await apiClient.post('/api/v1/stores', payload)
    window.alert('가맹점을 등록하였습니다.')
    handleReset()
    // 창고 선택 상태도 초기화
    selectedWareHouse.value = null
  } catch (e) {
    // try to surface server errors
    const errorMsg = e.response?.data?.message || e.message || '등록에 실패했습니다.'
    window.alert(errorMsg)
  }
}

const handleReset = () => {
  form.storeName = ''
  form.address = ''
  form.contactNumber = ''
  form.defaultWarehouseId = ''
  errors.storeName = ''
  errors.address = ''
  errors.contactNumber = ''
  errors.defaultWarehouseId = ''
}
</script>

<template>
  <div class="user-registration">
    <header>
      <p class="eyebrow">가맹점 관리</p>
      <h1>가맹점 등록</h1>
      <p class="subtitle">새로운 가맹점을 등록하세요.</p>
    </header>

    <section class="form-wrapper">
      <form class="form-card" @submit.prevent="handleSubmit">
        <div class="field">
          <label for="storeName">가맹점명</label>
          <div class="input-wrapper">
            <span class="leading-icon">✉️</span>
            <input
              id="storeName"
              v-model="form.storeName"
              type="text"
              placeholder="가맹점명을 입력하세요"
            />
          </div>
          <div v-if="errors.storeName" style="color: #ef4444; font-size: 13px">
            {{ errors.storeName }}
          </div>
        </div>

        <div class="field">
          <label for="address">주소</label>
          <div class="input-wrapper">
            <span class="leading-icon">👤</span>
            <input
              id="address"
              v-model="form.address"
              type="text"
              placeholder="주소를 입력하세요"
            />
          </div>
          <div v-if="errors.address" style="color: #ef4444; font-size: 13px">
            {{ errors.address }}
          </div>
        </div>

        <div class="field">
          <label for="contactNumber">연락처</label>
          <div class="input-wrapper">
            <span class="leading-icon">📞</span>
            <input
              id="contactNumber"
              v-model="form.contactNumber"
              type="text"
              placeholder="010-0000-0000"
            />
          </div>
          <div v-if="errors.contactNumber" style="color: #ef4444; font-size: 13px">
            {{ errors.contactNumber }}
          </div>
        </div>

        <div class="field">
          <label for="defaultWarehouseId">창고 선택</label>
          <div class="input-wrapper" style="gap: 8px">
            <span class="leading-icon">🏬</span>
            <input
              id="defaultWarehouseId"
              :value="selectedWareHouse ? selectedWareHouse.name : form.defaultWarehouseId"
              type="text"
              readonly
              placeholder="창고를 선택하세요"
            />
            <button
              type="button"
              class="ghost btn-compact"
              @click.prevent="openWareHouseModal"
              aria-label="창고 선택"
            >
              <svg viewBox="0 0 24 24" fill="none" class="btn-icon" aria-hidden>
                <path
                  d="M21 21l-4.35-4.35"
                  stroke="currentColor"
                  stroke-width="1.6"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
                <rect
                  x="3"
                  y="4"
                  width="12"
                  height="12"
                  rx="2"
                  stroke="currentColor"
                  stroke-width="1.6"
                />
                <path d="M7 8h6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
              </svg>
              <span>창고 선택</span>
            </button>
            <button
              v-if="form.defaultWarehouseId"
              type="button"
              class="ghost btn-compact btn-clear"
              @click.prevent="clearWareHouseSelection"
              aria-label="선택 삭제"
            >
              <svg viewBox="0 0 24 24" fill="none" class="btn-icon" aria-hidden>
                <path
                  d="M18 6L6 18"
                  stroke="currentColor"
                  stroke-width="1.6"
                  stroke-linecap="round"
                />
                <path
                  d="M6 6l12 12"
                  stroke="currentColor"
                  stroke-width="1.6"
                  stroke-linecap="round"
                />
              </svg>
              <span>지우기</span>
            </button>
          </div>
          <div v-if="errors.defaultWarehouseId" style="color: #ef4444; font-size: 13px">
            {{ errors.defaultWarehouseId }}
          </div>
        </div>

        <!-- warehouse search modal -->
        <WarehouseSearchModal
          v-if="showWareHouseModal"
          @close="showWareHouseModal = false"
          @select="onWareHouseSelect"
        />

        <div class="form-actions">
          <button type="button" class="ghost" @click="handleReset">초기화</button>
          <button type="submit" class="primary" :disabled="!canSubmit">저장</button>
        </div>
      </form>
    </section>
  </div>
</template>

<style scoped>
.user-registration {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.eyebrow {
  font-size: 14px;
  color: #6b7280;
  font-weight: 600;
}

h1 {
  font-size: 26px;
  font-weight: 700;
}

.subtitle {
  color: #6b7280;
}

.form-wrapper {
  /* center the card horizontally and give room on the left for the sidebar */
  display: flex;
  justify-content: center;
  padding: 8px 0 40px 0;
}

.form-card {
  background-color: #fff;
  border-radius: 20px;
  padding: 40px;
  border: 1px solid #f1f3f6;
  display: flex;
  flex-direction: column;
  gap: 20px;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.04);
  width: 680px; /* fixed card width like the design */
  max-width: calc(100% - 48px);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field label,
.field legend {
  font-weight: 600;
  color: #111827;
}

.required {
  color: #f87171;
}

.radio-group {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.radio-group label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.input-wrapper {
  display: flex;
  align-items: center;
  border: 1px solid #eef0f3;
  border-radius: 12px;
  padding: 12px 16px;
  background-color: #fff;
  gap: 8px;
}

.leading-icon {
  font-size: 16px;
  opacity: 0.7;
}

input[type='text'],
input[type='password'] {
  border: none;
  flex: 1;
  font-size: 15px;
  background: transparent;
  outline: none;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.form-actions button {
  min-width: 96px;
  padding: 10px 16px;
  border-radius: 12px;
  font-weight: 600;
  border: 1px solid transparent;
  cursor: pointer;
}

.form-actions .ghost {
  border-color: #e5e7eb;
  background-color: #fff;
}

.form-actions .primary {
  background-color: #6b63f6;
  color: #fff;
  border-color: #6b63f6;
}

/* Compact button used inside input fields (select / clear) */
.btn-compact {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #e6e9ef;
  color: #374151;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition:
    background 0.12s,
    border-color 0.12s,
    transform 0.06s;
}
.btn-compact:hover {
  background: #f8fafc;
  border-color: #dfe7ff;
  transform: translateY(-1px);
}
.btn-compact:active {
  transform: translateY(0);
}
.btn-compact .btn-icon {
  width: 16px;
  height: 16px;
  opacity: 0.9;
}
.btn-clear {
  border-color: #f3e6e6;
  color: #b91c1c;
}
.btn-clear:hover {
  background: #fff5f5;
}

.vendor-item.inactive {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 1024px) {
  .form-card {
    width: 100%;
    padding: 28px;
  }

  .form-actions {
    flex-direction: column-reverse;
    align-items: stretch;
  }
}
</style>
