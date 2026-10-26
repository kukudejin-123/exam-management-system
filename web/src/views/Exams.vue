<template>
  <div class="page">
    <div class="page__header">
      <h2 class="page__title">考试发布</h2>
      <el-button v-if="user.canWrite" type="primary" :icon="Plus" @click="formVisible = true">
        发布考试
      </el-button>
    </div>

    <div class="page__toolbar">
      <el-input
        v-model="classIdInput"
        placeholder="输入班级 ID"
        style="width: 200px"
        clearable
        @keyup.enter="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" class="exam-table" row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="考试名称" min-width="200" />
        <el-table-column prop="startAt" label="开始时间" width="180" />
        <el-table-column prop="endAt" label="截止时间" width="180" />
        <el-table-column prop="duration" label="时长(分)" width="100" />
        <el-table-column prop="maxAttempts" label="可考次数" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewRoster(row)">名单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" title="发布考试" width="720px" top="5vh">
      <ExamForm ref="formRef" @submitted="onSubmit" />
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onConfirm">发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rosterVisible" title="应考 / 实考 / 缺考名单" width="640px">
      <el-descriptions v-if="roster" :column="3" border>
        <el-descriptions-item label="应考">{{ roster.expected }}</el-descriptions-item>
        <el-descriptions-item label="实考">{{ roster.attended }}</el-descriptions-item>
        <el-descriptions-item label="缺考">{{ roster.absent }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="rosterVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { examApi } from '@/api/exam'
import ExamForm from '@/components/ExamForm.vue'

const user = useUserStore()

const classIdInput = ref('')
const rows = ref([])
const loading = ref(false)

const formVisible = ref(false)
const formRef = ref()
const saving = ref(false)

const rosterVisible = ref(false)
const roster = ref(null)

function statusLabel(s) {
  return ({ 0: '未开始', 1: '进行中', 2: '已结束' })[s] || '未知'
}

function statusType(s) {
  return ({ 0: 'info', 1: 'success', 2: '' })[s] || 'info'
}

async function load() {
  const id = Number(classIdInput.value)
  if (!id) {
    return
  }
  loading.value = true
  try {
    rows.value = await examApi.byClass(id)
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function onConfirm() {
  await formRef.value.submit()
}

async function onSubmit(payload) {
  saving.value = true
  try {
    const id = await examApi.create(payload)
    ElMessage.success(`考试已发布，ID ${id}`)
    formVisible.value = false
    formRef.value.reset()
    if (Number(classIdInput.value) === payload.classId) {
      await load()
    }
  } catch {
    // 拦截器已提示（含时长超窗、及格线超总分等具体原因）
  } finally {
    saving.value = false
  }
}

async function viewRoster(row) {
  try {
    roster.value = await examApi.roster(row.id)
    rosterVisible.value = true
  } catch {
    // 拦截器已提示
  }
}
</script>
