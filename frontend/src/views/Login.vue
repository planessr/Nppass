<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
    
    <!-- 主要内容区域 -->
    <div class="login-wrapper">
      <!-- 登录表单 -->
      <div class="login-form-container">
        <div class="login-form">
          <div class="form-header">
            <h2 class="form-title">账户登录</h2>
            <p class="form-subtitle">请输入您的登录凭据</p>
          </div>
          
          <el-form 
            :model="loginForm" 
            :rules="rules" 
            ref="loginForm" 
            @keyup.enter.native="handleLogin"
            class="login-form-content"
          >
            <el-form-item prop="username">
              <div class="input-wrapper">
                <el-input 
                  v-model="loginForm.username" 
                  prefix-icon="el-icon-user" 
                  placeholder="请输入用户名" 
                  clearable
                  size="large"
                  class="custom-input"
                ></el-input>
              </div>
            </el-form-item>
            
            <el-form-item prop="password">
              <div class="input-wrapper">
                <el-input 
                  v-model="loginForm.password" 
                  prefix-icon="el-icon-lock" 
                  placeholder="请输入密码" 
                  show-password
                  size="large"
                  class="custom-input"
                ></el-input>
              </div>
            </el-form-item>
            
            <el-form-item class="login-btn-item">
              <el-button 
                type="primary" 
                :loading="loading" 
                @click="handleLogin" 
                class="login-btn"
                size="large"
              >
                <span v-if="!loading">立即登录</span>
                <span v-else>登录中...</span>
              </el-button>
            </el-form-item>
          </el-form>
          
          <div class="form-footer">
            <p class="copyright">© 2024 管理系统. All rights reserved.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {login} from "@/api";

export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度至少6位', trigger: 'blur' }
        ]
      },
      loading: false
    };
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;
          login(this.loginForm).then(res=>{
            this.loading = false
            if (res.code !== 0) return this.$message.error(res.msg)
            localStorage.setItem('token', res.data.token);
            localStorage.setItem("e", '/index')
            localStorage.setItem("role_id", res.data.role_id)
            localStorage.setItem("name", res.data.name)
            this.$message.success('登录成功');
            this.$router.push("/index");
          }).catch(() => {
            this.loading = false;
          })
        }
      });
    }
  }
};
</script>

<style scoped>
.login-container {
  height: 100vh;
  width: 100vw;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 1;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 6s ease-in-out infinite;
}

.circle-1 {
  width: 200px;
  height: 200px;
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.circle-2 {
  width: 150px;
  height: 150px;
  top: 60%;
  right: 15%;
  animation-delay: 2s;
}

.circle-3 {
  width: 100px;
  height: 100px;
  bottom: 20%;
  left: 20%;
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(180deg); }
}

/* 主要内容区域 */
.login-wrapper {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  max-width: 500px;
}

/* 登录表单 */
.login-form-container {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-form {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  padding: 40px;
  backdrop-filter: blur(10px);
}

.form-header {
  text-align: center;
  margin-bottom: 40px;
}

.form-title {
  font-size: 2rem;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 0.5rem;
}

.form-subtitle {
  color: #6b7280;
  font-size: 1rem;
  margin: 0;
}

.login-form-content {
  margin-bottom: 30px;
}

.input-wrapper {
  position: relative;
  margin-bottom: 20px;
}

/* 自定义输入框样式 */
.custom-input >>> .el-input__inner {
  height: 50px;
  border-radius: 12px;
  border: 2px solid #e5e7eb;
  font-size: 16px;
  padding-left: 45px;
  transition: all 0.3s ease;
  background: #f9fafb;
}

.custom-input >>> .el-input__inner:focus {
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.custom-input >>> .el-input__prefix {
  left: 15px;
  color: #6b7280;
}

.custom-input >>> .el-input__suffix {
  right: 15px;
}

/* 登录按钮 */
.login-btn-item {
  margin-bottom: 0;
}

.login-btn {
  width: 100%;
  height: 50px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.6);
}

.login-btn:active {
  transform: translateY(0);
}

/* 表单底部 */
.form-footer {
  text-align: center;
  margin-top: 30px;
}

.copyright {
  color: #9ca3af;
  font-size: 0.875rem;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-form-container {
    padding: 20px;
  }
  
  .login-form {
    padding: 30px 20px;
  }
  
  .form-title {
    font-size: 1.5rem;
  }
}

/* 表单验证错误样式 */
.login-form-content >>> .el-form-item__error {
  font-size: 14px;
  color: #ef4444;
  margin-top: 5px;
}

/* 加载状态 */
.login-btn.is-loading {
  pointer-events: none;
}
</style> 