# Test Data - Courses Tests

## Test Accounts

| Role | Email | Password | Notes |
|------|-------|----------|-------|
| Student | studentnew@test.com | Student123! | Auto-created |
| Teacher | teacher@test.com | Teacher123! | Auto-created |
| Admin | admin@gmail.com | admin@gmail.com | Pre-existing |

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

## Test Execution

### Run all courses tests
```bash
pytest -v tests/test_courses.py --html=file-html/reports/courses_report.html
```

### Run specific test
```bash
pytest -v tests/test_courses.py::TestCourses::test_course_021_courses_page_loads
```

## Results

- **Total**: 25 tests
- **Passed**: 25
- **Failed**: 0
- **Skipped**: 0
- **Time**: ~2.5 minutes

## Test Cases

| Test ID | Description | Expected |
|---------|-------------|----------|
| TC-COURSE-021 | Courses page loads | /courses in URL |
| TC-COURSE-022 | Display course list | count >= 0 |
| TC-COURSE-023 | Search courses | Results displayed |
| TC-COURSE-024 | Sort by newest | No error |
| TC-COURSE-025 | Sort by price low | No error |
| TC-COURSE-026 | Sort by price high | No error |
| TC-COURSE-027 | Click course card | Navigate to detail |
| TC-COURSE-028 | Course detail title | Title displayed |
| TC-COURSE-029 | Course price | Price displayed |
| TC-COURSE-030 | Enroll button | Button visible |
| TC-COURSE-031 | Enroll free course | Enrolled or redirect |
| TC-COURSE-032 | Enroll paid course | Redirect to payment |
| TC-COURSE-033 | Start Learning button | Button visible if enrolled |
| TC-COURSE-034 | Click Start Learning | Navigate to /learn/ |
| TC-COURSE-035 | Lesson list | Lesson items visible |
| TC-COURSE-036 | Instructor info | Instructor visible |
| TC-COURSE-037 | Unenroll | No error |
| TC-COURSE-038 | Breadcrumb | Breadcrumb visible |
| TC-COURSE-039 | Empty state | Message or courses |
| TC-COURSE-040 | Pagination | Controls present |
| TC-COURSE-041 | No search results | Empty state |
| TC-COURSE-042 | Reset filters | Reset successful |
| TC-COURSE-043 | Rating badge | Badge visible |
| TC-COURSE-044 | Enrolled badge | Badge visible if enrolled |
| TC-COURSE-045 | My courses page | URL contains /my-courses |
