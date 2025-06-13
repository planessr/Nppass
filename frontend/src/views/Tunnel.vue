<template>
  <div class="tunnel-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="el-icon-connection"></i>
        隧道管理
      </h1>
      <div class="header-actions">
        <el-button 
          type="primary" 
          icon="el-icon-plus" 
          @click="handleAdd"
          class="add-btn"
        >
          新增隧道
        </el-button>
        
        <el-button 
          type="success" 
          icon="el-icon-refresh" 
          @click="loadTunnels"
          :loading="loading"
        >
          刷新
        </el-button>
      </div>
    </div>

    <!-- 隧道卡片展示 -->
    <div class="cards-container" v-loading="loading">
      <div class="cards-grid">
        <div 
          v-for="tunnel in tunnelList" 
          :key="tunnel.id" 
          class="tunnel-card"
          :class="{ 
            'disabled': tunnel.status === 0
          }"
        >
          <!-- 隧道头部 -->
          <div class="tunnel-header">
            <h3 class="tunnel-name">{{ tunnel.name }}</h3>
            <div class="tunnel-badges">
              <el-tag v-if="tunnel.type === 1" type="primary" size="mini">端口转发</el-tag>
              <el-tag v-else type="warning" size="mini">隧道转发</el-tag>
              <el-tag v-if="tunnel.flow === 1" type="success" size="mini">单向计算</el-tag>
              <el-tag v-else type="info" size="mini">双向计算</el-tag>
            </div>
          </div>

          <!-- 流程图 -->
          <div class="tunnel-flow">
            <!-- 客户端 -->
            <div class="flow-item client">
              <div class="flow-icon">
                <i class="el-icon-monitor"></i>
              </div>
              <div class="flow-label">客户端</div>
            </div>

            <!-- 箭头 -->
            <div class="flow-arrow">
              <i class="el-icon-right"></i>
            </div>

            <!-- 入口 -->
            <div class="flow-item entrance">
              <div class="flow-icon">
                <i class="el-icon-upload"></i>
              </div>
              <div class="flow-label">入口</div>
              <div class="flow-detail">
                <div class="detail-item">
                  <span class="label">IP:</span>
                  <span class="value">{{ tunnel.inIp }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">端口:</span>
                  <span class="value">{{ tunnel.inPortSta === tunnel.inPortEnd ? tunnel.inPortSta : `${tunnel.inPortSta}-${tunnel.inPortEnd}` }}</span>
                </div>
              </div>
            </div>

            <!-- 箭头 -->
            <div class="flow-arrow">
              <i class="el-icon-right"></i>
            </div>

            <!-- 出口 -->
            <div class="flow-item exit">
              <div class="flow-icon">
                <i class="el-icon-download"></i>
              </div>
              <div class="flow-label">出口</div>
              <div class="flow-detail">
                <div class="detail-item">
                  <span class="label">IP:</span>
                  <span class="value">{{ tunnel.outIp }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">端口:</span>
                  <span class="value">{{ tunnel.outIpSta === tunnel.outIpEnd ? tunnel.outIpSta : `${tunnel.outIpSta}-${tunnel.outIpEnd}` }}</span>
                </div>
              </div>
            </div>

            <!-- 箭头 -->
            <div class="flow-arrow">
              <i class="el-icon-right"></i>
            </div>

            <!-- 目标 -->
            <div class="flow-item target">
              <div class="flow-icon">
                <i class="el-icon-position"></i>
              </div>
              <div class="flow-label">目标</div>
              <div class="flow-detail">
                <div class="detail-item">
                  <span class="label">目标服务</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 创建时间 -->
          <div class="tunnel-meta">
            <div class="meta-item">
              <i class="el-icon-time"></i>
              <span>{{ tunnel.createdTime | dateFormat }}</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="tunnel-actions">
            <el-button 
              size="small" 
              type="danger" 
              icon="el-icon-delete"
              @click="handleDelete(tunnel)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && tunnelList.length === 0" class="empty-state">
        <i class="el-icon-connection"></i>
        <p>暂无隧道数据</p>
        <el-button type="primary" @click="handleAdd">创建第一个隧道</el-button>
      </div>
    </div>

    <!-- 新增/编辑隧道对话框 -->
    <el-dialog 
      :title="dialogTitle" 
      :visible.sync="dialogVisible" 
      width="600px"
      @close="resetForm"
    >
      <el-form 
        :model="tunnelForm" 
        :rules="rules" 
        ref="tunnelForm" 
        label-width="100px"
      >
        <el-form-item label="隧道名称" prop="name">
          <el-input 
            v-model="tunnelForm.name" 
            placeholder="请输入隧道名称"
            clearable
          ></el-input>
        </el-form-item>

        <el-form-item label="隧道类型" prop="type">
          <el-radio-group v-model="tunnelForm.type" @change="handleTypeChange">
            <el-radio :label="1">端口转发</el-radio>
            <el-radio :label="2">隧道转发</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="流量计算" prop="flow">
          <el-radio-group v-model="tunnelForm.flow">
            <el-radio :label="1">单向计算（仅上传）</el-radio>
            <el-radio :label="2">双向计算（上传+下载）</el-radio>
          </el-radio-group>
          <div class="form-hint">
            单向：仅计算上传流量；双向：计算上传和下载的总流量
          </div>
        </el-form-item>

        <el-divider content-position="left">入口配置</el-divider>

        <el-form-item label="入口节点" prop="inNodeId">
          <el-select 
            v-model="tunnelForm.inNodeId" 
            placeholder="请选择入口节点"
            style="width: 100%"
            @change="handleNodeChange"
          >
            <el-option 
              v-for="node in onlineNodes" 
              :key="node.id" 
              :label="`${node.name} (${node.status === 1 ? '在线' : '离线'})`" 
              :value="node.id"
              :disabled="false"
            >
              <span style="float: left">{{ node.name }}</span>
              <span style="float: right; font-size: 13px"
                    :style="{ color: node.status === 1 ? '#67c23a' : '#f56c6c' }">
                {{ node.status === 1 ? '在线' : '离线' }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="起始端口" prop="inPortSta">
              <el-input-number 
                v-model="tunnelForm.inPortSta" 
                :min="1" 
                :max="65535"
                placeholder="起始端口"
                style="width: 100%"
              ></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束端口" prop="inPortEnd">
              <el-input-number 
                v-model="tunnelForm.inPortEnd" 
                :min="1" 
                :max="65535"
                placeholder="结束端口"
                style="width: 100%"
              ></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 只有隧道转发(type=2)时才显示出口配置 -->
        <template v-if="tunnelForm.type === 2">
          <el-divider content-position="left">出口配置</el-divider>

          <el-form-item label="出口节点" prop="outNodeId">
            <el-select 
              v-model="tunnelForm.outNodeId" 
              placeholder="请选择出口节点"
              style="width: 100%"
              @change="handleNodeChange"
            >
              <el-option 
                v-for="node in onlineNodes" 
                :key="node.id" 
                :label="`${node.name} (${node.status === 1 ? '在线' : '离线'})`" 
                :value="node.id"
                :disabled="tunnelForm.inNodeId && node.id === tunnelForm.inNodeId"
              >
                <span style="float: left">{{ node.name }}</span>
                <span style="float: right; font-size: 13px"
                      :style="{ color: node.status === 1 ? '#67c23a' : '#f56c6c' }">
                  {{ node.status === 1 ? '在线' : '离线' }}
                  {{ tunnelForm.inNodeId && node.id === tunnelForm.inNodeId ? ' (已选为入口)' : '' }}
                </span>
              </el-option>
            </el-select>
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="起始端口" prop="outIpSta">
                <el-input-number 
                  v-model="tunnelForm.outIpSta" 
                  :min="10000"
                  :max="65535"
                  placeholder="起始端口"
                  style="width: 100%"
                ></el-input-number>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束端口" prop="outIpEnd">
                <el-input-number 
                  v-model="tunnelForm.outIpEnd" 
                  :min="10000"
                  :max="65535"
                  placeholder="结束端口"
                  style="width: 100%"
                ></el-input-number>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-alert
          v-if="tunnelForm.type === 1"
          title="端口转发模式：出口配置将自动使用入口配置"
          type="info"
          :closable="false"
          show-icon
          style="margin-top: 20px;">
        </el-alert>
      </el-form>
      
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button 
          type="primary" 
          @click="handleSubmit"
          :loading="submitLoading"
        >
          {{ submitLoading ? '创建中...' : '创 建' }}
        </el-button>
      </span>
    </el-dialog>
          </div>
</template>

<script>
import { createTunnel, deleteTunnel, getTunnelList, getNodeList } from "@/api";

export default {
  name: "Tunnel",
  data() {
    return {
      tunnelList: [],
      nodeList: [],
      loading: false,
      dialogVisible: false,
      dialogTitle: '新增隧道',
      submitLoading: false,
      tunnelForm: {
        id: null,
        name: '',
        inNodeId: null,
        inPortSta: 10000,
        inPortEnd: 65535,
        outNodeId: null,
        outIpSta: 10000,
        outIpEnd: 65535,
        type: 1,
        flow: 1,  // 默认单向计算
        status: 1  // 默认启用
      },
      rules: {
        name: [
          { required: true, message: '请输入隧道名称', trigger: 'blur' },
          { min: 2, message: '隧道名称长度至少2位', trigger: 'blur' },
          { max: 50, message: '隧道名称长度不能超过50位', trigger: 'blur' }
        ],
        inNodeId: [
          { required: true, message: '请选择入口节点', trigger: 'change' }
        ],
        inPortSta: [
          { required: true, message: '请输入起始端口', trigger: 'blur' }
        ],
        inPortEnd: [
          { required: true, message: '请输入结束端口', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '请选择隧道类型', trigger: 'change' }
        ],
        flow: [
          { required: true, message: '请选择流量计算类型', trigger: 'change' }
        ],
        // 隧道转发时的出口验证规则
        outNodeId: [
          { required: false, message: '请选择出口节点', trigger: 'change' }
        ],
        outIpSta: [
          { required: false, message: '请输入起始端口', trigger: 'blur' }
        ],
        outIpEnd: [
          { required: false, message: '请输入结束端口', trigger: 'blur' }
        ]
      }
    };
  },

  filters: {
    dateFormat(timestamp) {
      if (!timestamp) return '-';
      const date = new Date(timestamp);
      return date.toLocaleString('zh-CN');
    }
  },

  computed: {
    // 可用节点列表（显示所有节点，但在选择器中标识状态）
    onlineNodes() {
      return this.nodeList || [];
    }
  },

  mounted() {
    this.loadTunnels();
  },

  methods: {
    // 加载节点列表
    loadNodes() {
      getNodeList().then(res => {
        if (res.code === 0) {
          this.nodeList = res.data || [];
        } else {
          this.$message.error(res.msg || '加载节点列表失败');
        }
      }).catch(error => {
        this.$message.error('加载节点列表失败');
      });
    },

    // 节点选择改变时的处理
    handleNodeChange() {
      // 可以在这里添加一些节点选择后的逻辑
      // 例如：更新相关配置或验证
    },

    // 加载隧道列表
    loadTunnels() {
      this.loading = true;

      getTunnelList().then(res => {
        this.loading = false;
        if (res.code === 0) {
          this.tunnelList = res.data || [];
        } else {
          this.$message.error(res.msg || '加载隧道列表失败');
        }
      }).catch(error => {
        this.loading = false;
        this.$message.error('网络错误，请重试');
      });
    },

    // 新增隧道
    handleAdd() {
      this.dialogVisible = true;
      this.resetForm();
      this.loadNodes(); // 打开对话框时加载节点列表
    },

    // 编辑功能已移除 - 隧道创建后不能修改

    // 删除隧道
    handleDelete(tunnel) {
      this.$confirm(`确定要删除隧道 "${tunnel.name}" 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteTunnel(tunnel.id).then(res => {
          if (res.code === 0) {
            this.$message.success('删除成功');
            this.loadTunnels();
          } else {
            this.$message.error(res.msg || '删除失败');
          }
        });
      }).catch(() => {
        // 用户取消删除
      });
    },

    // 状态切换功能已移除 - 隧道创建后不能修改状态

    // 隧道类型改变时的处理
    handleTypeChange(value) {
      if (value === 2) {
        // 隧道转发时，出口字段变为必填
        this.rules.outNodeId[0].required = true;
        this.rules.outIpSta[0].required = true;
        this.rules.outIpEnd[0].required = true;
      } else {
        // 端口转发时，出口字段不必填
        this.rules.outNodeId[0].required = false;
        this.rules.outIpSta[0].required = false;
        this.rules.outIpEnd[0].required = false;
        
        // 清空出口字段
        this.tunnelForm.outNodeId = null;
        this.tunnelForm.outIpSta = null;
        this.tunnelForm.outIpEnd = null;
      }
    },

    // 提交表单
    handleSubmit() {
      this.$refs.tunnelForm.validate(valid => {
        if (valid) {
          // 端口范围验证
          if (this.tunnelForm.inPortSta > this.tunnelForm.inPortEnd) {
            this.$message.error('入口起始端口不能大于结束端口');
            return;
          }

          // 隧道转发时验证入口和出口节点不能相同
          if (this.tunnelForm.type === 2 && 
              this.tunnelForm.inNodeId === this.tunnelForm.outNodeId) {
            this.$message.error('隧道转发模式下，入口和出口不能是同一个节点');
            return;
          }

          if (this.tunnelForm.type === 2 && 
              this.tunnelForm.outIpSta && this.tunnelForm.outIpEnd &&
              this.tunnelForm.outIpSta > this.tunnelForm.outIpEnd) {
            this.$message.error('出口起始端口不能大于结束端口');
            return;
          }

          this.submitLoading = true;
          
          // 只支持创建隧道，不支持编辑
          const data = { ...this.tunnelForm };
          
          createTunnel(data).then(res => {
            this.submitLoading = false;
            if (res.code === 0) {
              this.$message.success('创建成功');
              this.dialogVisible = false;
              this.loadTunnels();
            } else {
              this.$message.error(res.msg || '创建失败');
            }
          }).catch(() => {
            this.submitLoading = false;
            this.$message.error('网络错误，请重试');
          });
        }
      });
    },

    // 重置表单
    resetForm() {
      this.tunnelForm = {
        id: null,
        name: '',
        inNodeId: null,
        inPortSta: 10000,
        inPortEnd: 65535,
        outNodeId: null,
        outIpSta: 10000,
        outIpEnd: 65535,
        type: 1,
        flow: 1,  // 默认单向计算
        status: 1  // 默认启用
      };
      if (this.$refs.tunnelForm) {
        this.$refs.tunnelForm.clearValidate();
      }
    }
  }
};
</script>

<style scoped>
.tunnel-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  background: white;
  padding: 20px 30px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  display: flex;
  align-items: center;
}

.page-title i {
  margin-right: 8px;
  color: #409eff;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 卡片容器 */
.cards-container {
  min-height: 400px;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(600px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
  align-items: start;
}

/* 隧道卡片 */
.tunnel-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  border-left: 4px solid #409eff;
  position: relative;
  height: auto;
  min-height: 320px;
  display: flex;
  flex-direction: column;
}

.tunnel-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.15);
}

/* 禁用状态样式 */
.tunnel-card.disabled {
  opacity: 0.6;
  background: #f5f7fa !important;
  border-left-color: #c0c4cc !important;
}

.tunnel-card.disabled .flow-icon {
  background: #c0c4cc !important;
}

.tunnel-card.disabled .tunnel-name {
  color: #909399 !important;
}

.tunnel-card.disabled .detail-item .value {
  color: #c0c4cc !important;
}

.tunnel-card.disabled .flow-label {
  color: #c0c4cc !important;
}

.tunnel-card.disabled::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(192, 196, 204, 0.1);
  pointer-events: none;
  border-radius: 12px;
}

/* 隧道头部 */
.tunnel-header {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tunnel-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  flex: 1;
}

.tunnel-badges {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 流程图 */
.tunnel-flow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  overflow-x: auto;
}

.flow-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 100px;
  flex-shrink: 0;
}

.flow-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  font-size: 18px;
  color: white;
}

.client .flow-icon {
  background: #909399;
}

.entrance .flow-icon {
  background: #409eff;
}

.exit .flow-icon {
  background: #f56c6c;
}

.target .flow-icon {
  background: #67c23a;
}

.flow-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

.flow-detail {
  text-align: center;
}

.detail-item {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.detail-item .label {
  font-weight: 500;
}

.detail-item .value {
  color: #303133;
  font-family: monospace;
}

.flow-arrow {
  color: #c0c4cc;
  font-size: 16px;
  margin: 0 10px;
}

/* 隧道元信息 */
.tunnel-meta {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  font-size: 12px;
  color: #909399;
}

.meta-item {
  display: flex;
  align-items: center;
  margin-right: 20px;
}

.meta-item i {
  margin-right: 5px;
}

/* 操作按钮 */
.tunnel-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 15px;
  margin-top: auto;
}

/* 表单提示 */
.form-hint {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
  line-height: 1.4;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-state i {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 16px;
  margin-bottom: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .cards-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
  
  .tunnel-flow {
    flex-direction: column;
    gap: 15px;
  }
  
  .flow-arrow {
    transform: rotate(90deg);
  }
  
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }
  
  .header-actions {
    justify-content: center;
  }
  
  .tunnel-card {
    padding: 15px;
  }
  
  .tunnel-name {
    margin-right: 80px;
  }
}
</style>