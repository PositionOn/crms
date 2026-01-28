<template>
  <el-card>
    <template #header>菜品分类</template>

    <div style="display: flex; gap: 12px; align-items: center; margin-bottom: 12px">
      <el-button type="success" @click="openCreate">新增</el-button>
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="sort" label="排序" width="120" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          {{ statusLabel(row.status) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlgVisible" :title="dlgMode === 'create' ? '新增分类' : '编辑分类'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
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
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '../../api/http'
import type { DishCategory } from '../../types/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref<DishCategory[]>([])

const dlgVisible = ref(false)
const dlgMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)

const form = reactive<any>({
  name: '',
  sort: 0,
  status: 'ENABLED'
})

function statusLabel(s: string) {
  if (s === 'ENABLED') return '启用'
  if (s === 'DISABLED') return '停用'
  return s
}

async function load() {
  loading.value = true
  try {
    const res = await http.get('/dish-categories')
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
  Object.assign(form, { name: '', sort: 0, status: 'ENABLED' })
  dlgVisible.value = true
}

function openEdit(row: DishCategory) {
  dlgMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, { name: row.name, sort: row.sort, status: row.status })
  dlgVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (dlgMode.value === 'create') {
      await http.post('/dish-categories', { name: form.name, sort: form.sort })
      ElMessage.success('创建成功')
    } else {
      await http.put(`/dish-categories/${editingId.value}`, { name: form.name, sort: form.sort, status: form.status })
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

async function remove(row: DishCategory) {
  try {
    await ElMessageBox.confirm(`确认删除分类「${row.name}」？`, '提示', { type: 'warning' })
    await http.delete(`/dish-categories/${row.id}`)
    ElMessage.success('删除成功')
    await load()
  } catch (e: any) {
    if (e?.message) ElMessage.error(e.message)
  }
}

load()
</script>

