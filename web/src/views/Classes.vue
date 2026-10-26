<template>
  <div class="page">
    <div class="page__header">
      <h2 class="page__title">班级管理</h2>
      <el-button v-if="user.canWrite" type="primary" :icon="Plus" @click="openCreate">
        新建班级
      </el-button>
    </div>

    <div class="page__toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索班级名称或科目"
        :prefix-icon="Search"
        clearable
        style="width: 280px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" class="exam-table" row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="班级名称" min-width="160" />
        <el-table-column prop="subject" label="科目" min-width="120" />
        <el-table-column prop="teacherId" label="班主任" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.archived ? 'info' : 'success'" size="small">
              {{ row.archived ? '已归档' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="user.canWrite" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="user.canWrite"
              link
              :type="row.archived ? 'success' : 'warning'"
              @click="onArchive(row)"
            >
              {{ row.archived ? '取消归档' : '归档' }}
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

    <!-- 新建 / 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑班级' : '新建班级'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="exam-form">
        <el-form-item label="班级名称" prop="name">
          <el-input v-model="form.name" placeholder="如：计科 2401 班" />
        </el-form-item>
        <el-form-item label="科目" prop="subject">
          <el-input v-model="form.subject" placeholder="如：数据结构" />
        </el-form-item>
        <el-form-item label="班主任" prop="teacherId">
          <el-input-number v-model="form.teacherId" :min="1" style="width: 100%" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { classApi } from '@/api/clazz'

const user = useUserStore()

const rows = ref([])
const loading = ref(false)
const keyword = ref('')
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ name: '', subject: '', teacherId: null })

const rules = {
  name: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  subject: [{ required: true, message: '请输入科目', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const data = await classApi.page({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
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

function openCreate() {
  editingId.value = null
  form.name = ''
  form.subject = ''
  form.teacherId = null
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.name = row.name
  form.subject = row.subject
  form.teacherId = row.teacherId
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingId.value) {
      await classApi.update(editingId.value, form)
      ElMessage.success('已保存')
    } else {
      await classApi.create(form)
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

async function onArchive(row) {
  try {
    await classApi.archive(row.id, !row.archived)
    ElMessage.success(row.archived ? '已取消归档' : '已归档')
    load()
  } catch {
    // 拦截器已提示
  }
}

async function onRemove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除「${row.name}」？删除后历史成绩仍会保留。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return   // 用户取消
  }
  try {
    await classApi.remove(row.id)
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
