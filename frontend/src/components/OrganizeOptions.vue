<template>
  <el-card class="organize-options-card">
    <template #header>
      <div class="card-header">
        <span>整理选项</span>
      </div>
    </template>

    <div class="options-content">
      <div class="mode-selector">
        <label class="section-label">整理方式：</label>
        <el-radio-group v-model="selectedMode" class="mode-radio-group">
          <el-radio :value="modeOptions.BY_TIME" class="mode-radio">
            <div class="radio-content">
              <el-icon class="radio-icon"><Calendar /></el-icon>
              <div class="radio-text">
                <span class="radio-title">按时间整理</span>
                <span class="radio-desc">根据照片拍摄时间（或文件创建/修改时间）按日期分类</span>
              </div>
            </div>
          </el-radio>

          <el-radio :value="modeOptions.BY_LOCATION" class="mode-radio">
            <div class="radio-content">
              <el-icon class="radio-icon"><Location /></el-icon>
              <div class="radio-text">
                <span class="radio-title">按地理位置整理</span>
                <span class="radio-desc">根据照片GPS信息按国家、省市分类，无地理信息则单独存放</span>
              </div>
            </div>
          </el-radio>

          <el-radio :value="modeOptions.BY_TIME_AND_LOCATION" class="mode-radio">
            <div class="radio-content">
              <el-icon class="radio-icon"><Picture /></el-icon>
              <div class="radio-text">
                <span class="radio-title">时间+地理位置</span>
                <span class="radio-desc">一级目录：年份+国家，二级目录：月份+省市</span>
              </div>
            </div>
          </el-radio>
        </el-radio-group>
      </div>

      <div class="time-unit-selector" v-if="showTimeUnit">
        <label class="section-label">时间粒度：</label>
        <el-radio-group v-model="selectedTimeUnit">
          <el-radio :value="timeUnitOptions.YEAR">按年</el-radio>
          <el-radio :value="timeUnitOptions.MONTH">按月</el-radio>
          <el-radio :value="timeUnitOptions.DAY">按天</el-radio>
        </el-radio-group>
      </div>

      <div class="mode-description">
        <el-alert :title="modeDescription" :type="'info'" :closable="false">
          <template #default>
            <div v-if="showTimeUnit && selectedTimeUnit">
              <br />
              <strong>当前时间粒度：</strong>{{ timeUnitDescription }}
            </div>
          </template>
        </el-alert>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Calendar, Location, Picture } from '@element-plus/icons-vue'

const modeOptions = {
  BY_TIME: 'BY_TIME',
  BY_LOCATION: 'BY_LOCATION',
  BY_TIME_AND_LOCATION: 'BY_TIME_AND_LOCATION'
}

const timeUnitOptions = {
  YEAR: 'YEAR',
  MONTH: 'MONTH',
  DAY: 'DAY'
}

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({
      mode: 'BY_TIME',
      timeUnit: 'MONTH'
    })
  }
})

const emit = defineEmits(['update:modelValue'])

const selectedMode = computed({
  get() {
    return props.modelValue?.mode || modeOptions.BY_TIME
  },
  set(val) {
    emit('update:modelValue', {
      ...props.modelValue,
      mode: val
    })
  }
})

const selectedTimeUnit = computed({
  get() {
    return props.modelValue?.timeUnit || timeUnitOptions.MONTH
  },
  set(val) {
    emit('update:modelValue', {
      ...props.modelValue,
      timeUnit: val
    })
  }
})

const showTimeUnit = computed(() => {
  return selectedMode.value === modeOptions.BY_TIME || 
         selectedMode.value === modeOptions.BY_TIME_AND_LOCATION
})

const modeDescription = computed(() => {
  switch (selectedMode.value) {
    case modeOptions.BY_TIME:
      return '照片将根据拍摄时间（无拍摄时间则使用文件创建/修改时间）整理到对应日期文件夹中。'
    case modeOptions.BY_LOCATION:
      return '照片将根据GPS地理信息按国家、省市分类。无法提取地理位置的照片将存放到"未知位置"文件夹中。'
    case modeOptions.BY_TIME_AND_LOCATION:
      return '照片将同时按时间和地理位置分类。一级目录为"年份-国家"，二级目录为"月份-省市"。'
    default:
      return ''
  }
})

const timeUnitDescription = computed(() => {
  switch (selectedTimeUnit.value) {
    case timeUnitOptions.YEAR:
      return '所有照片按年份归类（如：2025/）'
    case timeUnitOptions.MONTH:
      return '照片按年月归类（如：2025/03/）'
    case timeUnitOptions.DAY:
      return '照片按年月日归类（如：2025/03/15/）'
    default:
      return ''
  }
})
</script>

<style scoped>
.organize-options-card {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.options-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-label {
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  display: block;
}

.mode-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.mode-radio {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 16px 20px;
  margin: 0;
  transition: all 0.2s;
}

.mode-radio:hover {
  border-color: #409eff;
}

.mode-radio :deep(.el-radio__label) {
  padding-left: 12px;
  width: 100%;
}

.radio-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.radio-icon {
  font-size: 24px;
  color: #409eff;
  flex-shrink: 0;
  margin-top: 2px;
}

.radio-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.radio-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.radio-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}

.time-unit-selector {
  padding: 16px 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.time-unit-selector .section-label {
  margin-bottom: 8px;
}

.mode-description {
  margin-top: 8px;
}
</style>
