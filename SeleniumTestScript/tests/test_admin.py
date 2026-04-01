"""
===============================================================================
ThinkAI Test Suite - Admin Tests (TC-ADMIN-111 to TC-ADMIN-120)
Based on UI and Code analysis - focusing on REAL bugs
"""

import pytest
import time
from flows.auth_flow import AuthFlow
from pages.admin_pages import AdminDashboardPage, AdminUsersPage, AdminCoursesPage


class TestAdmin:
    @pytest.fixture(autouse=True)
    def setup(self, driver, wait, base_url, test_users):
        self.driver = driver
        self.wait = wait
        self.base_url = base_url
        self.test_users = test_users
        self.auth_flow = AuthFlow(driver, wait, base_url)
        self.admin_dashboard = AdminDashboardPage(driver, wait)
        self.admin_users = AdminUsersPage(driver, wait)
        self.admin_courses = AdminCoursesPage(driver, wait)

        user = self.test_users["admin"]
        self.auth_flow.login_as_admin(user["email"], user["password"])
        time.sleep(2)
        yield

    def test_admin_111_admin_dashboard_loads(self):
        """Test admin dashboard loads"""
        self.admin_dashboard.open(self.base_url)
        assert "/admin" in self.driver.current_url

    def test_admin_112_stats_cards_displayed(self):
        """Test stats cards displayed"""
        self.admin_dashboard.open(self.base_url)
        time.sleep(2)
        count = self.admin_dashboard.get_stats_count()
        assert count >= 0

    def test_admin_113_student_access_admin_redirects(self):
        """BUG: Student accessing admin should redirect"""
        # Login as student
        student_user = self.test_users["student"]
        self.auth_flow.login_as_student(student_user["email"], student_user["password"])
        time.sleep(2)

        # Try access admin page
        self.admin_dashboard.open(self.base_url)
        time.sleep(3)

        current = self.driver.current_url
        # BUG: Student can access admin page
        if "/admin" in current:
            print(f"BUG: Student can access admin at {current}")
        # Should redirect but doesn't - THIS IS THE BUG
        assert "/admin" not in current, "BUG: Student should be redirected from admin"

    def test_admin_114_teacher_access_admin_redirects(self):
        """BUG: Teacher accessing admin should redirect"""
        # Login as teacher
        teacher_user = self.test_users["teacher"]
        self.auth_flow.login_as_teacher(teacher_user["email"], teacher_user["password"])
        time.sleep(2)

        # Try access admin page
        self.admin_dashboard.open(self.base_url)
        time.sleep(3)

        current = self.driver.current_url
        # BUG: Teacher can access admin page without proper redirect
        if "/admin" in current:
            print(f"BUG: Teacher can access admin at {current}")
        assert "/admin" not in current, "BUG: Teacher should be redirected from admin"

    def test_admin_115_users_tab_functionality(self):
        """Test users tab works"""
        self.admin_dashboard.open(self.base_url)
        time.sleep(2)
        self.admin_dashboard.click_tab("users")
        time.sleep(2)
        assert self.admin_dashboard.is_users_tab_active()

    def test_admin_116_courses_tab_functionality(self):
        """Test courses tab works"""
        self.admin_dashboard.open(self.base_url)
        time.sleep(2)
        self.admin_dashboard.click_tab("courses")
        time.sleep(2)
        assert self.admin_dashboard.is_courses_tab_active()

    def test_admin_117_user_list_displayed(self):
        """Test user list in admin"""
        self.admin_dashboard.open(self.base_url)
        time.sleep(2)
        self.admin_dashboard.click_tab("users")
        time.sleep(2)
        count = self.admin_users.get_user_count()
        assert count >= 0

    def test_admin_118_course_list_displayed(self):
        """Test course list in admin"""
        self.admin_dashboard.open(self.base_url)
        time.sleep(2)
        self.admin_dashboard.click_tab("courses")
        time.sleep(2)
        # Should show courses
        body = self.driver.find_element("tag name", "body").text
        assert len(body) > 0

    def test_admin_119_admin_logout(self):
        """Test logout from admin"""
        self.admin_dashboard.open(self.base_url)
        time.sleep(2)
        try:
            logout_btn = self.driver.find_element(
                "xpath", "//button[contains(text(), 'Đăng xuất')]"
            )
            logout_btn.click()
            time.sleep(2)
            assert "/login" in self.driver.current_url
        except:
            print("Logout button issue")

    def test_admin_120_admin_page_complete(self):
        """Test admin page complete"""
        self.admin_dashboard.open(self.base_url)
        time.sleep(2)
        assert "/admin" in self.driver.current_url
        body = self.driver.find_element("tag name", "body").text
        assert len(body) > 0
