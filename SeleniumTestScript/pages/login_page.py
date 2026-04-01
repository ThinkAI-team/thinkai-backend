from selenium.webdriver.common.by import By
from pages.base_page import BasePage


class LoginPage(BasePage):
    PATH = "/login"

    EMAIL_INPUT = (By.ID, "email")
    PASSWORD_INPUT = (By.ID, "password")
    SUBMIT_BUTTON = (By.CSS_SELECTOR, "button[type='submit']")
    GOOGLE_BUTTON = (By.CSS_SELECTOR, "button:has(svg)")
    FORGOT_PASSWORD_LINK = (By.XPATH, "//a[contains(@href, 'forgot-password')]")
    REGISTER_LINK = (By.XPATH, "//a[contains(@href, '/register')]")
    ERROR_ALERT = (By.CSS_SELECTOR, "[class*='errorAlert']")
    FIELD_ERROR = (By.CSS_SELECTOR, "[class*='fieldError']")

    def open(self, base_url=None):
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + self.PATH)

    def enter_email(self, email):
        self.enter_text(self.EMAIL_INPUT, email)

    def enter_password(self, password):
        self.enter_text(self.PASSWORD_INPUT, password)

    def click_submit(self):
        self.click(self.SUBMIT_BUTTON)

    def login(self, email, password):
        self.enter_email(email)
        self.enter_password(password)
        self.click_submit()

    def get_error_message(self):
        try:
            return self.find_element(self.ERROR_ALERT).text
        except:
            return None

    def is_login_form_displayed(self):
        return self.is_displayed(self.EMAIL_INPUT) and self.is_displayed(
            self.PASSWORD_INPUT
        )

    def is_on_login_page(self):
        return self.PATH in self.current_url

    def click_forgot_password(self):
        self.click(self.FORGOT_PASSWORD_LINK)

    def click_register_link(self):
        self.click(self.REGISTER_LINK)
