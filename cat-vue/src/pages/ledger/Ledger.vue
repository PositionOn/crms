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
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
          <el-button size="small" type="primary" @click="openReceipt(row)">打印小票</el-button>
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

    <el-dialog v-model="receiptVisible" title="结账凭证" width="420px">
      <div class="receipt" v-if="receiptData">
        <div class="receipt__header">
          <div class="receipt__brand">收银小票</div>
          <div class="receipt__muted">Order No: {{ receiptData.order.orderNo }}</div>
        </div>
        <div class="receipt__section">
          <div class="receipt__row">
            <span>桌台</span>
            <span>{{ receiptData.order.tableCode || '-' }}</span>
          </div>
          <div class="receipt__row">
            <span>开台</span>
            <span>{{ receiptData.order.openedAt }}</span>
          </div>
          <div class="receipt__row">
            <span>结账</span>
            <span>{{ receiptData.order.closedAt || '-' }}</span>
          </div>
          <div class="receipt__row">
            <span>状态</span>
            <span>{{ orderStatusLabel(receiptData.order.status) }}</span>
          </div>
        </div>

        <div class="receipt__section">
          <div class="receipt__row receipt__row--head">
            <span>菜品</span>
            <span>数量</span>
            <span>金额</span>
          </div>
          <div v-for="it in receiptData.items" :key="it.id" class="receipt__row receipt__row--item">
            <span>{{ it.dishNameSnapshot }}</span>
            <span>x{{ it.quantity }}</span>
            <span>￥{{ money(it.lineAmount) }}</span>
          </div>
        </div>

        <div class="receipt__section">
          <div class="receipt__row">
            <span>折前金额</span>
            <span>￥{{ money(receiptData.order.amountBeforeDiscount) }}</span>
          </div>
          <div class="receipt__row">
            <span>折扣</span>
            <span>-￥{{ money(receiptData.order.discountAmount) }}</span>
          </div>
          <div class="receipt__row">
            <span>折扣原因</span>
            <span>{{ receiptData.order.discountReason || '-' }}</span>
          </div>
        </div>

        <div class="receipt__section">
          <div class="receipt__row receipt__row--total">
            <span>应收</span>
            <span>￥{{ money(receiptData.order.amountAfterDiscount) }}</span>
          </div>
        </div>

        <div class="receipt__footer">感谢光临</div>
      </div>
      <template #footer>
        <el-button @click="receiptVisible = false">关闭</el-button>
        <el-button type="primary" @click="printReceipt">打印</el-button>
      </template>
    </el-dialog>
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
const receiptVisible = ref(false)
const receiptData = ref<OrderDetail | null>(null)

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

async function openReceipt(row: any) {
  if (!row?.id) return
  try {
    const res = await http.get(`/orders/${row.id}`)
    receiptData.value = res.data
    receiptVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '加载小票失败')
  }
}

function printReceipt() {
  window.print()
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
.receipt {
  border: 1px dashed #d9d9d9;
  padding: 12px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.5;
}
.receipt__header {
  text-align: center;
  margin-bottom: 6px;
}
.receipt__brand {
  font-weight: 700;
  font-size: 16px;
}
.receipt__muted {
  color: #666;
  font-size: 12px;
}
.receipt__section {
  margin: 8px 0;
}
.receipt__row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.receipt__row--head {
  font-weight: 600;
  border-bottom: 1px dashed #ccc;
  padding-bottom: 4px;
}
.receipt__row--head span:nth-child(1),
.receipt__row--item span:nth-child(1) {
  flex: 1;
  min-width: 0;
  word-break: break-all;
}
.receipt__row--head span:nth-child(2),
.receipt__row--item span:nth-child(2) {
  flex: 0 0 50px;
  text-align: center;
}
.receipt__row--head span:nth-child(3),
.receipt__row--item span:nth-child(3) {
  flex: 0 0 70px;
  text-align: right;
}
.receipt__row--total {
  font-weight: 700;
  border-top: 1px dashed #ccc;
  padding-top: 4px;
}
.receipt__footer {
  text-align: center;
  margin-top: 8px;
}
</style>

