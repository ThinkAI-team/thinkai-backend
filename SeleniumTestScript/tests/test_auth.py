"""
===============================================================================
ThinkAI Test Suite - Authentication Tests (TC-AUTH-001 to TC-AUTH-020)
===============================================================================
"""

import pytest
import time
from flows.auth_flow import AuthFlow
from pages.login_page import LoginPage
from pages.register_page import RegisterPage
from pages.dashboard_page import DashboardPage


class TestAuthentication:
    @pytest.fixture(autouse=True)
    def setup(self, driver, wait, base_url, test_users):
        self.driver = driver
        self.wait = wait
        self.base_url = base_url
        self.test_users = test_users
        self.auth_flow = AuthFlow(driver, wait, base_url)
        self.login_page = LoginPage(driver, wait)
        self.register_page = RegisterPage(driver, wait)
        self.dashboard_page = DashboardPage(driver, wait)

    def test_auth_001_login_page_loads_successfully(self):
        self.login_page.open(self.base_url)
        assert self.login_page.is_on_login_page()
        assert self.login_page.is_login_form_displayed()

    def test_auth_002_login_with_valid_credentials_student(self):
        user = self.test_users["student"]
        result = self.auth_flow.login_as_student(user["email"], user["password"])
        assert result

    def test_auth_003_login_with_valid_credentials_teacher(self):
        user = self.test_users["teacher"]
        result = self.auth_flow.login_as_teacher(user["email"], user["password"])
        assert result

    def test_auth_004_login_with_valid_credentials_admin(self):
        user = self.test_users["admin"]
        result = self.auth_flow.login_as_admin(user["email"], user["password"])
        assert result

    def test_auth_005_login_with_invalid_password(self):
        user = self.test_users["student"]
        result = self.auth_flow.login_failure(user["email"], "WrongPassword123")
        assert result

    def test_auth_006_login_with_nonexistent_email(self):
        result = self.auth_flow.login_failure(
            f"nonexistent{int(time.time())}@test.com", "Password123"
        )
        assert result

    def test_auth_007_login_with_empty_credentials(self):
        self.login_page.open(self.base_url)
        self.login_page.enter_email("")
        self.login_page.enter_password("")
        self.login_page.click_submit()
        time.sleep(2)
        error = self.login_page.get_error_message()
        assert error is not None or "/login" in self.driver.current_url

    def test_auth_008_login_navigate_to_forgot_password(self):
        self.login_page.open(self.base_url)
        time.sleep(1)
        self.login_page.click_forgot_password()
        time.sleep(2)
        assert "/forgot-password" in self.driver.current_url

    def test_auth_009_login_navigate_to_register(self):
        self.login_page.open(self.base_url)
        time.sleep(1)
        self.login_page.click_register_link()
        time.sleep(2)
        assert self.register_page.is_on_register_page()

    def test_auth_010_register_new_student_account(self):
        self.register_page.open(self.base_url)
        self.register_page.register(
            "Test",
            "Student",
            f"student{int(time.time())}@test.com",
            "Student123!",
            "Student123!",
            "STUDENT",
        )
        time.sleep(2)
        assert (
            "/dashboard" in self.driver.current_url
            or self.register_page.is_on_register_page() == False
        )

    def test_auth_011_register_new_teacher_account(self):
        self.register_page.open(self.base_url)
        self.register_page.register(
            "Test",
            "Teacher",
            f"teacher{int(time.time())}@test.com",
            "Teacher123!",
            "Teacher123!",
            "TEACHER",
        )
        time.sleep(2)
        assert (
            "/teacher" in self.driver.current_url
            or self.register_page.is_on_register_page() == False
        )

    def test_auth_012_register_password_mismatch(self):
        self.register_page.open(self.base_url)
        self.register_page.register(
            "Test",
            "User",
            f"test{int(time.time())}@test.com",
            "Password123!",
            "DifferentPass123!",
            "STUDENT",
        )
        error = self.register_page.get_error_message()
        assert error is not None or "/register" in self.driver.current_url

    def test_auth_013_register_duplicate_email(self):
        user = self.test_users["student"]
        self.register_page.open(self.base_url)
        self.register_page.register(
            "Test", "User", user["email"], "Password123!", "Password123!", "STUDENT"
        )
        time.sleep(2)
        error = self.register_page.get_error_message()
        assert error is not None or "/register" in self.driver.current_url

    def test_auth_014_register_empty_required_fields(self):
        self.register_page.open(self.base_url)
        self.register_page.click_submit()
        error = self.register_page.get_error_message()
        assert error is not None or self.register_page.is_on_register_page()

    def test_auth_015_logout_from_dashboard(self):
        user = self.test_users["student"]
        self.auth_flow.login_as_student(user["email"], user["password"])
        self.auth_flow.logout()
        assert "/login" in self.driver.current_url

    def test_auth_016_google_login_button_visible(self):
        self.login_page.open(self.base_url)
        assert self.login_page.is_displayed(self.login_page.GOOGLE_BUTTON, timeout=3)

    def test_auth_017_register_google_button_visible(self):
        self.register_page.open(self.base_url)
        assert self.register_page.is_displayed(
            self.register_page.GOOGLE_BUTTON, timeout=3
        )

    def test_auth_018_login_remember_user_redirects_to_dashboard(self):
        user = self.test_users["student"]
        self.login_page.open(self.base_url)
        self.login_page.login(user["email"], user["password"])
        time.sleep(3)
        current = self.driver.current_url.lower()
        assert "dashboard" in current or "teacher" in current or "admin" in current

    def test_auth_019_login_role_based_redirect(self):
        user = self.test_users["student"]
        self.login_page.open(self.base_url)
        self.login_page.login(user["email"], user["password"])
        time.sleep(2)
        current = self.driver.current_url
        assert "/dashboard" in current or "/login" not in current

    def test_auth_020_session_persists_on_page_refresh(self):
        user = self.test_users["student"]
        self.auth_flow.login_as_student(user["email"], user["password"])
        original_url = self.driver.current_url
        self.driver.refresh()
        time.sleep(1)
        assert (
            "/login" not in self.driver.current_url
            or self.driver.current_url == original_url
        )
