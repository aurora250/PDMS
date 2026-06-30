import { defineConfig } from 'unocss'

export default defineConfig({
  shortcuts: {
    'pdm-card': 'bg-white rounded-lg shadow-sm border border-gray-100',
    'pdm-page-header': 'mb-4 flex justify-between items-center flex-wrap gap-2',
    'pdm-stat-card': 'bg-white rounded-lg p-4 shadow-sm border-l-4',
  },
  theme: {
    colors: {
      pdm: {
        primary: '#1e3a5f',
        light: '#2c5aa0',
        dark: '#152a45',
        sidebar: '#0f1f38',
        accent: '#c9a84c',
        danger: '#d42020',
        bg: '#f0f2f5',
      },
    },
  },
})
