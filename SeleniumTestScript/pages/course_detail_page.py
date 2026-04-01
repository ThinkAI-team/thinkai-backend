from selenium.webdriver.common.by import By
from pages.base_page import BasePage


class CourseDetailPage(BasePage):
    TITLE = (By.CSS_SELECTOR, "h1")
    DESCRIPTION = (By.CSS_SELECTOR, "[class*='subtitle']")
    ENROLL_BUTTON = (By.XPATH, "//button[contains(text(), 'Đăng ký')]")
    START_LEARNING_BUTTON = (By.XPATH, "//button[contains(text(), 'Vào học')]")
    UNENROLL_BUTTON = (By.XPATH, "//button[contains(text(), 'Hủy')]")
    LESSON_ITEMS = (By.CSS_SELECTOR, "[class*='curriculum'], [class*='lesson']")
    INSTRUCTOR_NAME = (
        By.CSS_SELECTOR,
        "[class*='instructor'] h3, [class*='instructor'] h4",
    )
    PRICE = (By.CSS_SELECTOR, "[class*='price'], [class*='Price']")
    ENROLLED_BADGE = (By.CSS_SELECTOR, "[class*='discount'], [class*='enrolled']")
    BREADCRUMB = (By.CSS_SELECTOR, "[class*='breadcrumb'], nav a")

    def __init__(self, driver, course_id=None):
        super().__init__(driver)
        self.course_id = course_id

    def open(self, base_url=None, course_id=None):
        cid = course_id or self.course_id or 1
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + f"/courses/{cid}")

    def is_enrolled(self):
        try:
            return self.is_displayed(self.START_LEARNING_BUTTON, timeout=3)
        except:
            return False

    def click_enroll(self):
        self.click(self.ENROLL_BUTTON)

    def click_start_learning(self):
        self.click(self.START_LEARNING_BUTTON)

    def click_unenroll(self):
        self.click(self.UNENROLL_BUTTON)

    def get_title(self):
        return self.get_text(self.TITLE)

    def get_price(self):
        try:
            return self.get_text(self.PRICE)
        except:
            return None
