<template>
  <div style="background:#fff;padding:20px;border-radius:4px;height:80vh;display:flex;flex-direction:column">
    <div style="flex:1;overflow-y:auto;padding:10px;background:#f8f9fa;border-radius:4px;margin-bottom:15px" ref="scrollBox">
      <div v-for="(msg, idx) in messages" :key="idx" style="margin-bottom:15px">
        <div style="text-align:right" v-if="msg.isUser">
          <span style="background:#409EFF;color:#fff;padding:8px 12px;border-radius:8px;display:inline-block;max-width:70%">{{msg.content}}</span>
        </div>
        <div v-else>
          <span style="background:#e5e7eb;padding:8px 12px;border-radius:8px;display:inline-block;max-width:70%">{{msg.content}}<span v-if="msg.typing" class="cursor-blink">|</span></span>
        </div>
      </div>
    </div>
    <div style="display:flex;gap:10px;margin-bottom:10px">
      <el-button @click="clearHistory" type="danger" size="small">清空对话历史</el-button>
      <el-button @click="openTicketDialog" type="warning" size="small">转人工处理</el-button>
    </div>
    <div style="display:flex;gap:10px">
      <el-input v-model="inputMsg" placeholder="请输入您的问题" style="flex:1" @keyup.enter="send" />
      <el-button type="primary" @click="send" :loading="loading">发送</el-button>
    </div>
  </div>
  <el-dialog v-model="ticketDialogVisible" title="提交人工报障工单" width="500px">
    <el-form :model="ticketForm">
      <el-form-item label="您的姓名">
        <el-input v-model="ticketForm.reporterName" />
      </el-form-item>
      <el-form-item label="联系方式">
        <el-input v-model="ticketForm.reporterPhone" />
      </el-form-item>
      <el-form-item label="问题标题">
        <el-input v-model="ticketForm.problemTitle" />
      </el-form-item>
      <el-form-item label="问题描述">
        <el-input v-model="ticketForm.problemDesc" type="textarea" :rows="4" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="ticketDialogVisible=false">取消</el-button>
      <el-button type="primary" @click="submitTicket">提交工单</el-button>
    </template>
  </el-dialog>
</template>
<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
const STORAGE_KEY = 'ops_digital_staff_chat_history'
const scrollBox = ref(null)
const messages = ref([{isUser: false, content:'您好，我是运维数字员工，请问有什么可以帮助您的？'}])
const inputMsg = ref('')
const loading = ref(false)
let abortController = null
const ticketDialogVisible = ref(false)
const ticketForm = ref({
  reporterName: '',
  reporterPhone: '',
  problemTitle: '',
  problemDesc: ''
})

onMounted(() => {
  const savedHistory = localStorage.getItem(STORAGE_KEY)
  if (savedHistory) {
    try {
      messages.value = JSON.parse(savedHistory)
    } catch (e) {}
  }
  scrollToBottom()
})

watch(messages, (newVal) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(newVal))
  nextTick(scrollToBottom)
}, { deep: true })

const scrollToBottom = () => {
  if (scrollBox.value) {
    scrollBox.value.scrollTop = scrollBox.value.scrollHeight
  }
}

const clearHistory = () => {
  messages.value = [{isUser: false, content:'您好，我是运维数字员工，请问有什么可以帮助您的？'}]
}

const send = async () => {
  if(!inputMsg.value.trim() || loading.value) return
  const currentQuestion = inputMsg.value
  messages.value.push({isUser: true, content: currentQuestion})
  loading.value = true
  inputMsg.value = ''
  const botMsgIdx = messages.value.push({isUser: false, content: '', typing: true}) - 1

  abortController = new AbortController()

  // typewriter state - scoped to this send() call
  let typingTimer = null
  let stopped = false
  let currentTarget = ''
  let streamEnded = false

  const stopTyping = () => {
    stopped = true
    if (typingTimer) {
      clearInterval(typingTimer)
      typingTimer = null
    }
    if (messages.value[botMsgIdx]) {
      messages.value[botMsgIdx].typing = false
    }
  }

  const finishTyping = async () => {
    // signal end-of-stream, let interval finish naturally
    if (stopped) return
    streamEnded = true
    // wait for interval to catch up and clear itself
    while (typingTimer) {
      await new Promise(r => setTimeout(r, 50))
    }
  }

  const typewriter = (targetText) => {
    if (stopped || streamEnded) return
    currentTarget = targetText
    const msg = messages.value[botMsgIdx]
    if (!msg) return
    msg.typing = true

    // only start one interval; it always reads the latest currentTarget
    if (typingTimer) return

    typingTimer = setInterval(() => {
      if (stopped) {
        clearInterval(typingTimer)
        typingTimer = null
        return
      }
      const m = messages.value[botMsgIdx]
      if (!m) {
        clearInterval(typingTimer)
        typingTimer = null
        return
      }
      const target = currentTarget
      const cur = m.content.length
      if (!target || cur >= target.length) {
        if (streamEnded) {
          // stream done and we've caught up — finish
          clearInterval(typingTimer)
          typingTimer = null
          m.typing = false
        }
        return
      }
      const chunk = Math.min(3, target.length - cur)
      m.content = target.substring(0, cur + chunk)
    }, 25)
  }

  try {
    const response = await fetch('/api/faq/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: currentQuestion }),
      signal: abortController.signal
    })
    if (!response.ok) throw new Error('HTTP ' + response.status)

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const parts = buffer.split('\n\n')
      buffer = parts.pop()
      for (const part of parts) {
        const dataLines = part.split('\n')
          .filter(l => l.startsWith('data:'))
          .map(l => l.substring(5).trim())
        const content = dataLines.join('\n')
        if (content) typewriter(content)
      }
    }
    // stream ended — wait for typewriter to finish
    await finishTyping()
  } catch (e) {
    if (e.name !== 'AbortError') {
      stopTyping()
      if (messages.value[botMsgIdx]) {
        messages.value[botMsgIdx].content = '抱歉，服务暂时不可用，请稍后重试。'
        messages.value[botMsgIdx].typing = false
      }
      ElMessage.error('请求失败：' + e.message)
    } else {
      stopTyping()
    }
  } finally {
    loading.value = false
    abortController = null
  }
}

const openTicketDialog = () => {
  ticketForm.value = {
    reporterName: '',
    reporterPhone: '',
    problemTitle: '',
    problemDesc: ''
  }
  ticketDialogVisible.value = true
}

const submitTicket = async () => {
  await request.post('/ticket', ticketForm.value)
  ticketDialogVisible.value = false
  ElMessage.success('工单已提交，运维人员将尽快处理')
}
</script>
<style scoped>
.cursor-blink {
  animation: blink 0.6s infinite;
  color: #409EFF;
  font-weight: bold;
}
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}
</style>
