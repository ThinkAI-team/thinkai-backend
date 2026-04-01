from pages.courses_page import CoursesPage
from pages.course_detail_page import CourseDetailPage


class CourseFlow:
    def __init__(self, driver, wait, base_url):
        self.driver = driver
        self.wait = wait
        self.base_url = base_url
        self.courses_page = CoursesPage(driver, wait)
        self.course_detail_page = CourseDetailPage(driver, wait)

    def browse_courses(self):
        self.courses_page.open(self.base_url)
        return self.courses_page.get_course_count() >= 0

    def search_course(self, keyword):
        self.courses_page.open(self.base_url)
        self.courses_page.search(keyword)
        return self.courses_page.get_course_count()

    def enroll_in_course(self, course_id, already_enrolled=False):
        self.course_detail_page.open(self.base_url, course_id)

        if already_enrolled or self.course_detail_page.is_enrolled():
            return False

        self.course_detail_page.click_enroll()
        from selenium.webdriver.support.ui import WebDriverWait

        WebDriverWait(self.driver, 10).until(
            lambda d: "payment" in d.current_url or "/courses/" in d.current_url
        )
        return True

    def start_learning(self, course_id):
        self.course_detail_page.open(self.base_url, course_id)
        if self.course_detail_page.is_enrolled():
            self.course_detail_page.click_start_learning()
            return "/learn/" in self.driver.current_url
        return False

    def view_my_courses(self):
        self.driver.get(f"{self.base_url}/my-courses")
        return "/my-courses" in self.driver.current_url
