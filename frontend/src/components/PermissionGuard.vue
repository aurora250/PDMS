<template>
  <slot v-if="hasPermission(perm)" />
</template>

<script setup lang="ts">
import { usePermission } from '@/composables/usePermission'

defineProps<{
  perm: string
}>()

const { hasPermission } = usePermission()
</script>
