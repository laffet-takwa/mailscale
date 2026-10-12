import axios, { AxiosError } from 'axios'
import { authStore } from '../lib/authStore'
import type { ApiError } from '../types'

const client = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.request.use((config) => {
  const token = authStore.getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      const isAuthRoute = window.location.pathname.startsWith('/login')
        || window.location.pathname.startsWith('/register')
      if (!isAuthRoute) {
        authStore.logout()
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  }
)

export default client
