<template>
  <div ref="chartRef" :style="{ width: width, height: height }"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: {
    type: Object,
    required: true
  },
  width: {
    type: String,
    default: '100%'
  },
  height: {
    type: String,
    default: '400px'
  }
})

const chartRef = ref(null)
let myChart = null

const initChart = async () => {
  if (!chartRef.value) return
  
  try {
    await nextTick()
    if (chartRef.value && props.option) {
      // 销毁之前的图表实例（如果有）
      if (myChart) {
        myChart.dispose()
      }
      
      myChart = echarts.init(chartRef.value)
      myChart.setOption(props.option, true) // 使用不合并的方式设置选项
      
      // 监听窗口大小变化，自动调整图表大小
      window.addEventListener('resize', resizeChart)
    }
  } catch (error) {
    console.error('初始化图表失败:', error)
  }
}

const resizeChart = () => {
  if (myChart) {
    try {
      myChart.resize()
    } catch (error) {
      console.error('调整图表大小失败:', error)
    }
  }
}

// 监听option变化，重新渲染图表
watch(() => props.option, (newOption) => {
  if (myChart && newOption) {
    try {
      myChart.setOption(newOption, true)
    } catch (error) {
      console.error('更新图表选项失败:', error)
      // 如果更新失败，重新初始化图表
      setTimeout(initChart, 100)
    }
  }
}, { deep: true })

onMounted(() => {
  initChart()
})

onUnmounted(() => {
  if (myChart) {
    try {
      myChart.dispose()
      myChart = null
    } catch (error) {
      console.error('销毁图表失败:', error)
    }
  }
  window.removeEventListener('resize', resizeChart)
})

defineExpose({
  refresh: () => {
    if (myChart && props.option) {
      try {
        myChart.setOption(props.option, true)
      } catch (error) {
        console.error('刷新图表失败:', error)
      }
    }
  }
})
</script>