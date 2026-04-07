
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

### Enterprise-Grade AI Assistant Platform

*An out-of-the-box full-stack AI platform supporting multi-agent collaboration, Supervisor mode orchestration, and multiple decision models, with advanced RAG technology and visual workflow orchestration capabilities*

**[中文](README.md)** | **[📖 Documentation](https://doc.pandarobot.chat)** |
**[🚀 Live Demo](https://web.pandarobot.chat)** | **[🐛 Report Issues](https://github.com/ageerle/ruoyi-ai/issues)** | **[💡 Feature Requests](https://github.com/ageerle/ruoyi-ai/issues)**

</div>




## ✨ Core Features

| Module | Current Capabilities | Extension Direction |
|:---:|---|---|
| **Model Management** | Multi-model integration (OpenAI/DeepSeek/Tongyi/Zhipu), multi-modal understanding, Coze/DIFY/FastGPT platform integration | Auto mode, fault tolerance |
| **Knowledge Base** | Local RAG + Vector DB (Milvus/Weaviate) + Knowledge Graph + Document parsing + Reranking | Audio/video parsing, knowledge source |
| **Tool Management** | MCP protocol integration, Skills capability + Extensible tool ecosystem | Tool plugin marketplace, toolAgent auto-loading |
| **Workflow Orchestration** | Visual workflow designer, drag-and-drop node orchestration, SSE streaming execution, currently supports model (with RAG) calls, email sending, manual review nodes | More node types |
| **Multi-Agent** | Agent framework based on Langchain4j, Supervisor mode orchestration, supports multiple decision models | Configurable agents |
| **AI Coding** | Intelligent code analysis, project scaffolding generation, Copilot assistant | Code generation optimization |

## 🚀 Quick Start

### Live Demo

|   Platform   | URL | Account |
|:------:|---|---|
|  User Frontend   | [web.pandarobot.chat](https://web.pandarobot.chat) | admin / admin123 |
| Admin Panel | [admin.pandarobot.chat](https://admin.pandarobot.chat) | admin / admin123 |

### Project Repositories

| Module     | GitHub Repository                                             | Gitee Repository                                             | GitCode Repository                                             |
|----------|-------------------------------------------------------|------------------------------------------------------|--------------------------------------------------------|
## 🛠️ Technical Architecture

### Core Framework
- **Backend**: Spring Boot 4.0 + Spring AI 2.0 + Langchain4j
- **Data Storage**: MySQL 8.0 + Redis + Vector Databases (Milvus/Weaviate)
- **Frontend**: Vue 3 + Vben Admin + element-plus-x
- **Security**: Sa-Token + JWT dual-layer security


## 📚 Documentation

Want to learn more about installation, deployment, configuration, and secondary development?

**👉 [Complete Documentation](https://doc.pandarobot.chat)**

## 🤝 Contributing

We warmly welcome community contributions! Whether you are a seasoned developer or just getting started, you can contribute to the project 💪

### How to Contribute

1. **Fork** the project to your account
2. **Create a branch** (`git checkout -b feature/new-feature-name`)
3. **Commit your changes** (`git commit -m 'Add new feature'`)
4. **Push to the branch** (`git push origin feature/new-feature-name`)
5. **Create a Pull Request**

> 💡 **Tip**: We recommend submitting PRs to GitHub, we will automatically sync to other code hosting platforms

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

Thanks to the following excellent open-source projects for their support:

## Outstanding Open-Source Projects and Community Recommendations
## 💬 Community Chat

<div align="center">

**[📱 Join Telegram Group](https://t.me/+LqooQAc5HxRmYmE1)**

</div>

---

<div align="center">

**[⭐ Star to Support](https://github.com/ageerle/ruoyi-ai)** • **[Fork to Contribute](https://github.com/ageerle/ruoyi-ai/fork)** • **[📚 中文](README.md)** • **[📖 Complete Documentation](https://doc.pandarobot.chat)**

*Built with ❤️, maintained by the RuoYi AI open-source community*

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
