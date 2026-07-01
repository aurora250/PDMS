<template>
  <div class="family-tree" :style="{ minHeight: height }">
    <div class="tree-container">
      <!-- 第3代：祖父母层（maxGenerations >= 3） -->
      <div v-if="maxGenerations >= 3 && hasGrandparents" class="grandparents-row">
        <div class="gp-group">
          <div class="gp-pair">
            <div v-if="paternalGrandfather" class="tree-node gp father" @click="$router.push(`/resident/${paternalGrandfather.uuid}`)">
              <el-icon><UserFilled /></el-icon>
              <span class="node-label">{{ paternalGrandfather.name }}</span>
              <span class="node-tag">祖父</span>
            </div>
            <div v-if="paternalGrandmother" class="tree-node gp mother" @click="$router.push(`/resident/${paternalGrandmother.uuid}`)">
              <el-icon><UserFilled /></el-icon>
              <span class="node-label">{{ paternalGrandmother.name }}</span>
              <span class="node-tag">祖母</span>
            </div>
          </div>
          <span class="gp-label">父系</span>
        </div>
        <div class="gp-divider" />
        <div class="gp-group">
          <div class="gp-pair">
            <div v-if="maternalGrandfather" class="tree-node gp father" @click="$router.push(`/resident/${maternalGrandfather.uuid}`)">
              <el-icon><UserFilled /></el-icon>
              <span class="node-label">{{ maternalGrandfather.name }}</span>
              <span class="node-tag">外祖父</span>
            </div>
            <div v-if="maternalGrandmother" class="tree-node gp mother" @click="$router.push(`/resident/${maternalGrandmother.uuid}`)">
              <el-icon><UserFilled /></el-icon>
              <span class="node-label">{{ maternalGrandmother.name }}</span>
              <span class="node-tag">外祖母</span>
            </div>
          </div>
          <span class="gp-label">母系</span>
        </div>
      </div>

      <!-- 祖父母→父母连接线 -->
      <div v-if="maxGenerations >= 3 && hasGrandparents" class="connector-row">
        <div class="connector-v" />
        <div class="connector-v" />
      </div>

      <!-- 第2代：父母层（maxGenerations >= 2） -->
      <div v-if="maxGenerations >= 2" class="parents-row">
        <div v-if="father" class="tree-branch">
          <div class="tree-node parent father" @click="$router.push(`/resident/${father.uuid}`)">
            <el-icon><UserFilled /></el-icon>
            <span class="node-label">{{ father.name }}</span>
            <span class="node-tag">父亲</span>
          </div>
        </div>
        <div v-if="mother" class="tree-branch">
          <div class="tree-node parent mother" @click="$router.push(`/resident/${mother.uuid}`)">
            <el-icon><UserFilled /></el-icon>
            <span class="node-label">{{ mother.name }}</span>
            <span class="node-tag">母亲</span>
          </div>
        </div>
      </div>

      <!-- 父母→本人连接线 -->
      <div v-if="maxGenerations >= 2 && (father || mother)" class="connector-row single">
        <div class="connector-v" />
      </div>

      <!-- 第1代：本人 + 配偶 -->
      <div class="tree-root">
        <div class="tree-node self" @click="$router.push(`/resident/${person.uuid}`)">
          <el-icon><UserFilled /></el-icon>
          <span class="node-label">{{ person.name }}</span>
          <span class="node-tag">本人</span>
        </div>
        <div v-if="spouse" class="spouse-row">
          <div class="connector-h" />
          <div class="tree-node spouse" @click="$router.push(`/resident/${spouse.uuid}`)">
            <el-icon><UserFilled /></el-icon>
            <span class="node-label">{{ spouse.name }}</span>
            <span class="node-tag">配偶</span>
          </div>
        </div>
      </div>

      <!-- 本人→子女连接线 -->
      <div v-if="children.length > 0" class="connector-row single">
        <div class="connector-v" />
      </div>

      <!-- 第1代：子女层 -->
      <div v-if="children.length > 0" class="children-row">
        <div v-for="child in children" :key="child.uuid" class="tree-branch">
          <div class="tree-node child" @click="$router.push(`/resident/${child.uuid}`)">
            <el-icon><UserFilled /></el-icon>
            <span class="node-label">{{ child.name }}</span>
            <span class="node-tag">子女</span>
          </div>
        </div>
      </div>

      <!-- 子女→孙子女连接线（maxGenerations >= 2） -->
      <div v-if="maxGenerations >= 2 && grandchildren.length > 0" class="connector-row single">
        <div class="connector-v" />
      </div>

      <!-- 第2代：孙子女层（maxGenerations >= 2） -->
      <div v-if="maxGenerations >= 2 && grandchildren.length > 0" class="grandchildren-row">
        <div v-for="gc in grandchildren" :key="gc.uuid" class="tree-branch">
          <div class="tree-node gc" @click="$router.push(`/resident/${gc.uuid}`)">
            <el-icon><UserFilled /></el-icon>
            <span class="node-label">{{ gc.name }}</span>
            <span class="node-tag">{{ gc.parentName ? gc.parentName + '之子女' : '孙' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  person: { name: string; gender: string; uuid: string }
  father: { uuid: string; name: string } | null
  mother: { uuid: string; name: string } | null
  spouse: { uuid: string; name: string } | null
  children: { uuid: string; name: string }[]
  maxGenerations?: number
  paternalGrandfather?: { uuid: string; name: string } | null
  paternalGrandmother?: { uuid: string; name: string } | null
  maternalGrandfather?: { uuid: string; name: string } | null
  maternalGrandmother?: { uuid: string; name: string } | null
  grandchildren?: { uuid: string; name: string; gender: string; parentName: string }[]
  height?: string
}>(), {
  maxGenerations: 1,
  grandchildren: () => [],
})

const hasGrandparents = computed(() =>
  props.paternalGrandfather || props.paternalGrandmother ||
  props.maternalGrandfather || props.maternalGrandmother
)

</script>

<style scoped>
.family-tree { display: flex; align-items: center; justify-content: center; padding: 24px; }
.tree-container { text-align: center; }

/* 连接线 */
.connector-h { width: 48px; height: 2px; background: #c0c4cc; flex-shrink: 0; }
.connector-v { width: 2px; height: 28px; background: #c0c4cc; margin: 0 auto; }
.connector-row { display: flex; justify-content: center; gap: 80px; }
.connector-row.single { gap: 0; }

/* 本人+配偶行 */
.tree-root { display: flex; align-items: center; justify-content: center; gap: 4px; }
.spouse-row { display: flex; align-items: center; gap: 0; }

/* 父母行 */
.parents-row { display: flex; align-items: flex-start; justify-content: center; gap: 80px; }

/* 子女/孙子女行 */
.children-row, .grandchildren-row {
  display: flex; align-items: flex-start; justify-content: center; gap: 24px; flex-wrap: wrap;
}

/* 祖父母行 */
.grandparents-row { display: flex; align-items: flex-start; justify-content: center; gap: 48px; }
.gp-group { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.gp-pair { display: flex; gap: 16px; }
.gp-label { font-size: 11px; color: #909399; }
.gp-divider { width: 1px; height: 40px; background: #e4e7ed; margin-top: 8px; }

.tree-branch { display: flex; flex-direction: column; align-items: center; }

/* 节点样式 */
.tree-node {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 10px 16px; border-radius: 12px; cursor: pointer;
  transition: all 0.2s; border: 2px solid #e4e7ed;
  background: #fff; min-width: 70px;
}
.tree-node:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,.1); }
.tree-node.self { border-color: #409EFF; background: #ecf5ff; }
.tree-node.spouse { border-color: #E6A23C; background: #fdf6ec; }
.tree-node.parent.father { border-color: #F56C6C; background: #fef0f0; }
.tree-node.parent.mother { border-color: #E6A23C; background: #fdf6ec; }
.tree-node.child { border-color: #67C23A; background: #f0f9eb; }
.tree-node.gc { border-color: #909399; background: #f5f7fa; }
.tree-node.gp.father { border-color: #C0C4CC; background: #fafafa; }
.tree-node.gp.mother { border-color: #C0C4CC; background: #fafafa; }
.tree-node .el-icon { font-size: 20px; color: #606266; }
.node-label { font-weight: 600; font-size: 13px; color: #303133; }
.node-tag { font-size: 11px; color: #909399; }
</style>
