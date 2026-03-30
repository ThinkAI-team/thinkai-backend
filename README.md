# 🚀 ThinkAI - Smart Education Platform

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](./CHANGELOG.md)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](./LICENSE)
[![Status](https://img.shields.io/badge/status-In%20Development-yellow.svg)]()

> Nền tảng E-learning thế hệ mới, tích hợp sức mạnh của **Google Gemini API** để mang đến trải nghiệm học tập thông minh.

---

## ✨ Features

### 🤖 AI Tutor

Gia sư ảo hỗ trợ giải đáp thắc mắc 24/7, trả lời dựa trên ngữ cảnh bài học.

### 📝 Smart Exam

Tự động tạo đề thi từ nội dung bài học và phân tích lỗ hổng kiến thức bằng AI.

### 📚 Learning Management

Quản lý khóa học, theo dõi tiến độ, và tổ chức nội dung học tập hiệu quả.

---

## 🛠 Tech Stack

| Layer        | Technology              | Description                           |
| ------------ | ----------------------- | ------------------------------------- |
| **Frontend** | Next.js 14 (App Router) | Tailwind CSS, Shadcn/UI, Lucide React |
| **Backend**  | Spring Boot 3           | Spring Security, JPA, Lombok          |
| **Database** | MySQL 8.0               | Relational data storage               |
| **AI Core**  | Google Gemini Pro       | NLP, Chatbot, Exam Generation         |
| **DevOps**   | Docker, GitLab CI       | Deployment on Render/Vercel           |

---

## 📁 Project Structure

```
thinkai/
├── applications/
│   ├── frontend/          # Next.js Frontend
│   └── backend/           # Spring Boot Backend
├── operations/
│   └── infrastructure/    # Docker Compose & K8s
└── knowledge/
    └── docs/              # Documentation (you are here)
        ├── README.md
        ├── SRS.md         # Software Requirements
        ├── Architecture.md
        ├── API_SPEC.md    # API Documentation
        └── DB_Scheme.md   # Database Schema
```

---

## 🚀 Quick Start

### Prerequisites

- Docker Desktop
- Node.js 18+
- JDK 17+
- MySQL 8.0 (or use Docker)

### Development Setup

```bash
# 1. Clone infrastructure repository
git clone <link-repo-infrastructure>
cd infrastructure

# 2. Start Database & Backend with Docker
docker-compose up -d mysql backend

# 3. Run Frontend (in frontend directory)
cd ../frontend
npm install
npm run dev
```

### Environment Variables

**Frontend (.env.local):**

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
```

**Backend (application.yml):**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/thinkai
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

gemini:
  api-key: ${GEMINI_API_KEY}
```

---

## 📖 Documentation

| Document                                 | Description                         |
| ---------------------------------------- | ----------------------------------- |
| [SRS.md](./SRS.md)                       | Software Requirements Specification |
| [Architecture.md](./Architecture.md)     | System Architecture & Data Flow     |
| [API_SPEC.md](./API_SPEC.md)             | RESTful API Documentation           |
| [DB_Scheme.md](./DB_Scheme.md)           | Database Schema Design              |
| [DESIGN_SYSTEM.md](./DESIGN_SYSTEM.md)   | UI/UX Design System & Guidelines    |

---

## 👥 Team

| Role             | Name              | Responsibilities     |
| ---------------- | ----------------- | -------------------- |
| **Project Lead** | Bình Minh         | Architecture, DevOps |
| **Backend**      | Nguyên, Pháp      | Spring Boot, API     |
| **Frontend**     | Minh, Trang, Khoa | Next.js, UI/UX       |

---

## 🗓 Roadmap

- [x] **Phase 1:** Core Features (Auth, Courses, Learning Room)
- [ ] **Phase 2:** AI Integration (Tutor, Smart Exam)
- [ ] **Phase 3:** Advanced Features (Analytics, Mobile App)

---

## 📝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.

---

<p align="center">
  Made with ❤️ by ThinkAI Team
</p>

<p align="center">
  Nguyễn Bình Minh
</p>
