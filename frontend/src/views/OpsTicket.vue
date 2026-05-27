<template>
  <div style="background:#fff;padding:20px;border-radius:4px">
    <el-form :inline="true" :model="queryForm">
      <el-form-item label="工单状态">
        <el-select v-model="queryForm.status" placeholder="请选择">
          <el-option label="全部" :value="null" />
          <el-option label="待处理" :value="0" />
          <el-option label="处理中" :value="1" />
          <el-option label="已完成" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="tableData" border style="width:100%;margin-top:15px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="problemTitle" label="问题标题" />
      <el-table-column prop="reporterName" label="报障人" />
      <el-table-column label="状态">
        <template #default="{row}">
          <el-tag>{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{row}">
          <el-button size="small" @click="openHandleDialog(row)" v-if="row.status !== 2">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="loadData" style="margin-top:20px;text-align:right" />
  </div>
  <el-dialog v-model="handleDialogVisible" title="工单处理" width="600px">
    <el-form :model="handleForm">
      <el-form-item label="处理人">
        <el-select v-model="handleForm.handlerId" placeholder="请选择处理人">
          <el-option v-for="u in userList" :key="u.id" :label="u.realName" :value="u.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理结果">
        <el-input v-model="handleForm.handleResult" type="textarea" :rows="4" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleDialogVisible=false">取消</el-button>
      <el-button type="primary" @click="submitHandle">提交处理</el-button>
    </template>
  </el-dialog>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
const statusMap = ['待处理', '处理中', '已完成']
const queryForm = ref({status: null})
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const userList = ref([])
const handleDialogVisible = ref(false)
const handleForm = ref({})

const loadData = async () => {
  const res = await request.get('/ticket/page', {params: {pageNum: pageNum.value, pageSize: pageSize.value, ...queryForm.value}})
  tableData.value = res.data.records
  total.value = res.data.total
}

const loadUsers = async () => {
  const res = await request.get('/user/page', {params: {pageNum: 1, pageSize: 100}})
  userList.value = res.data.records
}

const openHandleDialog = (row) => {
  handleForm.value = {id: row.id, handlerId: null, handleResult: ''}
  handleDialogVisible.value = true
}

const submitHandle = async () => {
  await request.put(`/ticket/handle/${handleForm.value.id}`, null, {
    params: {
      handlerId: handleForm.value.handlerId,
      handleResult: handleForm.value.handleResult
    }
  })
  handleDialogVisible.value = false
  ElMessage.success('处理完成')
  loadData()
}

onMounted(() => {
  loadData()
  loadUsers()
})
</script>
