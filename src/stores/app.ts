import { defineStore } from 'pinia'
import type { ToastMessage } from '@/types'

let toastId = 0

export const useAppStore = defineStore('app', {
  state: () => ({ campus: '主校区', toasts: [] as ToastMessage[] }),
  actions: {
    notify(text: string, type: ToastMessage['type'] = 'success') {
      const id = ++toastId
      this.toasts.push({ id, text, type })
      window.setTimeout(() => { this.toasts = this.toasts.filter((item) => item.id !== id) }, 2800)
    },
  },
})
