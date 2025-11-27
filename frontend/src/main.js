import './assets/main.css'
import 'primeicons/primeicons.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import formatWonPlugin from './plugins/formatWon'
import Money from './components/global/Money.vue'
import axios from 'axios'

const app = createApp(App)

axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    // 인터셉터를 무시하고, 사용자의 본래요청인 화면으로 라우팅
    return Promise.reject(error)
  },
)

app.use(createPinia())
app.use(router)
app.use(formatWonPlugin)
app.component('Money', Money)

app.mount('#app')
