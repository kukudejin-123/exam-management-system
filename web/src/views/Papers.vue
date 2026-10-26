<template>
  <div class="page">
    <div class="page__header">
      <h2 class="page__title">试卷管理</h2>
    </div>

    <el-card shadow="never">
      <template #header>
        <span>试卷列表</span>
      </template>
      <el-table :data="rows" v-loading="loading" class="exam-table" row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="试卷名称" min-width="200" />
        <el-table-column prop="questionCount" label="题目数" width="90" />
        <el-table-column prop="totalScore" label="总分" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : 'info'" size="small">
              {{ row.status === 2 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewQuestions(row)">查看题目</el-button>
            <el-button
              v-if="user.canWrite && row.status !== 2"
              link
              type="success"
              @click="onPublish(row)"
            >
              发布
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pageNo"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="试卷题目" width="720px">
      <el-table :data="questions" class="exam-table">
        <el-table-column prop="seq" label="题号" width="70" />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" min-width="300" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="80" />
      </el-table>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { paperApi } from '@/api/paper'

const user = useUserStore()

const QUESTION_TYPES = [
  { value: 'SINGLE', label: '单选题' },
  { value: 'MULTI', label: '多选题' },
  { value: 'JUDGE', label: '判断题' },
  { value: 'FILL', label: '填空题' },
  { value: 'SHORT', label: '简答题' }
]
const typeLabel = (t) => QUESTION_TYPES.find((x) => x.value === t)?.label || t

const rows = ref([])
const loading = ref(false)
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailVisible = ref(false)
const questions = ref([])

async function load() {
  loading.value = true
  try {
    const data = await paperApi.page({ pageNo: pageNo.value, pageSize: pageSize.value })
    rows.value = data.records
    total.value = data.total
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function viewQuestions(row) {
  try {
    questions.value = await paperApi.questions(row.id)
    detailVisible.value = true
  } catch {
    // 拦截器已提示
  }
}

async function onPublish(row) {
  try {
    await paperApi.validate(row.id)
  } catch {
    return   // 分值不平整时后端已提示，不继续发布
  }
  try {
    await ElMessageBox.confirm(`确认发布「${row.name}」？发布后才能用于考试。`, '发布确认')
  } catch {
    return
  }
  try {
    await paperApi.publish(row.id)
    ElMessage.success('已发布')
    load()
  } catch {
    // 拦截器已提示
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
