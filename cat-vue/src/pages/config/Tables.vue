<template>
  <el-card>
    <template #header>桌台管理</template>

    <div style="display: flex; gap: 12px; align-items: center; margin-bottom: 12px">
      <el-select v-model="filterStatus" clearable placeholder="状态" style="width: 140px">
        <el-option label="空闲" value="FREE" />
        <el-option label="占用" value="OCCUPIED" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="load">查询</el-button>
      <el-button type="success" @click="openCreate">新增</el-button>
      <el-button type="warning" :disabled="selectedIds.length === 0" @click="batchSet('FREE')">批量设为空闲</el-button>
      <el-button type="danger" :disabled="selectedIds.length === 0" @click="batchSet('OCCUPIED')">批量设为占用</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" @selection-change="onSelect" style="width: 100%">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="code" label="桌台号" width="120" />
      <el-table-column prop="type" label="类型" width="120">
        <template #default="{ row }">
          {{ typeLabel(row.type) }}
        </template>
      </el-table-column>
      <el-table-column prop="capacity" label="人数" width="100" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          {{ statusLabel(row.status) }}
        </template>
      </el-table-column>
      <el-table-column prop="currentOrderId" label="当前订单" width="120" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlgVisible" :title="dlgMode === 'create' ? '新增桌台' : '编辑桌台'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="桌台号">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="大厅" value="HALL" />
            <el-option label="包间" value="ROOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="人数">
          <el-input-number v-model="form.capacity" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '../../api/http'
import type { DiningTable } from '../../types/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref<DiningTable[]>([])
const filterStatus = ref('')
const selectedIds = ref<number[]>([])

const dlgVisible = ref(false)
const dlgMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)

const form = reactive<any>({
  code: '',
  type: 'HALL',
  capacity: 4
})

function typeLabel(t: string) {
  if (t === 'HALL') return '大厅'
  if (t === 'ROOM') return '包间'
  return t
}

function statusLabel(s: string) {
  if (s === 'FREE') return '空闲'
  if (s === 'OCCUPIED') return '占用'
  return s
}

function onSelect(list: DiningTable[]) {
  selectedIds.value = list.map((i) => i.id)
}

async function load() {
  loading.value = true
  try {
    const res = await http.get('/tables', { params: { status: filterStatus.value || undefined } })
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
  Object.assign(form, { code: '', type: 'HALL', capacity: 4 })
  dlgVisible.value = true
}

function openEdit(row: DiningTable) {
  dlgMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, { code: row.code, type: row.type, capacity: row.capacity })
  dlgVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (dlgMode.value === 'create') {
      await http.post('/tables', { code: form.code, type: form.type, capacity: form.capacity })
      ElMessage.success('创建成功')
    } else {
      await http.put(`/tables/${editingId.value}`, { code: form.code, type: form.type, capacity: form.capacity })
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

async function remove(row: DiningTable) {
  try {
    await ElMessageBox.confirm(`确认删除桌台「${row.code}」？`, '提示', { type: 'warning' })
    await http.delete(`/tables/${row.id}`)
    ElMessage.success('删除成功')
    await load()
  } catch (e: any) {
    if (e?.message) ElMessage.error(e.message)
  }
}

async function batchSet(status: 'FREE' | 'OCCUPIED') {
  saving.value = true
  try {
    await http.post('/tables/batch-status', { ids: selectedIds.value, status })
    ElMessage.success('批量更新成功')
    await load()
  } catch (e: any) {
    ElMessage.error(e.message || '批量更新失败')
  } finally {
    saving.value = false
  }
}

load()
</script>

