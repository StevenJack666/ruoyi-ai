# RuoYi AI

<div align="center">

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]


<p align="center">
  <a href="https://trendshift.io/repositories/13209">
    <img src="https://trendshift.io/api/badge/repositories/13209" alt="GitHub Trending">
  </a>
</p>

<img src="docs/image/logo.png" alt="RuoYi AI Logo" width="120" height="120">

### 企业级AI助手平台

*开箱即用的全栈AI平台，支持多智能体协同、Supervisor模式编排、多种决策模型，提供先进的RAG技术和可视化流程编排能力*

**[English](README_EN.md)** | **[📖 使用文档](https://doc.pandarobot.chat)** |
**[🚀 在线体验](https://web.pandarobot.chat)** | **[🐛 问题反馈](https://github.com/ageerle/ruoyi-ai/issues)** | **[💡 功能建议](https://github.com/ageerle/ruoyi-ai/issues)**

</div>




## ✨ 核心亮点

| 模块 | 现有能力 | 扩展方向 |
|:---:|---|---|
| **模型管理** | 多模型接入(OpenAI/DeepSeek/通义/智谱)、多模态理解、Coze/DIFY/FastGPT平台集成 | 自动模式、容错机制             |
| **知识库**  | 本地RAG + 向量库(Milvus/Weaviate) + 知识图谱 + 文档解析 +重排序          | 音频视频解析、知识出处           |
| **工具管理** | Mcp协议集成、Skills能力 + 可扩展工具生态                               | 工具插件市场、toolAgent自动加载工具 |
| **流程编排** | 可视化工作流设计器、节点拖拽编排、SSE流式执行,目前已经支持模型调用,邮件发送,人工审核等节点  | 更多节点类型                |
| **多智能体** | 基于Langchain4j的Agent框架、Supervisor模式编排,支持多种决策模型            | 智能体可配置                |
| **AI编程** | 智能代码分析、项目脚手架生成、Copilot助手                                 | 代码生成优化                |

## 🚀 快速体验

### 在线演示

|   平台   | 地址 | 账号 |
|:------:|---|---|
|  用户端   | [web.pandarobot.chat](https://web.pandarobot.chat) | admin / admin123 |
| 管理后台 | [admin.pandarobot.chat](https://admin.pandarobot.chat) | admin / admin123 |

### 项目源码

| 项目模块     | GitHub 仓库                                             | Gitee 仓库                                             | GitCode 仓库                                             |
|----------|-------------------------------------------------------|------------------------------------------------------|--------------------------------------------------------|
### 合作项目
| 项目名称           | GitHub 仓库                                             | Gitee 仓库                                             
|----------------|-------------------------------------------------------|------------------------------------------------------|

## 🛠️ 技术架构

### 核心框架
- **后端架构**：Spring Boot 4.0 + Spring ai 2.0 + Langchain4j
- **数据存储**：MySQL 8.0 + Redis + 向量数据库（Milvus/Weaviate）
- **前端技术**：Vue 3 + Vben Admin + element-plus-x
- **安全认证**：Sa-Token + JWT 双重保障


## 📚 使用文档

想要深入了解安装部署、功能配置和二次开发？

**👉 [完整使用文档](https://doc.pandarobot.chat)**

## 🤝 参与贡献

我们热烈欢迎社区贡献！无论您是资深开发者还是初学者，都可以为项目贡献力量 💪

### 贡献方式

1. **Fork** 项目到您的账户
2. **创建分支** (`git checkout -b feature/新功能名称`)
3. **提交代码** (`git commit -m '添加某某功能'`)
4. **推送分支** (`git push origin feature/新功能名称`)
5. **发起 Pull Request**

> 💡 **小贴士**：建议将 PR 提交到 GitHub，我们会自动同步到其他代码托管平台

## 📄 开源协议

本项目采用 **MIT 开源协议**，详情请查看 [LICENSE](LICENSE) 文件。

## 🙏 特别鸣谢


## 优秀开源项目及社区推荐

<div align="center">

<table>
<tr>
<td align="center">
<img src="docs/image/wx.png" alt="微信二维码" width="200" height="200"><br>
<strong>扫码添加作者微信</strong><br>
<em>邀请进群学习</em>
</td>
<td align="center">
<img src="docs/image/qq.png" alt="QQ群二维码" width="200" height="200"><br>
<strong>QQ技术交流群</strong><br>
<em>技术讨论</em>
</td>

</tr>
</table>

</div>

---


## 📺 视频教程

<div align="center">

<table>
<tr>
<td align="center">
<img src="docs/image/dy.png" alt="微信二维码" width="200" height="200"><br>
<strong>打开抖音扫一扫</strong><br>
<em>获取免费视频教程</em>
</td>
<td align="center">
<img src="docs/image/bibi.png" alt="QQ群二维码" width="200" height="200"><br>
<strong>打开B站扫一扫</strong><br>
<em>获取免费视频教程</em>
</td>

</tr>
</table>

</div>

<div align="center">

**[⭐ 点个Star支持一下](https://github.com/ageerle/ruoyi-ai)** • **[ Fork 开始贡献](https://github.com/ageerle/ruoyi-ai/fork)** • **[📚 English](README_EN.md)** • **[📖 查看完整文档](https://doc.pandarobot.chat)**

*用 ❤️ 打造，由 RuoYi AI 开源社区维护*

</div>

<!-- Badge Links -->

[contributors-shield]: https://img.shields.io/github/contributors/ageerle/ruoyi-ai.svg?style=flat-square

[contributors-url]: https://github.com/ageerle/ruoyi-ai/graphs/contributors

[forks-shield]: https://img.shields.io/github/forks/ageerle/ruoyi-ai.svg?style=flat-square

[forks-url]: https://github.com/ageerle/ruoyi-ai/network/members

[stars-shield]: https://img.shields.io/github/stars/ageerle/ruoyi-ai.svg?style=flat-square

[stars-url]: https://github.com/ageerle/ruoyi-ai/stargazers

[issues-shield]: https://img.shields.io/github/issues/ageerle/ruoyi-ai.svg?style=flat-square

[issues-url]: https://github.com/ageerle/ruoyi-ai/issues

[license-shield]: https://img.shields.io/github/license/ageerle/ruoyi-ai.svg?style=flat-square

[license-url]: https://github.com/ageerle/ruoyi-ai/blob/main/LICENSE



```bash
docker run -d --name ruoyi-admin \
  -p 9090:8080 \
  -p 29090:28080 \
  -m 1g \                                    # 限制容器 1G 内存
  -e JAVA_OPTS="-Xms256m -Xmx512m" \          # JVM 堆 256-512M
  -v /Users/zhangmingming/data/coder/LLM/0526/ruoyi-ai/ruoyi-admin/src/main/resources/application-dev.yml:/config/application-dev.yml \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_CONFIG_ADDITIONAL_LOCATION=file:/config/ \
  ruoyi-admin:latest