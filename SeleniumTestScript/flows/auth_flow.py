from pages.login_page import LoginPage
from pages.dashboard_page import DashboardPage
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time


class AuthFlow:
    def __init__(self, driver, wait, base_url):
        self.driver = driver
        self.wait = wait
        self.base_url = base_url
        self.login_page = LoginPage(driver, wait)
        self.dashboard_page = DashboardPage(driver, wait)

    def login_as_student(self, email, password):
        self.login_page.open(self.base_url)
        self.login_page.login(email, password)
        self.dashboard_page.wait_for_url_contains("dashboard", timeout=10)
        return self.dashboard_page.is_on_dashboard()

    def login_as_teacher(self, email, password):
        self.login_page.open(self.base_url)
        self.login_page.login(email, password)
        self.login_page.wait_for_url_contains("teacher", timeout=10)
        return "/teacher" in self.driver.current_url

    def login_as_admin(self, email, password):
        self.login_page.open(self.base_url)
        self.login_page.login(email, password)
        self.login_page.wait_for_url_contains("admin", timeout=10)
        return "/admin" in self.driver.current_url

    def login_success(self, email, password):
        self.login_page.open(self.base_url)
        self.login_page.login(email, password)
        WebDriverWait(self.driver, 10).until(lambda d: "/login" not in d.current_url)
        return "/login" not in self.driver.current_url

    def login_failure(self, email, password):
        self.login_page.open(self.base_url)
        self.login_page.login(email, password)
        time.sleep(2)
        error = self.login_page.get_error_message()
        return error is not None

    def logout(self):
        from selenium.webdriver.common.by import By

        try:
            logout_btn = self.driver.find_element(
                By.CSS_SELECTOR, "button:has-text('Đăng xuất'), a:has-text('Đăng xuất')"
            )
            logout_btn.click()
        except:
            self.driver.get(f"{self.base_url}/login")
