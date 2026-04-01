from selenium.webdriver.common.by import By
from pages.base_page import BasePage


class AdminDashboardPage(BasePage):
    PATH = "/admin"

    STATS_CARDS = (By.CSS_SELECTOR, "[class*='statCard']")
    TAB_NAV = (By.CSS_SELECTOR, "[role='tablist']")
    TAB_ITEMS = (By.CSS_SELECTOR, "[role='tab']")
    REFRESH_BUTTON = (By.CSS_SELECTOR, "button:has-text('Làm mới')")

    def open(self, base_url=None):
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + self.PATH)

    def click_tab(self, tab_id):
        tab = (By.ID, f"admin-tab-{tab_id}")
        self.click(tab)

    def get_stats_count(self):
        return len(self.find_elements(self.STATS_CARDS))

    def is_users_tab_active(self):
        return self.is_displayed((By.ID, "admin-panel-users"), timeout=2)

    def is_courses_tab_active(self):
        return self.is_displayed((By.ID, "admin-panel-courses"), timeout=2)


class AdminUsersPage(BasePage):
    USER_TABLE = (By.CSS_SELECTOR, "table")
    USER_ROWS = (By.CSS_SELECTOR, "tbody tr")
    TOGGLE_STATUS_BUTTON = (
        By.CSS_SELECTOR,
        "button:has-text('Khóa'), button:has-text('Mở')",
    )
    SEARCH_INPUT = (By.CSS_SELECTOR, "input[placeholder*='tìm']")

    def get_user_count(self):
        return len(self.find_elements(self.USER_ROWS))

    def click_toggle_status(self):
        self.click(self.TOGGLE_STATUS_BUTTON)


class AdminCoursesPage(BasePage):
    COURSE_TABLE = (By.CSS_SELECTOR, "table")
    CREATE_FORM = (By.CSS_SELECTOR, "form")
    TITLE_INPUT = (By.CSS_SELECTOR, "input[placeholder='Tiêu đề']")
    PRICE_INPUT = (By.CSS_SELECTOR, "input[type='number']")
    SAVE_BUTTON = (By.CSS_SELECTOR, "button:has-text('Lưu')")
    EDIT_BUTTON = (By.CSS_SELECTOR, "button:has-text('Sửa')")
    DELETE_BUTTON = (By.CSS_SELECTOR, "button:has-text('Xóa')")

    def create_course(self, title, price=0):
        self.enter_text(self.TITLE_INPUT, title)
        self.enter_text(self.PRICE_INPUT, str(price))
        self.click(self.SAVE_BUTTON)


class AdminPromptsPage(BasePage):
    TUTOR_PROMPT_INPUT = (By.CSS_SELECTOR, "textarea:first-child")
    EXAM_PROMPT_INPUT = (By.CSS_SELECTOR, "textarea:last-child")
    SAVE_BUTTON = (By.CSS_SELECTOR, "button:has-text('Lưu')")

    def save_prompts(self):
        self.click(self.SAVE_BUTTON)
