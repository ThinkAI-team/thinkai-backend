# Test Data - Auth Tests

## Test Accounts

| Role | Email | Password | Created | Notes |
|------|-------|----------|---------|-------|
| Student | studentnew@test.com | Student123! | ✅ Auto-created | Created via test_auth_010 |
| Teacher | teacher@test.com | Teacher123! | ✅ Auto-created | Created via test_auth_011 |
| Admin | admin@gmail.com | admin@gmail.com | ✅ Pre-existing | From user database |

## Credentials Configuration

File: `conftest.py`

```python
@pytest.fixture(scope="session")
def test_users():
    return {
        "student": {
            "email": "studentnew@test.com",
            "password": "Student123!",
            "role": "STUDENT"
        },
        "teacher": {
            "email": "teacher@test.com",
            "password": "Teacher123!",
            "role": "TEACHER"
        },
        "admin": {
            "email": "admin@gmail.com",
            "password": "admin@gmail.com",
            "role": "ADMIN"
        }
    }
```

## Environment Variables (Optional)

```bash
export TEST_STUDENT_EMAIL="studentnew@test.com"
export TEST_STUDENT_PASSWORD="Student123!"
export TEST_TEACHER_EMAIL="teacher@test.com"
export TEST_TEACHER_PASSWORD="Teacher123!"
export TEST_ADMIN_EMAIL="admin@gmail.com"
export TEST_ADMIN_PASSWORD="admin@gmail.com"
```

## Test Execution

### Run all auth tests
```bash
pytest -v tests/test_auth.py --html=file-html/reports/auth_report.html
```

### Run specific test
```bash
pytest -v tests/test_auth.py::TestAuthentication::test_auth_001_login_page_loads_successfully
```

### Run with headless
```bash
pytest -v tests/test_auth.py --headless
```

## Results

- **Total**: 20 tests
- **Passed**: 20
- **Failed**: 0
- **Skipped**: 0
- **Time**: ~3 minutes

## Test Data Used in Each Test Case

| Test ID | Email Used | Password Used | Expected |
|---------|------------|---------------|----------|
| TC-AUTH-001 | N/A | N/A | Page loads |
| TC-AUTH-002 | studentnew@test.com | Student123! | Success → dashboard |
| TC-AUTH-003 | teacher@test.com | Teacher123! | Success → /teacher |
| TC-AUTH-004 | admin@gmail.com | admin@gmail.com | Success → /admin |
| TC-AUTH-005 | studentnew@test.com | WrongPassword123 | Error message |
| TC-AUTH-006 | nonexistent@test.com | Password123 | Error message |
| TC-AUTH-007 | (empty) | (empty) | Error or no redirect |
| TC-AUTH-008 | N/A | N/A | Navigate to /forgot-password |
| TC-AUTH-009 | N/A | N/A | Navigate to /register |
| TC-AUTH-010 | student{int(time)}@test.com | Student123! | Success → dashboard |
| TC-AUTH-011 | teacher{int(time)}@test.com | Teacher123! | Success → /teacher |
| TC-AUTH-012 | testuser@test.com | Password123! | DifferentPass123! | Error |
| TC-AUTH-013 | studentnew@test.com | Student123! | Duplicate error |
| TC-AUTH-014 | (empty) | (empty) | Validation error |
| TC-AUTH-015 | studentnew@test.com | Student123! | Logout → /login |
| TC-AUTH-016 | N/A | N/A | Button visible |
| TC-AUTH-017 | N/A | N/A | Button visible |
| TC-AUTH-018 | studentnew@test.com | Student123! | Redirect to dashboard |
| TC-AUTH-019 | (any valid) | (valid) | Role-based redirect |
| TC-AUTH-020 | studentnew@test.com | Student123! | Session persists |
