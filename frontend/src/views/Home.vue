<template>
  <el-container>
    <el-header style="height: 50px;">
      <span style="line-height: 50px">管理后台</span>
      <div class="header-avatar">
        <el-avatar style="margin-right: 10px" size="medium" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"></el-avatar>
        <el-dropdown>
						<span class="el-dropdown-link"  style="color: white">{{name}}</span>

          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item @click.native="showPasswordDialog">
              <i class="el-icon-key"></i>
              修改密码
            </el-dropdown-item>
            <el-dropdown-item @click.native="logout">
              <i class="el-icon-switch-button"></i>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </el-header>

    <el-container>
      <el-aside width="200px">
        <el-menu
            :default-active="path"
            class="el-menu-vertical-demo">

          <router-link to="/index">
            <el-menu-item index="/index" @click="save('/index')">
              <template slot="title">
                <i class="el-icon-box"></i>
                <span slot="title">首页</span>
              </template>
            </el-menu-item>
          </router-link>

          <router-link to="/limit" v-admin>
            <el-menu-item index="/limit" @click="save('/limit')">
              <template slot="title">
                <i class="el-icon-box"></i>
                <span slot="title">限速管理</span>
              </template>
            </el-menu-item>
          </router-link>

          <router-link to="/forward">
          <el-menu-item index="/forward" @click="save('/forward')">
            <template slot="title">
              <i class="el-icon-postcard"></i>
              <span slot="title">转发管理</span>
            </template>
          </el-menu-item>
        </router-link>

          <router-link to="/tunnel" v-admin>
            <el-menu-item index="/tunnel" @click="save('/tunnel')">
              <template slot="title">
                <i class="el-icon-postcard"></i>
                <span slot="title">隧道管理</span>
              </template>
            </el-menu-item>
          </router-link>

          <router-link to="/node" v-admin>
            <el-menu-item index="/node" @click="save('/node')">
              <template slot="title">
                <i class="el-icon-postcard"></i>
                <span slot="title">节点监控</span>
              </template>
            </el-menu-item>
          </router-link>

          <router-link to="/user" v-admin>
            <el-menu-item index="/user" @click="save('/user')">
              <template slot="title">
                <i class="el-icon-user"></i>
                <span slot="title">用户管理</span>
              </template>
            </el-menu-item>
          </router-link>

        </el-menu>
      </el-aside>
      <el-main>
        <div style="margin: 15px 15px;">
          <router-view/>

          <div style="background-color: white; height: 50px; position: absolute; bottom: 0;left: 199px;right: 0;line-height: 50px; padding-left: 30px;font-size: 12px;color:#58666e;">
            © 2023 Copyright.
          </div>
        </div>
      </el-main>
    </el-container>

    <!-- 修改密码弹窗 -->
    <el-dialog title="修改密码" :visible.sync="passwordDialogVisible" width="400px" :before-close="handlePasswordDialogClose">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordForm" label-width="100px">
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="passwordForm.currentPassword" type="password" placeholder="请输入当前密码" show-password clearable></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password clearable></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password clearable></el-input>
        </el-form-item>
      </el-form>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="handlePasswordDialogClose">取 消</el-button>
        <el-button type="primary" @click="submitPasswordChange" :loading="passwordLoading">确 定</el-button>
      </div>
    </el-dialog>

  </el-container>
</template>

<script>
import { updatePassword } from '@/api'

export default {
  name: "home",
  data(){
    return{
      path:'',
      name: '',
      // 修改密码相关数据
      passwordDialogVisible: false,
      passwordLoading: false,
      passwordForm: {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        currentPassword: [
          { required: true, message: '请输入当前密码', trigger: 'blur' },
          { min: 1, message: '密码不能为空', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '新密码长度不能少于6位', trigger: 'blur' },
          { max: 20, message: '新密码长度不能超过20位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { validator: this.validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.path = localStorage.getItem('e')
    this.name = localStorage.getItem("name")
  },
  methods:{
    save(e){
      localStorage.setItem('e',e)
      this.path = e
    },
    logout(){
      localStorage.clear()
      this.$router.push("/login")
    },
    // 显示修改密码弹窗
    showPasswordDialog() {
      this.passwordDialogVisible = true
    },
    // 关闭修改密码弹窗
    handlePasswordDialogClose() {
      this.passwordDialogVisible = false
      this.resetPasswordForm()
    },
    // 重置修改密码表单
    resetPasswordForm() {
      this.passwordForm = {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
      if (this.$refs.passwordForm) {
        this.$refs.passwordForm.clearValidate()
      }
    },
    // 确认密码验证器
    validateConfirmPassword(rule, value, callback) {
      if (value === '') {
        callback(new Error('请再次输入密码'))
      } else if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    },
    // 提交密码修改
    async submitPasswordChange() {
      try {
        // 表单验证
        await this.$refs.passwordForm.validate()
        
        this.passwordLoading = true
        
        // 调用修改密码接口
        const response = await updatePassword(this.passwordForm)
        
        if (response.code === 0) {
          this.$message.success(response.msg || '密码修改成功')
          this.handlePasswordDialogClose()
          
          // 提示用户重新登录
          this.$confirm('密码修改成功，请重新登录', '提示', {
            confirmButtonText: '确定',
            type: 'success',
            showCancelButton: false
          }).then(() => {
            this.logout()
          }).catch(() => {
            this.logout()
          })
        } else {
          this.$message.error(response.msg || '密码修改失败')
        }
      } catch (error) {
        if (error !== 'validate') {
          this.$message.error('修改密码时发生错误')
          console.error('修改密码错误:', error)
        }
      } finally {
        this.passwordLoading = false
      }
    }
  }
}
</script>

<style scoped>
.el-container {
  padding: 0;
  margin: 0;
  height: 100%;
}

.header-avatar {
  float: right;
  display: flex;
  justify-content: space-around;
  align-items: center;
}

.el-dropdown-link {
  cursor: pointer;
}

.el-header {
  background-color: #2F54EB;
  color: white;
  line-height: 50px;
}

.el-aside {
  color: white;
  line-height: 200px;
}

.el-main {
  padding: 0;
  height: calc(100vh - 100px);
}

a {
  text-decoration: none;
}
.el-menu-vertical-demo {
  height: 100%;
}

/* 修改密码弹窗样式 */
.dialog-footer {
  text-align: right;
}

.el-dialog__body {
  padding: 20px;
}

.el-form-item {
  margin-bottom: 22px;
}
</style>