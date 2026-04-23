<template>
  <div class="folder-selector">
    <el-card class="folder-card">
      <template #header>
        <div class="card-header">
          <span>{{ title }}</span>
          <el-button type="primary" size="small" @click="handleAddFolder" v-if="allowMultiple">
            <el-icon><Plus /></el-icon>
            添加文件夹
          </el-button>
        </div>
      </template>

      <div class="folder-list" v-if="selectedFolders.length > 0">
        <div
          v-for="(folder, index) in selectedFolders"
          :key="index"
          class="folder-item"
        >
          <el-icon class="folder-icon"><Folder /></el-icon>
          <span class="folder-path">{{ folder }}</span>
          <el-button
            type="danger"
            size="small"
            text
            @click="removeFolder(index)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <div class="empty-hint" v-else>
        <el-icon class="empty-icon"><FolderOpened /></el-icon>
        <span>{{ emptyText }}</span>
        <el-button type="primary" @click="handleAddFolder" v-if="!allowMultiple">
          选择文件夹
        </el-button>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="选择文件夹"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="folder-browser">
        <el-breadcrumb separator=">">
          <el-breadcrumb-item
            v-for="(item, index) in currentPathParts"
            :key="index"
            @click="navigateToPath(index)"
            class="breadcrumb-item"
          >
            {{ item }}
          </el-breadcrumb-item>
        </el-breadcrumb>

        <div class="drive-list" v-if="currentPathParts.length === 0">
          <div
            v-for="drive in drives"
            :key="drive"
            class="drive-item"
            @click="selectDrive(drive)"
          >
            <el-icon><Cpu /></el-icon>
            <span>{{ drive }}</span>
          </div>
        </div>

        <div class="folder-list-dialog" v-else>
          <div
            v-for="folder in currentFolders"
            :key="folder"
            class="folder-item-dialog"
            @click="selectFolder(folder)"
          >
            <el-icon><Folder /></el-icon>
            <span>{{ getFolderName(folder) }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="selected-path-display">
          <el-icon><FolderOpened /></el-icon>
          <span>当前选择: {{ currentPath }}</span>
        </div>
        <div class="dialog-buttons">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmSelection" :disabled="!currentPath">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { photoService } from '../api/photoService'
import {
  Plus,
  Delete,
  Folder,
  FolderOpened,
  Cpu
} from '@element-plus/icons-vue'

const props = defineProps({
  title: {
    type: String,
    default: '文件夹选择'
  },
  emptyText: {
    type: String,
    default: '请选择文件夹'
  },
  allowMultiple: {
    type: Boolean,
    default: true
  },
  modelValue: {
    type: [Array, String],
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const dialogVisible = ref(false)
const drives = ref([])
const currentPath = ref('')
const currentFolders = ref([])

const selectedFolders = computed({
  get() {
    if (props.allowMultiple) {
      return Array.isArray(props.modelValue) ? props.modelValue : []
    }
    return props.modelValue ? [props.modelValue] : []
  },
  set(val) {
    if (props.allowMultiple) {
      emit('update:modelValue', val)
    } else {
      emit('update:modelValue', val[0] || '')
    }
  }
})

const currentPathParts = computed(() => {
  if (!currentPath.value) return []
  const parts = currentPath.value.split(/[\\/]/).filter(p => p)
  return parts
})

onMounted(async () => {
  await loadDrives()
})

const loadDrives = async () => {
  drives.value = await photoService.getDrives()
}

const handleAddFolder = () => {
  currentPath.value = ''
  currentFolders.value = []
  dialogVisible.value = true
}

const loadFolders = async (path) => {
  currentFolders.value = await photoService.getFolders(path)
}

const selectDrive = (drive) => {
  currentPath.value = drive
  loadFolders(drive)
}

const selectFolder = (folder) => {
  currentPath.value = folder
  loadFolders(folder)
}

const navigateToPath = (index) => {
  const parts = currentPathParts.value.slice(0, index + 1)
  if (parts.length === 0) {
    currentPath.value = ''
    currentFolders.value = []
  } else {
    currentPath.value = parts.join('\\') + '\\'
    loadFolders(currentPath.value)
  }
}

const getFolderName = (path) => {
  const parts = path.split(/[\\/]/).filter(p => p)
  return parts[parts.length - 1]
}

const confirmSelection = () => {
  if (!currentPath.value) return

  if (props.allowMultiple) {
    if (!selectedFolders.value.includes(currentPath.value)) {
      selectedFolders.value = [...selectedFolders.value, currentPath.value]
    }
  } else {
    selectedFolders.value = [currentPath.value]
  }

  dialogVisible.value = false
}

const removeFolder = (index) => {
  const newFolders = [...selectedFolders.value]
  newFolders.splice(index, 1)
  selectedFolders.value = newFolders
}
</script>

<style scoped>
.folder-selector {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.folder-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.folder-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.folder-icon {
  margin-right: 8px;
  font-size: 18px;
  color: #409eff;
}

.folder-path {
  flex: 1;
  font-size: 14px;
  word-break: break-all;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  color: #909399;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-hint span {
  margin-bottom: 16px;
}

.folder-browser {
  min-height: 300px;
}

.breadcrumb {
  margin-bottom: 16px;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.breadcrumb-item {
  cursor: pointer;
}

.breadcrumb-item:hover {
  color: #409eff;
}

.drive-list,
.folder-list-dialog {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  max-height: 250px;
  overflow-y: auto;
}

.drive-item,
.folder-item-dialog {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 120px;
}

.drive-item:hover,
.folder-item-dialog:hover {
  background: #ecf5ff;
  color: #409eff;
}

.drive-item .el-icon,
.folder-item-dialog .el-icon {
  margin-right: 8px;
  font-size: 20px;
}

.selected-path-display {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.selected-path-display .el-icon {
  margin-right: 8px;
  color: #409eff;
}

.selected-path-display span {
  font-size: 14px;
  color: #606266;
  word-break: break-all;
}

.dialog-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
