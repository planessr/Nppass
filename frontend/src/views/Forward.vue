<template>
  <div class="forward-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">转发管理</h1>
      <div class="header-actions">
        <!-- 搜索框 -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索转发名称或远程地址"
          prefix-icon="el-icon-search"
          @keyup.enter.native="handleSearch"
          clearable
          style="width: 250px; margin-right: 15px;"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
        </el-input>
        
        <el-button 
          type="primary" 
          icon="el-icon-plus" 
          @click="handleAdd"
          class="add-btn"
        >
          新增转发
        </el-button>
      </div>
    </div>

    <!-- 转发列表 -->
    <div class="table-container">
      <div v-loading="loading" class="cards-container">
        <div v-if="forwardList.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无转发配置"></el-empty>
        </div>
        
        <div 
          v-for="forward in forwardList" 
          :key="forward.id" 
          class="forward-card"
        >
          <div class="card-header">
            <div class="card-title">
              <i class="el-icon-connection"></i>
              {{ forward.name }}
            </div>
            <div class="card-actions">
              <!-- 服务开关 -->
              <el-switch
                v-model="forward.serviceRunning"
                :loading="forward.switchLoading"
                :disabled="forward.status !== 1 && forward.status !== 0"
                active-color="#13ce66"
                inactive-color="#ff4949"
                @change="handleServiceToggle(forward)"
                style="margin-right: 10px;"
              ></el-switch>
              
              <el-button 
                size="mini" 
                type="primary" 
                icon="el-icon-edit"
                @click="handleEdit(forward)"
                circle
              ></el-button>
              <el-button 
                size="mini" 
                type="danger" 
                icon="el-icon-delete"
                @click="handleDelete(forward)"
                circle
              ></el-button>
            </div>
          </div>
          
          <div class="card-body">
            <div class="info-row">
              <div class="info-item">
                <span class="label">隧道名称:</span>
                <span class="value">{{ forward.tunnelName || '未知隧道' }}</span>
              </div>
              <div class="info-item">
                <span class="label">状态:</span>
                <el-tag 
                  :type="getStatusType(forward.status)"
                  size="small"
                >
                  {{ getStatusText(forward.status) }}
                </el-tag>
              </div>
            </div>
            
            <div class="info-row">
              <div class="info-item">
                <span class="label">入口地址:</span>
                <span class="value">{{ forward.inIp}}:{{ forward.inPort }}</span>
              </div>
              <div class="info-item">
                <span class="label">目标地址:</span>
                <span class="value">{{ forward.remoteAddr}}</span>
              </div>
            </div>
            
            <div class="info-row">
              <div class="info-item full-width">
                <span class="label">创建时间:</span>
                <span class="value">{{ forward.createdTime | dateFormat }}</span>
              </div>
              <div class="info-item full-width" v-admin>
                <span class="label">归属账号:</span>
                <span class="value">{{ forward.userName}}</span>
              </div>
            </div>
            
            <div class="info-row">
              <div class="info-item">
                <span class="label">流量统计:</span>
                <div class="flow-stats-mini">
                  <div class="flow-stat-item">
                    <span class="flow-stat-label">入站:</span>
                    <span class="flow-stat-value in">{{ formatFlow(forward.inFlow || 0) }}</span>
                  </div>
                  <div class="flow-stat-item">
                    <span class="flow-stat-label">出站:</span>
                    <span class="flow-stat-value out">{{ formatFlow(forward.outFlow || 0) }}</span>
                  </div>
                  <div class="flow-stat-item">
                    <span class="flow-stat-label">总计:</span>
                    <span class="flow-stat-value total">{{ formatFlow((forward.inFlow || 0) + (forward.outFlow || 0)) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑转发对话框 -->
    <el-dialog 
      :title="dialogTitle" 
      :visible.sync="dialogVisible" 
      width="600px"
      @close="resetForm"
    >
      <el-form 
        :model="forwardForm" 
        :rules="rules" 
        ref="forwardForm" 
        label-width="100px"
      >
        <el-form-item label="转发名称" prop="name">
          <el-input 
            v-model="forwardForm.name" 
            placeholder="请输入转发名称"
            clearable
          ></el-input>
        </el-form-item>
        
        <el-form-item label="选择隧道" prop="tunnelId">
          <el-select 
            v-model="forwardForm.tunnelId" 
            placeholder="请选择关联的隧道"
            style="width: 100%;"
            @change="handleTunnelChange"
          >
            <el-option
              v-for="tunnel in tunnelList"
              :key="tunnel.id || tunnel.tunnelId"
              :label="getTunnelDisplayName(tunnel)"
              :value="tunnel.id || tunnel.tunnelId"
            ></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="远程地址" prop="remoteAddr">
          <el-input 
            v-model="forwardForm.remoteAddr" 
            placeholder="例如: 192.168.1.100:8080"
            clearable
          >
            <template slot="prepend">目标</template>
          </el-input>
          <div class="form-hint">
            格式: IP:端口 或 域名:端口
          </div>
        </el-form-item>
        

      </el-form>
      
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button 
          type="primary" 
          @click="handleSubmit"
          :loading="submitLoading"
        >
          {{ submitLoading ? '提交中...' : '确 定' }}
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { 
  createForward, 
  getForwardList, 
  updateForward, 
  deleteForward, 
  userTunnel, 
  getTunnelList,
  pauseForwardService,
  resumeForwardService
} from "@/api";

export default {
  name: "Forward",
  data() {
    return {
      loading: false,
      submitLoading: false,
      searchKeyword: '',
      forwardList: [],
      tunnelList: [],
      selectedTunnel: null,
      
      // 对话框状态
      dialogVisible: false,
      dialogTitle: '',
      isEdit: false,
      
      // 表单数据
      forwardForm: {
        id: null,
        userId: null,
        name: '',
        tunnelId: null,
        remoteAddr: ''
      },
      
      // 表单验证规则
      rules: {
        name: [
          { required: true, message: '请输入转发名称', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        tunnelId: [
          { required: true, message: '请选择关联隧道', trigger: 'change' }
        ],
        remoteAddr: [
          { required: true, message: '请输入远程地址', trigger: 'blur' },
          { 
            pattern: /^[\w\.-]+:\d+$/, 
            message: '请输入正确的地址格式，如: 192.168.1.100:8080', 
            trigger: 'blur' 
          }
        ]
      }
    }
  },
  
  created() {
    this.loadForwardList();
  },
  
  methods: {
    // 加载转发列表
    async loadForwardList() {
      try {
        this.loading = true;
        const res = await getForwardList();
        if (res.code === 0) {
          // 直接使用后端返回的状态数据
          this.forwardList = res.data.map(forward => ({
            ...forward,
            serviceRunning: forward.status === 1, // 根据 status 初始化开关状态
            switchLoading: false // 开关加载状态
          }));
        } else {
          this.$message.error(res.msg || '获取转发列表失败');
        }
      } catch (error) {
        console.error('加载转发列表失败:', error);
        this.$message.error('加载转发列表失败');
      } finally {
        this.loading = false;
      }
    },
    
    // 加载隧道列表
    async loadTunnelList(callback, forceReload = false) {
      try {
        // 如果已经有隧道数据且不强制重新加载，直接返回
        if (this.tunnelList.length > 0 && !forceReload) {
          if (callback && typeof callback === 'function') {
            callback();
          }
          return Promise.resolve();
        }

        // 先尝试获取用户可用的隧道权限列表
        let res = await userTunnel();
        if (res.code === 0 && res.data && res.data.length > 0) {
          // 如果是普通用户，使用用户隧道权限列表
          this.tunnelList = res.data;
          // 如果有回调函数，在隧道列表加载完成后执行
          if (callback && typeof callback === 'function') {
            callback();
          }
          return Promise.resolve();
        } else {
          throw new Error(res.msg || '没有可用的隧道');
        }
      } catch (error) {
        console.error('加载隧道列表失败:', error);
        this.$message.error('加载隧道列表失败');
        return Promise.reject(error);
      }
    },
    
    // 搜索功能
    handleSearch() {
      if (!this.searchKeyword.trim()) {
        this.loadForwardList();
        return;
      }
      
      // 实现本地搜索过滤
      this.loading = true;
      setTimeout(() => {
        const filtered = this.forwardList.filter(forward => 
          forward.name.includes(this.searchKeyword) || 
          forward.remoteAddr.includes(this.searchKeyword)
        );
        this.forwardList = filtered;
        this.loading = false;
      }, 300);
    },
    
    // 新增转发
    async handleAdd() {
      this.isEdit = false;
      this.dialogTitle = '新增转发';
      
      try {
        // 只在没有隧道数据时才加载
        await this.loadTunnelList();
        this.dialogVisible = true;
      } catch (error) {
        this.$message.error('加载隧道列表失败，无法新增');
      }
    },
    
    // 编辑转发
    async handleEdit(row) {
      this.isEdit = true;
      this.dialogTitle = '编辑转发';
      
      try {
        // 只在没有隧道数据时才加载
        await this.loadTunnelList();
        
        // 隧道列表加载完成后，设置表单数据并弹出对话框
      this.forwardForm = { 
        ...row,
        userId: row.userId // 确保userId被正确设置
      };
      this.handleTunnelChange(row.tunnelId);
      this.dialogVisible = true;
      } catch (error) {
        this.$message.error('加载隧道列表失败，无法编辑');
      }
    },
    
    // 删除转发
    async handleDelete(row) {
      try {
        await this.$confirm(`确定要删除转发 "${row.name}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        });
        
        const res = await deleteForward(row.id);
        if (res.code === 0) {
          this.$message.success('删除成功');
          this.loadForwardList();
        } else {
          this.$message.error(res.msg || '删除失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除转发失败:', error);
          this.$message.error('删除失败');
        }
      }
    },
    
    // 隧道选择变化处理
    handleTunnelChange(tunnelId) {
      this.selectedTunnel = this.tunnelList.find(tunnel => 
        tunnel.id === tunnelId || tunnel.tunnelId === tunnelId
      ) || null;
    },
    
    // 提交表单
    async handleSubmit() {
      try {
        await this.$refs.forwardForm.validate();
        
        this.submitLoading = true;
        let res;
        
        if (this.isEdit) {
          // 更新时确保包含必要字段
          const updateData = {
            id: this.forwardForm.id,
            userId: this.forwardForm.userId,
            name: this.forwardForm.name,
            tunnelId: this.forwardForm.tunnelId,
            remoteAddr: this.forwardForm.remoteAddr
          };
          res = await updateForward(updateData);
        } else {
          // 创建时不需要id和userId（后端会自动设置）
          const createData = {
            name: this.forwardForm.name,
            tunnelId: this.forwardForm.tunnelId,
            remoteAddr: this.forwardForm.remoteAddr
          };
          res = await createForward(createData);
        }
        
        if (res.code === 0) {
          this.$message.success(this.isEdit ? '修改成功' : '创建成功');
          this.dialogVisible = false;
          await this.loadForwardList();
        } else {
          this.$message.error(res.msg || '操作失败');
        }
      } catch (error) {
        if (error !== false) { // 表单验证失败时不显示错误消息
          console.error('提交失败:', error);
          this.$message.error('操作失败');
        }
      } finally {
        this.submitLoading = false;
      }
    },
    
    // 重置表单
    resetForm() {
      this.forwardForm = {
        id: null,
        userId: null,
        name: '',
        tunnelId: null,
        remoteAddr: ''
      };
      this.selectedTunnel = null;
      this.$nextTick(() => {
        if (this.$refs.forwardForm) {
          this.$refs.forwardForm.clearValidate();
        }
      });
    },
    
    // 获取隧道显示名称
    getTunnelDisplayName(tunnel) {
      if (!tunnel) return '未知隧道';
      
      // 处理用户隧道权限列表的数据结构
      if (tunnel.tunnelId) {
        const tunnelInfo = this.tunnelList.find(t => t.id === tunnel.tunnelId);
        if (tunnelInfo && tunnelInfo.ip && tunnelInfo.port) {
          return `${tunnelInfo.name || tunnel.tunnelId} (${tunnelInfo.ip}:${tunnelInfo.port})`;
        }
        return `隧道ID: ${tunnel.tunnelId}`;
      }
      
      // 处理直接隧道数据结构
      if (tunnel.name) {
        if (tunnel.ip && tunnel.port) {
          return `${tunnel.name} (${tunnel.ip}:${tunnel.port})`;
        }
        return tunnel.name;
      }
      
      return `隧道ID: ${tunnel.id || 'unknown'}`;
    },
    
    formatFlow(value) {
      if (value === 0) return '0';
      if (value < 1024) return value + ' B';
      if (value < 1024 * 1024) return (value / 1024).toFixed(2) + ' KB';
      if (value < 1024 * 1024 * 1024) return (value / (1024 * 1024)).toFixed(2) + ' MB';
      return (value / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
    },
    
    // 获取状态类型
    getStatusType(status) {
      switch (parseInt(status)) {
        case 1:
          return 'success'; // 绿色 - 正常
        case 0:
          return 'danger';  // 红色 - 停用
        case -1:
          return 'danger';  // 红色 - 停用
        default:
          return 'info';    // 蓝色 - 未知状态
      }
    },
    
    // 获取状态文本
    getStatusText(status) {
      switch (parseInt(status)) {
        case 1:
          return '正常';
        case 0:
          return '停用';
        case -1:
          return '异常';

        default:
          return '未知';
      }
    },
    



     
     // 处理服务开关
      async handleServiceToggle(forward) {
        // 检查转发状态，只有状态为1（正常）或0（暂停）时才能操作
        if (forward.status !== 1 && forward.status !== 0) {
          this.$message.warning('转发状态异常，无法操作');
          // 恢复开关状态
          this.$set(forward, 'serviceRunning', !forward.serviceRunning);
          return;
        }
        
        const targetState = forward.serviceRunning;
       
       try {
         // 设置加载状态
         this.$set(forward, 'switchLoading', true);
         
         let res;
         if (targetState) {
           // 启动服务
           res = await resumeForwardService(forward.id);
         } else {
           // 暂停服务
           res = await pauseForwardService(forward.id);
         }
         
         if (res.code === 0) {
           this.$message.success(targetState ? '服务已启动' : '服务已暂停');
           // 更新转发状态
           forward.status = targetState ? 1 : 0;
         } else {
           // 操作失败，恢复开关状态
           this.$set(forward, 'serviceRunning', !targetState);
           this.$message.error(res.msg || '操作失败');
         }
       } catch (error) {
         console.error('服务开关操作失败:', error);
         // 操作失败，恢复开关状态
         this.$set(forward, 'serviceRunning', !targetState);
         this.$message.error('网络错误，操作失败');
       } finally {
         // 取消加载状态
         this.$set(forward, 'switchLoading', false);
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
}
</script>

<style scoped>
.forward-container {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.page-title {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
}

.add-btn {
  border-radius: 6px;
  font-weight: 500;
}

.table-container {
  padding: 20px;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.tunnel-info {
  margin-top: 10px;
}

.dialog-footer {
  text-align: right;
}

/* Element UI 样式调整 */
.el-table {
  border-radius: 8px;
}

.el-dialog {
  border-radius: 12px;
}

.el-dialog__title {
  font-weight: 600;
}

.el-button--mini {
  border-radius: 4px;
}

.el-tag {
  border-radius: 4px;
}

.el-input-group__prepend {
  background-color: #f5f7fa;
  border-color: #dcdfe6;
  color: #606266;
}

.cards-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  padding: 10px 0;
}

.forward-card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  padding: 24px;
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
}

.forward-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
}

.card-title i {
  margin-right: 8px;
  color: #409EFF;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-body {
  margin-top: 16px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-item {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.info-item.full-width {
  flex: 1;
}

.info-item:not(:last-child) {
  margin-right: 20px;
}

.label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  font-weight: 500;
}

.value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.flow-stats-mini {
  display: flex;
  gap: 8px;
}

.flow-stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.flow-stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  font-weight: 500;
}

.flow-stat-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.flow-stat-value.in {
  color: #67C23A;
}

.flow-stat-value.out {
  color: #E6A23C;
}

.flow-stat-value.total {
  color: #F56C6C;
}

/* 服务开关样式优化 */
.el-switch {
  margin-right: 8px;
}

.el-switch__label {
  color: #606266;
  font-weight: 500;
}

.el-switch__label.is-active {
  color: #13ce66;
}

.card-actions .el-switch {
  transform: scale(0.9);
}

/* 禁用状态的开关样式 */
.el-switch.is-disabled {
  opacity: 0.6;
}

.el-switch.is-disabled .el-switch__label {
  color: #c0c4cc !important;
}


</style>