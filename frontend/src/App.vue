<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App',
  created() {
    // 设置移动端适配
    this.setRem();
    window.addEventListener('resize', this.setRem);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.setRem);
  },
  methods: {
    setRem() {
      const width = document.documentElement.clientWidth || document.body.clientWidth;
      // 移动端适配，基准值为375px宽度设备
      if (width < 768) {
        const ratio = width / 375;
        document.documentElement.style.fontSize = 14 * ratio + 'px';
      } else {
        document.documentElement.style.fontSize = '16px';
      }
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  width: 100%;
  height: 100%;
  overflow-x: hidden;
  -webkit-text-size-adjust: 100%;
}

#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100%;
  color: #333;
}

/* 移动端样式适配 */
@media screen and (max-width: 768px) {
  .el-table {
    width: 100% !important;
    font-size: 0.8rem;
  }
  
  .el-button {
    padding: 8px 15px;
    font-size: 0.9rem;
  }
  
  .el-form-item__label {
    font-size: 0.9rem;
  }
  
  .el-dialog {
    width: 95% !important;
    margin-top: 15vh !important;
  }
  
  .el-card {
    padding: 5px !important;
  }
  
  .el-card__body {
    padding: 10px !important;
  }
  
  /* 确保文本可以换行，避免溢出 */
  .el-table .cell,
  .el-form-item__content,
  .el-input__inner {
    word-break: break-word !important;
    white-space: normal !important;
  }
  
  .el-message-box {
    width: 90% !important;
  }
}
</style>
