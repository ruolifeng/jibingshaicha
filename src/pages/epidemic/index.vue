<script lang="ts" setup>
// 大疫情导入筛查 — 待开发占位页（后续补充数据模板与解析逻辑）
</script>

<template>
  <div class="app-container">
    <el-empty description="大疫情导入筛查功能开发中">
      <template #description>
        <div class="desc-text">
          <p>本模块用于导入大疫情网（报告卡）数据，提取以下字段后进入追踪→诊断流程：</p>
          <ul>
            <li>患者姓名、有效证件号、性别、出生日期、年龄、联系电话</li>
            <li>现详细住址、病例分类、疾病名称、报告单位</li>
          </ul>
          <p>导入后无需补录筛查信息，直接进入追踪与诊断，根据诊断结果流转至患者管理或潜伏感染者管理。</p>
          <p class="hint">（数据模板确认后将完成本模块开发）</p>
        </div>
      </template>
    </el-empty>
  </div>
</template>

<style scoped>
.desc-text {
  text-align: left;
  line-height: 1.8;
  color: #606266;
  font-size: 14px;
}
.desc-text ul {
  padding-left: 20px;
}
.hint {
  color: #909399;
  font-style: italic;
  margin-top: 8px;
}
</style>
