import os
import pytest
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
from config.config import Config


def pytest_addoption(parser):
    parser.addoption(
        "--base-url",
        action="store",
        default=Config.BASE_URL,
        help="Base URL of the AUT",
    )
    parser.addoption(
        "--headless",
        action="store_true",
        default=Config.HEADLESS,
        help="Run browser in headless mode",
    )


@pytest.fixture(scope="session")
def base_url(request):
    return request.config.getoption("--base-url")


@pytest.fixture(scope="function")
def driver(request):
    options = Options()
    options.add_argument("--window-size=1440,900")
    options.add_argument("--ignore-certificate-errors")
    options.add_argument("--disable-blink-features=AutomationControlled")
    options.add_experimental_option("excludeSwitches", ["enable-automation"])
    options.add_experimental_option("useAutomationExtension", False)

    if request.config.getoption("--headless") or os.getenv("HEADLESS", "0") == "1":
        options.add_argument("--headless=new")

    service = Service(ChromeDriverManager().install())
    browser = webdriver.Chrome(service=service, options=options)
    browser.implicitly_wait(Config.IMPLICIT_WAIT)

    yield browser

    browser.quit()


@pytest.fixture(scope="function")
def wait(driver):
    from selenium.webdriver.support.ui import WebDriverWait

    return WebDriverWait(driver, Config.TIMEOUT)


@pytest.fixture(scope="session")
def test_users():
    return {
        "student": {
            "email": os.getenv("TEST_STUDENT_EMAIL", "studentnew@test.com"),
            "password": os.getenv("TEST_STUDENT_PASSWORD", "Student123!"),
            "role": "STUDENT",
        },
        "teacher": {
            "email": os.getenv("TEST_TEACHER_EMAIL", "teacher@test.com"),
            "password": os.getenv("TEST_TEACHER_PASSWORD", "Teacher123!"),
            "role": "TEACHER",
        },
        "admin": {
            "email": os.getenv("TEST_ADMIN_EMAIL", "admin@gmail.com"),
            "password": os.getenv("TEST_ADMIN_PASSWORD", "admin@gmail.com"),
            "role": "ADMIN",
        },
    }
