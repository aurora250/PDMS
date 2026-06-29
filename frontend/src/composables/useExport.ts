import { showError } from '@/utils/auth'

export function useExport() {
  async function doExport(apiFn: () => Promise<Blob>, filename: string) {
    try {
      const blob = await apiFn()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    } catch {
      showError('导出失败')
    }
  }

  /** 从 API 响应（可能是 blob 或带 url 的对象）下载 */
  async function downloadFromApi(apiFn: () => Promise<any>, filename: string) {
    try {
      const result = await apiFn()
      if (result instanceof Blob) {
        const url = URL.createObjectURL(result)
        const a = document.createElement('a')
        a.href = url
        a.download = filename
        a.click()
        URL.revokeObjectURL(url)
      } else if (result?.url) {
        window.open(result.url, '_blank')
      } else {
        showError('导出数据格式不正确')
      }
    } catch {
      showError('导出失败')
    }
  }

  return { doExport, downloadFromApi }
}
