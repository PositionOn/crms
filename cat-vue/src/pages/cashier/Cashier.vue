<template>
  <el-row :gutter="12">
    <el-col :span="8">
      <el-card>
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span>桌台</span>
            <el-button size="small" :loading="loadingTables" @click="loadTables">刷新</el-button>
          </div>
        </template>

        <div style="display: flex; gap: 8px; margin-bottom: 12px">
          <el-select v-model="tableFilter" clearable placeholder="状态" style="width: 140px">
            <el-option label="空闲" value="FREE" />
            <el-option label="占用" value="OCCUPIED" />
          </el-select>
        </div>

        <el-table :data="filteredTables" v-loading="loadingTables" height="520">
          <el-table-column prop="code" label="桌台" width="90" />
          <el-table-column prop="type" label="类型" width="90">
            <template #default="{ row }">
              {{ tableTypeLabel(row.type) }}
            </template>
          </el-table-column>
          <el-table-column prop="capacity" label="人数" width="80" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              {{ tableStatusLabel(row.status) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" >
            <template #default="{ row }">
              <el-button size="small" type="primary" v-if="row.status === 'FREE'" @click="openTable(row)">开台</el-button>
              <el-button size="small" v-else @click="selectOccupied(row)">进入</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>

    <el-col :span="16">
      <el-card>
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span>当前订单</span>
            <div style="display: flex; gap: 8px">
              <el-button size="small" :disabled="!currentOrderId" @click="reloadOrder">刷新</el-button>
              <el-button size="small" :disabled="!currentOrderId" @click="openReceipt">小票</el-button>
              <el-button size="small" type="warning" :disabled="!currentOrderId" @click="openDiscount">折扣</el-button>
              <el-button size="small" type="danger" :disabled="!currentOrderId" @click="cancelOpen">取消开台</el-button>
              <el-button size="small" type="success" :disabled="!currentOrderId" @click="openCheckout">结账</el-button>
            </div>
          </div>
        </template>

        <div v-if="!currentOrderId" style="color: #666">请选择桌台开台或进入占用桌台。</div>

        <div v-else>
          <el-descriptions :column="2" border style="margin-bottom: 12px">
            <el-descriptions-item label="订单号">{{ order?.order?.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="桌台">{{ currentTable?.code }}</el-descriptions-item>
            <el-descriptions-item label="折前">{{ order?.order?.amountBeforeDiscount }}</el-descriptions-item>
            <el-descriptions-item label="折扣">{{ order?.order?.discountAmount }}</el-descriptions-item>
            <el-descriptions-item label="实收">{{ order?.order?.amountAfterDiscount }}</el-descriptions-item>
            <el-descriptions-item label="折扣原因">{{ order?.order?.discountReason || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="add-bar">
            <el-input-number v-model="addForm.quantity" :min="1" />
            <el-button type="primary" :loading="acting" @click="openDishSelect">加菜</el-button>
          </div>

          <div class="order-items-wrapper" v-loading="acting">
            <div v-if="!order?.items?.length" class="order-items-empty">暂无点餐明细，请先加菜。</div>
            <div v-else class="order-items-list">
              <div v-for="it in order?.items || []" :key="it.id" class="order-item">
                <div class="order-item-main">
                  <div class="order-item-title">
                    {{ it.dishNameSnapshot }}
                  </div>
                  <div class="order-item-meta">
                    单价 ￥{{ money(it.unitPriceSnapshot) }} · 小计 ￥{{ money(it.lineAmount) }}
                  </div>
                </div>
                <div class="order-item-actions">
                  <el-input-number
                    v-model="it.quantity"
                    :min="1"
                    controls-position="right"
                    style="width: 120px"
                    @change="(v:any) => updateItem(it.id, v)"
                  />
                  <el-button size="small" type="danger" link @click="deleteItem(it.id)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>

  <el-dialog v-model="discountVisible" title="折扣" width="480px">
    <el-form :model="discountForm" label-width="110px">
      <el-form-item label="折扣金额">
        <el-input-number
          v-model="discountForm.manualDiscountAmount"
          :min="0"
          :precision="2"
          :step="1"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="原因">
        <el-input v-model="discountForm.reason" placeholder="例如：活动优惠 / 特殊照顾等" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="discountVisible = false">取消</el-button>
      <el-button type="primary" :loading="acting" @click="applyDiscount">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="checkoutVisible" title="结账" width="520px">
    <el-form :model="checkoutForm" label-width="110px">
      <el-form-item label="支付方式">
        <el-select v-model="checkoutForm.method" style="width: 100%">
          <el-option label="现金" value="CASH" />
          <el-option label="微信" value="WECHAT" />
          <el-option label="支付宝" value="ALIPAY" />
          <el-option label="刷卡" value="CARD" />
        </el-select>
      </el-form-item>
      <el-form-item label="支付金额">
        <el-input-number v-model="checkoutForm.payAmount" :min="0.01" :precision="2" :step="1" style="width: 100%" />
      </el-form-item>
      <el-form-item label="应收">
        <span>{{ order?.order?.amountAfterDiscount }}</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="checkoutVisible = false">取消</el-button>
      <el-button type="primary" :loading="acting" @click="checkout">结账</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="dishSelectVisible" title="选择菜品" width="720px" class="dish-select-dialog">
    <div class="dish-dialog">
      <div class="dish-dialog-sidebar">
<!--        <div class="category-header">菜品分类</div>-->
        <div
          class="dish-cat"
          :class="{ 'dish-cat--active': !selectedCategoryId }"
          @click="selectedCategoryId = null"
        >
<!--          <span class="dish-cat-icon">🍽️</span>-->
          <span class="dish-cat-name">全部</span>
        </div>
        <div
          v-for="c in categories"
          :key="c.id"
          class="dish-cat"
          :class="{ 'dish-cat--active': selectedCategoryId === c.id }"
          @click="selectedCategoryId = c.id"
        >
<!--          <span class="dish-cat-icon">📋</span>-->
          <span class="dish-cat-name">{{ c.name }}</span>
        </div>
      </div>
      <div class="dish-dialog-main">
        <div v-if="!filteredDishes.length" class="dish-empty">
<!--          <div class="dish-empty-icon">🍜</div>-->
          <div class="dish-empty-text">该分类下暂无菜品</div>
        </div>
        <div v-else class="dish-grid">
          <div
            v-for="d in filteredDishes"
            :key="d.id"
            class="dish-card"
            @click="selectDish(d)"
          >
<!--            <div class="dish-card-icon">🍴</div>-->
            <div class="dish-card-content">
              <div class="dish-card-name">{{ d.name }}</div>
              <div class="dish-card-price">￥{{ money(d.price) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="dishSelectVisible = false" size="large">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="receiptVisible" title="结账凭证" width="420px">
    <div class="receipt" v-if="order">
      <div class="receipt__header">
        <div class="receipt__brand">收银小票</div>
        <div class="receipt__muted">Order No: {{ order.order.orderNo }}</div>
      </div>
      <div class="receipt__section">
        <div class="receipt__row">
          <span>桌台</span>
          <span>{{ currentTable?.code || '-' }}</span>
        </div>
        <div class="receipt__row">
          <span>开台</span>
          <span>{{ order.order.openedAt }}</span>
        </div>
        <div class="receipt__row">
          <span>结账</span>
          <span>{{ order.order.closedAt || '-' }}</span>
        </div>
        <div class="receipt__row">
          <span>状态</span>
          <span>{{ orderStatusLabel(order.order.status) }}</span>
        </div>
      </div>

      <div class="receipt__section">
        <div class="receipt__row receipt__row--head">
          <span>菜品</span>
          <span>数量</span>
          <span>金额</span>
        </div>
        <div v-for="it in order.items" :key="it.id" class="receipt__row receipt__row--item">
          <span>{{ it.dishNameSnapshot }}</span>
          <span>x{{ it.quantity }}</span>
          <span>￥{{ it.lineAmount }}</span>
        </div>
      </div>

      <div class="receipt__section">
        <div class="receipt__row">
          <span>折前金额</span>
          <span>￥{{ order.order.amountBeforeDiscount }}</span>
        </div>
        <div class="receipt__row">
          <span>折扣</span>
          <span>-￥{{ order.order.discountAmount }}</span>
        </div>
        <div class="receipt__row">
          <span>折扣原因</span>
          <span>{{ order.order.discountReason || '-' }}</span>
        </div>
        <div class="receipt__row receipt__row--total">
          <span>应收</span>
          <span>￥{{ order.order.amountAfterDiscount }}</span>
        </div>
      </div>

      <div class="receipt__section" v-if="order.payments?.length">
        <div class="receipt__row receipt__row--head">
          <span>支付方式</span>
          <span>金额</span>
          <span>找零</span>
        </div>
        <div v-for="p in order.payments" :key="p.id" class="receipt__row receipt__row--item">
          <span>{{ payMethodLabel(p.method) }}</span>
          <span>￥{{ p.payAmount }}</span>
          <span>￥{{ p.changeAmount }}</span>
        </div>
      </div>

      <div class="receipt__footer">
        <div class="receipt__muted">感谢光临</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="receiptVisible = false">关闭</el-button>
      <el-button type="primary" @click="printReceipt">打印</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../../api/http'
import type { ConsumeOrder, DiningTable, Dish, OrderItem, Payment, DishCategory } from '../../types/api'

type OrderDetail = { order: ConsumeOrder; items: OrderItem[]; payments: Payment[] }

function money(v: any) {
  const n = Number(v ?? 0)
  if (Number.isFinite(n)) return n.toFixed(2)
  return '0.00'
}

function tableTypeLabel(t: string) {
  if (t === 'HALL') return '大厅'
  if (t === 'ROOM') return '包间'
  return t
}

function tableStatusLabel(s: string) {
  if (s === 'FREE') return '空闲'
  if (s === 'OCCUPIED') return '占用'
  return s
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

const loadingTables = ref(false)
const acting = ref(false)
const tables = ref<DiningTable[]>([])
const tableFilter = ref<string>('')

const currentTable = ref<DiningTable | null>(null)
const currentOrderId = ref<number | null>(null)
const order = ref<OrderDetail | null>(null)

const dishes = ref<Dish[]>([])
const categories = ref<DishCategory[]>([])
const selectedCategoryId = ref<number | null>(null)

const addForm = reactive<{ dishId: number | null; quantity: number }>({ dishId: null, quantity: 1 })

const discountVisible = ref(false)
const discountForm = reactive<{ manualDiscountAmount: number | null; reason: string }>({
  manualDiscountAmount: null,
  reason: ''
})

const dishSelectVisible = ref(false)

const checkoutVisible = ref(false)
const checkoutForm = reactive<{ method: string; payAmount: number }>({ method: 'CASH', payAmount: 0 })

const receiptVisible = ref(false)

const filteredTables = computed(() => {
  if (!tableFilter.value) return tables.value
  return tables.value.filter((t) => t.status === tableFilter.value)
})

const filteredDishes = computed(() =>
  dishes.value.filter((d) => (selectedCategoryId.value ? d.categoryId === selectedCategoryId.value : true))
)

async function loadTables() {
  loadingTables.value = true
  try {
    const res = await http.get('/tables')
    tables.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e.message || '加载桌台失败')
  } finally {
    loadingTables.value = false
  }
}

async function loadDishes() {
  const res = await http.get('/dishes', { params: { status: 'ON' } })
  dishes.value = res.data || []
}

async function loadCategories() {
  const res = await http.get('/dish-categories')
  categories.value = res.data || []
}

async function reloadOrder() {
  if (!currentOrderId.value) return
  try {
    const res = await http.get(`/orders/${currentOrderId.value}`)
    order.value = res.data
    // 结账后刷新桌台状态
    await loadTables()
  } catch (e: any) {
    ElMessage.error(e.message || '加载订单失败')
  }
}

async function openTable(t: DiningTable) {
  acting.value = true
  try {
    const res = await http.post('/cashier/open-table', { tableId: t.id })
    currentTable.value = t
    currentOrderId.value = res.data.id
    await reloadOrder()
    ElMessage.success('开台成功')
  } catch (e: any) {
    ElMessage.error(e.message || '开台失败')
  } finally {
    acting.value = false
  }
}

async function selectOccupied(t: DiningTable) {
  currentTable.value = t
  currentOrderId.value = t.currentOrderId || null
  if (!currentOrderId.value) {
    ElMessage.warning('该桌台缺少 currentOrderId，请刷新或检查数据')
    return
  }
  await reloadOrder()
}

async function addItem() {
  if (!currentOrderId.value) return
  if (!addForm.dishId) return ElMessage.warning('请选择菜品')
  acting.value = true
  try {
    await http.post(`/orders/${currentOrderId.value}/items`, { dishId: addForm.dishId, quantity: addForm.quantity })
    await reloadOrder()
    ElMessage.success('加菜成功')
    dishSelectVisible.value = false
  } catch (e: any) {
    ElMessage.error(e.message || '加菜失败')
  } finally {
    acting.value = false
  }
}

async function updateItem(itemId: number, quantity: number) {
  if (!currentOrderId.value) return
  acting.value = true
  try {
    await http.put(`/order-items/${itemId}`, { quantity })
    await reloadOrder()
  } catch (e: any) {
    ElMessage.error(e.message || '更新失败')
  } finally {
    acting.value = false
  }
}

async function deleteItem(itemId: number) {
  if (!currentOrderId.value) return
  acting.value = true
  try {
    await http.delete(`/order-items/${itemId}`)
    await reloadOrder()
    ElMessage.success('删除成功')
  } catch (e: any) {
    ElMessage.error(e.message || '删除失败')
  } finally {
    acting.value = false
  }
}

function openDiscount() {
  if (!currentOrderId.value) return
  discountForm.manualDiscountAmount = null
  discountForm.reason = ''
  discountVisible.value = true
}

function openDishSelect() {
  if (!currentOrderId.value) return
  dishSelectVisible.value = true
}

function selectDish(dish: Dish) {
  addForm.dishId = dish.id
  addItem()
}

async function openReceipt() {
  if (!currentOrderId.value) return
  if (!order.value) await reloadOrder()
  receiptVisible.value = true
}

function printReceipt() {
  const el = document.querySelector('.receipt')
  if (!el) return
  const win = window.open('', '_blank', 'width=400,height=600')
  if (!win) return
  win.document.write(`<html><head><title>Receipt</title>
  <style>
    body{font-family: monospace; padding:12px;}
    .receipt{border:1px dashed #ddd;padding:12px;}
    .receipt__row{display:flex;justify-content:space-between;font-size:13px;margin:4px 0;}
    .receipt__row--head{font-weight:600;border-bottom:1px dashed #ccc;padding-bottom:4px;}
    .receipt__row--item span:first-child{max-width:180px;word-break:break-all;}
    .receipt__row--total{font-weight:700;border-top:1px dashed #ccc;padding-top:4px;}
    .receipt__header{margin-bottom:6px;text-align:center;}
    .receipt__brand{font-weight:700;font-size:16px;}
    .receipt__section{margin:8px 0;}
    .receipt__muted{color:#666;font-size:12px;}
  </style>
  </head><body>${el.outerHTML}</body></html>`)
  win.document.close()
  win.focus()
  win.print()
  win.close()
}

async function applyDiscount() {
  if (!currentOrderId.value) return
  acting.value = true
  try {
    await http.post(`/orders/${currentOrderId.value}/apply-discount`, {
      manualDiscountAmount: discountForm.manualDiscountAmount,
      reason: discountForm.reason
    })
    discountVisible.value = false
    await reloadOrder()
    ElMessage.success('折扣已应用')
  } catch (e: any) {
    ElMessage.error(e.message || '折扣失败')
  } finally {
    acting.value = false
  }
}

function openCheckout() {
  if (!currentOrderId.value) return
  const due = Number(order.value?.order?.amountAfterDiscount || 0)
  checkoutForm.payAmount = due
  checkoutForm.method = 'CASH'
  checkoutVisible.value = true
}

async function checkout() {
  if (!currentOrderId.value) return
  acting.value = true
  try {
    await http.post(`/orders/${currentOrderId.value}/checkout`, { method: checkoutForm.method, payAmount: checkoutForm.payAmount })
    checkoutVisible.value = false
    await reloadOrder()
    ElMessage.success('结账成功')
    // 结账后清空当前订单（桌台已释放）
    currentOrderId.value = null
    order.value = null
    currentTable.value = null
  } catch (e: any) {
    ElMessage.error(e.message || '结账失败')
  } finally {
    acting.value = false
  }
}

async function cancelOpen() {
  if (!currentOrderId.value) return
  acting.value = true
  try {
    await http.post('/cashier/cancel-open', { orderId: currentOrderId.value })
    ElMessage.success('已取消开台')
    currentOrderId.value = null
    order.value = null
    currentTable.value = null
    await loadTables()
  } catch (e: any) {
    ElMessage.error(e.message || '取消失败')
  } finally {
    acting.value = false
  }
}

await Promise.all([loadTables(), loadDishes(), loadCategories()])
</script>

<style scoped>
.add-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.order-items-wrapper {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px 0;
  max-height: 360px;
  overflow-y: auto;
}

.order-items-empty {
  text-align: center;
  color: #999;
  padding: 24px 0;
  font-size: 13px;
}

.order-items-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  border-bottom: 1px dashed #f0f0f0;
}

.order-item:last-child {
  border-bottom: none;
}

.order-item-main {
  flex: 1;
  min-width: 0;
}

.order-item-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 2px;
}

.order-item-meta {
  font-size: 12px;
  color: #999;
}

.order-item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 12px;
}

.dish-dialog {
  display: flex;
  gap: 20px;
  min-height: 400px;
  max-height: 500px;
}

.dish-dialog-sidebar {
  width: 160px;
  background: #f8f9fa;
  border-radius: 12px;
  padding: 16px 12px;
  overflow-y: auto;
}

.category-header {
  font-size: 13px;
  font-weight: 600;
  color: #666;
  margin-bottom: 12px;
  padding: 0 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.dish-cat {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.2s ease;
  user-select: none;
}

.dish-cat + .dish-cat {
  margin-top: 6px;
}

.dish-cat:hover {
  background-color: #e8f4ff;
  transform: translateX(2px);
}

.dish-cat--active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.dish-cat-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.dish-cat-name {
  flex: 1;
  font-weight: 500;
}

.dish-dialog-main {
  flex: 1;
  min-height: 450px;
  overflow-y: auto;
  padding: 8px;
}

.dish-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 300px;
  color: #999;
}

.dish-empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.dish-empty-text {
  font-size: 14px;
  color: #999;
}

.dish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

.dish-card {
  background: #fff;
  border: 2px solid #ebeef5;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.dish-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.dish-card:hover {
  border-color: #667eea;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
  transform: translateY(-4px);
}

.dish-card:hover::before {
  transform: scaleX(1);
}

.dish-card:active {
  transform: translateY(-2px);
}

.dish-card-icon {
  font-size: 36px;
  margin-bottom: 12px;
  filter: grayscale(0.2);
}

.dish-card-content {
  width: 100%;
}

.dish-card-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.4;
}

.dish-card-price {
  font-size: 16px;
  font-weight: 700;
  color: #667eea;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

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