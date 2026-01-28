<template>
  <el-card>
    <template #header>折扣规则</template>

    <div style="display: flex; gap: 12px; align-items: center; margin-bottom: 12px; flex-wrap: wrap">
      <el-select v-model="enabled" clearable placeholder="启用状态" style="width: 160px">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="load">查询</el-button>
      <el-button type="success" @click="openCreate">新增</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" width="160" />
      <el-table-column prop="type" label="类型" width="200">
        <template #default="{ row }">
          {{ typeLabel(row.type) }}
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="120" />
      <el-table-column prop="enabled" label="启用" width="120">
        <template #default="{ row }">{{ row.enabled === 1 ? '启用' : '停用' }}</template>
      </el-table-column>
      <el-table-column prop="paramsJson" label="参数(JSON)" min-width="260" />
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="success" v-if="row.enabled !== 1" @click="toggle(row, true)">启用</el-button>
          <el-button size="small" type="warning" v-else @click="toggle(row, false)">停用</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlgVisible" :title="dlgMode === 'create' ? '新增折扣规则' : '编辑折扣规则'" width="640px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="FIXED_RATE（固定折扣）" value="FIXED_RATE" />
            <el-option label="FULL_REDUCTION（满减）" value="FULL_REDUCTION" />
            <el-option label="MEMBER（会员）" value="MEMBER" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" />
        </el-form-item>
        <el-form-item label="参数(JSON)">
          <el-input v-model="form.paramsJson" type="textarea" :rows="5" placeholder='例如：{"rate":0.9} 或 {"threshold":100,"minus":10}' />
        </el-form-item>
        <el-form-item label="启用" v-if="dlgMode === 'edit'">
          <el-switch v-model="form.enabledBool" />
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
import type { DiscountRule } from '../../types/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref<DiscountRule[]>([])
const enabled = ref<number | ''>('')

const dlgVisible = ref(false)
const dlgMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)

const form = reactive<any>({
  name: '',
  type: 'FIXED_RATE',
  priority: 0,
  paramsJson: '{"rate":0.9}',
  enabledBool: true
})

function typeLabel(t: string) {
  if (t === 'FIXED_RATE') return '固定折扣'
  if (t === 'FULL_REDUCTION') return '满减'
  if (t === 'MEMBER') return '会员折扣'
  return t
}

async function load() {
  loading.value = true
  try {
    const res = await http.get('/discount-rules', { params: { enabled: enabled.value === '' ? undefined : enabled.value } })
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
  Object.assign(form, { name: '', type: 'FIXED_RATE', priority: 0, paramsJson: '{"rate":0.9}', enabledBool: true })
  dlgVisible.value = true
}

function openEdit(row: DiscountRule) {
  dlgMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    type: row.type,
    priority: row.priority,
    paramsJson: row.paramsJson,
    enabledBool: row.enabled === 1
  })
  dlgVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (dlgMode.value === 'create') {
      await http.post('/discount-rules', { name: form.name, type: form.type, paramsJson: form.paramsJson, priority: form.priority })
      ElMessage.success('创建成功')
    } else {
      await http.put(`/discount-rules/${editingId.value}`, {
        name: form.name,
        type: form.type,
        paramsJson: form.paramsJson,
        enabled: form.enabledBool ? 1 : 0,
        priority: form.priority
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

async function toggle(row: DiscountRule, on: boolean) {
  saving.value = true
  try {
    await http.post(`/discount-rules/${row.id}/${on ? 'enable' : 'disable'}`)
    ElMessage.success('操作成功')
    await load()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: DiscountRule) {
  try {
    await ElMessageBox.confirm(`确认删除规则「${row.name}」？`, '提示', { type: 'warning' })
    await http.delete(`/discount-rules/${row.id}`)
    ElMessage.success('删除成功')
    await load()
  } catch (e: any) {
    if (e?.message) ElMessage.error(e.message)
  }
}

load()
</script>

