# Test Data - Learning Tests

## Test Accounts

| Role | Email | Password | Notes |
|------|-------|----------|-------|
| Student | studentnew@test.com | Student123! | Auto-created |

## Test Execution

```bash
pytest -v tests/test_learning.py --html=reports/file-html/learning_report.html
```

## Results

- **Total**: 15 tests
- **Passed**: 14
- **Failed**: 1 (Bug found)
- **Skipped**: 0
- **Time**: ~2 minutes

## Bugs Found

| Test ID | Bug Description | Severity |
|---------|-----------------|----------|
| TC-LEARN-057 | Invalid lesson ID (99999) shows blank page instead of error message | Medium |

## Notes

- Tests focus on verifying actual UI behavior based on code analysis
- Not enrolled state shows "Không thể tải dữ liệu" error - this is expected behavior
- Invalid lesson IDs don't render proper error state
