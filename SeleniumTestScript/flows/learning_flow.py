from pages.learning_room_page import LearningRoomPage


class LearningFlow:
    def __init__(self, driver, wait, base_url):
        self.driver = driver
        self.wait = wait
        self.base_url = base_url
        self.learning_room = LearningRoomPage(driver, wait)

    def open_lesson(self, lesson_id):
        self.learning_room.open(self.base_url, lesson_id)
        return (
            self.learning_room.is_video_player_ready()
            or "/learn/" in self.driver.current_url
        )

    def complete_lesson(self, lesson_id):
        self.learning_room.open(self.base_url, lesson_id)
        if self.learning_room.is_completed():
            return True
        self.learning_room.click_complete()
        from selenium.webdriver.support.ui import WebDriverWait

        try:
            WebDriverWait(self.driver, 10).until(
                lambda d: self.learning_room.is_completed()
            )
            return True
        except:
            return False

    def navigate_lesson_list(self, lesson_id):
        self.learning_room.open(self.base_url, lesson_id)
        return self.learning_room.is_displayed(
            self.learning_room.LESSON_LIST, timeout=3
        )
