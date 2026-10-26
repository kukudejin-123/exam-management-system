<template>
  <div class="page">
    <div class="page__header">
      <h2 class="page__title">题库管理</h2>
      <el-button v-if="user.canWrite" type="primary" :icon="Plus" @click="openCreate">
        新建题目
      </el-button>
    </div>

    <div class="page__toolbar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索题干"
        :prefix-icon="Search"
        clearable
        style="width: 240px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
      <el-select v-model="filters.type" placeholder="题型" clearable style="width: 140px" @change="onSearch">
        <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-select v-model="filters.difficulty" placeholder="难度" clearable style="width: 120px" @change="onSearch">
        <el-option :value="1" label="易" />
        <el-option :value="2" label="中" />
        <el-option :value="3" label="难" />
      </el-select>
      <el-input
        v-model="filters.chapter"
        placeholder="章节"
        clearable
        style="width: 140px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
      <el-button :icon="Refresh" @click="onReset">重置</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" class="exam-table" row-key="id">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" min-width="280" show-overflow-tooltip />
        <el-table-column prop="chapter" label="章节" width="110" />
        <el-table-column label="难度" width="80">
          <template #default="{ row }">
            <el-rate :model-value="row.difficulty" disabled :max="3" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="user.canWrite" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="user.canWrite"
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="onToggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button
              v-if="user.isAdmin"
              link
              type="danger"
              @click="onRemove(row)"
            >
              删除
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑题目' : '新建题目'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="exam-form">
        <el-form-item label="题型" prop="type">
          <el-select v-model="form.type" style="width: 100%" @change="onTypeChange">
            <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="题干" prop="stem">
          <el-input v-model="form.stem" type="textarea" :rows="3" placeholder="请输入题干" />
        </el-form-item>

        <!-- 选项编辑器：只有选择题需要 -->
        <template v-if="needOptions">
          <el-form-item
            v-for="(opt, key) in optionRows"
            :key="key"
            :label="`选项 ${key}`"
          >
            <el-input v-model="optionRows[key]" :placeholder="`选项 ${key} 内容`" />
          </el-form-item>
          <el-form-item>
            <el-button link type="primary" :icon="Plus" @click="addOption">添加选项</el-button>
          </el-form-item>
        </template>

        <el-form-item :label="answerLabel" prop="answer">
          <el-input
            v-model="form.answer"
            :type="isMulti ? 'textarea' : 'text'"
            :rows="isMulti ? 2 : 1"
            :placeholder="answerPlaceholder"
          />
        </el-form-item>

        <el-form-item label="章节" prop="chapter">
          <el-input v-model="form.chapter" placeholder="如：第一章" />
        </el-form-item>

        <el-form-item label="知识点" prop="knowledge">
          <el-input v-model="form.knowledge" placeholder="如：基本概念" />
        </el-form-item>

        <el-form-item label="难度" prop="difficulty">
          <el-radio-group v-model="form.difficulty">
            <el-radio :value="1">易</el-radio>
            <el-radio :value="2">中</el-radio>
            <el-radio :value="3">难</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { questionApi } from '@/api/question'

const user = useUserStore()

const QUESTION_TYPES = [
  { value: 'SINGLE', label: '单选题' },
  { value: 'MULTI', label: '多选题' },
  { value: 'JUDGE', label: '判断题' },
  { value: 'FILL', label: '填空题' },
  { value: 'SHORT', label: '简答题' }
]

const rows = ref([])
const loading = ref(false)
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filters = reactive({ keyword: '', type: '', difficulty: null, chapter: '' })

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref()
const optionRows = reactive({})
const form = reactive({
  bankId: 1,
  type: 'SINGLE',
  stem: '',
  answer: '',
  chapter: '',
  knowledge: '',
  difficulty: 2,
  status: 1
})

const rules = {
  bankId: [{ required: true, message: '请输入题库 ID', trigger: 'blur' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  stem: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }]
}

const needOptions = computed(() => form.type === 'SINGLE' || form.type === 'MULTI')
const isMulti = computed(() => form.type === 'MULTI')
const answerLabel = computed(() => (form.type === 'FILL' ? '可接受答案' : '正确答案'))
const answerPlaceholder = computed(() => {
  switch (form.type) {
    case 'SINGLE':
    case 'JUDGE':
      return '单个选项字母，如 A'
    case 'MULTI':
      return '多个选项字母，用逗号分隔，如 A,B,D'
    case 'FILL':
      return '多个可接受答案，用逗号分隔'
    default:
      return '参考答案要点'
  }
})

function typeLabel(type) {
  return QUESTION_TYPES.find((t) => t.value === type)?.label || type
}

async function load() {
  loading.value = true
  try {
    const data = await questionApi.page({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      difficulty: filters.difficulty || undefined,
      chapter: filters.chapter || undefined
    })
    rows.value = data.records
    total.value = data.total
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pageNo.value = 1
  load()
}

function onReset() {
  filters.keyword = ''
  filters.type = ''
  filters.difficulty = null
  filters.chapter = ''
  onSearch()
}

function onTypeChange() {
  // 切题型时清掉上一类题的答案与选项，避免把选择题的选项带进填空题
  form.answer = ''
  Object.keys(optionRows).forEach((k) => delete optionRows[k])
  if (needOptions.value) {
    optionRows.A = ''
    optionRows.B = ''
  }
}

function addOption() {
  const keys = Object.keys(optionRows)
  const next = String.fromCharCode(65 + keys.length)
  optionRows[next] = ''
}

function buildOptionsJson() {
  if (!needOptions.value) return null
  return JSON.stringify(optionRows)
}

function buildAnswerJson(raw) {
  if (form.type === 'SINGLE' || form.type === 'JUDGE') {
    return JSON.stringify({ correct: [raw.trim()] })
  }
  if (form.type === 'MULTI') {
    const list = raw.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
    return JSON.stringify({ correct: list })
  }
  if (form.type === 'FILL') {
    const list = raw.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
    return JSON.stringify({ accept: list })
  }
  return JSON.stringify({ accept: [] })
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    bankId: 1, type: 'SINGLE', stem: '', answer: '',
    chapter: '', knowledge: '', difficulty: 2, status: 1
  })
  Object.keys(optionRows).forEach((k) => delete optionRows[k])
  optionRows.A = ''
  optionRows.B = ''
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    bankId: row.bankId,
    type: row.type,
    stem: row.stem,
    answer: row.answer,
    chapter: row.chapter || '',
    knowledge: row.knowledge || '',
    difficulty: row.difficulty,
    status: row.status
  })
  Object.keys(optionRows).forEach((k) => delete optionRows[k])
  if (row.options) {
    try {
      Object.assign(optionRows, JSON.parse(row.options))
    } catch {
      ElMessage.warning('选项解析失败，请手动填写')
    }
  }
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      ...form,
      options: buildOptionsJson(),
      answer: buildAnswerJson(form.answer)
    }
    if (editingId.value) {
      await questionApi.update(editingId.value, payload)
      ElMessage.success('已保存')
    } else {
      await questionApi.create(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function onToggleStatus(row) {
  try {
    await questionApi.changeStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(row.status === 1 ? '已停用' : '已启用')
    load()
  } catch {
    // 拦截器已提示
  }
}

async function onRemove(row) {
  try {
    await ElMessageBox.confirm(`确认删除该题目？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await questionApi.remove(row.id)
    ElMessage.success('已删除')
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
