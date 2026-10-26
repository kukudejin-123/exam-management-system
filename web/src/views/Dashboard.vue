<template>
  <div class="page">
    <div class="page__header">
      <h2 class="page__title">工作台</h2>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never">
          <div class="stat">
            <div class="stat__value">{{ stats.classes }}</div>
            <div class="stat__label">班级</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never">
          <div class="stat">
            <div class="stat__value">{{ stats.questions }}</div>
            <div class="stat__label">题库题目</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never">
          <div class="stat">
            <div class="stat__value">{{ stats.papers }}</div>
            <div class="stat__label">试卷</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never">
          <div class="stat">
            <div class="stat__value">{{ stats.exams }}</div>
            <div class="stat__label">考试</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="welcome">
      <template #header>
        <span>当前角色：{{ user.roleLabel || '未设置' }}</span>
      </template>
      <p class="text-muted">
        左侧菜单按角色显示：教师与管理员可做班级、题库、试卷、考试管理；学生可查看成绩册与我的成绩。
      </p>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { classApi } from '@/api/clazz'
import { questionApi } from '@/api/question'
import { paperApi } from '@/api/paper'

const user = useUserStore()
const stats = reactive({ classes: 0, questions: 0, papers: 0, exams: 0 })

onMounted(async () => {
  if (!user.canWrite) return
  try {
    const [classes, questions, papers] = await Promise.all([
      classApi.page({ pageNo: 1, pageSize: 1 }),
      questionApi.page({ pageNo: 1, pageSize: 1 }),
      paperApi.page({ pageNo: 1, pageSize: 1 })
    ])
    stats.classes = classes.total
    stats.questions = questions.total
    stats.papers = papers.total
  } catch {
    // 统计数字加载失败不影响页面使用
  }
})
</script>

<style scoped lang="scss">
.stat {
  text-align: center;
  padding: 8px 0;

  &__value {
    font-size: 32px;
    font-weight: 600;
    color: #16338D;
  }

  &__label {
    margin-top: 8px;
    font-size: 12px;
    color: #6B7280;
  }
}

.welcome {
  margin-top: 16px;
}
</style>
