<template>
  <div class="composer">
    <el-steps :active="step" simple class="composer__steps">
      <el-step title="选题库" />
      <el-step title="配配额" />
      <el-step title="预览组卷" />
    </el-steps>

    <!-- 第一步：选题库 -->
    <div v-show="step === 0" class="composer__panel">
      <el-form label-width="90px">
        <el-form-item label="试卷名称" required>
          <el-input v-model="form.paperName" placeholder="如：数据结构期中考试卷" />
        </el-form-item>
        <el-form-item label="参与题库" required>
          <el-select
            v-model="form.bankIds"
            multiple
            filterable
            placeholder="选择一个或多个题库"
            style="width: 100%"
          >
            <el-option
              v-for="b in banks"
              :key="b.id"
              :label="`${b.name}（${b.subject}）`"
              :value="b.id"
            />
          </el-select>
          <div class="text-muted">组卷会从所选题库的启用题目中抽题</div>
        </el-form-item>
        <el-form-item label="目标总分">
          <el-input-number v-model="form.totalScore" :min="1" :max="1000" style="width: 100%" />
        </el-form-item>
      </el-form>
    </div>

    <!-- 第二步：配配额 -->
    <div v-show="step === 1" class="composer__panel">
      <el-form label-width="90px">
        <el-form-item label="章节配比">
          <div v-for="(q, i) in form.chapters" :key="i" class="composer__row">
            <el-input v-model="q.chapter" placeholder="章节名，如：第一章" style="flex: 1" />
            <span class="composer__row-x">抽</span>
            <el-input-number v-model="q.count" :min="1" :max="100" style="width: 110px" />
            <span class="composer__row-x">题</span>
            <el-button link type="danger" :icon="Delete" @click="form.chapters.splice(i, 1)" />
          </div>
          <el-button link type="primary" :icon="Plus" @click="addChapter">添加章节配额</el-button>
        </el-form-item>

        <el-form-item label="题型配额">
          <el-select v-model="form.types" multiple placeholder="选择题型" style="width: 100%">
            <el-option v-for="t in TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <div class="text-muted">每种题型各抽 1 题</div>
        </el-form-item>

        <el-form-item label="难度配额">
          <div class="composer__diff">
            <el-tag
              v-for="(d, i) in form.difficulty"
              :key="i"
              closable
              :type="diffType(d)"
              @close="form.difficulty.splice(i, 1)"
            >
              {{ diffLabel(d) }}
            </el-tag>
            <el-select
              :model-value="''"
              placeholder="添加"
              style="width: 100px"
              @change="addDifficulty"
            >
              <el-option v-for="d in [1, 2, 3]" :key="d" :label="diffLabel(d)" :value="d" />
            </el-select>
          </div>
          <div class="text-muted">每个难度等级各抽 1 题，可重复添加来配置数量</div>
        </el-form-item>

        <el-form-item label="打乱顺序">
          <el-switch v-model="form.shuffle" />
          <span class="text-muted" style="margin-left: 8px">关闭时按配额顺序输出</span>
        </el-form-item>
      </el-form>
    </div>

    <!-- 第三步：预览 -->
    <div v-show="step === 2" class="composer__panel">
      <div v-if="previewing" v-loading="true" class="composer__loading" />
      <template v-else-if="preview.length">
        <el-alert type="success" :closable="false" class="composer__alert">
          共 {{ preview.length }} 题，合计 {{ previewScore }} 分
        </el-alert>
        <el-table :data="preview" class="exam-table" max-height="320">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="题型" width="90">
            <template #default="{ row }">
              <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="stem" label="题干" min-width="260" show-overflow-tooltip />
          <el-table-column prop="score" label="分值" width="80" />
        </el-table>
      </template>
      <el-empty v-else description="点击「预览」查看组卷结果" />
    </div>

    <div class="composer__footer">
      <el-button v-if="step > 0" @click="step -= 1">上一步</el-button>
      <el-button v-if="step === 0" type="primary" :disabled="!canNext" @click="step = 1">
        下一步：配配额
      </el-button>
      <el-button v-if="step === 1" type="primary" :disabled="!hasQuota" @click="onPreview">
        预览组卷
      </el-button>
      <el-button v-if="step === 2" type="primary" :loading="composing" @click="onCompose">
        确认组卷
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { paperApi } from '@/api/paper'
import { bankApi } from '@/api/questionBank'

const emit = defineEmits(['composed'])

const TYPES = [
  { value: 'SINGLE', label: '单选题' },
  { value: 'MULTI', label: '多选题' },
  { value: 'JUDGE', label: '判断题' },
  { value: 'FILL', label: '填空题' },
  { value: 'SHORT', label: '简答题' }
]

const typeLabel = (t) => TYPES.find((x) => x.value === t)?.label || t
const diffLabel = (d) => ({ 1: '易', 2: '中', 3: '难' })[d] || d
const diffType = (d) => ({ 1: 'success', 2: 'warning', 3: 'danger' })[d] || 'info'

const banks = ref([])
const step = ref(0)
const previewing = ref(false)
const composing = ref(false)
const preview = ref([])

const form = reactive({
  paperName: '',
  bankIds: [],
  totalScore: 100,
  chapters: [{ chapter: '', count: 5 }],
  types: [],
  difficulty: [2],
  shuffle: true
})

const canNext = computed(() =>
    form.paperName.trim() && form.bankIds.length > 0 && form.totalScore > 0)

const hasQuota = computed(() =>
    form.chapters.some((c) => c.chapter.trim() && c.count > 0)
    || form.types.length > 0
    || form.difficulty.length > 0)

const previewScore = computed(() =>
    preview.value.reduce((sum, q) => sum + (q.score || 0), 0))

function addChapter() {
  form.chapters.push({ chapter: '', count: 5 })
}

function addDifficulty(d) {
  form.difficulty.push(d)
}

/** 组装成后端要的结构：过滤掉空章节 */
function buildRequest() {
  return {
    paperName: form.paperName,
    bankIds: form.bankIds,
    totalScore: form.totalScore,
    chapters: form.chapters
        .filter((c) => c.chapter.trim() && c.count > 0)
        .map((c) => ({ chapter: c.chapter.trim(), count: c.count })),
    types: form.types,
    difficulty: form.difficulty,
    shuffle: form.shuffle,
    createdBy: Number(localStorage.getItem('userId')) || 0
  }
}

async function onPreview() {
  previewing.value = true
  try {
    preview.value = await paperApi.previewInline(buildRequest())
    if (!preview.value.length) {
      ElMessage.warning('没有抽到题目，请检查配额')
    }
  } catch {
    // 拦截器已提示（含"候选题目不足"的具体缺口）
  } finally {
    previewing.value = false
  }
}

async function onCompose() {
  composing.value = true
  try {
    const paperId = await paperApi.composeInline(buildRequest())
    ElMessage.success(`组卷完成，试卷 ID ${paperId}`)
    emit('composed', paperId)
  } catch {
    // 拦截器已提示
  } finally {
    composing.value = false
  }
}

async function loadBanks() {
  try {
    banks.value = await bankApi.all()
  } catch {
    // 题库加载失败不阻塞页面，用户仍可手动输入
  }
}

onMounted(loadBanks)
</script>

<style scoped lang="scss">
.composer {
  &__steps {
    margin-bottom: 24px;
  }

  &__panel {
    min-height: 240px;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    &-x {
      color: #6B7280;
      font-size: 12px;
      white-space: nowrap;
    }
  }

  &__diff {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  &__alert {
    margin-bottom: 16px;
  }

  &__loading {
    height: 200px;
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 24px;
    padding-top: 16px;
    border-top: 1px solid #E5E7EB;
  }
}
</style>
