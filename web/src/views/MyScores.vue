<template>
  <div class="page">
    <div class="page__header">
      <h2 class="page__title">我的成绩</h2>
    </div>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" class="exam-table" row-key="examId">
        <el-table-column prop="examName" label="考试" min-width="200" />
        <el-table-column prop="startAt" label="考试时间" width="180" />
        <el-table-column prop="finalScore" label="得分" width="100" />
        <el-table-column label="是否及格" width="100">
          <template #default="{ row }">
            <el-tag :type="row.passed === 1 ? 'success' : 'danger'" size="small">
              {{ row.passed === 1 ? '及格' : '不及格' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { scoreApi } from '@/api/score'

const rows = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    rows.value = await scoreApi.mine()
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
})
</script>
