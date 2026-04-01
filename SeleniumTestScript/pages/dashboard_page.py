from selenium.webdriver.common.by import By
from pages.base_page import BasePage


class DashboardPage(BasePage):
    PATH = "/dashboard"

    GREETING = (By.CSS_SELECTOR, "h1")
    STATS_CARDS = (By.CSS_SELECTOR, "[class*='statCard']")
    COURSE_CARD = (By.CSS_SELECTOR, "[class*='courseCard']")
    PROGRESS_BAR = (By.CSS_SELECTOR, "[class*='progressBar']")
    SIDEBAR = (By.CSS_SELECTOR, "[class*='sidebar']")
    ENROLLED_COURSES = (By.CSS_SELECTOR, "[class*='chapters']")

    def open(self, base_url=None):
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + self.PATH)

    def get_greeting(self):
        return self.get_text(self.GREETING)

    def get_stats_count(self):
        return len(self.find_elements(self.STATS_CARDS))

    def is_on_dashboard(self):
        return self.PATH in self.current_url

    def has_enrolled_courses(self):
        return len(self.find_elements(self.COURSE_CARD)) > 0
