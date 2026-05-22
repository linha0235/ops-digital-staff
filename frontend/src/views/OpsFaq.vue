<template>
  <div style="background:#fff;padding:20px;border-radius:4px">
    <el-form :inline="true" :model="queryForm">
      <el-form-item label="关键词">
        <el-input v-model="queryForm.keyword" placeholder="问题/答案关键词" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="openDialog">新增</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="tableData" border style="width:100%;margin-top:15px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="question" label="问题" />
      <el-table-column prop="category" label="分类" />
      <el-table-column label="同步状态">
        <template #default="{row}">
          <el-tag :type="row.isSynced === 1 ? 'success' : 'warning'">{{ row.isSynced === 1 ? '已同步' : '未同步' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{row}">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="success" @click="sync(row.id)" v-if="row.isSynced !== 1">同步到向量库</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="loadData" style="margin-top:20px;text-align:right" />
  </div>
  <el-dialog v-model="dialogVisible" title="FAQ编辑" width="600px">
    <el-form :model="form">
      <el-form-item label="问题" prop="question">
        <el-input v-model="form.question" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="答案" prop="answer">
        <el-input v-model="form.answer" type="textarea" :rows="4" />
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-input v-model="form.category" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible=false">取消</el-button>
      <el-button type="primary" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
const queryForm = ref({keyword:''})
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const dialogVisible = ref(false)
const form = ref({})
const loadData = async () => {
  const res = await request.get('/faq/page', {params: {pageNum: pageNum.value, pageSize: pageSize.value, ...queryForm.value}})
  tableData.value = res.data.records
  total.value = res.data.total
}
const openDialog = (row = null) => {
  form.value = row ? {...row} : {}
  dialogVisible.value = true
}
const submit = async () => {
  form.value.id ? await request.put('/faq', form.value) : await request.post('/faq', form.value)
  dialogVisible.value = false
  ElMessage.success('操作成功')
  loadData()
}
const sync = async (id) => {
  await request.post(`/faq/sync/${id}`)
  ElMessage.success('同步成功')
  loadData()
}
const del = async (id) => {
  await ElMessageBox.confirm('确认删除?', '提示')
  await request.delete(`/faq/${id}`)
  ElMessage.success('删除成功')
  loadData()
}
onMounted(loadData)
</script>
