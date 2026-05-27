<template>
  <el-row :gutter="20">
    <el-col :span="6">
      <el-card shadow="hover" style="text-align:center">
        <h2>运维账号总数</h2>
        <p style="font-size:32px;margin-top:20px;color:#409EFF">{{ stats.userCount }}</p>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover" style="text-align:center">
        <h2>知识库FAQ数</h2>
        <p style="font-size:32px;margin-top:20px;color:#67C23A">{{ stats.faqCount }}</p>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover" style="text-align:center">
        <h2>待处理工单</h2>
        <p style="font-size:32px;margin-top:20px;color:#E6A23C">{{ stats.pendingTicketCount }}</p>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover" style="text-align:center">
        <h2>今日处理工单</h2>
        <p style="font-size:32px;margin-top:20px;color:#F56C6C">{{ stats.todayHandledCount }}</p>
      </el-card>
    </el-col>
  </el-row>
  <div style="margin-top:30px;padding:20px;background:#fff;border-radius:4px">
    <h3>系统简介</h3>
    <p>运维数字员工系统基于AI+RPA技术构建，实现运维知识库自助问答、运维账号全生命周期管理、工单自动流转闭环，替代人类员工完成重复性高、规则性强的运维任务。</p>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'

const stats = ref({
  userCount: 0,
  faqCount: 0,
  pendingTicketCount: 0,
  todayHandledCount: 0
})

onMounted(async () => {
  try {
    const res = await request.get('/dashboard/stats')
    stats.value = res.data
  } catch (e) {}
})
</script>
