<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <h2>人口数据库管理系统</h2>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
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
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="当前密码" show-password />
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="新密码" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="确认密码" show-password />
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

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const loading = ref(false)

const showChangePwd = ref(false)
const changingPwd = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_: any, v: string, cb: any) => v === pwdForm.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' },
  ],
}

async function handleLogin() {
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
  display: flex; align-items: center; justify-content: center;
  min-height: 100vh; background: linear-gradient(135deg, #1a365d 0%, #2d5f8a 50%, #4a90d9 100%);
}
.login-card {
  width: 400px;
}
.login-card h2 {
  text-align: center; margin: 0; color: #303133;
}
</style>
