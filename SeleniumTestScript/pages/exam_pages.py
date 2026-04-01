from selenium.webdriver.common.by import By
from pages.base_page import BasePage


class ExamsPage(BasePage):
    PATH = "/exams"

    EXAM_CARDS = (By.CSS_SELECTOR, "a[href^='/exams/']")
    LOADING_STATE = (By.CSS_SELECTOR, "[class*='PageState']")
    EMPTY_STATE = (By.CSS_SELECTOR, "[class*='empty']")

    def open(self, base_url=None):
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + self.PATH)

    def get_exam_count(self):
        return len(self.find_elements(self.EXAM_CARDS))

    def is_loading(self):
        return self.is_displayed(self.LOADING_STATE, timeout=2)

    def click_first_exam(self):
        self.click(self.EXAM_CARDS)


class ExamTakingPage(BasePage):
    TIMER = (By.CSS_SELECTOR, "[class*='timer']")
    QUESTION_TEXT = (By.CSS_SELECTOR, "h1")
    OPTION_BUTTONS = (By.CSS_SELECTOR, "[role='radio']")
    PROGRESS_BAR = (By.CSS_SELECTOR, "[class*='progressBar']")
    PREV_BUTTON = (By.CSS_SELECTOR, "button:has-text('Câu trước')")
    NEXT_BUTTON = (By.CSS_SELECTOR, "button:has-text('Câu sau')")
    SUBMIT_BUTTON = (By.CSS_SELECTOR, "button:has-text('Nộp bài')")
    EXIT_BUTTON = (By.CSS_SELECTOR, "button:has-text('Thoát')")

    def __init__(self, driver, exam_id=None):
        super().__init__(driver)
        self.exam_id = exam_id

    def open(self, base_url=None, exam_id=None):
        eid = exam_id or self.exam_id
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + f"/exams/{eid}")

    def get_timer_text(self):
        return self.get_text(self.TIMER)

    def select_answer(self, option_index=0):
        options = self.find_elements(self.OPTION_BUTTONS)
        if options and option_index < len(options):
            options[option_index].click()

    def is_submit_enabled(self):
        return self.is_displayed(self.SUBMIT_BUTTON, timeout=2)

    def click_submit(self):
        self.click(self.SUBMIT_BUTTON)
