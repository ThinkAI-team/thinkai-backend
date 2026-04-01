from selenium.webdriver.common.by import By
from pages.base_page import BasePage


class RegisterPage(BasePage):
    PATH = "/register"

    FIRST_NAME_INPUT = (By.ID, "firstName")
    LAST_NAME_INPUT = (By.ID, "lastName")
    EMAIL_INPUT = (By.ID, "email")
    PASSWORD_INPUT = (By.ID, "password")
    CONFIRM_PASSWORD_INPUT = (By.ID, "confirmPassword")
    TERMS_CHECKBOX = (By.ID, "terms")
    ROLE_STUDENT = (By.CSS_SELECTOR, "[class*='roleCard']:first-child")
    ROLE_TEACHER = (By.CSS_SELECTOR, "[class*='roleCard']:last-child")
    SUBMIT_BUTTON = (By.CSS_SELECTOR, "button[type='submit']")
    GOOGLE_BUTTON = (By.CSS_SELECTOR, "button:has(svg)")
    LOGIN_LINK = (By.CSS_SELECTOR, "a[href='/login']")
    ERROR_ALERT = (By.CSS_SELECTOR, ".errorAlert")
    FIELD_ERROR = (By.CSS_SELECTOR, ".fieldError")

    def open(self, base_url=None):
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + self.PATH)

    def enter_first_name(self, first_name):
        self.enter_text(self.FIRST_NAME_INPUT, first_name)

    def enter_last_name(self, last_name):
        self.enter_text(self.LAST_NAME_INPUT, last_name)

    def enter_email(self, email):
        self.enter_text(self.EMAIL_INPUT, email)

    def enter_password(self, password):
        self.enter_text(self.PASSWORD_INPUT, password)

    def enter_confirm_password(self, confirm_password):
        self.enter_text(self.CONFIRM_PASSWORD_INPUT, confirm_password)

    def select_role(self, role="STUDENT"):
        if role.upper() == "STUDENT":
            self.click(self.ROLE_STUDENT)
        else:
            self.click(self.ROLE_TEACHER)

    def accept_terms(self):
        self.click(self.TERMS_CHECKBOX)

    def click_submit(self):
        self.click(self.SUBMIT_BUTTON)

    def register(
        self, first_name, last_name, email, password, confirm_password, role="STUDENT"
    ):
        self.enter_first_name(first_name)
        self.enter_last_name(last_name)
        self.enter_email(email)
        self.enter_password(password)
        self.enter_confirm_password(confirm_password)
        self.select_role(role)
        self.accept_terms()
        self.click_submit()

    def get_error_message(self):
        try:
            return self.find_element(self.ERROR_ALERT).text
        except:
            return None

    def is_on_register_page(self):
        return self.PATH in self.current_url

    def click_login_link(self):
        self.click(self.LOGIN_LINK)
