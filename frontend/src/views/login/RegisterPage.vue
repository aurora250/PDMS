<template>
  <div class="login-page">
    <div class="login-bg-pattern" />
    <el-card class="register-card">
      <div class="login-card-top" />
      <template #header>
        <div class="login-header">
          <svg class="login-shield" viewBox="0 0 48 48" width="48" height="48">
            <path d="M24 3 L42 9 L42 24 C42 33 24 45 24 45 C24 45 6 33 6 24 L6 9 Z" fill="#c9a84c" stroke="#d4b55a" stroke-width="2"/>
            <polygon points="24,12 27.5,19.5 36,21 30,27 32,34.5 24,30 16,34.5 18,27 12,21 20.5,19.5" fill="#1e3a5f"/>
          </svg>
          <h2>账号注册</h2>
          <p class="login-subtitle">实名认证 · 审核通过后可登录</p>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名 (3-50位字母/数字/下划线)" size="large" maxlength="50" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码 (8-16位，含大小写字母+数字+特殊字符)" size="large" show-password maxlength="16" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" size="large" show-password maxlength="16" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" size="large" maxlength="11" />
        </el-form-item>
        <el-form-item label="实名认证" prop="residentUuid">
          <ResidentPicker v-model="form.residentUuid" placeholder="搜索姓名或身份证号绑定您的实名信息" />
        </el-form-item>
        <el-form-item label="申请角色" prop="userRole">
          <el-select v-model="form.userRole" size="large" style="width:100%">
            <el-option label="群众 (普通用户)" value="普通用户" />
            <el-option label="采集员" value="采集员" />
            <el-option label="街道办" value="街道办" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.userRole !== '普通用户'" label="附加材料" prop="registerMaterials">
          <AttachmentUploader v-model="form.registerMaterials" />
          <div style="color:#909399;font-size:12px;margin-top:4px">非群众角色需上传相关资质证明文件</div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" native-type="submit" style="width:100%">
            提 交 注 册
          </el-button>
        </el-form-item>
        <div style="text-align:center">
          <el-button text size="small" @click="$router.push('/login')">已有账号？去登录</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showError, showSuccess } from '@/utils/auth'
import { passwordRule, phoneRule } from '@/utils/validators'
import request from '@/api/request'
import ResidentPicker from '@/components/ResidentPicker.vue'
import AttachmentUploader from '@/components/AttachmentUploader.vue'

const router = useRouter()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  residentUuid: '',
  userRole: '普通用户',
  registerMaterials: [] as string[],
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,50}$/, message: '3-50位字母/数字/下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    passwordRule,
    { validator: (_: any, v: string, cb: any) => {
      if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d\s])/.test(v)) {
        cb(new Error('需包含大小写字母+数字+特殊字符'))
      } else cb()
    }, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_: any, v: string, cb: any) => v === form.password ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    phoneRule,
  ],
  residentUuid: [
    { required: true, message: '请选择关联居民完成实名认证', trigger: 'change' },
  ],
  userRole: [{ required: true, message: '请选择申请角色', trigger: 'change' }],
  registerMaterials: [
    { required: true, message: '请上传附加材料', trigger: 'change' },
  ],
}

// 非群众角色时动态控制材料必填
watch(() => form.userRole, (role) => {
  if (role !== '普通用户') {
    rules.registerMaterials = [
      { required: true, message: '非群众角色需上传资质证明', trigger: 'change' },
    ]
  } else {
    rules.registerMaterials = []
  }
})

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const payload: any = {
      username: form.username,
      password: form.password,
      phone: form.phone,
      residentUuid: form.residentUuid,
      userRole: form.userRole,
    }
    if (form.userRole !== '普通用户' && form.registerMaterials.length > 0) {
      payload.registerMaterials = form.registerMaterials
    }
    await request.post('/auth/register', payload)
    showSuccess('注册申请已提交，请等待管理员审核')
    router.push('/login')
  } catch (e: any) {
    showError(e.message || '注册失败')
  } finally {
    loading.value = false
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
.register-card {
  width: 500px;
  border-radius: 8px;
  box-shadow: 0 8px 40px rgba(0,0,0,.25);
  position: relative;
  z-index: 1;
  max-height: 90vh;
  overflow-y: auto;
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
  gap: 8px;
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
  letter-spacing: 2px;
}
</style>
