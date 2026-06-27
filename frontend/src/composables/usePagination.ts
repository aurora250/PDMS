import { reactive } from 'vue'

export function usePagination(defaultSize = 20) {
  const page = reactive({
    current: 1,
    size: defaultSize,
    total: 0,
  })

  function reset() {
    page.current = 1
    page.total = 0
  }

  function onPageChange(p: number) {
    page.current = p
  }

  function onSizeChange(s: number) {
    page.size = s
    page.current = 1
  }

  return { page, reset, onPageChange, onSizeChange }
}
