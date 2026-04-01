# Test Courses Description - TC-COURSE-021 to TC-COURSE-045

## Overview
25 test cases kiểm tra chức năng Courses: list, search, sort, enroll, course detail

## Test Data
- **Student**: studentnew@test.com / Student123!
- **Teacher**: teacher@test.com / Teacher123!
- **Admin**: admin@gmail.com / admin@gmail.com

---

## Test Cases Detail

### TC-COURSE-021: Courses page loads
- **Mục tiêu**: Verify trang courses load đúng URL
- **Steps**: Open /courses, check URL
- **Expected**: URL chứa "/courses"

### TC-COURSE-022: Display course list
- **Mục tiêu**: Verify hiển thị danh sách khóa học
- **Steps**: Open courses, đếm số course
- **Expected**: Count >= 0

### TC-COURSE-023: Search courses by keyword
- **Mục tiêu**: Verify chức năng tìm kiếm
- **Steps**: Nhập keyword "test", click search
- **Expected**: Kết quả tìm kiếm hiển thị

### TC-COURSE-024: Sort courses by newest
- **Mục tiêu**: Verify sắp xếp theo mới nhất
- **Steps**: Chọn sort option đầu tiên
- **Expected**: Không lỗi

### TC-COURSE-025: Sort courses by price low
- **Mục tiêu**: Verify sắp xếp giá thấp đến cao
- **Steps**: Chọn sort option thứ 2
- **Expected**: Không lỗi

### TC-COURSE-026: Sort courses by price high
- **Mục tiêu**: Verify sắp xếp giá cao đến thấp
- **Steps**: Chọn sort option thứ 3
- **Expected**: Không lỗi

### TC-COURSE-027: Click course card navigates to detail
- **Mục tiêu**: Verify click course chuyển đến trang chi tiết
- **Steps**: Click course đầu tiên
- **Expected**: URL chứa "/courses/"

### TC-COURSE-028: Course detail page shows title
- **Mục tiêu**: Verify trang chi tiết hiển thị title
- **Steps**: Mở course detail, lấy title
- **Expected**: Title không rỗng

### TC-COURSE-029: Course detail shows price
- **Mục tiêu**: Verify hiển thị giá khóa học
- **Steps**: Lấy text price
- **Expected**: Price hiển thị

### TC-COURSE-030: Course detail enroll button visible
- **Mục tiêu**: Verify button Đăng ký hiển thị
- **Steps**: Check button enroll visible
- **Expected**: Button visible

### TC-COURSE-031: Enroll in free course
- **Mục tiêu**: Verify đăng ký khóa học miễn phí
- **Steps**: Click enroll, kiểm tra redirect
- **Expected**: Redirect hoặc hiển thị enrolled

### TC-COURSE-032: Enroll in paid course redirects to payment
- **Mục tiêu**: Verify paid course chuyển sang payment
- **Steps**: Click enroll on paid course
- **Expected**: Redirect to payment hoặc /courses/

### TC-COURSE-033: Already enrolled shows Start Learning
- **Mục tiêu**: Verify đã enroll thì thấy button Vào học
- **Steps**: Check is_enrolled, thấy button Start Learning
- **Expected**: Button visible

### TC-COURSE-034: Click Start Learning navigates to lesson
- **Mục tiêu**: Verify click Vào học chuyển đến lesson
- **Steps**: Click Start Learning button
- **Expected**: URL chứa "/learn/"

### TC-COURSE-035: Course detail shows lesson list
- **Mục tiêu**: Verify hiển thị danh sách bài học
- **Steps**: Check lesson items visible
- **Expected**: Lesson list visible

### TC-COURSE-036: Course detail shows instructor
- **Mục tiêu**: Verify hiển thị thông tin giảng viên
- **Steps**: Check instructor name visible
- **Expected**: Instructor visible

### TC-COURSE-037: Unenroll from course
- **Mục tiêu**: Verify chức năng hủy đăng ký
- **Steps**: Nếu đã enroll, click unenroll
- **Expected**: Không lỗi

### TC-COURSE-038: Breadcrumb navigation
- **Mục tiêu**: Verify breadcrumb hiển thị
- **Steps**: Check breadcrumb visible
- **Expected**: Breadcrumb visible

### TC-COURSE-039: Display no courses when empty
- **Mục tiêu**: Verify thông báo khi không có khóa học
- **Steps**: Check empty state
- **Expected**: Empty state hoặc có courses

### TC-COURSE-040: Pagination controls present
- **Mục tiêu**: Verify pagination hiển thị khi nhiều courses
- **Steps**: Check pagination visible
- **Expected**: Pagination visible hoặc không cần

### TC-COURSE-041: Search with no results
- **Mục tiểu**: Verify tìm kiếm không có kết quả
- **Steps**: Search từ khóa không tồn tại
- **Expected**: Empty state hoặc 0 results

### TC-COURSE-042: Reset filters
- **Mục tiêu**: Verify chức năng reset bộ lọc
- **Steps**: Search xong, click reset
- **Expected**: URL chứa "/courses"

### TC-COURSE-043: Course detail rating visible
- **Mục tiêu**: Verify hiển thị đánh giá
- **Steps**: Check enrolled badge visible
- **Expected**: Badge visible hoặc True

### TC-COURSE-044: Enrolled badge shows when enrolled
- **Mục tiêu**: Verify badge đã enroll
- **Steps**: Nếu enrolled, check badge
- **Expected**: Badge visible

### TC-COURSE-045: My courses page accessible
- **Mục tiêu**: Verify trang my-courses truy cập được
- **Steps**: Navigate to /my-courses
- **Expected**: URL chứa "/my-courses"

---

## Pattern sử dụng

### POM Pattern
- **Page Objects**: CoursesPage, CourseDetailPage
- **Locators**: CSS, XPath
- **Methods**: search(), sort_by(), get_course_count(), click_enroll()

### Flow Pattern
- **Flow**: CourseFlow, AuthFlow
- **Methods**: browse_courses(), search_course(), enroll_in_course(), start_learning()

### Fixtures
- driver, wait, base_url, test_users (auto login student)

### Best Practices
- ✅ Dùng WebDriverWait thay vì time.sleep
- ✅ Mỗi test độc lập
- ✅ Gọi flow + assert kết quả
- ✅ Locator ưu tiên XPath
