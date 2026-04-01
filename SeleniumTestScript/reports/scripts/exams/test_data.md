# Test Data - Exams Tests

## Test Accounts

| Role | Email | Password | Notes |
|------|-------|----------|-------|
| Student | studentnew@test.com | Student123! | Auto-created |

## Test Execution

```bash
pytest -v tests/test_exams.py --html=reports/file-html/exams_report.html
```

## Results

- **Total**: 20 tests
- **Passed**: 20
- **Failed**: 0
- **Skipped**: 0
- **Time**: ~3 minutes

## Key Observations

1. **No Exams in Database**: System shows "Chưa có dữ liệu" - no exams available
2. **Error Handling Works**: Invalid exam IDs properly show "Không tìm thấy bài thi"
3. **UI Renders Correctly**: All pages render without errors

## Notes

- Tests pass because UI and error handling work correctly
- Cannot test full exam flow due to no exam data in system
- Need to create exam data for complete testing
