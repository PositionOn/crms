<template>
  <div class="wrap">
    <div class="login-container">
      <div class="login-card">
        <div class="card-header">
          <div class="logo-wrapper">
<!--            <div class="logo-icon">🐱</div>-->
          </div>
          <h1 class="title">RCMS 餐饮管理系统</h1>
          <p class="subtitle">欢迎登录</p>
        </div>
        <el-form :model="form" class="login-form" @keyup.enter="onSubmit">
          <el-form-item>
            <el-input
              v-model="form.username"
              placeholder="请输入账号"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="form.password"
              placeholder="请输入密码"
              type="password"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="onSubmit"
          >
            登录
          </el-button>
        </el-form>
      </div>
    </div>
    <div class="footer">
      Created by Mr.Huang
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
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
  min-height: 98vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.wrap::before {
  content: '';
  position: absolute;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: moveBackground 20s linear infinite;
}

@keyframes moveBackground {
  0% {
    transform: translate(0, 0);
  }
  100% {
    transform: translate(50px, 50px);
  }
}

.login-container {
  position: relative;
  z-index: 1;
}

.login-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideIn 0.5s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.logo-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 0 0 10px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.login-form {
  margin-top: 30px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.login-button {
  width: 100%;
  border-radius: 12px;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
  transition: all 0.3s;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(102, 126, 234, 0.4);
}

.login-button:active {
  transform: translateY(0);
}

.footer {
  position: absolute;
  bottom: 30px;
  left: 0;
  right: 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  z-index: 1;
  letter-spacing: 0.5px;
}
</style>
