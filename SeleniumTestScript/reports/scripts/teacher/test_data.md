# Test Data - Teacher Tests

## Test Accounts

| Role | Email | Password | Notes |
|------|-------|----------|-------|
| Teacher | teacher@test.com | Teacher123! | Auto-created |
| Student | studentnew@test.com | Student123! | Auto-created |
| Admin | admin@gmail.com | admin@gmail.com | Pre-existing |

## Test Execution

```bash
pytest -v tests/test_teacher.py --html=reports/file-html/teacher_report.html
```

## Results

- **Total**: 15 tests
- **Passed**: 12
- **Failed**: 2
- **Error**: 1
- **Time**: ~2 minutes

## Notes

- Teacher page works for teacher role
- Some authorization edge cases found
- No critical bugs in functionality
