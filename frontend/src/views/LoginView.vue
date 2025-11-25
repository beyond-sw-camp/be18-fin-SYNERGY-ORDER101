<script setup>
import { reactive } from 'vue'
import logoUrl from '@/assets/logo.png'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  email: '',
  password: '',
})

const handleLogin = async () => {
  try {
    const stayLoggedIn = true
    const result = await auth.login(form.email, form.password, stayLoggedIn)
    if (!result || !result.success) {
      window.alert(result && result.message ? result.message : '로그인에 실패했습니다.')
      return
    }

    const role =
      auth.userInfo.role || (auth.userInfo.roles && auth.userInfo.roles[0]) || auth.userInfo.type
    if (role === 'STORE_ADMIN') {
      router.push('/store/dashboard')
    } else {
      router.push('/hq/dashboard')
    }
  } catch (err) {
    console.error('login error', err)
    window.alert('로그인 중 오류가 발생했습니다.')
  }
}
</script>

<template>
  <div class="login-page">
    <div class="warning-box">
      <div class="warning-icon">⚠️</div>
      <div>
        <p class="warning-title">보안 경고</p>
        <p class="warning-text">
          회사 인트라넷 또는 VPN에 접속되어 있지 않습니다. 보안을 위해 주의하세요.
        </p>
      </div>
    </div>

    <main class="login-card-wrap">
      <div class="login-card">
        <div class="logo-wrap">
          <!-- simple SVG mark similar to project style -->
          <div><img :src="logoUrl" alt="ORDER 101" class="logo-image" /></div>
        </div>

        <h2 class="login-heading">로그인</h2>
        <p class="login-sub">Order101에 로그인하여 주문을 관리하세요.</p>

        <form class="login-form" @submit.prevent="handleLogin">
          <label class="field">
            <div class="field-label">이메일</div>
            <div class="input-wrap">
              <span class="icon">✉️</span>
              <input v-model="form.email" placeholder="Hallu@hallu.com" />
            </div>
          </label>

          <label class="field">
            <div class="field-label">비밀번호</div>
            <div class="input-wrap">
              <span class="icon">🔒</span>
              <input v-model="form.password" type="password" placeholder="********" />
            </div>
          </label>

          <button class="btn-primary" type="submit">로그인</button>
        </form>
      </div>
    </main>
  </div>
</template>

<style scoped>
.login-page {
  min-height: calc(100vh - 0px);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 24px;
  background: #fbfbfd;
}

.warning-box {
  width: 640px;
  max-width: calc(100% - 48px);
  display: flex;
  gap: 16px;
  align-items: center;
  border: 1px solid #eef0f3;
  border-radius: 10px;
  padding: 14px 18px;
  background: #fff;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.02);
  margin-bottom: 28px;
}
.warning-icon {
  font-size: 20px;
}
.warning-title {
  font-weight: 700;
  margin: 0;
}
.warning-text {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.login-card-wrap {
  width: 100%;
  display: flex;
  justify-content: center;
}

.login-card {
  width: 420px;
  background: #fff;
  border-radius: 10px;
  padding: 28px 28px 36px 28px;
  border: 1px solid #eef0f3;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

.logo-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
  margin-bottom: 8px;
}
.logo-mark {
  width: 48px;
  height: 48px;
}
.brand-title {
  font-weight: 700;
  color: #6b63f6;
  font-size: 20px;
}

.login-heading {
  text-align: center;
  margin: 8px 0 4px 0;
  font-size: 18px;
}
.login-sub {
  text-align: center;
  margin: 0 0 16px 0;
  color: #6b7280;
  font-size: 13px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.field-label {
  font-weight: 600;
  color: #111827;
  font-size: 14px;
}
.input-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #e6e9ee;
  padding: 10px 12px;
  border-radius: 8px;
}
.input-wrap .icon {
  opacity: 0.7;
}
.input-wrap input {
  border: none;
  outline: none;
  flex: 1;
  font-size: 14px;
  background: transparent;
}

.btn-primary {
  margin-top: 8px;
  background: #6b63f6;
  color: white;
  border: none;
  padding: 10px 12px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 480px) {
  .warning-box {
    width: calc(100% - 32px);
  }
  .login-card {
    width: calc(100% - 32px);
  }
}
.logo-image {
  height: 55px;
  object-fit: cover;
}
</style>
