# Test Data - Admin Tests

## Test Accounts

| Role | Email | Password | Notes |
|------|-------|----------|-------|
| Admin | admin@gmail.com | admin@gmail.com | Pre-existing |
| Student | studentnew@test.com | Student123! | Auto-created |
| Teacher | teacher@test.com | Teacher123! | Auto-created |

## Test Execution

```bash
pytest -v tests/test_admin.py --html=reports/file-html/admin_report.html
```

## Results

- **Total**: 10 tests
- **Passed**: 8
- **Failed**: 2 (Security bugs found)
- **Time**: ~1.5 minutes

## 🔴 Critical Bugs Found

### Bug 1: Student Can Access Admin Page
- **Severity**: CRITICAL
- **Issue**: Student có thể truy cập /admin mà không bị redirect
- **Security**: Lỗ hổng bảo mật nghiêm trọng

### Bug 2: Teacher Can Access Admin Page
- **Severity**: CRITICAL  
- **Issue**: Teacher có thể truy cập /admin mà không bị redirect
- **Security**: Lỗ hổng bảo mật nghiêm trọng

## Recommendation

Cần implement authorization check ở admin page:
1. Kiểm tra role trước khi render
2. Redirect non-admin users về dashboard
3. Thêm middleware/guard cho admin routes
