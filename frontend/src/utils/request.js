import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.response.use(res => {
  if (res.data.code === 200) {
    return res.data
  } else {
    ElMessage.error(res.data.msg)
    return Promise.reject(res.data)
  }
}, err => {
  ElMessage.error('网络请求异常')
  return Promise.reject(err)
})

export default request
