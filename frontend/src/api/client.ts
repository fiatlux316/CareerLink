import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

client.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error),
)

client.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API response error', error)
    return Promise.reject(error)
  },
)

export default client
