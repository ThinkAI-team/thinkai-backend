# ThinkAI Selenium Test Suite

## Overview
120 test cases Selenium Python cho ThinkAI Backend và Frontend.

## Structure

```
SeleniumTestScript/
├── config/
│   └── config.py           # Configuration (URL, timeouts, browser)
├── pages/
│   ├── base_page.py        # BasePage với WebDriverWait
│   ├── login_page.py      # Login page object
│   ├── register_page.py   # Register page object
│   ├── courses_page.py   # Courses catalog page
│   ├── course_detail_page.py
│   ├── dashboard_page.py # Student dashboard
│   ├── learning_room_page.py
│   ├── exam_pages.py     # Exams pages
│   ├── teacher_pages.py  # Teacher portal
│   ├── admin_pages.py    # Admin panel
│   └── ai_tutor_page.py # AI Tutor
├── flows/
│   ├── auth_flow.py      # Business flow: login/logout
│   ├── course_flow.py    # Course enrollment flow
│   └── learning_flow.py  # Learning flow
├── tests/
│   ├── test_auth.py      # TC-AUTH-001 to TC-AUTH-020 (20 tests)
│   ├── test_courses.py   # TC-COURSE-021 to TC-COURSE-045 (25 tests)
│   ├── test_learning.py  # TC-LEARN-046 to TC-LEARN-060 (15 tests)
│   ├── test_exams.py     # TC-EXAM-061 to TC-EXAM-080 (20 tests)
│   ├── test_ai_tutor.py # TC-AI-081 to TC-AI-095 (15 tests)
│   ├── test_teacher.py   # TC-TEACHER-096 to TC-TEACHER-110 (15 tests)
│   └── test_admin.py     # TC-ADMIN-111 to TC-ADMIN-120 (10 tests)
├── conftest.py           # Pytest fixtures
├── requirements.txt      # Dependencies
└── README.md
```

## Features
- BasePage với WebDriverWait (không dùng time.sleep)
- POM Pattern - mỗi page có locator và method riêng
- Business Flow layer - gom các actions thành flows
- Fixtures cho driver, wait, base_url, test_users
- Hỗ trợ headless mode
- Retry mechanism cho flaky elements
- Screenshot on failure support

## Setup

```bash
cd SeleniumTestScript
python -m venv .venv
source .venv/bin/activate  # Linux/Mac
.venv\Scripts\activate   # Windows
pip install -r requirements.txt
```

## Run Tests

```bash
# Run all tests
pytest -v

# Run with custom base URL
pytest -v --base-url http://localhost:3000

# Run in headless mode
pytest -v --headless

# Run specific test file
pytest -v tests/test_auth.py

# Run specific test
pytest -v tests/test_auth.py::TestAuthentication::test_auth_001_login_page_loads_successfully

# Run with parallel execution
pytest -v -n 4

# Generate HTML report
pytest -v --html=report.html
```

## Test Accounts

Set environment variables hoặc update trong conftest.py:
```bash
export TEST_STUDENT_EMAIL="student@thinkai.com"
export TEST_STUDENT_PASSWORD="Student123!"
export TEST_TEACHER_EMAIL="teacher@thinkai.com"
export TEST_TEACHER_PASSWORD="Teacher123!"
export TEST_ADMIN_EMAIL="admin@thinkai.com"
export TEST_ADMIN_PASSWORD="Admin123!"
```

## Test Coverage

| Module | Test Cases | Range |
|--------|------------|-------|
| Authentication | 20 | TC-AUTH-001 → TC-AUTH-020 |
| Courses | 25 | TC-COURSE-021 → TC-COURSE-045 |
| Learning | 15 | TC-LEARN-046 → TC-LEARN-060 |
| Exams | 20 | TC-EXAM-061 → TC-EXAM-080 |
| AI Tutor | 15 | TC-AI-081 → TC-AI-095 |
| Teacher | 15 | TC-TEACHER-096 → TC-TEACHER-110 |
| Admin | 10 | TC-ADMIN-111 → TC-ADMIN-120 |
| **Total** | **120** | |

## Best Practices

1. **Không dùng time.sleep** - Dùng WebDriverWait
2. **Mọi thao tác qua BasePage** - Đảm bảo đồng nhất
3. **Locator ưu tiên id > name > data-testid** - Tránh XPath dài
4. **Test ngắn gọn** - Mỗi test 5-10 dòng, gọi flow + assert
5. **Data-driven** - Dùng pytest.mark.parametrize
6. **Run 5-10 lần stable** trước khi scale
