# Test Auth Description - TC-AUTH-001 to TC-AUTH-020

## Overview
20 test cases kiểm tra chức năng Authentication: Login, Register, Logout, Session

## Test Data
- **Student**: studentnew@test.com / Student123!
- **Teacher**: teacher@test.com / Teacher123!
- **Admin**: admin@gmail.com / admin@gmail.com

---

## Test Cases Detail

### TC-AUTH-001: Login page loads successfully
- **Mục tiêu**: Verify trang login load đúng URL và form hiển thị
- **Steps**:
  1. Open http://localhost:3000/login
  2. Check URL chứa "/login"
  3. Check email & password input hiển thị
- **Expected**: Page load thành công, form login visible

### TC-AUTH-002: Login with valid credentials (Student)
- **Mục tiêu**: Verify student login thành công
- **Steps**:
  1. Open login page
  2. Nhập email: studentnew@test.com
  3. Nhập password: Student123!
  4. Click submit
  5. Chờ redirect đến dashboard
- **Expected**: Redirect đến /dashboard

### TC-AUTH-003: Login with valid credentials (Teacher)
- **Mục tiêu**: Verify teacher login thành công
- **Steps**:
  1. Open login page
  2. Nhập email: teacher@test.com
  3. Nhập password: Teacher123!
  4. Click submit
  5. Chờ redirect đến /teacher
- **Expected**: Redirect đến /teacher

### TC-AUTH-004: Login with valid credentials (Admin)
- **Mục tiêu**: Verify admin login thành công
- **Steps**:
  1. Open login page
  2. Nhập email: admin@gmail.com
  3. Nhập password: admin@gmail.com
  4. Click submit
  5. Chờ redirect đến /admin
- **Expected**: Redirect đến /admin

### TC-AUTH-005: Login with invalid password
- **Mục tiêu**: Verify báo lỗi khi nhập sai password
- **Steps**:
  1. Open login page
  2. Nhập email đúng: studentnew@test.com
  3. Nhập password sai: WrongPassword123
  4. Click submit
  5. Check error message hiển thị
- **Expected**: Hiển thị error "Email hoặc mật khẩu không đúng"

### TC-AUTH-006: Login with nonexistent email
- **Mục tiêu**: Verify báo lỗi khi email không tồn tại
- **Steps**:
  1. Open login page
  2. Nhập email không tồn tại
  3. Nhập password bất kỳ
  4. Click submit
  5. Check error message hiển thị
- **Expected**: Hiển thị error message

### TC-AUTH-007: Login with empty credentials
- **Mục tiêu**: Verify validation khi để trống fields
- **Steps**:
  1. Open login page
  2. Để trống email và password
  3. Click submit
  4. Check error hoặc validation message
- **Expected**: Có error message hoặc không chuyển trang

### TC-AUTH-008: Navigate to forgot password
- **Mục tiêu**: Verify link "Quên mật khẩu" hoạt động
- **Steps**:
  1. Open login page
  2. Click link "Quên mật khẩu?"
  3. Check URL chuyển đến /forgot-password
- **Expected**: URL chứa "/forgot-password"

### TC-AUTH-009: Navigate to register
- **Mục tiêu**: Verify link "Đăng ký" hoạt động
- **Steps**:
  1. Open login page
  2. Click link "Đăng ký ngay"
  3. Check URL chuyển đến /register
- **Expected**: URL chứa "/register"

### TC-AUTH-010: Register new student account
- **Mục tiêu**: Verify đăng ký student thành công
- **Steps**:
  1. Open register page
  2. Nhập firstName, lastName, email, password, confirmPassword
  3. Select role STUDENT
  4. Accept terms
  5. Click submit
  6. Chờ redirect đến dashboard
- **Expected**: Redirect đến /dashboard

### TC-AUTH-011: Register new teacher account
- **Mục tiêu**: Verify đăng ký teacher thành công
- **Steps**:
  1. Open register page
  2. Nhập thông tin đăng ký
  3. Select role TEACHER
  4. Accept terms và submit
- **Expected**: Redirect đến /teacher

### TC-AUTH-012: Register password mismatch
- **Mục tiêu**: Verify báo lỗi khi password không khớp
- **Steps**:
  1. Open register page
  2. Nhập password và confirmPassword khác nhau
  3. Click submit
  4. Check error message
- **Expected**: Hiển thị error về password mismatch

### TC-AUTH-013: Register duplicate email
- **Mục tiêu**: Verify báo lỗi khi email đã tồn tại
- **Steps**:
  1. Open register page
  2. Nhập email đã đăng ký trước đó
  3. Click submit
  4. Check error message
- **Expected**: Hiển thị error duplicate email

### TC-AUTH-014: Register empty required fields
- **Mục tiêu**: Verify validation khi để trống fields
- **Steps**:
  1. Open register page
  2. Click submit mà không nhập gì
  4. Check error/validation message
- **Expected**: Có error message yêu cầu nhập đầy đủ

### TC-AUTH-015: Logout from dashboard
- **Mục tiêu**: Verify chức năng logout hoạt động
- **Steps**:
  1. Login thành công với student
  2. Click button "Đăng xuất"
  3. Check redirect về login page
- **Expected**: Redirect đến /login

### TC-AUTH-016: Google login button visible
- **Mục tiêu**: Verify button Google login hiển thị
- **Steps**:
  1. Open login page
  2. Check Google button visible
- **Expected**: Button Google login visible

### TC-AUTH-017: Google register button visible
- **Mục tiêu**: Verify button Google register hiển thị
- **Steps**:
  1. Open register page
  2. Check Google button visible
- **Expected**: Button Google register visible

### TC-AUTH-018: Login remember user redirects to dashboard
- **Mục tiêu**: Verify sau login thành công redirect về dashboard
- **Steps**:
  1. Login với student account
  2. Check URL chuyển đến trang có dashboard
- **Expected**: URL chứa "dashboard" hoặc không còn /login

### TC-AUTH-019: Login role-based redirect
- **Mục tiêu**: Verify mỗi role được redirect đúng trang
- **Steps**:
  1. Login với student → check /dashboard
  2. Login với teacher → check /teacher
  3. Login với admin → check /admin
- **Expected**: Mỗi role redirect đúng trang tương ứng

### TC-AUTH-020: Session persists on page refresh
- **Mục tiêu**: Verify session không bị mất khi refresh
- **Steps**:
  1. Login thành công
  2. Refresh trang
  3. Check vẫn ở trang authenticated (không bị redirect về login)
- **Expected**: Vẫn ở trang authenticated, không về login

---

## Pattern sử dụng

### POM Pattern
- **Page Objects**: LoginPage, RegisterPage, DashboardPage
- **Locators**: ID, CSS, XPath
- **Methods**: login(), click_submit(), get_error_message()

### Flow Pattern
- **Flow**: AuthFlow
- **Methods**: login_as_student(), login_as_teacher(), login_as_admin(), login_failure(), logout()

### Fixtures
- driver, wait, base_url, test_users

### Best Practices
- ✅ Dùng WebDriverWait thay vì time.sleep
- ✅ Mỗi test độc lập (setup/teardown driver)
- ✅ Gọi flow + assert kết quả
- ✅ Locator ưu tiên id > CSS > XPath
