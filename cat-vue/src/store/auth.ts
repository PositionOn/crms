import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('rcms_token') || '',
    operatorName: localStorage.getItem('rcms_operator_name') || ''
  }),
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('rcms_token', token)
    },
    setOperatorName(name: string) {
      this.operatorName = name
      localStorage.setItem('rcms_operator_name', name)
    },
    logout() {
      this.token = ''
      this.operatorName = ''
      localStorage.removeItem('rcms_token')
      localStorage.removeItem('rcms_operator_name')
    }
  }
})

