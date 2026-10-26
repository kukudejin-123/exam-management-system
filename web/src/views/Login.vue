<template>
  <div class="login">
    <div class="login__card">
      <h1 class="login__title">考试管理系统</h1>
      <p class="login__subtitle">以班级/角色为组织单元，独立题库 + 灵活组卷 + 规范考务</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="onSubmit"
      >
        <el-form-item label="学号 / 工号" prop="userNo">
          <el-input v-model="form.userNo" placeholder="请输入学号或工号" size="large" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="login__submit"
          :loading="loading"
          @click="onSubmit"
        >
          登 录
        </el-button>
      </el-form>

      <p class="login__hint">
        本地测试账号：ADMIN001（管理员）/ T2024001（教师）/ S2024001（学生），密码统一 123456
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const user = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ userNo: '', password: '' })

const rules = {
  userNo: [{ required: true, message: '请输入学号或工号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await user.login(form.userNo.trim(), form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    // 错误提示由请求拦截器统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  // 主色渐变作背景，与后台的浅色底形成对比
  background: linear-gradient(135deg, #16338D 0%, #112870 100%);

  &__card {
    width: 400px;
    padding: 40px;
    background: #FFFFFF;
    border-radius: 8px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }

  &__title {
    margin: 0 0 8px;
    font-size: 24px
    ;
    font-weight: 600;
    color: #1E2937;
  }

  &__subtitle {
    margin: 0 0 32px;
    font-size: 12px;
    color: #6B7280;
    line-height: 1.6;
  }

  &__submit {
    width: 100%;
    margin-top: 8px;
  }

  &__hint {
    margin: 24px 0 0;
    font-size: 12px;
    color: #9CA3AF;
    line-height: 1.6;
  }
}
</style>
