<template>
  <div class="question-card">
    <div class="question-card__head">
      <span class="question-card__seq">{{ seq }}</span>
      <el-tag size="small">{{ typeLabel }}</el-tag>
      <span class="question-card__score">{{ score }} 分</span>
    </div>

    <div class="question-card__stem">{{ stem }}</div>

    <!-- 选项区 -->
    <div v-if="options.length" class="question-card__options">
      <div
        v-for="(opt, key) in options"
        :key="key"
        class="question-card__option"
        :class="{ 'is-selected': isSelected(key) }"
        @click="onPick(key)"
      >
        <span class="question-card__option-key">{{ key }}</span>
        <span>{{ opt }}</span>
      </div>
    </div>

    <!-- 填空与简答用输入框 -->
    <el-input
      v-else
      v-model="textAnswer"
      :type="type === 'SHORT' ? 'textarea' : 'text'"
      :rows="type === 'SHORT' ? 4 : 1"
      :placeholder="type === 'SHORT' ? '请输入简答内容' : '请输入答案'"
      @input="onTextInput"
    />

    <div class="question-card__foot">
      <el-button link type="primary" :icon="Flag" @click="onMark">
        {{ marked ? '取消标记' : '标记待定' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Flag } from '@element-plus/icons-vue'

const props = defineProps({
  seq: { type: Number, required: true },
  type: { type: String, required: true },
  stem: { type: String, required: true },
  score: { type: Number, default: 0 },
  /** 选项 JSON 字符串 */
  options: { type: String, default: null },
  /** 已保存的作答，用于断网续答恢复 */
  savedAnswer: { type: String, default: null },
  marked: { type: Boolean, default: false }
})

const emit = defineEmits(['answer', 'mark'])

const TYPE_LABELS = {
  SINGLE: '单选题',
  MULTI: '多选题',
  JUDGE: '判断题',
  FILL: '填空题',
  SHORT: '简答题'
}
const typeLabel = computed(() => TYPE_LABELS[props.type] || props.type)

const optionMap = computed(() => {
  if (!props.options) return {}
  try {
    return JSON.parse(props.options)
  } catch {
    return {}
  }
})
const options = computed(() => Object.entries(optionMap.value))

const isMulti = computed(() => props.type === 'MULTI')
const picked = ref([])
const textAnswer = ref('')

// 断网续答：把已保存的作答还原到界面上
watch(
    () => props.savedAnswer,
    (val) => {
      if (!val) return
      if (isMulti.value) {
        picked.value = val.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
      } else if (props.type === 'SINGLE' || props.type === 'JUDGE') {
        picked.value = [val]
      } else {
        textAnswer.value = val
      }
    },
    { immediate: true }
)

function isSelected(key) {
  return picked.value.includes(key)
}

function onPick(key) {
  if (isMulti.value) {
    picked.value = picked.value.includes(key)
        ? picked.value.filter((k) => k !== key)
        : [...picked.value, key]
  } else {
    picked.value = [key]
  }
  emit('answer', picked.value.join(','))
}

function onTextInput() {
  emit('answer', textAnswer.value)
}

function onMark() {
  emit('mark', !props.marked)
}
</script>

<style scoped lang="scss">
.question-card {
  padding: 16px;
  background: #FFFFFF;
  border-radius: 4px;
  margin-bottom: 16px;

  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  &__seq {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: #16338D;
    color: #FFFFFF;
    font-size: 12px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  &__score {
    margin-left: auto;
    font-size: 12px;
    color: #6B7280;
  }

  &__stem {
    font-size: 14px;
    line-height: 1.8;
    margin-bottom: 12px;
  }

  &__options {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__option {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 8px 12px;
    border: 1px solid #E5E7EB;
    border-radius: 4px;
    cursor: pointer;
    transition: border-color 0.15s, background-color 0.15s;

    &:hover {
      border-color: #7B88BD;
    }

    &.is-selected {
      border-color: #16338D;
      background-color: #DEE1EB;
    }

    &-key {
      font-weight: 600;
      color: #16338D;
    }
  }

  &__foot {
    margin-top: 12px;
    text-align: right;
  }
}
</style>
