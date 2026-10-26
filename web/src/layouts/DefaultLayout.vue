<template>
  <el-container class="layout">
    <!-- 侧边菜单：1280px 以下自动折叠 -->
    <el-aside :width="collapsed ? '64px' : '220px'" class="layout__aside">
      <div class="logo">
        <el-icon><Notebook /></el-icon>
        <span v-show="!collapsed">考试管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        router
        background-color="#16338D"
        text-color="#C5CAE0"
        active-text-color="#FFFFFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>

        <el-menu-item v-if="user.canWrite" index="/classes">
          <el-icon><User /></el-icon>
          <template #title>班级管理</template>
        </el-menu-item>

        <el-menu-item v-if="user.canWrite" index="/questions">
          <el-icon><Files /></el-icon>
          <template #title>题库管理</template>
        </el-menu-item>

        <el-menu-item v-if="user.canWrite" index="/papers">
          <el-icon><Document /></el-icon>
          <template #title>试卷管理</template>
        </el-menu-item>

        <el-menu-item v-if="user.canWrite" index="/exams">
          <el-icon><Timer /></el-icon>
          <template #title>考试发布</template>
        </el-menu-item>

        <el-menu-item index="/grade-book">
          <el-icon><TrendCharts /></el-icon>
          <template #title>成绩册</template>
        </el-menu-item>

        <el-menu-item v-if="user.isStudent" index="/my-scores">
          <el-icon><Medal /></el-icon>
          <template #title>我的成绩</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout__header">
        <div class="layout__header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <!-- 面包屑：让用户知道当前在哪一层 -->
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">
              {{ currentTitle }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <el-dropdown @command="onCommand">
          <span class="layout__user">
            <el-tag size="small" type="primary">{{ user.roleLabel }}</el-tag>
            <span class="layout__user-name">{{ user.name || user.userId }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="layout__main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const user = useUserStore()
const collapsed = ref(false)

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

async function onCommand(cmd) {
  if (cmd === 'logout') {
    await user.logout()
  }
}
</script>

<style scoped lang="scss">
.layout {
  height: 100%;

  &__aside {
    background-color: #16338D;
    transition: width 0.2s;
    overflow-x: hidden;

    .logo {
      height: 56px;
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 0 16px;
      color: #FFFFFF;
      font-size: 16px;
      font-weight: 600;
      white-space: nowrap;
    }

    :deep(.el-menu) {
      border-right: none;
    }
  }

  &__header {
    background-color: #FFFFFF;
    border-bottom: 1px solid #E5E7EB;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;

    &-left {
      display: flex;
      align-items: center;
      gap: 16px;
    }
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    &-name {
      font-size: 14px;
      color: #1E2937;
    }
  }

  .collapse-btn {
    font-size: 18px;
    cursor: pointer;
    color: #6B7280;
  }

  &__main {
    background-color: #F5F7FA;
    padding: 0;
  }
}
</style>
