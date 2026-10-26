<template>
  <div class="score-table">
    <div v-if="showSummary" class="score-table__summary">
      <el-statistic title="应考" :value="book.total" />
      <el-statistic title="及格" :value="book.pass" />
      <el-statistic title="不及格" :value="book.fail" />
      <el-statistic title="缺考" :value="book.absent" />
      <el-statistic title="未阅卷" :value="book.ungraded" />
    </div>

    <el-table :data="book.rows" class="exam-table" row-key="scoreId">
      <el-table-column prop="userNo" label="学号" width="140" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="rawScore" label="原始分" width="100" />
      <el-table-column prop="finalScore" label="最终分" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.statusLabel === '正常' ? 'success' : 'info'" size="small">
            {{ row.statusLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="$slots.action" label="操作" fixed="right">
        <template #default="{ row }">
          <slot name="action" :row="row" />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
defineProps({
  book: { type: Object, required: true },
  showSummary: { type: Boolean, default: true }
})
</script>

<style scoped lang="scss">
.score-table {
  &__summary {
    display: flex;
    gap: 32px;
    flex-wrap: wrap;
    padding: 16px;
    margin-bottom: 16px;
    background: #FFFFFF;
    border-radius: 4px;
  }
}
</style>
