<template>
  <div style="background:#fff;padding:20px;border-radius:4px">
    <el-form :inline="true" :model="queryForm">
      <el-form-item label="账号">
        <el-input v-model="queryForm.username" placeholder="请输入账号" />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="queryForm.realName" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="openDialog">新增</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="tableData" border style="width:100%;margin-top:15px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column label="状态">
        <template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '冻结' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{row}">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="freeze(row.id)" v-if="row.status === 1">冻结</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="loadData" style="margin-top:20px;text-align:right" />
  </div>
  <el-dialog v-model="dialogVisible" title="账号编辑" width="500px">
    <el-form :model="form">
      <el-form-item label="账号" prop="username">
        <el-input v-model="form.username" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" v-if="!form.id" />
      </el-form-item>
      <el-form-item label="姓名" prop="realName">
        <el-input v-model="form.realName" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" />
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
const queryForm = ref({username:'', realName:''})
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const dialogVisible = ref(false)
const form = ref({})
const loadData = async () => {
  const res = await request.get('/user/page', {params: {pageNum: pageNum.value, pageSize: pageSize.value, ...queryForm.value}})
  tableData.value = res.data.records
  total.value = res.data.total
}
const openDialog = (row = null) => {
  form.value = row ? {...row} : {}
  dialogVisible.value = true
}
const submit = async () => {
  form.value.id ? await request.put('/user', form.value) : await request.post('/user', form.value)
  dialogVisible.value = false
  ElMessage.success('操作成功')
  loadData()
}
const freeze = async (id) => {
  await request.put(`/user/freeze/${id}`)
  ElMessage.success('冻结成功')
  loadData()
}
const del = async (id) => {
  await ElMessageBox.confirm('确认删除?', '提示')
  await request.delete(`/user/${id}`)
  ElMessage.success('删除成功')
  loadData()
}
onMounted(loadData)
</script>
