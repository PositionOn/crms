<template>
  <div class="wrap">
    <el-card class="card">
      <template #header>
        <div class="title">RCMS 登录</div>
      </template>
      <el-form :model="form" label-width="80px" @keyup.enter="onSubmit">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" placeholder="admin123" type="password" show-password />
        </el-form-item>
        <el-button type="primary" style="width: 100%" :loading="loading" @click="onSubmit">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

async function onSubmit() {
  loading.value = true
  try {
    const res = await http.post('/auth/login', form)
    auth.setToken(res.data.token)
    auth.setOperatorName(res.data.operatorName || '')
    ElMessage.success('登录成功')
    router.replace('/')
  } catch (e: any) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6f7;
}
.card {
  width: 360px;
}
.title {
  font-weight: 600;
}
</style>

