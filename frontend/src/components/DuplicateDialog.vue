<template>
  <el-dialog
    v-model="visible"
    title="重复文件检测"
    width="700px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div class="duplicate-dialog-content">
      <el-alert
        title="检测到重复的照片文件"
        type="warning"
        :closable="false"
        show-icon
      >
        <template #default>
          共检测到 <strong>{{ duplicateFiles.length }}</strong> 组重复文件。
          请选择每组文件的处理方式：<strong>跳过</strong>或<strong>覆盖</strong>。
        </template>
      </el-alert>

      <div class="batch-actions">
        <span class="batch-label">批量操作：</span>
        <el-button size="small" @click="selectAllAction('SKIP')">
          全部跳过
        </el-button>
        <el-button size="small" type="primary" @click="selectAllAction('OVERWRITE')">
          全部覆盖
        </el-button>
      </div>

      <div class="duplicate-list">
        <div
          v-for="(item, index) in duplicateFiles"
          :key="item.fileHash"
          class="duplicate-item"
        >
          <div class="duplicate-header">
            <span class="file-name">
              <el-icon><Picture /></el-icon>
              {{ item.fileName }}
            </span>
            <el-badge :value="item.sourcePaths.length" class="count-badge">
            </el-badge>
          </div>

          <div class="file-paths">
            <div
              v-for="(path, pathIndex) in item.sourcePaths"
              :key="pathIndex"
              class="file-path-item"
            >
              <el-icon class="path-icon"><FolderOpened /></el-icon>
              <span>{{ path }}</span>
            </div>
          </div>

          <div class="action-selector">
            <span class="action-label">处理方式：</span>
            <el-radio-group v-model="item.action" size="small">
              <el-radio :value="'SKIP'">
                <span class="skip-text">跳过（保留已有文件</span>
              </el-radio>
              <el-radio :value="'OVERWRITE'">
                <span class="overwrite-text">覆盖（替换已有文件）</span>
              </el-radio>
            </el-radio-group>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消整理</el-button>
        <el-button type="primary" @click="handleConfirm">
          确认并继续整理
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Picture, FolderOpened } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  duplicateFiles: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const visible = computed({
  get() {
    return props.modelValue
  },
  set(val) {
    emit('update:modelValue', val)
  }
})

const selectAllAction = (action) => {
  props.duplicateFiles.forEach(item => {
    item.action = action
  })
}

const handleCancel = () => {
  emit('cancel')
  visible.value = false
}

const handleConfirm = () => {
  emit('confirm', props.duplicateFiles)
  visible.value = false
}
</script>

<style scoped>
.duplicate-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 500px;
  overflow: 500px;
  overflow-y: auto;
  padding-right: 8px;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.batch-label {
  font-weight: 500;
  color: #606266;
}

.duplicate-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.duplicate-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.duplicate-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
  font-size: 15px;
}

.file-name .el-icon {
  color: #409eff;
  font-size: 18px;
}

.count-badge {
  margin-right: 8px;
}

.file-paths {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 6px;
}

.file-path-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
  word-break: break-all;
}

.path-icon {
  color: #909399;
  flex-shrink: 0;
}

.action-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px dashed #dcdfe6;
}

.action-label {
  font-weight: 500;
  color: #606266;
}

.skip-text {
  color: #909399;
}

.overwrite-text {
  color: #f56c6c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.duplicate-dialog-content::-webkit-scrollbar {
  width: 6px;
}

.duplicate-dialog-content::-webkit-scrollbar-thumb {
  background-color: #c0c4cc;
  border-radius: 3px;
}

.duplicate-dialog-content::-webkit-scrollbar-track {
  background-color: #f5f7fa;
}
</style>
