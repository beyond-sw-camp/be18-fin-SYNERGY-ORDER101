import './assets/main.css'
import 'primeicons/primeicons.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import formatWonPlugin from './plugins/formatWon'
import Money from './components/global/Money.vue'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(formatWonPlugin)
app.component('Money', Money)

app.mount('#app')
