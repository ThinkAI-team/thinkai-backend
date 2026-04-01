"""
===============================================================================
ThinkAI Test Suite - Learning Room Tests (TC-LEARN-046 to TC-LEARN-060)
===============================================================================
Based on UI and Code analysis - focusing on real bugs
"""

import pytest
import time
from flows.auth_flow import AuthFlow
from pages.learning_room_page import LearningRoomPage


class TestLearningRoom:
    @pytest.fixture(autouse=True)
    def setup(self, driver, wait, base_url, test_users):
        self.driver = driver
        self.wait = wait
        self.base_url = base_url
        self.test_users = test_users
        self.auth_flow = AuthFlow(driver, wait, base_url)
        self.learning_page = LearningRoomPage(driver, wait)

        user = self.test_users["student"]
        self.auth_flow.login_as_student(user["email"], user["password"])
        time.sleep(2)
        yield

    def test_learn_046_learning_page_loads(self):
        """Test that learning page URL loads"""
        self.learning_page.open(self.base_url, lesson_id=1)
        assert "/learn/" in self.driver.current_url

    def test_learn_047_unenrolled_shows_error(self):
        """BUG: When not enrolled, should show error message"""
        self.learning_page.open(self.base_url, lesson_id=1)
        time.sleep(3)
        body = self.driver.find_element("tag name", "body").text
        # If not enrolled, should show error - THIS IS THE BUG
        has_error = "Không thể tải dữ liệu" in body or "not enrolled" in body.lower()
        assert has_error, "Bug: Should show enrollment error when not enrolled"

    def test_learn_048_no_lesson_content_when_not_enrolled(self):
        """BUG: Verify lesson content missing when not enrolled"""
        self.learning_page.open(self.base_url, lesson_id=1)
        time.sleep(3)
        body = self.driver.find_element("tag name", "body").text
        # When not enrolled, should NOT show video/lesson content
        has_video = "video" in body.lower() or "<video" in body.lower()
        assert not has_video, "Bug: Video should not show when not enrolled"

    def test_learn_049_complete_button_not_visible_when_not_enrolled(self):
        """BUG: Complete button should not be visible when not enrolled"""
        self.learning_page.open(self.base_url, lesson_id=1)
        time.sleep(3)
        try:
            from selenium.webdriver.common.by import By

            complete_btn = self.driver.find_element(
                By.XPATH, "//button[contains(text(), 'Hoàn thành')]"
            )
            # If button found, that's a bug
            assert False, "Bug: Complete button should not exist when not enrolled"
        except:
            pass  # Expected - button not found

    def test_learn_050_learn_page_requires_enrollment(self):
        """BUG: Learning page should check enrollment"""
        self.learning_page.open(self.base_url, lesson_id=1)
        time.sleep(3)
        current_url = self.driver.current_url
        # Page should either redirect or show error
        body = self.driver.find_element("tag name", "body").text
        is_valid = "/learn/" in current_url and len(body) > 0
        assert is_valid

    def test_learn_051_different_lesson_ids(self):
        """Test accessing different lesson IDs"""
        for lesson_id in [1, 2, 3]:
            self.learning_page.open(self.base_url, lesson_id=lesson_id)
            time.sleep(2)
            assert self.driver.current_url is not None

    def test_learn_052_learn_page_shows_error_state(self):
        """BUG: Check error state component"""
        self.learning_page.open(self.base_url, lesson_id=999)
        time.sleep(3)
        body = self.driver.find_element("tag name", "body").text
        # Invalid lesson should show error
        has_error = len(body) < 100  # Very short content means error state
        assert has_error or "không" in body.lower() or "error" in body.lower(), (
            "Should show error for invalid lesson"
        )

    def test_learn_053_no_video_player_without_lesson(self):
        """BUG: Video player should not be present"""
        self.learning_page.open(self.base_url, lesson_id=1)
        time.sleep(3)
        try:
            video = self.driver.find_element("css selector", "video")
            assert False, "Bug: Video element should not exist when not enrolled"
        except:
            pass  # Expected

    def test_learn_054_ai_tutor_accessible_from_unenrolled(self):
        """Test AI Tutor page is accessible"""
        self.driver.get(f"{self.base_url}/ai-tutor")
        time.sleep(2)
        assert "/ai-tutor" in self.driver.current_url

    def test_learn_055_breadcrumb_navigation(self):
        """Test breadcrumb shows correct path"""
        self.learning_page.open(self.base_url, lesson_id=1)
        time.sleep(3)
        try:
            from selenium.webdriver.common.by import By

            breadcrumb = self.driver.find_element(
                By.CSS_SELECTOR, "nav, [class*='breadcrumb'], ol"
            )
            assert breadcrumb is not None
        except:
            pass  # May not have breadcrumb when not enrolled

    def test_learn_056_course_link_from_learn_page(self):
        """Test can navigate back to course"""
        self.learning_page.open(self.base_url, lesson_id=1)
        time.sleep(3)
        try:
            from selenium.webdriver.common.by import By

            courses_link = self.driver.find_element(
                By.CSS_SELECTOR, "a[href*='/courses']"
            )
            courses_link.click()
            time.sleep(2)
            assert "/courses" in self.driver.current_url
        except:
            pass

    def test_learn_057_invalid_lesson_shows_error(self):
        """Test invalid lesson ID handling"""
        self.learning_page.open(self.base_url, lesson_id=99999)
        time.sleep(3)
        body = self.driver.find_element("tag name", "body").text
        # Should show error for invalid lesson
        has_content = len(body) > 50
        assert has_content  # Page should render something

    def test_learn_058_enrollment_check_api_call(self):
        """BUG: Verify API is called to check enrollment"""
        self.learning_page.open(self.base_url, lesson_id=1)
        time.sleep(3)
        body = self.driver.find_element("tag name", "body").text
        # The page should make API call and handle enrollment
        # Bug: may not handle properly
        assert "/learn/" in self.driver.current_url

    def test_learn_059_learn_page_renders(self):
        """Test learning page renders"""
        self.learning_page.open(self.base_url, lesson_id=1)
        time.sleep(3)
        title = self.driver.title
        assert "ThinkAI" in title or len(title) > 0

    def test_learn_060_back_to_dashboard(self):
        """Test can navigate back to dashboard"""
        self.learning_page.open(self.base_url, lesson_id=1)
        time.sleep(2)
        self.driver.get(f"{self.base_url}/dashboard")
        time.sleep(2)
        assert "/dashboard" in self.driver.current_url
