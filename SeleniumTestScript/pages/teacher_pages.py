from selenium.webdriver.common.by import By
from pages.base_page import BasePage


class TeacherDashboardPage(BasePage):
    PATH = "/teacher"

    STATS_CARDS = (By.CSS_SELECTOR, "[class*='statCard']")
    COURSE_LIST = (By.CSS_SELECTOR, "[class*='chapters']")
    PUBLISH_BUTTON = (By.CSS_SELECTOR, "button:has-text('Publish')")
    REFRESH_BUTTON = (By.CSS_SELECTOR, "button:has-text('Đồng bộ')")
    LOGOUT_BUTTON = (By.CSS_SELECTOR, "button:has-text('Đăng xuất')")

    def open(self, base_url=None):
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + self.PATH)

    def get_stats_count(self):
        return len(self.find_elements(self.STATS_CARDS))

    def click_publish(self):
        self.click(self.PUBLISH_BUTTON)

    def click_refresh(self):
        self.click(self.REFRESH_BUTTON)


class TeacherCoursesPage(BasePage):
    PATH = "/teacher/courses"

    CREATE_BUTTON = (By.CSS_SELECTOR, "button:has-text('Tạo')")
    COURSE_TABLE = (By.CSS_SELECTOR, "table")
    EDIT_BUTTON = (By.CSS_SELECTOR, "button:has-text('Sửa')")
    DELETE_BUTTON = (By.CSS_SELECTOR, "button:has-text('Xóa')")

    def open(self, base_url=None):
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + self.PATH)


class TeacherExamsPage(BasePage):
    PATH = "/teacher/exams"

    CREATE_BUTTON = (By.CSS_SELECTOR, "button:has-text('Tạo')")
    EXAM_TABLE = (By.CSS_SELECTOR, "table")

    def open(self, base_url=None):
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + self.PATH)


class TeacherQuestionsPage(BasePage):
    PATH = "/teacher/questions"

    IMPORT_BUTTON = (By.CSS_SELECTOR, "button:has-text('Import')")
    QUESTION_LIST = (By.CSS_SELECTOR, "table")

    def open(self, base_url=None):
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + self.PATH)
