<script lang="ts" setup>
// 专病网导入 — 占位页（数据模板确认后开发）
</script>

<template>
  <div class="app-container">
    <el-empty description="专病网导入功能开发中">
      <template #description>
        <div class="desc-text">
          <p>本模块用于导入专病/病案信息，系统提取以下字段后进入通知单→随访→服药管理流程：</p>
          <ul>
            <li>患者姓名、身份证号、性别、出生日期、年龄、联系电话</li>
            <li>人群分类、现详细住址、户籍地址、现管单位</li>
            <li>诊断结果（病原学阴/阳性）</li>
          </ul>
          <p class="hint">（数据模板确认后将完成本模块开发）</p>
        </div>
      </template>
    </el-empty>
  </div>
</template>

<style scoped>
.desc-text { text-align: left; line-height: 1.8; color: #606266; font-size: 14px; }
.desc-text ul { padding-left: 20px; }
.hint { color: #909399; font-style: italic; margin-top: 8px; }
</style>
