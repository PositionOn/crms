<template>
  <el-container style="min-height: 100vh">
    <el-aside width="210px" class="aside">
      <div class="brand">RCMS</div>
      <el-menu :default-active="active" router>
        <el-menu-item index="/cashier">收银台</el-menu-item>
        <el-menu-item index="/ledger">台账</el-menu-item>
        <el-menu-item index="/stats">统计</el-menu-item>
        <el-sub-menu index="config">
          <template #title>基础配置</template>
          <el-menu-item index="/config/operators">操作员</el-menu-item>
          <el-menu-item index="/config/tables">桌台</el-menu-item>
          <el-menu-item index="/config/categories">菜品分类</el-menu-item>
          <el-menu-item index="/config/dishes">菜品</el-menu-item>
<!--          <el-menu-item index="/config/discount-rules">折扣规则</el-menu-item>-->
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div />
        <div class="right">
          <span class="user">{{ auth.operatorName || '操作员' }}</span>
          <el-button size="small" @click="doLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <Suspense>
          <template #default>
            <RouterView />
          </template>
          <template #fallback>
            <div class="page-loading">页面加载中...</div>
          </template>
        </Suspense>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const active = computed(() => route.path)

function doLogout() {
  auth.logout()
  router.replace('/login')
}
</script>

<style scoped>
.aside {
  border-right: 1px solid #eee;
  background: #fff;
}
.brand {
  height: 56px;
  line-height: 56px;
  text-align: center;
  font-weight: 700;
  border-bottom: 1px solid #eee;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  background: #fff;
}
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.main {
  background: #f5f6f7;
}
.page-loading {
  padding: 24px;
  color: #666;
}
.user {
  color: #333;
}
</style>

