<template>
  <el-card>
    <template #header>菜品管理</template>

    <div style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-bottom: 12px">
      <el-select v-model="filters.categoryId" clearable placeholder="分类" style="width: 180px">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="状态" style="width: 140px">
        <el-option label="上架" value="ON" />
        <el-option label="下架" value="OFF" />
      </el-select>
      <el-input v-model="filters.keyword" placeholder="菜品名关键词" style="width: 220px" clearable />
      <el-button type="primary" :loading="loading" @click="load">查询</el-button>
      <el-button type="success" @click="openCreate">新增</el-button>
      <el-button type="warning" :disabled="selectedIds.length === 0" @click="batchStatus('ON')">批量上架</el-button>
      <el-button type="danger" :disabled="selectedIds.length === 0" @click="batchStatus('OFF')">批量下架</el-button>
      <el-button :disabled="selectedIds.length === 0" @click="openBatchPrice">批量调价</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" @selection-change="onSelect" style="width: 100%">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column label="分类" width="160">
        <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="120" />
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

    <el-dialog v-model="dlgVisible" :title="dlgMode === 'create' ? '新增菜品' : '编辑菜品'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="状态" v-if="dlgMode === 'edit'">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="上架" value="ON" />
            <el-option label="下架" value="OFF" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="priceVisible" title="批量调价" width="420px">
      <el-form :model="priceForm" label-width="90px">
        <el-form-item label="新价格">
          <el-input-number v-model="priceForm.newPrice" :min="0.01" :precision="2" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priceVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doBatchPrice">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '../../api/http'
import type { Dish, DishCategory } from '../../types/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref<Dish[]>([])
const selectedIds = ref<number[]>([])

const categories = ref<DishCategory[]>([])
const catMap = computed(() => new Map(categories.value.map((c) => [c.id, c.name])))
function categoryName(id: number) {
  return catMap.value.get(id) || String(id)
}

const filters = reactive<{ categoryId?: number | ''; status?: string; keyword?: string }>({
  categoryId: '',
  status: '',
  keyword: ''
})

const dlgVisible = ref(false)
const dlgMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)

const form = reactive<any>({
  name: '',
  categoryId: null as number | null,
  price: 1,
  status: 'ON'
})

const priceVisible = ref(false)
const priceForm = reactive<{ newPrice: number }>({ newPrice: 1 })

function statusLabel(s: string) {
  if (s === 'ON') return '上架'
  if (s === 'OFF') return '下架'
  return s
}

function onSelect(list: Dish[]) {
  selectedIds.value = list.map((i) => i.id)
}

async function loadCategories() {
  const res = await http.get('/dish-categories')
  categories.value = res.data || []
}

async function load() {
  loading.value = true
  try {
    const res = await http.get('/dishes', {
      params: {
        categoryId: filters.categoryId || undefined,
        status: filters.status || undefined,
        keyword: filters.keyword || undefined
      }
    })
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
  Object.assign(form, { name: '', categoryId: categories.value[0]?.id ?? null, price: 1, status: 'ON' })
  dlgVisible.value = true
}

function openEdit(row: Dish) {
  dlgMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, { name: row.name, categoryId: row.categoryId, price: row.price, status: row.status })
  dlgVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (dlgMode.value === 'create') {
      await http.post('/dishes', { name: form.name, categoryId: form.categoryId, price: form.price })
      ElMessage.success('创建成功')
    } else {
      await http.put(`/dishes/${editingId.value}`, { name: form.name, categoryId: form.categoryId, price: form.price, status: form.status })
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

async function remove(row: Dish) {
  try {
    await ElMessageBox.confirm(`确认删除菜品「${row.name}」？`, '提示', { type: 'warning' })
    await http.delete(`/dishes/${row.id}`)
    ElMessage.success('删除成功')
    await load()
  } catch (e: any) {
    if (e?.message) ElMessage.error(e.message)
  }
}

async function batchStatus(status: 'ON' | 'OFF') {
  saving.value = true
  try {
    await http.post('/dishes/batch-status', { ids: selectedIds.value, status })
    ElMessage.success('批量更新成功')
    await load()
  } catch (e: any) {
    ElMessage.error(e.message || '批量更新失败')
  } finally {
    saving.value = false
  }
}

function openBatchPrice() {
  priceForm.newPrice = 1
  priceVisible.value = true
}

async function doBatchPrice() {
  saving.value = true
  try {
    await http.post('/dishes/batch-price', { ids: selectedIds.value, newPrice: priceForm.newPrice })
    ElMessage.success('调价成功')
    priceVisible.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e.message || '调价失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  // 并行加载分类和菜品，避免 async setup 导致需要 Suspense
  loadCategories()
  load()
})
</script>

