<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-width="100px"
    class="exam-form"
  >
    <el-form-item label="考试名称" prop="name">
      <el-input v-model="form.name" placeholder="如：数据结构期中考试" />
    </el-form-item>

    <el-form-item label="使用试卷" prop="paperId">
      <el-select v-model="form.paperId" filterable placeholder="选择已发布的试卷" style="width: 100%">
        <el-option
          v-for="p in papers"
          :key="p.id"
          :label="`${p.name}（${p.questionCount} 题 / ${p.totalScore} 分）`"
          :value="p.id"
          :disabled="p.status !== 2"
        />
      </el-select>
      <div class="text-muted">只有已发布的试卷能用于考试</div>
    </el-form-item>

    <el-form-item label="应考班级" prop="classId">
      <el-select v-model="form.classId" filterable placeholder="选择班级" style="width: 100%">
        <el-option v-for="c in classes" :key="c.id" :label="`${c.name}（${c.subject}）`" :value="c.id" />
      </el-select>
    </el-form-item>

    <el-form-item label="开始时间" prop="startAt">
      <el-date-picker
        v-model="form.startAt"
        type="datetime"
        placeholder="选择开始时间"
        style="width: 100%"
        value-format="YYYY-MM-DDTHH:mm:ss"
      />
    </el-form-item>

    <el-form-item label="截止时间" prop="endAt">
      <el-date-picker
        v-model="form.endAt"
        type="datetime"
        placeholder="选择截止时间"
        style="width: 100%"
        value-format="YYYY-MM-DDTHH:mm:ss"
      />
    </el-form-item>

    <el-form-item label="答题时长" prop="duration">
      <el-input-number v-model="form.duration" :min="1" :max="600" style="width: 100%" />
      <span class="text-muted" style="margin-left: 8px">分钟</span>
      <div v-if="windowMinutes > 0" class="text-muted">
        当前时间窗 {{ windowMinutes }} 分钟，时长不能超过它
      </div>
    </el-form-item>

    <el-form-item label="及格线" prop="passLine">
      <el-input-number v-model="form.passLine" :min="0" style="width: 100%" />
      <span v-if="selectedPaper" class="text-muted" style="margin-left: 8px">
        试卷总分 {{ selectedPaper.totalScore }} 分
      </span>
    </el-form-item>

    <el-form-item label="可考次数" prop="maxAttempts">
      <el-input-number v-model="form.maxAttempts" :min="1" :max="10" style="width: 100%" />
      <div class="text-muted">超过 1 次时需指定成绩取值规则</div>
    </el-form-item>

    <el-form-item v-if="form.maxAttempts > 1" label="取值规则" prop="scoreRule">
      <el-radio-group v-model="form.scoreRule">
        <el-radio value="HIGHEST">取最高分</el-radio>
        <el-radio value="LAST">取末次</el-radio>
        <el-radio value="AVERAGE">取平均</el-radio>
      </el-radio-group>
    </el-form-item>

    <el-form-item label="成绩发布" prop="publishMode">
      <el-radio-group v-model="form.publishMode">
        <el-radio value="IMMEDIATE">交卷即出</el-radio>
        <el-radio value="DELAYED">延时发布</el-radio>
        <el-radio value="MANUAL">手动发布</el-radio>
      </el-radio-group>
    </el-form-item>

    <el-form-item label="允许补考" prop="retakePolicy">
      <el-radio-group v-model="form.retakePolicy">
        <el-radio :value="0">不允许</el-radio>
        <el-radio :value="1">缺考可补考</el-radio>
      </el-radio-group>
    </el-form-item>

    <el-alert v-if="studentsOfClass > 0" type="info" :closable="false" class="exam-form__hint">
      发布后会为班级 {{ studentsOfClass }} 名学生各建一条缺考态成绩行，缺考也占一行，
      成绩册从发布那一刻就是完整的。
    </el-alert>
  </el-form>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { classApi } from '@/api/clazz'
import { paperApi } from '@/api/paper'
import { examApi } from '@/api/exam'

const emit = defineEmits(['submitted'])

const formRef = ref()
const papers = ref([])
const classes = ref([])
const studentsOfClass = ref(0)

const form = reactive({
  name: '',
  paperId: null,
  classId: null,
  startAt: '',
  endAt: '',
  duration: 90,
  passLine: 60,
  maxAttempts: 1,
  scoreRule: 'LAST',
  publishMode: 'IMMEDIATE',
  retakePolicy: 0
})

const rules = {
  name: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  paperId: [{ required: true, message: '请选择试卷', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  startAt: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endAt: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
  duration: [{ required: true, message: '请填写答题时长', trigger: 'blur' }]
}

const selectedPaper = computed(() =>
    papers.value.find((p) => p.id === form.paperId) || null)

/** 时间窗分钟数，用于提示"时长不能超过时间窗" */
const windowMinutes = computed(() => {
  if (!form.startAt || !form.endAt) return 0
  const ms = new Date(form.endAt) - new Date(form.startAt)
  return ms > 0 ? Math.floor(ms / 60000) : 0
})

watch(windowMinutes, (v) => {
  // 时长超过时间窗时自动收敛，避免用户提交后才被后端拒绝
  if (v > 0 && form.duration > v) {
    form.duration = v
  }
})

watch(selectedPaper, (p) => {
  if (p && p.totalScore && form.passLine > p.totalScore) {
    form.passLine = p.totalScore
  }
})

watch(
    () => form.classId,
    async (id) => {
      studentsOfClass.value = 0
      if (!id) return
      try {
        const list = await examApi.students(id)
        studentsOfClass.value = list.length
      } catch {
        // 人数获取失败不影响表单使用
      }
    }
)

async function loadOptions() {
  try {
    const [p, c] = await Promise.all([
      paperApi.page({ pageNo: 1, pageSize: 100 }),
      classApi.all()
    ])
    papers.value = p.records
    classes.value = c
  } catch {
    // 拦截器已提示
  }
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return false

  // 前端先挡一轮后端会拒的规则，减少无谓请求
  if (windowMinutes.value > 0 && form.duration > windowMinutes.value) {
    ElMessage.warning('答题时长不能超过考试时间窗')
    return false
  }
  if (selectedPaper.value?.totalScore && form.passLine > selectedPaper.value.totalScore) {
    ElMessage.warning('及格线不能超过试卷总分')
    return false
  }

  emit('submitted', {
    ...form,
    createdBy: Number(localStorage.getItem('userId')) || 0
  })
  return true
}

function reset() {
  Object.assign(form, {
    name: '', paperId: null, classId: null, startAt: '', endAt: '',
    duration: 90, passLine: 60, maxAttempts: 1,
    scoreRule: 'LAST', publishMode: 'IMMEDIATE', retakePolicy: 0
  })
  studentsOfClass.value = 0
  formRef.value?.clearValidate()
}

defineExpose({ submit, reset, loadOptions })

loadOptions()
</script>

<style scoped lang="scss">
.exam-form {
  &__hint {
    margin-top: 8px;
  }
}
</style>
