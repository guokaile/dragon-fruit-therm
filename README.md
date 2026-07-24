# 火龙果智能温控系统 Dragon Therm

> Dragon Fruit Smart Temperature Control System

基于 Java 25 + Maven 构建的智能温控系统登录模块，支持三种登录方式。

## ✨ 功能特性

- 🔐 **账号密码登录** — 传统手机号 + 密码方式
- 📱 **短信验证码登录** — 手机号获取验证码快速登录
- 💬 **微信登录** — 支持微信授权码 / OpenId 登录
- 🌐 **内置 HTTP 服务器** — 基于 JDK 内置 `com.sun.net.httpserver`，零外部依赖
- 🎨 **现代化 Web 前端** — 粒子背景 + 标签切换的登录界面

## 🚀 快速启动

### 启动 HTTP 服务器（默认 8080 端口）
```bash
mvn compile exec:java -Dexec.mainClass="cn.puge.dragonThermLogin"
```

### 指定端口
```bash
mvn compile exec:java -Dexec.mainClass="cn.puge.dragonThermLogin" -Dexec.args="9090"
```

启动后用浏览器访问：**http://localhost:8080**

### 控制台测试模式
```bash
mvn compile exec:java -Dexec.mainClass="cn.puge.dragonThermLogin" -Dexec.args="console"
```

## 📋 测试账号

| 手机号 | 密码 |
|---|---|
| 13546069966 | 123456 |

## 🛠 技术栈

- **后端**: Java 25, Maven
- **前端**: HTML5 + CSS3 + Vanilla JS
- **HTTP 服务**: JDK 内置 HttpServer（零外部依赖）
- **效果**: particles.js 粒子动画

## 📁 项目结构

```
DragonTherm/
├── src/main/java/cn/puge/
│   ├── dragonThermLogin.java      # 主程序入口
│   ├── entity/PugeUser.java       # 用户实体
│   ├── exception/BusinessException.java
│   ├── request/LoginRequest.java  # 登录请求 DTO
│   ├── response/LoginResponse.java # 登录响应 DTO
│   ├── service/
│   │   ├── HttpServer.java        # HTTP 服务器
│   │   ├── LoginService.java      # 登录业务逻辑
│   │   └── SmsCodeService.java    # 短信验证码服务
│   └── util/ValidationUtil.java   # 校验工具
├── web/
│   ├── index.html                 # 首页
│   ├── login.html                 # 登录页
│   ├── login.js                   # 登录逻辑
│   ├── style.css                  # 样式
│   ├── app.js                     # 应用逻辑
│   └── particles-config.js        # 粒子配置
└── pom.xml                        # Maven 配置
```

## 📄 License

MIT

---

© 2026 火龙果智能温控系统
