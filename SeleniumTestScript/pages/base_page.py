from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.common.exceptions import TimeoutException, NoSuchElementException
import logging

logger = logging.getLogger(__name__)


class BasePage:
    def __init__(self, driver, wait=None):
        self.driver = driver
        self.wait = wait or WebDriverWait(driver, 10)

    def open(self, url):
        self.driver.get(url)

    def find_element(self, locator, timeout=None):
        wait = WebDriverWait(self.driver, timeout) if timeout else self.wait
        return wait.until(EC.presence_of_element_located(locator))

    def find_elements(self, locator):
        return self.driver.find_elements(*locator)

    def click(self, locator, timeout=None):
        wait = WebDriverWait(self.driver, timeout) if timeout else self.wait
        element = wait.until(EC.element_to_be_clickable(locator))
        element.click()
        return element

    def enter_text(self, locator, text, clear_first=True):
        element = self.find_element(locator)
        if clear_first:
            element.clear()
        element.send_keys(text)
        return element

    def get_text(self, locator):
        return self.find_element(locator).text

    def is_displayed(self, locator, timeout=5):
        try:
            wait = WebDriverWait(self.driver, timeout)
            element = wait.until(EC.presence_of_element_located(locator))
            return element.is_displayed()
        except TimeoutException:
            return False

    def wait_for_url_contains(self, text, timeout=10):
        wait = WebDriverWait(self.driver, timeout)
        return wait.until(EC.url_contains(text))

    def wait_for_url_changes(self, original_url, timeout=10):
        wait = WebDriverWait(self.driver, timeout)
        return wait.until(EC.url_changes(original_url))

    def execute_script(self, script, *args):
        return self.driver.execute_script(script, *args)

    def take_screenshot(self, name="screenshot"):
        self.driver.save_screenshot(f"{name}.png")

    @property
    def current_url(self):
        return self.driver.current_url

    @property
    def title(self):
        return self.driver.title
