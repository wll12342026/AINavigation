import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/theme.css'
import './assets/emerald.css'
import './assets/readability.css'

createApp(App).use(createPinia()).use(router).mount('#app')
