<template>
  <div ref="mapContainer" :style="{ height }" class="gis-map" />
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import L from 'leaflet'

const props = withDefaults(defineProps<{
  center?: [number, number]
  zoom?: number
  height?: string
  markers?: Array<{
    lat: number
    lng: number
    popup?: string
    color?: string
  }>
}>(), {
  center: () => [39.9042, 116.4074],
  zoom: 10,
  height: '600px',
  markers: () => [],
})

const mapContainer = ref<HTMLElement>()
let map: L.Map | null = null
const markerLayer = ref<L.LayerGroup>()

function getIcon(color: string) {
  return L.divIcon({
    className: 'custom-marker',
    html: `<div style="background:${color};width:24px;height:24px;border-radius:50%;border:2px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,.3);" />`,
    iconSize: [24, 24],
    iconAnchor: [12, 12],
  })
}

onMounted(() => {
  if (!mapContainer.value) return
  map = L.map(mapContainer.value).setView(props.center, props.zoom)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)

  markerLayer.value = L.layerGroup().addTo(map)
  updateMarkers()
})

function updateMarkers() {
  if (!markerLayer.value) return
  markerLayer.value.clearLayers()
  props.markers.forEach((m) => {
    const marker = L.marker([m.lat, m.lng], {
      icon: getIcon(m.color || '#e74c3c'),
    }).addTo(markerLayer.value!)
    if (m.popup) {
      marker.bindPopup(m.popup)
    }
  })
}

watch(() => props.markers, updateMarkers, { deep: true })

onBeforeUnmount(() => {
  map?.remove()
})
</script>

<style scoped>
.gis-map { border-radius: 8px; overflow: hidden; }
</style>
