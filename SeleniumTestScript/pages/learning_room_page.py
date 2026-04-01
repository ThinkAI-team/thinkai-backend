from selenium.webdriver.common.by import By
from pages.base_page import BasePage


class LearningRoomPage(BasePage):
    VIDEO_PLAYER = (By.CSS_SELECTOR, "video")
    YOUTUBE_PLAYER = (By.CSS_SELECTOR, "iframe[src*='youtube']")
    LESSON_TITLE = (By.CSS_SELECTOR, "h1")
    COMPLETE_BUTTON = (By.CSS_SELECTOR, "button:has-text('Hoàn thành')")
    COMPLETED_BUTTON = (By.CSS_SELECTOR, "button:has-text('Đã hoàn thành')")
    LESSON_LIST = (By.CSS_SELECTOR, "[class*='lessonItem']")
    PROGRESS_BADGE = (By.CSS_SELECTOR, "[class*='progressBadge']")
    AI_TUTOR_BUTTON = (By.CSS_SELECTOR, "a[href='/ai-tutor']")
    COURSE_LINK = (By.CSS_SELECTOR, "a[href='/courses']")

    def __init__(self, driver, lesson_id=None):
        super().__init__(driver)
        self.lesson_id = lesson_id
        self.PATH = f"/learn/{lesson_id}" if lesson_id else "/learn"

    def open(self, base_url=None, lesson_id=None):
        lid = lesson_id or self.lesson_id
        url = base_url or self.driver.current_url
        self.driver.get(url.rstrip("/") + f"/learn/{lid}")

    def is_video_player_ready(self):
        return self.is_displayed(self.VIDEO_PLAYER, timeout=5) or self.is_displayed(
            self.YOUTUBE_PLAYER, timeout=5
        )

    def click_complete(self):
        self.click(self.COMPLETE_BUTTON)

    def is_completed(self):
        return self.is_displayed(self.COMPLETED_BUTTON, timeout=2)

    def get_lesson_title(self):
        return self.get_text(self.LESSON_TITLE)

    def get_progress(self):
        return self.get_text(self.PROGRESS_BADGE)
