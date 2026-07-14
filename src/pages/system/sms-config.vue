<script lang="ts" setup>
import { useUserStore } from "@/pinia/stores/user"
import { getSmsConfigApi, saveSmsConfigApi, testSmsConfigApi } from "./apis/sms-config"

defineOptions({ name: "SmsConfig" })

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)

const form = reactive({
  enabled: false,
  secretId: "",
  secretKey: "",
  secretKeyConfigured: false,
  sdkAppId: "",
  signName: "",
  templateId: "",
  region: "ap-guangzhou"
})

const testForm = reactive({
  phone: "",
  message: "短信配置测试：您有一条系统消息提醒"
})

async function fetchConfig() {
  loading.value = true
  try {
    const { data } = await getSmsConfigApi()
    form.enabled = !!data.enabled
    form.secretId = data.secretId || ""
    form.secretKey = ""
    form.secretKeyConfigured = !!data.secretKeyConfigured
    form.sdkAppId = data.sdkAppId || ""
    form.signName = data.signName || ""
    form.templateId = data.templateId || ""
    form.region = data.region || "ap-guangzhou"
    if (!testForm.phone && userStore.phone) {
      testForm.phone = userStore.phone
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const payload: Record<string, any> = {
      enabled: form.enabled,
      secretId: form.secretId,
      sdkAppId: form.sdkAppId,
      signName: form.signName,
      templateId: form.templateId,
      region: form.region || "ap-guangzhou"
    }
    if (form.secretKey.trim()) {
      payload.secretKey = form.secretKey.trim()
    }
    await saveSmsConfigApi(payload)
    ElMessage.success("短信配置已保存")
    form.secretKey = ""
    await fetchConfig()
  } catch { /* handled */ } finally {
    saving.value = false
  }
}

async function handleTest() {
  testing.value = true
  try {
    const { data } = await testSmsConfigApi({
      phone: testForm.phone || undefined,
      message: testForm.message || undefined
    })
    ElMessage.success(data || "测试短信已发送")
  } catch { /* handled */ } finally {
    testing.value = false
  }
}

onMounted(() => {
  fetchConfig()
})
</script>

<template>
  <div class="app-container">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="text-lg font-bold">腾讯云短信配置</span>
          <el-switch
            v-model="form.enabled"
            inline-prompt
            active-text="开"
            inactive-text="关"
            style="margin-left: 16px"
          />
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        class="mb-4"
        title="开启后，系统内所有站内消息会同步向接收用户「联系电话」发送腾讯云短信。请使用单变量模板（{1}=消息标题摘要）。开关修改后需点击「保存配置」才会生效。"
      />

      <el-form label-width="120px" style="max-width: 640px">
        <el-form-item label="SecretId">
          <el-input v-model="form.secretId" placeholder="腾讯云 API 密钥 SecretId" />
        </el-form-item>
        <el-form-item label="SecretKey">
          <el-input
            v-model="form.secretKey"
            type="password"
            show-password
            :placeholder="form.secretKeyConfigured ? '已配置，留空表示不修改' : '腾讯云 API 密钥 SecretKey'"
          />
        </el-form-item>
        <el-form-item label="SdkAppId">
          <el-input v-model="form.sdkAppId" placeholder="短信应用 SdkAppId" />
        </el-form-item>
        <el-form-item label="短信签名">
          <el-input v-model="form.signName" placeholder="控制台审核通过的签名内容" />
        </el-form-item>
        <el-form-item label="模板 ID">
          <el-input v-model="form.templateId" placeholder="单变量模板 ID，例如 {1} 为消息摘要" />
        </el-form-item>
        <el-form-item label="地域">
          <el-input v-model="form.region" placeholder="默认 ap-guangzhou" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">
            保存配置
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">
        测试发送
      </el-divider>
      <el-form label-width="120px" style="max-width: 640px">
        <el-form-item label="接收手机号">
          <el-input v-model="testForm.phone" placeholder="默认当前管理员联系电话" maxlength="20" />
        </el-form-item>
        <el-form-item label="测试内容">
          <el-input v-model="testForm.message" type="textarea" :rows="2" placeholder="将填入模板变量" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :loading="testing" @click="handleTest">
            发送测试短信
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert type="warning" :closable="false" class="mt-4">
        <template #title>
          上线步骤
        </template>
        <ol class="guide-list">
          <li>在腾讯云短信控制台开通服务，创建签名与单变量模板（例：您有新的系统消息：{1}，请登录信息平台查看）。</li>
          <li>在本页填写 SdkAppId、密钥、签名、模板 ID，打开开关并保存。</li>
          <li>在「用户管理 / 个人信息」为各账号维护正确联系电话。</li>
          <li>使用上方测试发送验证通道是否畅通。</li>
        </ol>
      </el-alert>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.card-header {
  display: flex;
  align-items: center;
}
.mb-4 {
  margin-bottom: 16px;
}
.mt-4 {
  margin-top: 16px;
}
.guide-list {
  margin: 8px 0 0;
  padding-left: 18px;
  line-height: 1.7;
}
</style>
