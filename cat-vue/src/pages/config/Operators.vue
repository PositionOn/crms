<template>
  <el-card>
    <template #header>操作员管理</template>

    <div style="display: flex; gap: 12px; align-items: center; margin-bottom: 12px">
      <el-input v-model="keyword" placeholder="按用户名/姓名搜索" style="width: 260px" clearable />
      <el-select v-model="status" clearable placeholder="状态" style="width: 140px">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="load">查询</el-button>
      <el-button type="success" @click="openCreate">新增</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="账号" width="140" />
      <el-table-column prop="name" label="姓名" width="140" />
      <el-table-column prop="role" label="角色" width="120">
        <template #default="{ row }">
          {{ roleLabel(row.role) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          {{ statusLabel(row.status) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="openResetPwd(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlgVisible" :title="dlgMode === 'create' ? '新增操作员' : '编辑操作员'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="账号" v-if="dlgMode === 'create'">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" v-if="dlgMode === 'create'">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="收银员" value="CASHIER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" v-if="dlgMode === 'edit'">
          <el-select v-model="form.status" style="width: 100%">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdVisible" title="重置密码" width="420px">
      <el-form :model="pwdForm" label-width="90px">
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="resetPwd">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '../../api/http'
import type { Operator } from '../../types/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref<Operator[]>([])
const keyword = ref('')
const status = ref('')

const dlgVisible = ref(false)
const dlgMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)

const form = reactive<any>({
  username: '',
  password: '',
  name: '',
  role: 'CASHIER',
  status: 'ENABLED'
})

const pwdVisible = ref(false)
const pwdForm = reactive<{ operatorId: number | null; newPassword: string }>({ operatorId: null, newPassword: '' })

function roleLabel(role: string) {
  if (role === 'ADMIN') return '管理员'
  if (role === 'CASHIER') return '收银员'
  return role
}

function statusLabel(s: string) {
  if (s === 'ENABLED') return '启用'
  if (s === 'DISABLED') return '停用'
  return s
}

async function load() {
  loading.value = true
  try {
    const res = await http.get('/operators', { params: { status: status.value || undefined, keyword: keyword.value || undefined } })
    rows.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dlgMode.value = 'create'
  editingId.value = null
  Object.assign(form, { username: '', password: '', name: '', role: 'CASHIER', status: 'ENABLED' })
  dlgVisible.value = true
}

function openEdit(row: Operator) {
  dlgMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, { name: row.name, role: row.role, status: row.status })
  dlgVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (dlgMode.value === 'create') {
      await http.post('/operators', {
        username: form.username,
        password: form.password,
        name: form.name,
        role: form.role
      })
      ElMessage.success('创建成功')
    } else {
      await http.put(`/operators/${editingId.value}`, {
        name: form.name,
        role: form.role,
        status: form.status
      })
      ElMessage.success('保存成功')
    }
    dlgVisible.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function openResetPwd(row: Operator) {
  pwdForm.operatorId = row.id
  pwdForm.newPassword = ''
  pwdVisible.value = true
}

async function resetPwd() {
  saving.value = true
  try {
    await http.post('/operators/reset-password', { operatorId: pwdForm.operatorId, newPassword: pwdForm.newPassword })
    ElMessage.success('重置成功')
    pwdVisible.value = false
  } catch (e: any) {
    ElMessage.error(e.message || '重置失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: Operator) {
  try {
    await ElMessageBox.confirm(`确认删除操作员「${row.username}」？`, '提示', { type: 'warning' })
    await http.delete(`/operators/${row.id}`)
    ElMessage.success('删除成功')
    await load()
  } catch (e: any) {
    if (e?.message) ElMessage.error(e.message)
  }
}

load()
</script>

