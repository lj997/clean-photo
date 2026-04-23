<template>
  <div class="app-container">
    <header class="app-header">
      <div class="header-content">
        <h1 class="app-title">
          <el-icon class="title-icon"><Camera /></el-icon>
          照片整理工具
        </h1>
        <p class="app-subtitle">轻松整理您的照片，按时间或地理位置分类</p>
      </div>
    </header>

    <main class="app-main">
      <div class="main-content">
        <div class="form-section">
          <FolderSelector
            v-model="sourceFolders"
            title="源文件夹"
            empty-text="请选择包含照片的源文件夹"
            :allow-multiple="true"
            class="folder-selector"
          />

          <FolderSelector
            v-model="targetFolder"
            title="目标文件夹"
            empty-text="请选择存储整理后照片的目标文件夹"
            :allow-multiple="false"
            class="folder-selector"
          />

          <OrganizeOptions v-model="organizeOptions" />
        </div>

        <div class="action-section">
          <el-button
            type="primary"
            size="large"
            :disabled="!canOrganize"
            :loading="isOrganizing"
            @click="handleOrganize"
            class="organize-btn"
          >
            <el-icon><Edit /></el-icon>
            开始整理
          </el-button>

          <el-progress
            v-if="isOrganizing"
            :percentage="progress"
            :stroke-width="20"
            :text-inside="true"
            class="progress-bar"
          />
        </div>

        <div class="result-section" v-if="showResult">
          <el-card>
            <template #header>
              <div class="result-header">
                <span>整理结果</span>
                <el-tag :type="organizeResult.success ? 'success' : 'danger'">
                  {{ organizeResult.success ? '成功' : '失败' }}
                </el-tag>
              </div>
            </template>

            <div class="result-content">
              <div class="result-stats">
                <div class="stat-item">
                  <span class="stat-label">总照片数</span>
                  <span class="stat-value">{{ organizeResult.totalFiles }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">已处理</span>
                  <span class="stat-value success">{{ organizeResult.processedFiles }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">跳过重复</span>
                  <span class="stat-value warning">{{ organizeResult.duplicateFiles }}</span>
                </div>
              </div>

              <div class="result-message">
                <el-alert
                  :title="organizeResult.message"
                  :type="organizeResult.success ? 'success' : 'error'"
                  :closable="false"
                />
              </div>

              <div class="error-files" v-if="organizeResult.errorFiles && organizeResult.errorFiles.length > 0">
                <h4>处理失败的文件：</h4>
                <ul>
                  <li v-for="(file, index) in organizeResult.errorFiles" :key="index">
                    {{ file }}
                  </li>
                </ul>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </main>

    <DuplicateDialog
      v-model="showDuplicateDialog"
      :duplicate-files="duplicateFiles"
      @confirm="handleDuplicateConfirm"
      @cancel="handleDuplicateCancel"
    />

    <el-loading-mask :visible="isScanning">
      <div class="loading-content">
        <el-icon class="loading-icon"><Refresh /></el-icon>
        <p>正在扫描照片...</p>
      </div>
    </el-loading-mask>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Camera,
  Edit,
  Refresh
} from '@element-plus/icons-vue'

import FolderSelector from './components/FolderSelector.vue'
import OrganizeOptions from './components/OrganizeOptions.vue'
import DuplicateDialog from './components/DuplicateDialog.vue'
import { photoService } from './api/photoService'

const sourceFolders = ref([])
const targetFolder = ref('')
const organizeOptions = ref({
  mode: 'BY_TIME',
  timeUnit: 'MONTH'
})

const isOrganizing = ref(false)
const isScanning = ref(false)
const progress = ref(0)
const showResult = ref(false)
const organizeResult = ref({})

const showDuplicateDialog = ref(false)
const duplicateFiles = ref([])
const pendingOrganizeRequest = ref(null)

const canOrganize = computed(() => {
  return sourceFolders.value.length > 0 && 
         targetFolder.value && 
         !isOrganizing.value && 
         !isScanning.value
})

const handleOrganize = async () => {
  if (!canOrganize.value) return

  try {
    await ElMessageBox.confirm(
      `确定要开始整理照片吗？\n\n源文件夹: ${sourceFolders.value.join(', ')}\n目标文件夹: ${targetFolder.value}`,
      '确认整理',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  isScanning.value = true
  showResult.value = false

  try {
    const result = await photoService.detectDuplicates(
      sourceFolders.value,
      targetFolder.value
    )

    isScanning.value = false

    if (result.duplicateGroups > 0) {
      duplicateFiles.value = result.duplicates.map(d => ({
        ...d,
        action: 'SKIP'
      }))
      
      pendingOrganizeRequest.value = {
        sourceFolders: sourceFolders.value,
        targetFolder: targetFolder.value,
        mode: organizeOptions.value.mode,
        timeUnit: organizeOptions.value.timeUnit
      }
      
      showDuplicateDialog.value = true
    } else {
      await doOrganize({
        sourceFolders: sourceFolders.value,
        targetFolder: targetFolder.value,
        mode: organizeOptions.value.mode,
        timeUnit: organizeOptions.value.timeUnit,
        duplicateActions: []
      })
    }
  } catch (error) {
    isScanning.value = false
    ElMessage.error('扫描照片失败: ' + (error.message || '未知错误'))
  }
}

const handleDuplicateConfirm = async (actions) => {
  if (!pendingOrganizeRequest.value) return

  await doOrganize({
    ...pendingOrganizeRequest.value,
    duplicateActions: actions.map(a => ({
      fileHash: a.fileHash,
      action: a.action
    }))
  })

  pendingOrganizeRequest.value = null
}

const handleDuplicateCancel = () => {
  pendingOrganizeRequest.value = null
  ElMessage.info('已取消整理')
}

const doOrganize = async (request) => {
  isOrganizing.value = true
  progress.value = 0

  try {
    const result = await photoService.organizePhotos(request)
    
    isOrganizing.value = false
    progress.value = 100
    
    organizeResult.value = result
    showResult.value = true

    if (result.success) {
      ElMessage.success(`整理完成！已处理 ${result.processedFiles} 张照片`)
    } else {
      ElMessage.warning('整理完成，但存在错误')
    }
  } catch (error) {
    isOrganizing.value = false
    ElMessage.error('整理失败: ' + (error.message || '未知错误'))
  }
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.app-header {
  padding: 40px 20px;
  text-align: center;
  color: white;
}

.header-content {
  max-width: 800px;
  margin: 0 auto;
}

.app-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 12px 0;
}

.title-icon {
  font-size: 42px;
}

.app-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

.app-main {
  padding: 0 20px 40px;
}

.main-content {
  max-width: 800px;
  margin: 0 auto;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 24px;
}

.folder-selector {
  width: 100%;
}

.action-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: white;
  border-radius: 12px;
  margin-bottom: 24px;
}

.organize-btn {
  min-width: 200px;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 25px;
}

.progress-bar {
  width: 100%;
  max-width: 400px;
}

.result-section {
  margin-top: 24px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-stats {
  display: flex;
  justify-content: space-around;
  padding: 20px 0;
  border-bottom: 1px solid #ebeef5;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.warning {
  color: #e6a23c;
}

.error-files {
  padding: 16px;
  background: #fef0f0;
  border-radius: 8px;
}

.error-files h4 {
  margin: 0 0 8px 0;
  color: #f56c6c;
}

.error-files ul {
  margin: 0;
  padding-left: 20px;
}

.error-files li {
  font-size: 13px;
  color: #606266;
  word-break: break-all;
  margin: 4px 0;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: white;
}

.loading-icon {
  font-size: 48px;
  animation: rotate 2s linear infinite;
  margin-bottom: 16px;
}

.loading-content p {
  font-size: 18px;
  margin: 0;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
