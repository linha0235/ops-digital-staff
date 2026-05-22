<template>
  <div style="background:#fff;padding:20px;border-radius:4px;height:80vh;display:flex;flex-direction:column">
    <div style="flex:1;overflow-y:auto;padding:10px;background:#f8f9fa;border-radius:4px;margin-bottom:15px">
      <div v-for="(msg, idx) in messages" :key="idx" style="margin-bottom:15px">
        <div style="text-align:right" v-if="msg.isUser">
          <span style="background:#409EFF;color:#fff;padding:8px 12px;border-radius:8px;display:inline-block;max-width:70%">{{msg.content}}</span>
        </div>
        <div v-else>
          <span style="background:#e5e7eb;padding:8px 12px;border-radius:8px;display:inline-block;max-width:70%">{{msg.content}}</span>
        </div>
      </div>
    </div>
    <div style="display:flex;gap:10px">
      <el-input v-model="inputMsg" placeholder="请输入您的问题" style="flex:1" />
      <el-button type="primary" @click="send" :loading="loading">发送</el-button>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import request from '../utils/request'
const messages = ref([{isUser: false, content:'您好，我是运维数字员工，请问有什么可以帮助您的？'}])
const inputMsg = ref('')
const loading = ref(false)
const send = async () => {
  if(!inputMsg.value.trim()) return
  messages.value.push({isUser: true, content: inputMsg.value})
  loading.value = true
  const res = await request.get('/faq/chat', {params:{question: inputMsg.value}})
  messages.value.push({isUser: false, content: res.data})
  inputMsg.value = ''
  loading.value = false
}
</script>
