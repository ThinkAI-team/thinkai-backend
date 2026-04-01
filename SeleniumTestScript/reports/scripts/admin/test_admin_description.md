# Test Admin Description - TC-ADMIN-111 to TC-ADMIN-120

## Overview
10 test cases kiểm tra Admin portal - tìm bugs thực dựa trên UI và code

## Test Data
- **Admin**: admin@gmail.com / admin@gmail.com
- **Student**: studentnew@test.com / Student123!
- **Teacher**: teacher@test.com / Teacher123!

---

## Test Cases & Results

| Test ID | Description | Result |
|---------|-------------|--------|
| TC-ADMIN-111 | Admin dashboard loads | ✅ PASS |
| TC-ADMIN-112 | Stats cards displayed | ✅ PASS |
| TC-ADMIN-113 | Student access admin redirects | ❌ **BUG FOUND** |
| TC-ADMIN-114 | Teacher access admin redirects | ❌ **BUG FOUND** |
| TC-ADMIN-115 | Users tab functionality | ✅ PASS |
| TC-ADMIN-116 | Courses tab functionality | ✅ PASS |
| TC-ADMIN-117 | User list displayed | ✅ PASS |
| TC-ADMIN-118 | Course list displayed | ✅ PASS |
| TC-ADMIN-119 | Admin logout | ✅ PASS |
| TC-ADMIN-120 | Admin page complete | ✅ PASS |

---

## 🐛 BUGS FOUND

### Bug 1: Student Can Access Admin Page
- **Test**: TC-ADMIN-113
- **Severity**: 🔴 **CRITICAL - Security Vulnerability**
- **Description**: Student có thể truy cập trang admin (http://localhost:3000/admin) mà không bị redirect về dashboard
- **Expected**: Student nên bị redirect khỏi admin page
- **Actual**: Student vẫn ở trên /admin page và có thể thấy giao diện admin

### Bug 2: Teacher Can Access Admin Page  
- **Test**: TC-ADMIN-114
- **Severity**: 🔴 **CRITICAL - Security Vulnerability**
- **Description**: Teacher có thể truy cập trang admin mà không bị redirect
- **Expected**: Teacher nên bị redirect khỏi admin page
- **Actual**: Teacher vẫn ở trên /admin page

---

## Test Results Summary
- **Total**: 10 tests
- **Passed**: 8
- **Failed**: 2 (Bugs found)
- **Bugs Found**: 2

## Time
- **Duration**: ~1.5 minutes
