<template>
  <div class="package-container">

    <div class="overview-section">
      <div class="overview-card">
        <div class="card-header">
          <h3>账户概览</h3>
          <el-tag v-if="userInfo.expTime" :type="getExpStatus(userInfo.expTime).type">
            {{ getExpStatus(userInfo.expTime).text }}
          </el-tag>
          <el-tag v-else type="success">永久账户</el-tag>
        </div>
        <div class="overview-stats">
          <div class="stat-item">
            <div class="stat-icon">
              <i class="el-icon-pie-chart"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatFlow(userInfo.flow, 'gb') }}</div>
              <div class="stat-label">总流量配额</div>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">
              <i class="el-icon-upload"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatFlow(calculateUserTotalUsedFlow()) }}</div>
              <div class="stat-label">已用流量</div>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">
              <i class="el-icon-connection"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ userInfo.num || 0 }}</div>
              <div class="stat-label">转发配额</div>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">
              <i class="el-icon-s-operation"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ totalUsedForwards }}</div>
              <div class="stat-label">已用转发</div>
            </div>
          </div>
          <div class="stat-item" v-if="userInfo.flowResetTime">
            <div class="stat-icon">
              <i class="el-icon-refresh"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatResetTime(userInfo.flowResetTime) }}</div>
              <div class="stat-label">流量重置时间</div>
            </div>
          </div>
        </div>
        <div class="progress-section" v-if="userInfo.flow > 0">
          <div class="progress-item">
            <div class="progress-header">
              <span>流量使用率</span>
              <span>{{ calculateUsagePercentage('flow').toFixed(1) }}%</span>
            </div>
            <el-progress
                :percentage="calculateUsagePercentage('flow')"
                :status="getUsageStatus('flow')"
                :stroke-width="8"
            ></el-progress>
          </div>
          <div class="progress-item">
            <div class="progress-header">
              <span>转发使用率</span>
              <span>{{ calculateUsagePercentage('forwards').toFixed(1) }}%</span>
            </div>
            <el-progress
                :percentage="calculateUsagePercentage('forwards')"
                :status="getUsageStatus('forwards')"
                :stroke-width="8"
            ></el-progress>
          </div>
        </div>
      </div>
    </div>

    <!-- 隧道权限列表 -->
    <div class="tunnels-section">
      <h3 class="section-title">隧道权限</h3>
      <div v-loading="loading" class="tunnels-grid">
        <div v-if="userTunnels.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无隧道权限"></el-empty>
        </div>

        <div
            v-for="tunnel in userTunnels"
            :key="tunnel.id"
            class="tunnel-card"
        >
          <div class="tunnel-header">
            <div class="tunnel-name">
              <i class="el-icon-link"></i>
              {{ tunnel.tunnelName }}
            </div>
            <div class="tunnel-status">
              <el-tag v-if="tunnel.expTime" :type="getTunnelExpStatus(tunnel.expTime).type" size="mini">
                {{ getTunnelExpStatus(tunnel.expTime).text }}
              </el-tag>
              <el-tag v-else type="success" size="mini">永久</el-tag>
              <el-tag v-if="tunnel.tunnelFlow === 1" type="info" size="mini">单向计费</el-tag>
              <el-tag v-else type="warning" size="mini">双向计费</el-tag>
            </div>
          </div>

          <div class="tunnel-stats">
            <div class="tunnel-stat-row">
              <div class="tunnel-stat-item">
                <span class="stat-label">流量配额:</span>
                <span class="stat-value">{{ formatFlow(tunnel.flow, 'gb') }}</span>
              </div>
              <div class="tunnel-stat-item">
                <span class="stat-label">已用流量:</span>
                <span class="stat-value used">{{ formatFlow(calculateTunnelUsedFlow(tunnel)) }}</span>
              </div>
            </div>
            <div class="tunnel-stat-row">
              <div class="tunnel-stat-item">
                <span class="stat-label">转发配额:</span>
                <span class="stat-value">{{ tunnel.num }}</span>
              </div>
              <div class="tunnel-stat-item">
                <span class="stat-label">已用转发:</span>
                <span class="stat-value used">{{ getTunnelUsedForwards(tunnel.tunnelId) }}</span>
              </div>
            </div>
          </div>

          <div class="tunnel-progress">
            <div class="progress-mini">
              <div class="progress-mini-header">
                <span>流量使用</span>
                <span>{{ calculateTunnelFlowPercentage(tunnel).toFixed(1) }}%</span>
              </div>
              <el-progress
                  :percentage="calculateTunnelFlowPercentage(tunnel)"
                  :status="getTunnelFlowStatus(tunnel)"
                  :stroke-width="6"
                  :show-text="false"
              ></el-progress>
            </div>
            <div class="progress-mini">
              <div class="progress-mini-header">
                <span>转发使用</span>
                <span>{{ calculateTunnelForwardPercentage(tunnel).toFixed(1) }}%</span>
              </div>
              <el-progress
                  :percentage="calculateTunnelForwardPercentage(tunnel)"
                  :status="getTunnelForwardStatus(tunnel)"
                  :stroke-width="6"
                  :show-text="false"
              ></el-progress>
            </div>
          </div>

          <!-- 流量重置时间 -->
          <div class="tunnel-footer" v-if="tunnel.flowResetTime">
            <span class="reset-time">
              <i class="el-icon-time"></i>
              流量重置: {{ new Date(tunnel.flowResetTime) | dateFormat }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 转发详情 -->
    <div class="forwards-section">
      <h3 class="section-title">转发详情</h3>
      <div v-loading="loading" class="forwards-container">
        <div v-if="groupedForwards.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无转发配置"></el-empty>
        </div>

        <div v-for="group in groupedForwards" :key="group.tunnelName" class="forward-group">
          <div class="group-header">
            <h4>{{ group.tunnelName }}</h4>
            <el-tag type="info" size="mini">{{ group.forwards.length }} 个转发</el-tag>
          </div>
          <div class="forward-list">
            <div v-for="forward in group.forwards" :key="forward.id" class="forward-item">
              <div class="forward-info">
                <div class="forward-name">{{ forward.name }}</div>
                <div class="forward-address">
                  <div class="address-source">{{ forward.inIp }}:{{ forward.inPort }}</div>
                  <div class="address-arrow">↓</div>
                  <div class="address-target">{{ forward.remoteAddr }}</div>
                </div>
              </div>
              <div class="forward-traffic">
                <div class="traffic-item">
                  <span class="traffic-label">入站:</span>
                  <span class="traffic-value in">{{ formatFlow(forward.inFlow || 0) }}</span>
                </div>
                <div class="traffic-item">
                  <span class="traffic-label">出站:</span>
                  <span class="traffic-value out">{{ formatFlow(forward.outFlow || 0) }}</span>
                </div>
                <div class="traffic-item total">
                  <span class="traffic-label">计费:</span>
                  <span class="traffic-value total">{{ formatFlow(calculateForwardBillingFlow(forward)) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getUserPackageInfo } from "@/api";

export default {
  name: "Index",
  data() {
    return {
      refreshLoading: false,
      loading: false,
      userInfo: {},
      userTunnels: [],
      forwardList: [],
    };
  },
  computed: {
    totalUsedForwards() {
      return this.forwardList.length;
    },

    groupedForwards() {
      const groups = {};
      this.forwardList.forEach(forward => {
        const tunnelName = forward.tunnelName || '未知隧道';
        if (!groups[tunnelName]) {
          groups[tunnelName] = {
            tunnelName,
            forwards: []
          };
        }
        groups[tunnelName].forwards.push(forward);
      });
      return Object.values(groups);
    }
  },
  mounted() {
    this.loadPackageData();
  },
  methods: {
    async loadPackageData() {
      this.loading = true;
      try {
        const res = await getUserPackageInfo();
        if (res.code === 0) {
          const data = res.data;
          this.userInfo = data.userInfo;
          this.userTunnels = data.tunnelPermissions || [];
          this.forwardList = data.forwards || [];
        } else {
          this.$message.error(res.msg || '获取套餐信息失败');
        }
      } catch (error) {
        console.error('获取套餐信息失败:', error);
        this.$message.error('获取套餐信息失败');
      } finally {
        this.loading = false;
      }
    },

    async refreshData() {
      this.refreshLoading = true;
      try {
        await this.loadPackageData();
        this.$message.success('数据刷新成功');
      } catch (error) {
        this.$message.error('数据刷新失败');
      } finally {
        this.refreshLoading = false;
      }
    },

    formatFlow(value, unit = 'bytes') {
      if (unit === 'gb') {
        return value + ' GB';
      } else {
        if (value === 0) return '0 B';
        if (value < 1024) return value + ' B';
        if (value < 1024 * 1024) return (value / 1024).toFixed(2) + ' KB';
        if (value < 1024 * 1024 * 1024) return (value / (1024 * 1024)).toFixed(2) + ' MB';
        return (value / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
      }
    },

    getExpStatus(expTime) {
      const now = new Date();
      const expDate = new Date(expTime);

      if (expDate < now) {
        return { type: 'danger', text: '已过期' };
      }

      const diffTime = expDate - now;
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

      if (diffDays <= 7) {
        return { type: 'danger', text: `${diffDays}天后过期` };
      } else if (diffDays <= 30) {
        return { type: 'warning', text: `${diffDays}天后过期` };
      } else {
        return { type: 'success', text: '有效期充足' };
      }
    },

    getTunnelExpStatus(expTime) {
      return this.getExpStatus(expTime);
    },

    calculateUsagePercentage(type) {
      if (type === 'flow') {
        const totalUsed = this.calculateUserTotalUsedFlow();
        const totalLimit = (this.userInfo.flow || 0) * 1024 * 1024 * 1024;
        return totalLimit > 0 ? Math.min((totalUsed / totalLimit) * 100, 100) : 0;
      } else if (type === 'forwards') {
        const totalUsed = this.totalUsedForwards;
        const totalLimit = this.userInfo.num || 0;
        return totalLimit > 0 ? Math.min((totalUsed / totalLimit) * 100, 100) : 0;
      }
      return 0;
    },

    getUsageStatus(type) {
      const percentage = this.calculateUsagePercentage(type);
      if (percentage >= 90) return 'exception';
      if (percentage >= 70) return 'warning';
      return 'success';
    },

    calculateTunnelFlowPercentage(tunnel) {
      const totalUsed = this.calculateTunnelUsedFlow(tunnel);
      const totalLimit = (tunnel.flow || 0) * 1024 * 1024 * 1024;
      return totalLimit > 0 ? Math.min((totalUsed / totalLimit) * 100, 100) : 0;
    },

    calculateTunnelForwardPercentage(tunnel) {
      const totalUsed = this.getTunnelUsedForwards(tunnel.tunnelId);
      const totalLimit = tunnel.num || 0;
      return totalLimit > 0 ? Math.min((totalUsed / totalLimit) * 100, 100) : 0;
    },

    getTunnelFlowStatus(tunnel) {
      const percentage = this.calculateTunnelFlowPercentage(tunnel);
      if (percentage >= 90) return 'exception';
      if (percentage >= 70) return 'warning';
      return 'success';
    },

    getTunnelForwardStatus(tunnel) {
      const percentage = this.calculateTunnelForwardPercentage(tunnel);
      if (percentage >= 90) return 'exception';
      if (percentage >= 70) return 'warning';
      return 'success';
    },

    getTunnelUsedForwards(tunnelId) {
      return this.forwardList.filter(forward => forward.tunnelId === tunnelId).length;
    },

         /**
      * 计算隧道已用流量
      * 根据隧道的流量计算类型（tunnelFlow）决定单向还是双向计算
      */
     calculateTunnelUsedFlow(tunnel) {
       if (!tunnel) return 0;
       
       const inFlow = tunnel.inFlow || 0;   // 下载流量
       const outFlow = tunnel.outFlow || 0; // 上传流量
       
       // tunnelFlow: 1-单向计算（仅上传），2-双向计算（上传+下载）
       if (tunnel.tunnelFlow === 1) {
         // 单向计算：只计算上传流量（outFlow）
         return outFlow;
       } else {
         // 双向计算或默认：计算上传和下载的总流量
         return inFlow + outFlow;
       }
     },

         /**
      * 计算用户总已用流量
      * 直接使用后端计算好的用户流量统计
      */
         calculateUserTotalUsedFlow() {
      // 后端已经根据隧道类型计算了实际流量，直接使用
      return (this.userInfo.inFlow || 0) + (this.userInfo.outFlow || 0);
    },

    // 格式化重置时间显示
    formatResetTime(resetDay) {
      if (!resetDay) return '未设置';
      
      // 获取当前日期
      const now = new Date();
      const currentDay = now.getDate();
      
      // 计算下次重置还有多少天
      let daysUntilReset;
      if (resetDay > currentDay) {
        daysUntilReset = resetDay - currentDay;
      } else if (resetDay < currentDay) {
        // 下个月的重置日
        const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, resetDay);
        const diffTime = nextMonth - now;
        daysUntilReset = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      } else {
        // 今天就是重置日
        daysUntilReset = 0;
      }
      
      if (daysUntilReset === 0) {
        return `今天重置（${resetDay}号）`;
      } else if (daysUntilReset === 1) {
        return `明天重置（${resetDay}号）`;
      } else {
        return `${daysUntilReset}天后重置（${resetDay}号）`;
      }
    },

           /**
       * 计算转发的计费流量
       * 根据对应隧道的流量计算类型决定单向还是双向计算
       */
      calculateForwardBillingFlow(forward) {
        if (!forward) return 0;
        
        // 查找对应的隧道
        const tunnel = this.userTunnels.find(t => t.tunnelId === forward.tunnelId);
        if (!tunnel) {
          // 如果找不到隧道信息，默认使用双向计算
          return (forward.inFlow || 0) + (forward.outFlow || 0);
        }
        
        const inFlow = forward.inFlow || 0;   // 下载流量
        const outFlow = forward.outFlow || 0; // 上传流量
        
        // 根据隧道的流量计算类型
        if (tunnel.tunnelFlow === 1) {
          // 单向计算：只计算上传流量（outFlow）
          return outFlow;
        } else {
          // 双向计算：计算上传和下载的总流量
          return inFlow + outFlow;
        }
      }
  },

  filters: {
    dateFormat(value) {
      if (!value) return '';
      const date = new Date(value);
      return date.toLocaleString();
    }
  }
};
</script>

<style scoped>
.package-container {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.overview-section {
  margin-bottom: 24px;
}

.overview-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.card-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.overview-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.stat-icon i {
  font-size: 24px;
  color: white;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.progress-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.progress-item {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 500;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.tunnels-section, .forwards-section {
  margin-bottom: 24px;
}

.tunnels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
}

.tunnel-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease;
}

.tunnel-card:hover {
  transform: translateY(-2px);
}

.tunnel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.tunnel-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
}

.tunnel-name i {
  margin-right: 8px;
  color: #409EFF;
}

.tunnel-status {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tunnel-stats {
  margin-bottom: 16px;
}

.tunnel-stat-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.tunnel-stat-item {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tunnel-stat-item:first-child {
  margin-right: 16px;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

.stat-value {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.stat-value.used {
  color: #F56C6C;
}

.tunnel-progress {
  margin-bottom: 16px;
}

.progress-mini {
  margin-bottom: 12px;
}

.progress-mini:last-child {
  margin-bottom: 0;
}

.progress-mini-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  font-size: 12px;
  color: #666;
}

.tunnel-footer {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
  margin-top: 16px;
}

.reset-time {
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
}

.reset-time i {
  margin-right: 4px;
}

.forwards-container {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.forward-group {
  margin-bottom: 24px;
}

.forward-group:last-child {
  margin-bottom: 0;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.group-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.forward-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}

.forward-item {
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: #ffffff;
  border-radius: 12px;
  border-left: 4px solid #409EFF;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.forward-item:hover {
  background: #f8fafe;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.2);
}

.forward-info {
  width: 100%;
  margin-bottom: 8px;
}

.forward-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  text-align: center;
}

.forward-address {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 8px;
}

.address-source,
.address-target {
  font-size: 15px;
  color: #333;
  font-family: 'Courier New', monospace;
  font-weight: 600;
  text-align: center;
  padding: 4px 12px;
  background: #ebeef5;
  border-radius: 4px;
  min-width: 120px;
}

.address-source {
  color: #52c41a;
}

.address-target {
  color: #1890ff;
}

.address-arrow {
  font-size: 18px;
  color: #409EFF;
  font-weight: bold;
  margin: 2px 0;
}

.forward-traffic {
  display: flex;
  justify-content: space-around;
  align-items: center;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 8px;
}

.traffic-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.traffic-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
  font-weight: 500;
}

.traffic-value {
  font-size: 14px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
}

.traffic-value.in {
  color: #52c41a;
  background: #f6ffed;
}

.traffic-value.out {
  color: #fa8c16;
  background: #fff7e6;
}

.traffic-value.total {
  color: #722ed1;
  background: #f9f0ff;
  font-weight: 600;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .package-container {
    padding: 10px;
  }

  .page-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .overview-stats {
    grid-template-columns: 1fr;
  }

  .progress-section {
    grid-template-columns: 1fr;
  }

  .tunnels-grid {
    grid-template-columns: 1fr;
  }

  .forward-list {
    grid-template-columns: 1fr;
  }

  .forward-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .forward-info {
    width: 100%;
    margin-bottom: 8px;
  }

  .forward-traffic {
    width: 100%;
  }
}
</style>