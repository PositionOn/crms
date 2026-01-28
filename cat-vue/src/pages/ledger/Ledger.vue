<template>
  <el-card>
    <template #header>台账</template>
    <el-form inline>
      <el-form-item label="桌台">
        <el-input v-model="filters.tableCode" placeholder="如 A01" style="width: 120px" />
      </el-form-item>
      <el-form-item label="订单号">
        <el-input v-model="filters.orderNo" placeholder="RCMS..." style="width: 200px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable style="width: 120px" placeholder="全部状态">
          <el-option label="进行中" value="OPEN" />
          <el-option label="已结账" value="CLOSED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间">
        <el-date-picker
          v-model="filters.range"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
        />
      </el-form-item>
      <el-button type="primary" :loading="loading" @click="load">查询</el-button>
      <el-button @click="exportExcel('summary')">导出汇总</el-button>
      <el-button @click="exportExcel('detail')">导出明细</el-button>
    </el-form>

    <el-table :data="rows" v-loading="loading" style="width: 100%; margin-top: 12px">
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column prop="tableCode" label="桌台" width="80" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          {{ orderStatusLabel(row.status) }}
        </template>
      </el-table-column>
      <el-table-column prop="openedAt" label="开台时间" width="180" />
      <el-table-column prop="closedAt" label="结账时间" width="180" />
      <el-table-column prop="amountBeforeDiscount" label="折前" width="100">
        <template #default="{ row }">￥{{ money(row.amountBeforeDiscount) }}</template>
      </el-table-column>
      <el-table-column prop="discountAmount" label="折扣" width="100">
        <template #default="{ row }">￥{{ money(row.discountAmount) }}</template>
      </el-table-column>
      <el-table-column prop="amountAfterDiscount" label="实收" width="100">
        <template #default="{ row }">￥{{ money(row.amountAfterDiscount) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailVisible" title="订单详情" size="60%">
      <template v-if="detail && currentRow">
        <el-descriptions title="基本信息" :column="2" border>
          <el-descriptions-item label="订单号">
            {{ currentRow.orderNo }}
          </el-descriptions-item>
          <el-descriptions-item label="桌台">
            {{ currentRow.tableCode }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ orderStatusLabel(detail.order.status) }}
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">
            {{ detail.orderPaymentMethod }}
          </el-descriptions-item>
          <el-descriptions-item label="开台时间">
            {{ detail.order.openedAt }}
          </el-descriptions-item>
          <el-descriptions-item label="结账时间">
            {{ detail.order.closedAt || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="折前金额">
            ￥{{ money(detail.order.amountBeforeDiscount) }}
          </el-descriptions-item>
          <el-descriptions-item label="折扣金额">
            ￥{{ money(detail.order.discountAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="实收金额">
            ￥{{ money(detail.order.amountAfterDiscount) }}
          </el-descriptions-item>
          <el-descriptions-item label="折扣原因">
            {{ detail.order.discountReason || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-top: 16px">点餐明细</h4>
        <el-table :data="detail.items" size="small" border>
          <el-table-column prop="dishNameSnapshot" label="菜品" />
          <el-table-column prop="unitPriceSnapshot" label="单价" width="100">
            <template #default="{ row }">￥{{ money(row.unitPriceSnapshot) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="lineAmount" label="小计" width="120">
            <template #default="{ row }">￥{{ money(row.lineAmount) }}</template>
          </el-table-column>
        </el-table>

        <h4 style="margin-top: 16px">支付信息</h4>
        <el-table :data="detail.payments" size="small" border v-if="detail.payments?.length">
          <el-table-column prop="method" label="支付方式">
            <template #default="{ row }">
              {{ payMethodLabel(row.method) }}
            </template>
          </el-table-column>
          <el-table-column prop="payAmount" label="支付金额" width="120">
            <template #default="{ row }">￥{{ money(row.payAmount) }}</template>
          </el-table-column>
          <el-table-column prop="changeAmount" label="找零" width="120">
            <template #default="{ row }">￥{{ money(row.changeAmount) }}</template>
          </el-table-column>
          <el-table-column prop="paidAt" label="支付时间" width="180" />
        </el-table>
        <div v-else style="margin-top: 8px; color: #999">暂无支付记录</div>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../../api/http'
import type { ConsumeOrder, OrderItem, Payment } from '../../types/api'

type OrderDetail = { order: ConsumeOrder; items: OrderItem[]; payments: Payment[] }

const loading = ref(false)
const rows = ref<any[]>([])
const detailVisible = ref(false)
const detail = ref<OrderDetail | null>(null)
const currentRow = ref<any | null>(null)

const filters = reactive({
  tableCode: '',
  orderNo: '',
  status: '',
  range: [] as string[]
})

function money(v: any) {
  const n = Number(v ?? 0)
  if (Number.isFinite(n)) return n.toFixed(2)
  return '0.00'
}

function orderStatusLabel(s: string) {
  if (s === 'OPEN') return '进行中'
  if (s === 'CLOSED') return '已结账'
  if (s === 'CANCELLED') return '已取消'
  return s
}

function payMethodLabel(m: string) {
  if (m === 'CASH') return '现金'
  if (m === 'WECHAT') return '微信'
  if (m === 'ALIPAY') return '支付宝'
  if (m === 'CARD') return '刷卡'
  return m
}

async function load() {
  loading.value = true
  try {
    const [startTime, endTime] = filters.range || []
    const res = await http.get('/ledger/orders', {
      params: {
        tableCode: filters.tableCode || undefined,
        orderNo: filters.orderNo || undefined,
        status: filters.status || undefined,
        startTime: startTime || undefined,
        endTime: endTime || undefined
      }
    })
    rows.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(row: any) {
  if (!row?.id) return
  currentRow.value = row
  try {
    const res = await http.get(`/orders/${row.id}`)
    detail.value = res.data
    detailVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

async function exportExcel(mode: 'detail' | 'summary') {
  const [startTime, endTime] = filters.range || []
  try {
    const blob = await http.get('/ledger/export', {
      params: {
        mode,
        tableCode: filters.tableCode || undefined,
        orderNo: filters.orderNo || undefined,
        status: filters.status || undefined,
        startTime: startTime || undefined,
        endTime: endTime || undefined
      },
      responseType: 'blob'
    })

    const url = URL.createObjectURL(blob as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `ledger-${mode}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  }
}

load()
</script>

<style scoped>
</style>

