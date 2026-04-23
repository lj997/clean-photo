import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

export const photoService = {
  async getDrives() {
    try {
      const response = await apiClient.get('/photos/drives')
      return response.data
    } catch (error) {
      console.error('获取驱动器列表失败:', error)
      return []
    }
  },

  async getFolders(path) {
    try {
      const response = await apiClient.post('/photos/folders', { path })
      return response.data
    } catch (error) {
      console.error('获取文件夹列表失败:', error)
      return []
    }
  },

  async scanPhotos(sourceFolders) {
    try {
      const response = await apiClient.post('/photos/scan', { sourceFolders })
      return response.data
    } catch (error) {
      console.error('扫描照片失败:', error)
      throw error
    }
  },

  async detectDuplicates(sourceFolders, targetFolder) {
    try {
      const response = await apiClient.post('/photos/detect-duplicates', {
        sourceFolders,
        targetFolder
      })
      return response.data
    } catch (error) {
      console.error('检测重复文件失败:', error)
      throw error
    }
  },

  async organizePhotos(request) {
    try {
      const response = await apiClient.post('/photos/organize', request)
      return response.data
    } catch (error) {
      console.error('整理照片失败:', error)
      throw error
    }
  }
}
