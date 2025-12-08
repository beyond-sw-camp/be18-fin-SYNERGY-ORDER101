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
      window.alert(result?.message ?? '로그인에 실패했습니다.')
      return
    }

    const role =
      auth.userInfo.role ||
      (auth.userInfo.roles && auth.userInfo.roles[0]) ||
      auth.userInfo.type

    if (role === 'STORE_ADMIN') {
      router.push('/store/dashboard')
    } else {
      router.push('/hq/dashboard')
    }
  } catch {
    window.alert('로그인 중 오류가 발생했습니다.')
  }
}
</script>

<template>
  <div class="login-layout">
    <!-- 상단 보안 배너 -->
    <div class="security-banner">
      <strong>보안 알림</strong>
      <span>사내 인트라넷 또는 VPN 연결 상태를 확인하세요.</span>
    </div>

    <!-- 가운데 정렬 영역 -->
    <div class="center-wrap">
      <div class="login-container">
        <!-- 좌측 브랜드 패널 -->
        <section class="brand-panel">
          <div class="brand-inner">
            <h1>ORDER 101</h1>
            <p class="brand-desc">
              주문 · 재고 · 공급을 하나로<br />
              통합 관리하는 주문관리 시스템
            </p>
            <ul class="brand-points">
              <li>실시간 주문 흐름 관리</li>
              <li>매장 · 본사 통합 대시보드</li>
              <li>AI 수요 예측 기반 스마트 발주</li>
            </ul>
          </div>
        </section>

        <!-- 로그인 카드 -->
        <section class="login-card">
          <div class="login-logo-wrap">
            <img :src="logoUrl" class="login-logo" alt="ORDER101" />
          </div>

          <h2 class="login-title">로그인</h2>
          <p class="login-sub">Order101 시스템에 접속합니다</p>

          <form class="login-form" @submit.prevent="handleLogin">
            <label>
              이메일
              <input
                v-model="form.email"
                type="email"
                placeholder="order101@email.com"
              />
            </label>

            <label>
              비밀번호
              <input
                v-model="form.password"
                type="password"
                placeholder="비밀번호 입력"
              />
            </label>

            <button class="login-btn" type="submit">
              시스템 접속
            </button>
          </form>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Layout */
.login-layout {
  min-height: 100vh;
  background:
    linear-gradient(
      rgba(248, 250, 252, 0.92),
      rgba(238, 242, 255, 0.95)
    ),
    url('https://images.unsplash.com/photo-1581091870622-7c54c1f4f28b?auto=format&fit=crop&w=2400&q=80')
      center / cover no-repeat;

  display: flex;
  flex-direction: column;
}

/* Security banner */
.security-banner {
  background: #0f172a;
  color: #e5e7eb;
  font-size: 14px;
  padding: 12px 28px;
  display: flex;
  gap: 12px;
}

/* 중앙 정렬 래퍼 */
.center-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Main container */
.login-container {
  max-width: 1850px;
  width: 100%;
  padding: 0 40px;
  display: flex;
  gap: 64px;
  align-items: stretch;
}

/* Brand panel */
.brand-panel {
  flex: 1;
  background: linear-gradient(180deg, #1e1b4b, #17143a);
  border-radius: 28px;
  display: flex;
}

.brand-inner {
  padding: 72px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-panel h1 {
  font-size: 38px;
  color: #ffffff;
  margin-bottom: 18px;
}

.brand-desc {
  font-size: 16.5px;
  color: #c7d2fe;
  line-height: 1.75;
}

.brand-points {
  list-style: none;
  padding: 0;
  margin-top: 32px;
}

.brand-points li {
  font-size: 15.5px;
  color: #e0e7ff;
  margin-bottom: 14px;
}

/* Login card */
.login-card {
  flex: 1.4;
  width: auto; 
  background: #ffffff;
  border-radius: 28px;
  padding: 48px 64px;
  box-shadow: 0 30px 70px rgba(15, 23, 42, 0.22);

  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-logo-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 28px;
}

.login-logo {
  height: 56px;
  padding: 12px 16px;
  background: white;
  border-radius: 14px;
}

.login-title {
  font-size: 26px;
  font-weight: 800;
  color: #111827;
  margin-bottom: 8px;
  text-align: center;
}

.login-sub {
  font-size: 14.5px;
  color: #4b5563;
  margin-bottom: 30px;
  text-align: center;
}

/* Form */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.login-form label {
  font-size: 14.5px;
  font-weight: 600;
  color: #111827;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.login-form input {
  padding: 15px;
  font-size: 15px;
  border-radius: 12px;
  border: 1px solid #d1d5db;
}

.login-form input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.18);
}

/* Button */
.login-btn {
  margin-top: 16px;
  padding: 18px;
  font-size: 15.5px;
  font-weight: 700;
  border-radius: 14px;
  border: none;
  background: #4f46e5;
  color: white;
  cursor: pointer;
}

.login-btn:hover {
  background: #4338ca;
}

/* Responsive */
@media (max-width: 960px) {
  .login-container {
    flex-direction: column;
  }

  .login-card {
    width: 100%;
    padding: 40px;
  }

  .brand-inner {
    padding: 48px;
  }
}
</style>
