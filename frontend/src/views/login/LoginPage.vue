<template>
  <div class="login-page">
    <div class="login-bg-pattern" />
    <el-card class="login-card">
      <div class="login-card-top" />
      <template #header>
        <div class="login-header">
          <svg class="login-shield" viewBox="0 0 48 48" width="48" height="48">
            <path d="M24 3 L42 9 L42 24 C42 33 24 45 24 45 C24 45 6 33 6 24 L6 9 Z" fill="#c9a84c" stroke="#d4b55a" stroke-width="2"/>
            <polygon points="24,12 27.5,19.5 36,21 30,27 32,34.5 24,30 16,34.5 18,27 12,21 20.5,19.5" fill="#1e3a5f"/>
          </svg>
          <h2>人口数据库管理系统</h2>
          <p class="login-subtitle">人口数据库管理信息平台</p>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password maxlength="20" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" native-type="submit" style="width:100%">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 强制改密弹窗 -->
    <el-dialog v-model="showChangePwd" title="修改密码" :close-on-click-modal="false" :close-on-press-escape="false" width="400px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules">
        <el-form-item prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="当前密码" show-password maxlength="20" />
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="6-20位新密码" show-password maxlength="20" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="handleChangePwd" :loading="changingPwd">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { showError, showSuccess } from '@/utils/auth'
import request from '@/api/request'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref()
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const loading = ref(false)

const showChangePwd = ref(false)
const changingPwd = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在6-20位之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_: any, v: string, cb: any) => v === pwdForm.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' },
  ],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid === false) return
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    auth.persist()
    showSuccess('登录成功')

    if (auth.mustChangePassword) {
      showChangePwd.value = true
    } else if (auth.role === '普通用户') {
      router.push('/portal')
    } else {
      router.push('/dashboard')
    }
  } catch (e: any) {
    showError(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleChangePwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (valid === false) return
  changingPwd.value = true
  try {
    await request.put('/auth/change-password', { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    auth.passwordChanged()
    showSuccess('密码修改成功')
    showChangePwd.value = false
    if (auth.role === '普通用户') router.push('/portal')
    else router.push('/dashboard')
  } catch (e: any) {
    showError(e.message || '修改密码失败')
  } finally {
    changingPwd.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #0a1628 0%, #152d4f 40%, #1e3a5f 70%, #2c5aa0 100%);
  position: relative;
  overflow: hidden;
}
.login-bg-pattern {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 30%, rgba(201,168,76,.06) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(201,168,76,.04) 0%, transparent 50%);
  pointer-events: none;
}
.login-card {
  width: 420px;
  border-radius: 8px;
  box-shadow: 0 8px 40px rgba(0,0,0,.25);
  position: relative;
  z-index: 1;
}
.login-card-top {
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--pdm-primary), var(--pdm-accent), var(--pdm-primary-light));
  border-radius: 8px 8px 0 0;
}
.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}
.login-shield {
  flex-shrink: 0;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,.15));
}
.login-header h2 {
  text-align: center;
  margin: 0;
  color: #1e3a5f;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
}
.login-subtitle {
  margin: 0;
  font-size: 13px;
  color: #909399;
  letter-spacing: 3px;
}
</style>
