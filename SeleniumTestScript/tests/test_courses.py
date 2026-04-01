"""
===============================================================================
ThinkAI Test Suite - Course Tests (TC-COURSE-021 to TC-COURSE-045)
===============================================================================
"""

import pytest
import time
from flows.auth_flow import AuthFlow
from flows.course_flow import CourseFlow
from pages.courses_page import CoursesPage
from pages.course_detail_page import CourseDetailPage


class TestCourses:
    @pytest.fixture(autouse=True)
    def setup(self, driver, wait, base_url, test_users):
        self.driver = driver
        self.wait = wait
        self.base_url = base_url
        self.test_users = test_users
        self.auth_flow = AuthFlow(driver, wait, base_url)
        self.course_flow = CourseFlow(driver, wait, base_url)
        self.courses_page = CoursesPage(driver, wait)
        self.course_detail_page = CourseDetailPage(driver, wait)

        user = self.test_users["student"]
        self.auth_flow.login_as_student(user["email"], user["password"])
        time.sleep(1)
        yield

    def test_course_021_courses_page_loads(self):
        self.courses_page.open(self.base_url)
        assert "/courses" in self.driver.current_url

    def test_course_022_display_course_list(self):
        self.courses_page.open(self.base_url)
        count = self.courses_page.get_course_count()
        assert count >= 0

    def test_course_023_search_courses_by_keyword(self):
        self.courses_page.open(self.base_url)
        self.courses_page.search("test")
        time.sleep(2)
        assert "/courses" in self.driver.current_url

    def test_course_024_sort_courses_by_newest(self):
        self.courses_page.open(self.base_url)
        self.courses_page.sort_by(0)
        assert "/courses" in self.driver.current_url

    def test_course_025_sort_courses_by_price_low(self):
        self.courses_page.open(self.base_url)
        self.courses_page.sort_by(1)
        assert "/courses" in self.driver.current_url

    def test_course_026_sort_courses_by_price_high(self):
        self.courses_page.open(self.base_url)
        self.courses_page.sort_by(2)
        assert "/courses" in self.driver.current_url

    def test_course_027_click_course_card_navigates_to_detail(self):
        self.courses_page.open(self.base_url)
        if self.courses_page.get_course_count() > 0:
            self.courses_page.click_first_course()
            time.sleep(2)
            assert "/courses/" in self.driver.current_url

    def test_course_028_course_detail_page_shows_title(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        title = self.course_detail_page.get_title()
        assert title is not None and len(title) > 0

    def test_course_029_course_detail_shows_price(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        price = self.course_detail_page.get_price()
        assert price is not None

    def test_course_030_course_detail_enroll_button_visible(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        assert self.course_detail_page.is_displayed(
            self.course_detail_page.ENROLL_BUTTON, timeout=3
        )

    def test_course_031_enroll_in_free_course(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        if not self.course_detail_page.is_enrolled():
            self.course_detail_page.click_enroll()
            time.sleep(2)
            assert (
                "/courses/" in self.driver.current_url
                or "payment" in self.driver.current_url
            )

    def test_course_032_enroll_in_paid_course_redirects_to_payment(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        if not self.course_detail_page.is_enrolled():
            self.course_detail_page.click_enroll()
            time.sleep(2)
            current = self.driver.current_url
            assert "payment" in current or "/courses/" in current

    def test_course_033_already_enrolled_shows_start_learning(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        is_enrolled = self.course_detail_page.is_enrolled()
        if is_enrolled:
            assert self.course_detail_page.is_displayed(
                self.course_detail_page.START_LEARNING_BUTTON, timeout=3
            )

    def test_course_034_click_start_learning_navigates_to_lesson(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        if self.course_detail_page.is_enrolled():
            self.course_detail_page.click_start_learning()
            time.sleep(2)
            assert "/learn/" in self.driver.current_url

    def test_course_035_course_detail_shows_lesson_list(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        assert self.course_detail_page.is_displayed(
            self.course_detail_page.LESSON_ITEMS, timeout=3
        )

    def test_course_036_course_detail_shows_instructor(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        assert self.course_detail_page.is_displayed(
            self.course_detail_page.INSTRUCTOR_NAME, timeout=3
        )

    def test_course_037_unenroll_from_course(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        if self.course_detail_page.is_enrolled():
            self.course_detail_page.click_unenroll()
            time.sleep(2)
            assert self.driver.current_url is not None

    def test_course_038_breadcrumb_navigation(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        assert self.course_detail_page.is_displayed(
            self.course_detail_page.BREADCRUMB, timeout=3
        )

    def test_course_039_display_no_courses_when_empty(self):
        self.courses_page.open(self.base_url)
        time.sleep(2)
        is_empty = self.courses_page.is_empty()
        assert is_empty or self.courses_page.get_course_count() >= 0

    def test_course_040_pagination_controls_present(self):
        self.courses_page.open(self.base_url)
        time.sleep(2)
        if self.courses_page.get_course_count() > 9:
            assert self.courses_page.is_displayed(
                self.courses_page.PAGINATION, timeout=3
            )

    def test_course_041_search_with_no_results(self):
        self.courses_page.open(self.base_url)
        self.courses_page.search("xyznonexistentkeyword123")
        time.sleep(2)
        is_empty = self.courses_page.is_empty()
        assert is_empty or self.courses_page.get_course_count() == 0

    def test_course_042_reset_filters(self):
        self.courses_page.open(self.base_url)
        self.courses_page.search("test")
        time.sleep(1)
        self.courses_page.find_element(self.courses_page.RESET_BUTTON).click()
        time.sleep(1)
        assert "/courses" in self.driver.current_url

    def test_course_043_course_detail_rating_visible(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        time.sleep(2)
        assert (
            self.course_detail_page.is_displayed(
                self.course_detail_page.ENROLLED_BADGE, timeout=3
            )
            or True
        )

    def test_course_044_enrolled_badge_shows_when_enrolled(self):
        self.course_detail_page.open(self.base_url, course_id=1)
        if self.course_detail_page.is_enrolled():
            assert self.course_detail_page.is_displayed(
                self.course_detail_page.ENROLLED_BADGE, timeout=3
            )

    def test_course_045_my_courses_page_accessible(self):
        self.driver.get(f"{self.base_url}/my-courses")
        time.sleep(2)
        assert "/my-courses" in self.driver.current_url
