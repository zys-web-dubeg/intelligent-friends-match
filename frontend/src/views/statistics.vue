<template>
  <div class="statistics-container">
    <!-- 页面头部 -->
    <div class="page-header glass-effect">
      <div class="header-icon">
        <i class="fa-solid fa-chart-bar"></i>
      </div>
      <div class="header-content">
        <h2>数据统计</h2>
        <p>查看系统数据概览和趋势分析</p>
      </div>
      <div class="date-range-selector">
        <el-radio-group v-model="selectedDateRange" @change="handleDateRangeChange">
          <el-radio-button label="threeDays">近3天</el-radio-button>
          <el-radio-button label="sevenDays">近7天</el-radio-button>
          <el-radio-button label="thirtyDays">近30天</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon blue">
          <i class="fa-solid fa-users"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ userCount }}</div>
          <div class="stat-label">系统当前用户数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <i class="fa-solid fa-server"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ apiCount }}</div>
          <div class="stat-label">API请求数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">
          <i class="fa-solid fa-robot"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ agentCount }}</div>
          <div class="stat-label">智能体数量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <i class="fa-solid fa-users"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ teamCount }}</div>
          <div class="stat-label">队伍数量</div>
        </div>
      </div>
    </div>

    <!-- 图表容器 -->
    <div class="chart-container">
      <div class="chart-card glass-effect">
        <div class="chart-header">
          <h3>用户注册统计</h3>
          <div class="chart-type-selector">
            <el-radio-group v-model="userChartType" size="small">
              <el-radio-button label="line">折线图</el-radio-button>
              <el-radio-button label="bar">柱状图</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div class="chart-wrapper">
          <ChartComponent :option="userChartOption" />
        </div>
      </div>

      <div class="chart-card glass-effect">
        <div class="chart-header">
          <h3>API请求统计</h3>
          <div class="chart-type-selector">
            <el-radio-group v-model="apiChartType" size="small">
              <el-radio-button label="line">折线图</el-radio-button>
              <el-radio-button label="bar">柱状图</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div class="chart-wrapper">
          <ChartComponent :option="apiChartOption" />
        </div>
      </div>

      <div class="chart-card glass-effect full-width">
        <div class="chart-header">
          <h3>用户注册与API请求对比</h3>
          <div class="chart-type-selector">
            <el-radio-group v-model="comparisonChartType" size="small">
              <el-radio-button label="line">折线图</el-radio-button>
              <el-radio-button label="bar">柱状图</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div class="chart-wrapper">
          <ChartComponent :option="comparisonChartOption" />
        </div>
      </div>

      <div class="chart-card glass-effect">
        <div class="chart-header">
          <h3>各智能体API请求统计</h3>
        </div>
        <div class="chart-wrapper">
          <ChartComponent :option="allApiChartOption" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import apiClient from '@/api/httpClient'
import ChartComponent from '@/components/ChartComponent.vue'
import * as echarts from 'echarts'

// 图表类型选择
const userChartType = ref('line')
const apiChartType = ref('line')
const comparisonChartType = ref('line')

// 日期范围选择
const selectedDateRange = ref('threeDays')

// 统计数据
const userCount = ref(0)
const apiCount = ref(0)
const agentCount = ref(4)
const teamCount = ref(0)

// 统计数据
const statisticsData = ref({
  userRegistrationData: [],
  apiRequestData: [],
  comparisonData: [],
  allApiRequestsData: []
})

// API端点映射
const apiEndpoints = {
  threeDays: '/api/statistics/recent-three-days',
  sevenDays: '/api/statistics/recent-seven-days',
  thirtyDays: '/api/statistics/recent-thirty-days'
}

// 计算属性 - 用户注册图表选项
const userChartOption = computed(() => {
  try {
    const { userRegistrationData } = statisticsData.value
    
    // 检查数据是否存在且为数组
    if (!Array.isArray(userRegistrationData)) {
      return {
        title: { text: '暂无数据' },
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value' },
        series: []
      }
    }
    
    const dates = userRegistrationData.map(item => {
      if (typeof item === 'object' && item) {
        return item.date || item.日期 || ''
      }
      return ''
    })
    
    const counts = userRegistrationData.map(item => {
      if (typeof item === 'object' && item) {
        return item.count || item['用户注册数'] || 0
      }
      return 0
    })
    
    const chartType = userChartType.value === 'line' ? 'line' : 'bar'
    
    return {
      title: {
        show: false
      },
      tooltip: {
        trigger: 'axis',
        textStyle: { color: '#f1f5f9' },
        backgroundColor: 'rgba(30, 41, 59, 0.9)'
      },
      legend: {
        data: ['用户注册数'],
        textStyle: { color: '#94a3b8' }
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: {
          interval: 0,
          rotate: 45,
          color: '#94a3b8'
        },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { lineStyle: { color: '#334155' } }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#94a3b8' },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { lineStyle: { color: '#334155' } },
        splitLine: { lineStyle: { color: '#1e293b' } }
      },
      series: [{
        name: '用户注册数',
        type: chartType,
        data: counts,
        smooth: true,
        itemStyle: chartType === 'line'
          ? { color: '#0ea5e9', shadowBlur: 10, shadowColor: 'rgba(14, 165, 233, 0.5)' }
          : { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#38bdf8' },
              { offset: 1, color: '#0ea5e9' }
            ]), borderRadius: [6, 6, 0, 0] },
        lineStyle: chartType === 'line' ? { width: 3, shadowBlur: 10, shadowColor: 'rgba(14, 165, 233, 0.5)' } : undefined,
        areaStyle: chartType === 'line' ? {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(14, 165, 233, 0.5)' },
            { offset: 1, color: 'rgba(14, 165, 233, 0.02)' }
          ])
        } : undefined
      }]
    }
  } catch (error) {
    console.error('生成用户注册图表选项错误:', error)
    return {
      title: { text: '数据加载错误' },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: []
    }
  }
})

// 计算属性 - API请求图表选项
const apiChartOption = computed(() => {
  try {
    const { apiRequestData } = statisticsData.value
    
    // 检查数据是否存在且为数组
    if (!Array.isArray(apiRequestData)) {
      return {
        title: { text: '暂无数据' },
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value' },
        series: []
      }
    }
    
    const dates = apiRequestData.map(item => {
      if (typeof item === 'object' && item) {
        return item.date || item.日期 || ''
      }
      return ''
    })
    
    const counts = apiRequestData.map(item => {
      if (typeof item === 'object' && item) {
        return item.count || item['API请求次数'] || 0
      }
      return 0
    })
    
    const chartType = apiChartType.value === 'line' ? 'line' : 'bar'
    
    return {
      title: {
        show: false
      },
      tooltip: {
        trigger: 'axis',
        textStyle: { color: '#f1f5f9' },
        backgroundColor: 'rgba(30, 41, 59, 0.9)'
      },
      legend: {
        data: ['API请求次数'],
        textStyle: { color: '#94a3b8' }
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: {
          interval: 0,
          rotate: 45,
          color: '#94a3b8'
        },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { lineStyle: { color: '#334155' } }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#94a3b8' },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { lineStyle: { color: '#334155' } },
        splitLine: { lineStyle: { color: '#1e293b' } }
      },
      series: [{
        name: 'API请求次数',
        type: chartType,
        data: counts,
        smooth: true,
        itemStyle: chartType === 'line'
          ? { color: '#8b5cf6', shadowBlur: 10, shadowColor: 'rgba(139, 92, 246, 0.5)' }
          : { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#a78bfa' },
              { offset: 1, color: '#8b5cf6' }
            ]), borderRadius: [6, 6, 0, 0] },
        lineStyle: chartType === 'line' ? { width: 3, shadowBlur: 10, shadowColor: 'rgba(139, 92, 246, 0.5)' } : undefined,
        areaStyle: chartType === 'line' ? {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(139, 92, 246, 0.5)' },
            { offset: 1, color: 'rgba(139, 92, 246, 0.02)' }
          ])
        } : undefined
      }]
    }
  } catch (error) {
    console.error('生成API请求图表选项错误:', error)
    return {
      title: { text: '数据加载错误' },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: []
    }
  }
})

// 计算属性 - 对比图表选项
const comparisonChartOption = computed(() => {
  try {
    const { comparisonData } = statisticsData.value
    
    // 检查数据是否存在且为数组
    if (!Array.isArray(comparisonData)) {
      return {
        title: { text: '暂无数据' },
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value' },
        series: []
      }
    }
    
    const dates = comparisonData.map(item => {
      if (typeof item === 'object' && item) {
        return item.date || item.日期 || ''
      }
      return ''
    })
    
    const userCounts = comparisonData.map(item => {
      if (typeof item === 'object' && item) {
        return item.userCount || item['用户注册数'] || 0
      }
      return 0
    })
    
    const apiCounts = comparisonData.map(item => {
      if (typeof item === 'object' && item) {
        return item.apiCount || item['API请求次数'] || 0
      }
      return 0
    })
    
    const chartType = comparisonChartType.value === 'line' ? 'line' : 'bar'
    
    return {
      title: {
        show: false
      },
      tooltip: {
        trigger: 'axis',
        textStyle: { color: '#f1f5f9' },
        backgroundColor: 'rgba(30, 41, 59, 0.9)'
      },
      legend: {
        data: ['用户注册数', 'API请求次数'],
        textStyle: { color: '#94a3b8' }
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: {
          interval: 0,
          rotate: 45,
          color: '#94a3b8'
        },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { lineStyle: { color: '#334155' } }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#94a3b8' },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { lineStyle: { color: '#334155' } },
        splitLine: { lineStyle: { color: '#1e293b' } }
      },
      series: [
        {
          name: '用户注册数',
          type: chartType,
          data: userCounts,
          smooth: true,
          itemStyle: chartType === 'line'
            ? { color: '#0ea5e9', shadowBlur: 10, shadowColor: 'rgba(14, 165, 233, 0.5)' }
            : { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#38bdf8' },
                { offset: 1, color: '#0ea5e9' }
              ]), borderRadius: [6, 6, 0, 0] },
          lineStyle: chartType === 'line' ? { width: 3, shadowBlur: 10, shadowColor: 'rgba(14, 165, 233, 0.5)' } : undefined,
          areaStyle: chartType === 'line' ? {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(14, 165, 233, 0.5)' },
              { offset: 1, color: 'rgba(14, 165, 233, 0.02)' }
            ])
          } : undefined
        },
        {
          name: 'API请求次数',
          type: chartType,
          data: apiCounts,
          smooth: true,
          itemStyle: chartType === 'line'
            ? { color: '#ec4899', shadowBlur: 10, shadowColor: 'rgba(236, 72, 153, 0.5)' }
            : { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#f472b6' },
                { offset: 1, color: '#ec4899' }
              ]), borderRadius: [6, 6, 0, 0] },
          lineStyle: chartType === 'line' ? { width: 3, shadowBlur: 10, shadowColor: 'rgba(236, 72, 153, 0.5)' } : undefined,
          areaStyle: chartType === 'line' ? {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(236, 72, 153, 0.5)' },
              { offset: 1, color: 'rgba(236, 72, 153, 0.02)' }
            ])
          } : undefined
        }
      ]
    }
  } catch (error) {
    console.error('生成对比图表选项错误:', error)
    return {
      title: { text: '数据加载错误' },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: []
    }
  }
})

// 计算属性 - 所有API图表选项（汇总柱状图）
const allApiChartOption = computed(() => {
  try {
    const { allApiRequestsData } = statisticsData.value
    
    if (!Array.isArray(allApiRequestsData) || allApiRequestsData.length === 0) {
      return {
        title: { text: '暂无数据' },
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value' },
        series: []
      }
    }
    
    const first = allApiRequestsData[0]
    const agents = first.agents || []
    
    return {
      title: { show: false },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        textStyle: { color: '#f1f5f9' },
        backgroundColor: 'rgba(30, 41, 59, 0.9)'
      },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: agents.map(a => a.name),
        axisLabel: { interval: 0, rotate: 0, color: '#94a3b8' },
        axisTick: { alignWithLabel: true, lineStyle: { color: '#334155' } },
        axisLine: { lineStyle: { color: '#334155' } }
      },
      yAxis: { 
        type: 'value',
        minInterval: 1,
        axisLabel: { color: '#94a3b8' },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { lineStyle: { color: '#334155' } },
        splitLine: { lineStyle: { color: '#1e293b' } }
      },
      series: [{
        name: 'API请求数',
        type: 'bar',
        barWidth: '50%',
        data: agents.map(a => ({
          name: a.name,
          value: a.value,
          itemStyle: { color: a.color }
        })),
        itemStyle: { barMinHeight: 2, borderRadius: [6, 6, 0, 0] }
      }]
    }
  } catch (error) {
    console.error('生成所有API图表选项错误:', error)
    return {
      title: { text: '数据加载错误' },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: []
    }
  }
})




// 加载数据函数
const loadStatisticsData = async () => {
  try {
    const url = apiEndpoints[selectedDateRange.value]
    console.log('开始请求数据:', url)

    // 检查token是否存在
    const token = localStorage.getItem('auth_token')
    console.log('当前token:', token ? '存在' : '不存在')

    const res = await apiClient.get(url)

    console.log('API原始返回:', res)

    // 检查是否收到了HTML响应
    if (typeof res === 'string' && res.includes('<!DOCTYPE html>')) {
      console.error('错误: 收到了HTML响应而非JSON数据')
      ElMessage.error('后端API未正确配置，请检查后端服务')
      statisticsData.value = { userRegistrationData: [], apiRequestData: [], comparisonData: [], allApiRequestsData: [] }
      return
    }

    // res 已经是 response.data（拦截器返回）
    let extractedData = {}

    if (res && typeof res === 'object') {
      if (res.code === 200 && res.data) {
        extractedData = res.data
        console.log('使用结构: {code: 200, data: {...}}')
      } else if (res.code !== undefined) {
        extractedData = { ...res }
        delete extractedData.code
        delete extractedData.message
        console.log('使用结构: 直接提取（去除code/message）')
      } else {
        extractedData = res
        console.log('使用结构: 直接使用响应')
      }
    }

    console.log('提取的数据:', JSON.stringify(extractedData, null, 2))

    // 更新统计数据
    userCount.value = extractedData.currentUserCount ?? extractedData.userRegisterCount ?? 0
    apiCount.value = extractedData.totalApiRequests || 0
    teamCount.value = extractedData.teamCount || 0

    // 从后端获取的API请求数据
    const apiRequests = extractedData.apiRequests || {}

    // 检查是否后端返回了详细的时间序列数据
    const hasDetailedData = extractedData.hasOwnProperty('userRegistrationData') && 
                           Array.isArray(extractedData.userRegistrationData) && 
                           extractedData.userRegistrationData.length > 0
    
    // 如果后端没有返回详细数据，尝试从后端获取详细的时间序列数据
    let userRegistrationData = []
    let apiRequestData = []
    let comparisonData = []
    
    if (hasDetailedData) {
      // 使用后端返回的详细数据
      userRegistrationData = extractedData.userRegistrationData
      apiRequestData = extractedData.apiRequestData || []
      comparisonData = extractedData.comparisonData || []
    } else {
      // 如果后端没有提供详细时间序列数据，发起单独请求获取
      try {
        // 获取用户注册详细数据
        const userRegRes = await apiClient.get(`/api/statistics/user-register?startDate=${extractedData.startDate}&endDate=${extractedData.endDate}`)
        if (userRegRes.code === 200 && userRegRes.data) {
          userRegistrationData = userRegRes.data.seriesData || []
        }
        
        // 获取API请求详细数据
        const apiReqRes = await apiClient.get(`/api/statistics/api-request?apiType=all&startDate=${extractedData.startDate}&endDate=${extractedData.endDate}`)
        if (apiReqRes.code === 200 && apiReqRes.data) {
          apiRequestData = apiReqRes.data.seriesData || []
        }
        
        // 构建对比数据
        comparisonData = userRegistrationData.map((userItem, index) => {
          const apiItem = apiRequestData[index] || { date: userItem.date, count: 0 }
          return {
            date: userItem.date,
            userCount: userItem.count || 0,
            apiCount: apiItem.count || 0
          }
        })
      } catch (err) {
        console.error('获取详细统计数据失败:', err)
        // 如果获取详细数据失败，使用汇总数据构造单点数据
        userRegistrationData = [{ date: extractedData.endDate, count: extractedData.userRegisterCount || 0 }]
        apiRequestData = [{ date: extractedData.endDate, count: extractedData.totalApiRequests || 0 }]
        comparisonData = [{ date: extractedData.endDate, userCount: extractedData.userRegisterCount || 0, apiCount: extractedData.totalApiRequests || 0 }]
      }
    }
    
    // 各智能体API请求数据 - 使用后端返回的准确数据
    const allApiRequestsData = [{
      date: extractedData.endDate,
      agents: [
        { name: '学习助手', value: apiRequests['学习助手'] || 0, color: '#8b5cf6' },
        { name: '恋爱智能体', value: apiRequests['恋爱智能体'] || 0, color: '#ec4899' },
        { name: '购物助手', value: apiRequests['购物助手'] || 0, color: '#f59e0b' },
        { name: '硅谷小智', value: apiRequests['硅谷小智'] || 0, color: '#06b6d4' }
      ]
    }]

    statisticsData.value = {
      userRegistrationData,
      apiRequestData,
      comparisonData,
      allApiRequestsData
    }

    console.log('最终设置的统计数据:', statisticsData.value)

    const hasData = Object.values(statisticsData.value).some(arr => Array.isArray(arr) && arr.length > 0)
    if (!hasData) {
      console.warn('警告: 没有获取到任何统计数据')
      ElMessage.warning('暂无统计数据 - 请打开控制台(F12)查看详细日志')
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    console.error('错误详情:', error.response || error.message)
    statisticsData.value = { userRegistrationData: [], apiRequestData: [], comparisonData: [], allApiRequestsData: [] }
    const errMsg = error.response?.data?.message || error.message || '未知错误'
    ElMessage.error('获取统计数据失败: ' + errMsg)
  }
}

// 日期范围变化处理
const handleDateRangeChange = () => {
  loadStatisticsData()
}

// 初始化数据
onMounted(() => {
  loadStatisticsData()
})
</script>

<style scoped>
.statistics-container {
  padding: 24px;
  min-height: 100vh;
  overflow-x: auto;
  background: #0f172a;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 24px;
}

.header-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0ea5e9;
  font-size: 24px;
}

.header-content {
  flex: 1;
}

.header-content h2 {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 700;
  color: #f1f5f9;
}

.header-content p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.date-range-selector {
  display: flex;
  align-items: center;
}

:deep(.date-range-selector .el-radio-group) {
  background: rgba(15, 23, 42, 0.6);
  border-radius: 10px;
  padding: 4px;
  border: 1px solid rgba(14, 165, 233, 0.2);
}

:deep(.date-range-selector .el-radio-button__inner) {
  background: transparent;
  border: none;
  color: #94a3b8;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
  border-radius: 8px;
}

:deep(.date-range-selector .el-radio-button:hover .el-radio-button__inner) {
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.15) 0%, rgba(14, 165, 233, 0.15) 100%);
  color: #e2e8f0;
  box-shadow: 0 0 20px rgba(14, 165, 233, 0.2);
}

:deep(.date-range-selector .el-radio-button.is-active .el-radio-button__inner) {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.3) 0%, rgba(139, 92, 246, 0.3) 100%);
  color: #ffffff;
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(14, 165, 233, 0.5);
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  background: rgba(255, 255, 255, 0.05);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.stat-icon.blue {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.3) 0%, rgba(56, 189, 248, 0.3) 100%);
  color: #0ea5e9;
}

.stat-icon.green {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.3) 0%, rgba(74, 222, 128, 0.3) 100%);
  color: #22c55e;
}

.stat-icon.purple {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.3) 0%, rgba(167, 139, 250, 0.3) 100%);
  color: #8b5cf6;
}

.stat-icon.orange {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.3) 0%, rgba(251, 191, 36, 0.3) 100%);
  color: #f59e0b;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #f1f5f9;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

/* 图表容器 */
.chart-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 20px;
  padding-bottom: 20px;
}

.chart-card {
  padding: 24px;
  border-radius: 16px;
}

.chart-card.full-width {
  grid-column: 1 / -1;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-header h3 {
  margin: 0;
  color: #f1f5f9;
  font-size: 16px;
  font-weight: 600;
}

.chart-type-selector {
  display: flex;
  align-items: center;
}

:deep(.chart-type-selector .el-radio-group) {
  background: rgba(15, 23, 42, 0.6);
  border-radius: 8px;
  padding: 3px;
  border: 1px solid rgba(14, 165, 233, 0.2);
}

:deep(.chart-type-selector .el-radio-button__inner) {
  background: transparent;
  border: none;
  color: #94a3b8;
  padding: 5px 14px;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.3s ease;
  border-radius: 6px;
}

:deep(.chart-type-selector .el-radio-button:hover .el-radio-button__inner) {
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.15) 0%, rgba(14, 165, 233, 0.15) 100%);
  color: #e2e8f0;
  box-shadow: 0 0 15px rgba(14, 165, 233, 0.2);
}

:deep(.chart-type-selector .el-radio-button.is-active .el-radio-button__inner) {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.3) 0%, rgba(139, 92, 246, 0.3) 100%);
  color: #ffffff;
  box-shadow: 0 3px 12px rgba(14, 165, 233, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(14, 165, 233, 0.5);
}

.chart-wrapper {
  height: 400px;
}

@media (max-width: 768px) {
  .chart-container {
    grid-template-columns: 1fr;
  }

  .chart-wrapper {
    height: 300px;
  }

  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
