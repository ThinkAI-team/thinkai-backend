from selenium.webdriver.common.by import By
from pages.base_page import BasePage


class AiTutorPage(BasePage):
    PATH = "/ai-tutor"

    CHAT_INPUT = (By.CSS_SELECTOR, "textarea, input[placeholder*='hỏi']")
    SEND_BUTTON = (By.XPATH, "//button[contains(text(), 'Gửi')]")
    CHAT_MESSAGES = (By.CSS_SELECTOR, "[class*='message'], [class*='chat']")
    LOADING_INDICATOR = (By.CSS_SELECTOR, "[class*='loading'], [class*='typing']")
    SETTINGS_BUTTON = (By.XPATH, "//button[contains(text(), 'Cài đặt')]")
    HISTORY_BUTTON = (By.XPATH, "//button[contains(text(), 'Lịch sử')]")

    def open(self, base_url=None):
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + self.PATH)

    def send_message(self, message):
        self.enter_text(self.CHAT_INPUT, message)
        self.click(self.SEND_BUTTON)

    def is_loading(self):
        return self.is_displayed(self.LOADING_INDICATOR, timeout=2)

    def get_message_count(self):
        return len(self.find_elements(self.CHAT_MESSAGES))

    def click_settings(self):
        self.click(self.SETTINGS_BUTTON)

    def click_history(self):
        self.click(self.HISTORY_BUTTON)


class AiTutorFloatingLauncher(BasePage):
    LAUNCHER_BUTTON = (By.CSS_SELECTOR, "[class*='floating'], [class*='launcher']")
    CHAT_WIDGET = (By.CSS_SELECTOR, "[class*='widget'], [class*='chatbox']")

    def click_launcher(self):
        self.click(self.LAUNCHER_BUTTON)

    def is_chat_open(self):
        return self.is_displayed(self.CHAT_WIDGET, timeout=3)
