from selenium.webdriver.common.by import By
from pages.base_page import BasePage


class CoursesPage(BasePage):
    PATH = "/courses"

    SEARCH_INPUT = (By.CSS_SELECTOR, "input[placeholder*='Tìm']")
    SEARCH_BUTTON = (By.XPATH, "//button[contains(text(), 'Tìm')]")
    SORT_SELECT = (By.CSS_SELECTOR, "select")
    COURSE_CARDS = (By.CSS_SELECTOR, "a[href^='/courses/']")
    RESET_BUTTON = (By.XPATH, "//button[contains(text(), 'Đặt lại')]")
    LOADING_STATE = (By.CSS_SELECTOR, "[class*='PageState']")
    EMPTY_STATE = (By.CSS_SELECTOR, "[class*='empty']")
    PAGINATION = (By.CSS_SELECTOR, "[class*='pagination']")

    def open(self, base_url=None):
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + self.PATH)

    def search(self, keyword):
        self.enter_text(self.SEARCH_INPUT, keyword)
        self.click(self.SEARCH_BUTTON)

    def sort_by(self, option_index=0):
        select = self.find_element(self.SORT_SELECT)
        from selenium.webdriver.support.select import Select

        Select(select).select_by_index(option_index)

    def get_course_count(self):
        return len(self.find_elements(self.COURSE_CARDS))

    def is_loading(self):
        return self.is_displayed(self.LOADING_STATE, timeout=2)

    def is_empty(self):
        return self.is_displayed(self.EMPTY_STATE, timeout=2)

    def click_first_course(self):
        self.click(self.COURSE_CARDS)
