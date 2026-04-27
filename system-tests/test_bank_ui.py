import os
import time

import pytest
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import Select, WebDriverWait


BASE_URL = os.environ.get("WEBTEST_BASE_URL", "http://localhost:8080/bank").rstrip("/")
RUN_ID = str(int(time.time()))

for proxy_var in ("HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "http_proxy", "https_proxy", "all_proxy"):
    os.environ.pop(proxy_var, None)
os.environ["NO_PROXY"] = "localhost,127.0.0.1"
os.environ["no_proxy"] = "localhost,127.0.0.1"


@pytest.fixture(scope="session")
def driver():
    options = Options()
    options.binary_location = os.environ.get("CHROME_BINARY", "/usr/bin/chromium")
    options.add_argument("--headless=new")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1440,1000")
    service = Service(os.environ.get("CHROMEDRIVER", "/usr/bin/chromedriver"))
    browser = webdriver.Chrome(service=service, options=options)
    browser.implicitly_wait(0)
    yield browser
    browser.quit()


@pytest.fixture()
def wait(driver):
    return WebDriverWait(driver, 12)


def open_page(driver, wait, path="/"):
    driver.get(BASE_URL + path)
    wait.until(EC.presence_of_element_located((By.ID, "bank-root")))
    wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, "main")))


def by_test(test_id):
    return By.CSS_SELECTOR, f'[data-testid="{test_id}"]'


def click(driver, wait, test_id):
    element = wait.until(EC.element_to_be_clickable(by_test(test_id)))
    driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", element)
    element.click()
    return element


def set_value(driver, wait, test_id, value):
    element = wait.until(EC.visibility_of_element_located(by_test(test_id)))
    element.clear()
    element.send_keys(value)
    return element


def select_first_option(wait, test_id):
    element = wait.until(EC.presence_of_element_located(by_test(test_id)))
    wait.until(lambda _driver: len(Select(element).options) > 1)
    Select(element).select_by_index(1)
    return element


def select_value(wait, test_id, value):
    element = wait.until(EC.presence_of_element_located(by_test(test_id)))
    Select(element).select_by_value(value)
    return element


def notice_text(wait):
    return wait.until(EC.visibility_of_element_located(by_test("notice"))).text


def click_link_by_text(driver, wait, text):
    link = wait.until(EC.element_to_be_clickable((By.PARTIAL_LINK_TEXT, text)))
    driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", link)
    link.click()


def test_dashboard_and_department_filter(driver, wait):
    open_page(driver, wait, "/")
    assert "Сводка банка" in driver.page_source
    click(driver, wait, "nav-departments")
    wait.until(EC.url_contains("/departments"))
    set_value(driver, wait, "departments-search", "Central")
    driver.find_element(By.CSS_SELECTOR, "form button[type='submit']").click()
    wait.until(lambda _driver: "Central Branch" in driver.page_source)


def test_department_validation_success_and_delete_block(driver, wait):
    open_page(driver, wait, "/departments/new")
    click(driver, wait, "save-department")
    assert "Название отделения обязательно" in notice_text(wait)

    name = f"QA Branch {RUN_ID}"
    set_value(driver, wait, "department-name-input", name)
    click(driver, wait, "save-department")
    wait.until(EC.url_contains("/departments/"))
    wait.until(lambda _driver: name in driver.page_source)

    open_page(driver, wait, "/departments")
    click_link_by_text(driver, wait, "Central Branch")
    click(driver, wait, "delete-department")
    assert "Нельзя удалить отделение" in notice_text(wait)


def test_client_validation_success_and_delete_block(driver, wait):
    open_page(driver, wait, "/clients/new")
    click(driver, wait, "save-client")
    assert "Выберите отделение клиента" in notice_text(wait)

    select_first_option(wait, "client-department-select")
    name = f"QA Client {RUN_ID}"
    set_value(driver, wait, "client-name-input", name)
    click(driver, wait, "save-client")
    wait.until(EC.url_contains("/clients/"))
    wait.until(lambda _driver: name in driver.page_source)

    open_page(driver, wait, "/clients")
    set_value(driver, wait, "clients-search", "Ivanov")
    driver.find_element(By.CSS_SELECTOR, "form button[type='submit']").click()
    wait.until(lambda _driver: "Ivanov Ivan" in driver.page_source)
    click_link_by_text(driver, wait, "Ivanov")
    click(driver, wait, "delete-client")
    assert "Нельзя удалить клиента" in notice_text(wait)


def create_saving_account(driver, wait, suffix):
    open_page(driver, wait, "/accounts/new")
    select_first_option(wait, "account-client-select")
    select_first_option(wait, "account-currency-select")
    select_value(wait, "account-type-select", "saving")
    set_value(driver, wait, "account-number-input", f"QA-ACC-{RUN_ID}-{suffix}")
    set_value(driver, wait, "saving-rate-input", "4.5")
    set_value(driver, wait, "saving-limit-input", "10000")
    click(driver, wait, "save-account")
    wait.until(EC.url_matches(r".*/accounts/[0-9]+$"))
    wait.until(lambda _driver: f"QA-ACC-{RUN_ID}-{suffix}" in driver.page_source)
    return driver.current_url.rsplit("/", 1)[-1]


def test_account_open_close_operation_results(driver, wait):
    zero_account_id = create_saving_account(driver, wait, "zero")
    click(driver, wait, "close-account")
    assert "Счет закрыт" in notice_text(wait)

    account_id = create_saving_account(driver, wait, "ops")
    click(driver, wait, "debit-account")
    set_value(driver, wait, "operation-amount-input", "9999")
    click(driver, wait, "save-operation")
    assert "Недостаточно средств" in notice_text(wait)

    select_value(wait, "operation-kind-select", "credit")
    set_value(driver, wait, "operation-amount-input", "25")
    set_value(driver, wait, "operation-description-input", "QA credit")
    click(driver, wait, "save-operation")
    wait.until(EC.url_contains(f"/accounts/{account_id}"))
    wait.until(lambda _driver: "25" in driver.page_source)

    click(driver, wait, "close-account")
    assert "ненулевым балансом" in notice_text(wait)
