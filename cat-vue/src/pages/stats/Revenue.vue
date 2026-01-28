<template>
  <el-card>
    <template #header>营收统计</template>
    <el-form inline>
      <el-form-item label="开始">
        <el-date-picker v-model="start" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
      </el-form-item>
      <el-form-item label="结束">
        <el-date-picker v-model="end" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
      </el-form-item>
      <el-button type="primary" :loading="loading" @click="load">查询</el-button>
    </el-form>

    <el-descriptions v-if="data" :column="2" border>
      <el-descriptions-item label="订单数">{{ data.orderCount }}</el-descriptions-item>
      <el-descriptions-item label="消费总额(折前)">{{ data.totalBeforeDiscount }}</el-descriptions-item>
      <el-descriptions-item label="折扣总额">{{ data.totalDiscount }}</el-descriptions-item>
      <el-descriptions-item label="实收">{{ data.totalReceived }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../../api/http'

const start = ref<string>('')
const end = ref<string>('')
const loading = ref(false)
const data = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const res = await http.get('/stats/revenue', { params: { startTime: start.value || undefined, endTime: end.value || undefined } })
    data.value = res.data
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
  } finally {
    loading.value = false
  }
}

load()
</script>

<style scoped>
</style>

