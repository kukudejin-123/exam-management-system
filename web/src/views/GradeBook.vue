<template>
  <div class="page">
    <div class="page__header">
      <h2 class="page__title">成绩册</h2>
    </div>

    <div class="page__toolbar">
      <el-input
        v-model="examIdInput"
        placeholder="输入考试 ID"
        style="width: 200px"
        clearable
        @keyup.enter="load"
      />
      <el-select v-model="sortBy" style="width: 140px" @change="load">
        <el-option value="score" label="按分数降序" />
        <el-option value="userNo" label="按学号升序" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px" @change="load">
        <el-option label="缺考" :value="2" />
        <el-option label="0分" :value="3" />
        <el-option label="未阅卷" :value="4" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-row v-if="book" :gutter="16" class="summary">
      <el-col :span="6">
        <el-statistic title="应考" :value="book.total" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="及格" :value="book.pass" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="不及格" :value="book.fail" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="缺考" :value="book.absent" />
      </el-col>
    </el-row>

    <el-card v-if="book" shadow="never">
      <el-table :data="book.rows" class="exam-table" row-key="scoreId">
        <el-table-column prop="userNo" label="学号" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="finalScore" label="得分" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.statusLabel === '正常' ? 'success' : 'info'"
              size="small"
            >
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="user.canWrite" label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.statusLabel === '正常'"
              link
              type="primary"
              @click="openEdit(row)"
            >
              修改成绩
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-else description="请输入考试 ID 查询成绩册" />

    <el-dialog v-model="editVisible" title="修改成绩" width="400px">
      <el-form label-width="80px">
        <el-form-item label="学生">
          <span>{{ editingRow?.userNo }} {{ editingRow?.name }}</span>
        </el-form-item>
        <el-form-item label="新成绩">
          <el-input-number v-model="editScore" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { scoreApi } from '@/api/score'

const user = useUserStore()

const examIdInput = ref('')
const sortBy = ref('score')
const statusFilter = ref('')
const book = ref(null)

const editVisible = ref(false)
const saving = ref(false)
const editingRow = ref(null)
const editScore = ref(0)

async function load() {
  const id = Number(examIdInput.value)
  if (!id) {
    ElMessage.warning('请输入考试 ID')
    return
  }
  try {
    book.value = await scoreApi.gradeBook(id, {
      sortBy: sortBy.value,
      status: statusFilter.value === '' ? undefined : statusFilter.value
    })
  } catch {
    book.value = null
  }
}

function openEdit(row) {
  editingRow.value = row
  editScore.value = row.finalScore ?? 0
  editVisible.value = true
}

async function onSaveEdit() {
  saving.value = true
  try {
    await scoreApi.update(editingRow.value.scoreId, editScore.value)
    ElMessage.success('已修改，操作已留痕')
    editVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  // 不自动加载：成绩册必须显式指定考试，避免误看
})
</script>

<style scoped lang="scss">
.summary {
  margin-bottom: 16px;
  padding: 16px;
  background: #FFFFFF;
  border-radius: 4px;
}
</style>
