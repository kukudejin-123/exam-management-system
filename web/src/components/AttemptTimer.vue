<template>
  <div class="attempt-timer" :class="`attempt-timer--${level}`">
    <el-icon class="attempt-timer__icon"><Timer /></el-icon>
    <span class="attempt-timer__value">{{ formatted }}</span>
    <span class="attempt-timer__label">剩余时间</span>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'

const props = defineProps({
  /** 服务端返回的剩余秒数。以此为准，不用前端自己倒推 */
  remainingSeconds: {
    type: Number,
    required: true
  },
  /** 低于多少秒进入警告态 */
  warnThreshold: {
    type: Number,
    default: 300
  },
  /** 低于多少秒进入危险态 */
  dangerThreshold: {
    type: Number,
    default: 60
  }
})

const emit = defineEmits(['expire'])

// 本地只做"递减展示"，权威值始终来自服务端的 remainingSeconds。
// 每次父组件刷新（轮询或保存作答的响应）都会把 props 推回来纠正本地漂移。
const local = ref(props.remainingSeconds)
let timer = null

const level = computed(() => {
  if (local.value <= props.dangerThreshold) return 'danger'
  if (local.value <= props.warnThreshold) return 'warn'
  return 'normal'
})

const formatted = computed(() => {
  const s = Math.max(local.value, 0)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  const pad = (n) => String(n).padStart(2, '0')
  return h > 0 ? `${pad(h)}:${pad(m)}:${pad(sec)}` : `${pad(m)}:${pad(sec)}`
})

onMounted(() => {
  timer = setInterval(() => {
    if (local.value > 0) {
      local.value -= 1
      if (local.value === 0) {
        emit('expire')
      }
    }
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// 服务端值变化时以它为准，纠正本地计时器的漂移
defineExpose({
  syncFromServer(seconds) {
    local.value = seconds
  }
})
</script>

<style scoped lang="scss">
.attempt-timer {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 4px;
  font-variant-numeric: tabular-nums;
  transition: background-color 0.2s, color 0.2s;

  &__icon {
    font-size: 18px;
  }

  &__value {
    font-size: 20px;
    font-weight: 600;
  }

  &__label {
    font-size: 12px;
    opacity: 0.8;
  }

  &--normal {
    background-color: #DEE1EB;
    color: #16338D;
  }

  &--warn {
    background-color: #FAECD8;
    color: #B45309;
  }

  &--danger {
    background-color: #FEE2E2;
    color: #B91C1C;
  }
}
</style>
