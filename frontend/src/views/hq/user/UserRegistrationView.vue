<script setup>
import { reactive, computed, ref } from 'vue'
import apiClient from '@/components/api'
import router from '@/router'
import VenderSearchModal from '@/components/modal/VenderSearchModal.vue'
import { Mail, User, Phone, Lock, Home } from 'lucide-vue-next'

const form = reactive({
  memberType: 'HQ',
  loginId: '',
  password: '',
  passwordConfirm: '',
  name: '',
  phone: '',
  storeId: '',
})

const memberTypes = [
  { label: '본사 관리자', value: 'HQ_ADMIN' },
  { label: '본사 직원', value: 'HQ' },
  { label: '가맹점 관리자', value: 'STORE_ADMIN' },
]

const errors = reactive({
  loginId: '',
  password: '',
  name: '',
  phone: '',
  storeId: '',
})

const showStoreModal = ref(false)
const selectedStore = ref(null)

function onStoreSelect(result) {
  // result: { type, id, name, code }
  selectedStore.value = result
  form.storeId = result.id
  showStoreModal.value = false
}

function openStoreModal() {
  showStoreModal.value = true
}

function clearStoreSelection() {
  form.storeId = ''
  selectedStore.value = null
}

const isEmailValid = (email) => {
  const re =
    /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(".+"))@(([^<>()[\]\\.,;:\s@\"]+\.)+[^<>()[\]\\.,;:\s@\"]{2,})$/i
  return re.test(String(email).toLowerCase())
}

const canSubmit = computed(() => {
  if (!form.loginId || !isEmailValid(form.loginId)) return false
  if (!form.password || form.password !== form.passwordConfirm) return false
  if (!form.name) return false
  if (!form.phone) return false
  if (form.memberType === 'STORE_ADMIN' && !form.storeId) return false
  return true
})

const handleSubmit = async () => {
  errors.loginId = ''
  errors.password = ''
  errors.name = ''
  errors.phone = ''
  errors.storeId = ''

  if (!isEmailValid(form.loginId)) {
    errors.loginId = '유효한 이메일 주소를 입력하세요.'
    return
  }

  if (!form.name) {
    errors.name = '이름을 입력하세요.'
    return
  }
  if (!form.phone) {
    errors.phone = '연락처를 입력하세요.'
    return
  }
  if (!form.password) {
    errors.password = '비밀번호를 입력하세요.'
    return
  }
  if (form.password !== form.passwordConfirm) {
    errors.password = '비밀번호가 일치하지 않습니다.'
    return
  }
  if (form.memberType === 'STORE_ADMIN' && !form.storeId) {
    errors.storeId = '점포 ID를 입력하세요.'
    return
  }

  const payload = {
    email: form.loginId,
    password: form.password,
    name: form.name,
    phone: form.phone,
    role: form.memberType,
  }
  if (form.memberType === 'STORE_ADMIN') {
    payload.storeId = form.storeId
  }
  try {
    const resp = await apiClient.post('/api/v1/users/register', payload)
    try {
      window.alert('사용자를 등록하였습니다.')
    } catch (e) {}
    handleReset()
    router.push({ name: 'hq-users-list' })
  } catch (e) {
    if (e.response && e.response.data && e.response.data.message) {
      errors.loginId = e.response.data.message
    }
  }
}

const handleReset = () => {
  form.memberType = 'HQ'
  form.loginId = ''
  form.password = ''
  form.passwordConfirm = ''
  form.name = ''
  form.phone = ''
  form.storeId = ''
  errors.loginId = ''
  errors.password = ''
  errors.name = ''
  errors.phone = ''
  errors.storeId = ''
}
</script>

<template>
  <div class="user-registration">
    <header>
      <p class="eyebrow">사용자 관리</p>
      <h1>사용자 등록</h1>
      <p class="subtitle">새로운 관리자 패널 사용자를 위한 계정을 만드세요.</p>
    </header>

    <section class="form-wrapper">
      <form class="form-card" @submit.prevent="handleSubmit">
        <fieldset class="field">
          <legend>회원 유형 <span class="required">*</span></legend>
          <div class="radio-group">
            <label v-for="member in memberTypes" :key="member.value">
              <input
                v-model="form.memberType"
                type="radio"
                name="memberType"
                :value="member.value"
              />
              <span>{{ member.label }}</span>
            </label>
          </div>
        </fieldset>

        <div class="field">
          <label for="email">이메일</label>
          <div class="input-wrapper">
            <Mail class="leading-icon" aria-hidden />
            <input
              id="loginId"
              v-model="form.loginId"
              type="text"
              placeholder="이메일을 입력하세요"
            />
          </div>
          <div v-if="errors.loginId" style="color: #ef4444; font-size: 13px">
            {{ errors.loginId }}
          </div>
        </div>

        <div class="field">
          <label for="name">이름</label>
          <div class="input-wrapper">
            <User class="leading-icon" aria-hidden />
            <input id="name" v-model="form.name" type="text" placeholder="이름을 입력하세요" />
          </div>
          <div v-if="errors.name" style="color: #ef4444; font-size: 13px">{{ errors.name }}</div>
        </div>

        <div class="field">
          <label for="phone">연락처</label>
          <div class="input-wrapper">
            <Phone class="leading-icon" aria-hidden />
            <input id="phone" v-model="form.phone" type="text" placeholder="010-0000-0000" />
          </div>
          <div v-if="errors.phone" style="color: #ef4444; font-size: 13px">{{ errors.phone }}</div>
        </div>

        <div class="field">
          <label for="password">비밀번호</label>
          <div class="input-wrapper">
            <Lock class="leading-icon" aria-hidden />
            <input
              id="password"
              v-model="form.password"
              type="password"
              placeholder="비밀번호를 입력하세요"
            />
          </div>
        </div>

        <div class="field">
          <label for="passwordConfirm">비밀번호 확인</label>
          <div class="input-wrapper">
            <Lock class="leading-icon" aria-hidden />
            <input
              id="passwordConfirm"
              v-model="form.passwordConfirm"
              type="password"
              placeholder="비밀번호를 다시 입력하세요"
            />
          </div>
          <div v-if="errors.password" style="color: #ef4444; font-size: 13px">
            {{ errors.password }}
          </div>
        </div>

        <div v-if="form.memberType === 'STORE_ADMIN'" class="field">
          <label for="storeId">가맹점 선택</label>
          <div class="input-wrapper" style="gap: 8px">
            <Home class="leading-icon" aria-hidden />
            <input
              id="storeId"
              :value="selectedStore ? selectedStore.name : form.storeId"
              type="text"
              readonly
              placeholder="가맹점을 선택하세요"
            />
            <button
              type="button"
              class="ghost btn-compact"
              @click.prevent="openStoreModal"
              aria-label="가맹점 선택"
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
              <span>가맹점 선택</span>
            </button>
            <button
              v-if="form.storeId"
              type="button"
              class="ghost btn-compact btn-clear"
              @click.prevent="clearStoreSelection"
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
          <div v-if="errors.storeId" style="color: #ef4444; font-size: 13px">
            {{ errors.storeId }}
          </div>
        </div>

        <VenderSearchModal
          v-if="showStoreModal"
          :currentType="'FRANCHISE'"
          @close="showStoreModal = false"
          @select="onStoreSelect"
        />

        <div class="form-actions">
          <button type="button" class="ghost" @click="handleReset">초기화</button>
          <button type="submit" class="primary">저장</button>
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
  width: 680px;
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
  width: 16px;
  height: 16px;
  display: inline-flex;
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
