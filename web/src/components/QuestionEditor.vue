<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="exam-form">
    <el-form-item label="题型" prop="type">
      <el-select v-model="form.type" style="width: 100%" @change="onTypeChange">
        <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
    </el-form-item>

    <el-form-item label="题干" prop="stem">
      <el-input v-model="form.stem" type="textarea" :rows="3" placeholder="请输入题干" />
    </el-form-item>

    <template v-if="needOptions">
      <el-form-item v-for="(_, key) in optionRows" :key="key" :label="`选项 ${key}`">
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
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'

const QUESTION_TYPES = [
  { value: 'SINGLE', label: '单选题' },
  { value: 'MULTI', label: '多选题' },
  { value: 'JUDGE', label: '判断题' },
  { value: 'FILL', label: '填空题' },
  { value: 'SHORT', label: '简答题' }
]

const props = defineProps({
  /** 编辑时传入；null 表示新建 */
  modelValue: { type: Object, default: null },
  bankId: { type: Number, default: 1 }
})

const emit = defineEmits(['submit'])

const formRef = ref()
const optionRows = reactive({})

const form = reactive({
  bankId: props.bankId,
  type: 'SINGLE',
  stem: '',
  answer: '',
  chapter: '',
  knowledge: '',
  difficulty: 2,
  status: 1
})

const rules = {
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

function onTypeChange() {
  form.answer = ''
  Object.keys(optionRows).forEach((k) => delete optionRows[k])
  if (needOptions.value) {
    optionRows.A = ''
    optionRows.B = ''
  }
}

function addOption() {
  const next = String.fromCharCode(65 + Object.keys(optionRows).length)
  optionRows[next] = ''
}

/** 把表单转成后端要的 JSON 结构 */
function buildPayload() {
  return {
    ...form,
    options: needOptions.value ? JSON.stringify(optionRows) : null,
    answer: buildAnswerJson(form.answer)
  }
}

function buildAnswerJson(raw) {
  if (form.type === 'SINGLE' || form.type === 'JUDGE') {
    return JSON.stringify({ correct: [raw.trim()] })
  }
  if (form.type === 'MULTI') {
    return JSON.stringify({ correct: splitList(raw) })
  }
  if (form.type === 'FILL') {
    return JSON.stringify({ accept: splitList(raw) })
  }
  return JSON.stringify({ accept: [] })
}

function splitList(raw) {
  return raw.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
}

/** 编辑回填 */
function load(row) {
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
      // 选项坏了就让用户重填，不阻塞编辑其他字段
    }
  }
}

async function validate() {
  return formRef.value.validate().catch(() => false)
}

defineExpose({ load, validate, buildPayload })

// 初始两个选项
optionRows.A = ''
optionRows.B = ''
</script>
