<script lang="ts" setup>
import type { FormInstance, FormRules, UploadProps, UploadRequestOptions } from "element-plus"
import { updateCurrentUserApi } from "@@/apis/users"
import { resolveFileUrl, uploadAttachmentFile } from "@@/utils/attachment"
import { UserFilled } from "@element-plus/icons-vue"
import { useUserStore } from "@/pinia/stores/user"

defineOptions({ name: "Profile" })

const userStore = useUserStore()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const formData = reactive({
  realName: "",
  orgName: "",
  avatar: "",
  password: "",
  confirmPassword: ""
})

const userInfoItems = computed(() => [
  { label: "用户名", value: userStore.username || "-" },
  { label: "角色", value: userStore.roleName || "-" },
  { label: "用户 ID", value: userStore.userId || "-" }
])

const avatarPreview = computed(() => resolveFileUrl(formData.avatar))

const rules: FormRules<typeof formData> = {
  realName: [{ required: true, message: "请输入真实姓名", trigger: "blur" }],
  confirmPassword: [
    {
      validator: (_rule, value, callback) => {
        if (formData.password && value !== formData.password) {
          callback(new Error("两次输入的密码不一致"))
          return
        }
        callback()
      },
      trigger: "blur"
    }
  ]
}

function syncFormData() {
  formData.realName = userStore.realName
  formData.orgName = userStore.orgName
  formData.avatar = userStore.avatar
  formData.password = ""
  formData.confirmPassword = ""
}

function handleAvatarUpload(options: UploadRequestOptions) {
  return uploadAttachmentFile(options)
}

const beforeAvatarUpload: UploadProps["beforeUpload"] = (file) => {
  if (!file.type.startsWith("image/")) {
    ElMessage.error("仅支持图片格式")
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error("头像大小不能超过 2MB")
    return false
  }
  return true
}

const onAvatarSuccess: UploadProps["onSuccess"] = (response) => {
  const body = response as ApiResponseData<string>
  if (body.code === 200 && body.data) {
    formData.avatar = body.data
    ElMessage.success("头像上传成功，请保存修改")
    return
  }
  ElMessage.error(body.msg || "头像上传失败")
}

function removeAvatar() {
  formData.avatar = ""
}

async function handleSubmit() {
  await formRef.value?.validate()
  const isPasswordChanged = Boolean(formData.password)
  submitting.value = true
  try {
    await updateCurrentUserApi({
      realName: formData.realName,
      orgName: formData.orgName,
      avatar: formData.avatar,
      password: formData.password || undefined
    })
    if (isPasswordChanged) {
      userStore.logout()
      ElMessage.success("密码已修改，请重新登录")
      router.replace("/login")
      return
    }
    await userStore.getInfo()
    syncFormData()
    ElMessage.success("个人信息已更新")
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  syncFormData()
  formRef.value?.clearValidate()
}

syncFormData()
</script>

<template>
  <div class="app-container profile-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
        </div>
      </template>

      <el-row :gutter="24">
        <el-col :xs="24" :md="8">
          <div class="profile-summary">
            <el-avatar :size="72" :src="avatarPreview" :icon="UserFilled" />
            <div class="profile-name">
              {{ userStore.realName || userStore.username }}
            </div>
            <div class="profile-role">
              {{ userStore.roleName || "未设置角色" }}
            </div>
            <el-upload
              :show-file-list="false"
              :http-request="handleAvatarUpload"
              :before-upload="beforeAvatarUpload"
              :on-success="onAvatarSuccess"
              :disabled="submitting"
              accept="image/*"
              name="file"
              class="avatar-uploader"
            >
              <el-button size="small" type="primary">
                上传头像
              </el-button>
            </el-upload>
            <el-button v-if="formData.avatar" size="small" text type="danger" @click="removeAvatar">
              移除头像
            </el-button>
            <div class="avatar-tip">
              支持 jpg/png 等图片，大小不超过 2MB
            </div>
          </div>
          <el-descriptions :column="1" border class="profile-descriptions">
            <el-descriptions-item v-for="item in userInfoItems" :key="item.label" :label="item.label">
              {{ item.value }}
            </el-descriptions-item>
          </el-descriptions>
        </el-col>

        <el-col :xs="24" :md="16">
          <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model.trim="formData.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="所属机构">
              <el-input v-model.trim="formData.orgName" placeholder="请输入所属机构名称" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input
                v-model="formData.password"
                type="password"
                show-password
                placeholder="留空则不修改密码"
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="formData.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入新密码"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">
                保存修改
              </el-button>
              <el-button @click="handleReset">
                重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.profile-page {
  .card-header {
    font-size: 16px;
    font-weight: 600;
  }

  .profile-summary {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 16px 0 24px;
  }

  .profile-name {
    margin-top: 12px;
    font-size: 18px;
    font-weight: 600;
  }

  .profile-role {
    margin-top: 6px;
    margin-bottom: 12px;
    color: var(--el-text-color-secondary);
  }

  .avatar-uploader {
    margin-bottom: 4px;
  }

  .avatar-tip {
    margin-top: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .profile-descriptions {
    margin-bottom: 24px;
  }
}
</style>
