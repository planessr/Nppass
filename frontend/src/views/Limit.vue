<template>
  <div class="speed-limit-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="el-icon-odometer"></i>
        限速管理
      </h1>
      <div class="header-actions">
        <!-- 搜索框 -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索规则名称"
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
          新增限速规则
        </el-button>
        
        <el-button 
          type="success" 
          icon="el-icon-refresh" 
          @click="loadSpeedLimits"
          :loading="loading"
        >
          刷新
        </el-button>
      </div>
    </div>

    <!-- 限速规则列表 -->
    <div class="table-container">
      <el-table 
        :data="speedLimitList" 
        v-loading="loading"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="name" label="规则名称" show-overflow-tooltip></el-table-column>
        <el-table-column prop="speed" label="速度限制" >
          <template slot-scope="scope">
            <el-tag type="info">{{ scope.row.speed }} Mbps</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '运行' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tunnelName" label="绑定隧道" show-overflow-tooltip>
          <template slot-scope="scope">
            <el-tag type="success" v-if="scope.row.tunnelName">{{ scope.row.tunnelName }}</el-tag>
            <span v-else class="text-muted">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" >
          <template slot-scope="scope">
            {{ scope.row.createdTime | dateFormat }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedTime" label="更新时间" >
          <template slot-scope="scope">
            {{ scope.row.updatedTime | dateFormat }}
          </template>
        </el-table-column>
        <el-table-column label="操作"  fixed="right">
          <template slot-scope="scope">
            <el-button 
              size="mini" 
              type="primary" 
              icon="el-icon-edit"
              @click="handleEdit(scope.row)"
            >
            </el-button>
            <el-button 
              size="mini" 
              type="danger" 
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
            >
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 空状态 -->
      <div v-if="!loading && speedLimitList.length === 0" class="empty-state">
        <el-empty description="暂无限速规则">
          <el-button type="primary" @click="handleAdd">创建第一个限速规则</el-button>
        </el-empty>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible" 
      width="500px"
      @close="resetForm"
    >
      <el-form 
        :model="speedLimitForm" 
        :rules="rules" 
        ref="speedLimitForm"
        label-width="100px"
      >
        <el-form-item label="规则名称" prop="name">
          <el-input 
            v-model="speedLimitForm.name" 
            placeholder="请输入限速规则名称"
            clearable
          ></el-input>
        </el-form-item>
        
        <el-form-item label="速度限制" prop="speed">
          <el-input-number
            v-model="speedLimitForm.speed"
            :min="1"
            :max="1000000"
            placeholder="请输入速度限制"
            style="width: 100%;"
          ></el-input-number>
          <div class="form-hint">
            单位：Mbps
          </div>
        </el-form-item>
        

        
        <el-form-item label="绑定隧道" prop="tunnelId">
          <el-select 
            v-model="speedLimitForm.tunnelId" 
            placeholder="请选择要绑定的隧道"
            style="width: 100%;"
            filterable
            @change="handleTunnelChange"
          >
            <el-option
              v-for="tunnel in tunnelList"
              :key="tunnel.id"
              :label="tunnel.name"
              :value="tunnel.id"
            ></el-option>
          </el-select>
          <div class="form-hint">
            选择要应用限速规则的隧道
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
import { createSpeedLimit, getSpeedLimitList, updateSpeedLimit, deleteSpeedLimit, getTunnelList } from "@/api";

export default {
  name: "Limit",
  data() {
    return {
      speedLimitList: [],
      tunnelList: [],
      loading: false,
      dialogVisible: false,
      dialogTitle: '',
      isEdit: false,
      submitLoading: false,
      searchKeyword: '',
      
      // 表单数据
      speedLimitForm: {
        id: null,
        name: '',
        speed: 100,
        tunnelId: null,
        tunnelName: '',
        status: 1
      },
      
      // 表单验证规则
      rules: {
        name: [
          { required: true, message: '请输入规则名称', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        speed: [
          { required: true, message: '请输入速度限制', trigger: 'blur' },
          { type: 'number', min: 1, message: '速度限制必须大于0', trigger: 'blur' }
        ],
        tunnelId: [
          { required: true, message: '请选择要绑定的隧道', trigger: 'change' }
        ]
      }
    };
  },
  
  mounted() {
    this.loadSpeedLimits();
    this.loadTunnels();
  },
  
  methods: {
    // 加载限速规则列表
    async loadSpeedLimits() {
      try {
        this.loading = true;
        const res = await getSpeedLimitList();
        if (res.code === 0) {
          this.speedLimitList = res.data || [];
        } else {
          this.$message.error(res.msg || '获取限速规则列表失败');
        }
      } catch (error) {
        console.error('加载限速规则列表失败:', error);
        this.$message.error('加载限速规则列表失败');
      } finally {
        this.loading = false;
      }
    },

    // 加载隧道列表
    async loadTunnels() {
      try {
        const res = await getTunnelList();
        if (res.code === 0) {
          this.tunnelList = res.data || [];
        } else {
          console.error('获取隧道列表失败:', res.msg);
        }
      } catch (error) {
        console.error('加载隧道列表失败:', error);
      }
    },
    
    // 搜索功能
    handleSearch() {
      if (!this.searchKeyword.trim()) {
        this.loadSpeedLimits();
        return;
      }
      
      // 实现本地搜索过滤
      this.loading = true;
      setTimeout(() => {
        const filtered = this.speedLimitList.filter(item => 
          item.name.includes(this.searchKeyword)
        );
        this.speedLimitList = filtered;
        this.loading = false;
      }, 300);
    },
    
    // 处理隧道选择变化
    handleTunnelChange(tunnelId) {
      const selectedTunnel = this.tunnelList.find(tunnel => tunnel.id === tunnelId);
      if (selectedTunnel) {
        this.speedLimitForm.tunnelName = selectedTunnel.name;
      } else {
        this.speedLimitForm.tunnelName = '';
      }
    },

    // 新增限速规则
    handleAdd() {
      this.isEdit = false;
      this.dialogTitle = '新增限速规则';
      this.dialogVisible = true;
    },
    
    // 编辑限速规则
    handleEdit(row) {
      this.isEdit = true;
      this.dialogTitle = '编辑限速规则';
      this.speedLimitForm = { 
        id: row.id,
        name: row.name,
        speed: row.speed,
        tunnelId: row.tunnelId,
        tunnelName: row.tunnelName,
        status: row.status || 1
      };
      this.dialogVisible = true;
    },
    
    // 删除限速规则
    async handleDelete(row) {
      try {
        await this.$confirm(`确定要删除限速规则 "${row.name}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        });
        
        const res = await deleteSpeedLimit(row.id);
        if (res.code === 0) {
          this.$message.success('删除成功');
          this.loadSpeedLimits();
        } else {
          this.$message.error(res.msg || '删除失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除限速规则失败:', error);
          this.$message.error('删除失败');
        }
      }
    },
    
    // 提交表单
    async handleSubmit() {
      try {
        await this.$refs.speedLimitForm.validate();
        
        this.submitLoading = true;
        let res;
        
        if (this.isEdit) {
          res = await updateSpeedLimit(this.speedLimitForm);
        } else {
          const { id, ...createData } = this.speedLimitForm;
          res = await createSpeedLimit(createData);
        }
        
        if (res.code === 0) {
          this.$message.success(this.isEdit ? '修改成功' : '创建成功');
          this.dialogVisible = false;
          this.loadSpeedLimits();
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
      this.speedLimitForm = {
        id: null,
        name: '',
        speed: 100,
        tunnelId: null,
        tunnelName: '',
        status: 1
      };
      this.$nextTick(() => {
        if (this.$refs.speedLimitForm) {
          this.$refs.speedLimitForm.clearValidate();
        }
      });
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
.speed-limit-container {
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
  display: flex;
  align-items: center;
}

.page-title i {
  margin-right: 12px;
  font-size: 28px;
  color: #409EFF;
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
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  padding: 20px;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.dialog-footer {
  text-align: right;
}

.empty-state {
  text-align: center;
  padding: 40px;
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

.text-muted {
  color: #909399;
  font-style: italic;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .speed-limit-container {
    padding: 10px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }
  
  .header-actions {
    flex-direction: column;
    gap: 10px;
  }
  
  .page-title {
    text-align: center;
  }
}
</style> 