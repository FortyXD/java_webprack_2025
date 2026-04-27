# WEB PRAC

Работа выполнена Моисейкиным Андреем Денисовичем 328

## Этап 3: Spring MVC Web, React/Tailwind и Selenium

### Быстрый запуск Web

```bash
sh download-libs.sh
npm install
ant init
ant run-web
```

Приложение будет доступно по адресу `http://localhost:8080/bank/`.

### Сборка и тесты

- `cp db.properties.example db.properties` - настройка подключения к БД
- `gradle initDb` - создать БД/схему, очистить и заполнить данными
- `gradle compileJava` - компиляция проекта
- `gradle test` - запуск тест
- `ant war` - сборка WAR с JSP, Spring MVC и React/Tailwind assets
- `ant deploy` - копирование WAR в `build/deploy/bank.war`
- `ant run-web` - запуск на встроенном Tomcat
- `ant system-test` - системные тесты Selenium против `http://localhost:8080/bank/`
- `ant all` - init + DAO-тесты + WAR + deploy

Скрипты:
- `./run-all.sh ` - проверка всего с установкой
- `./run-tests.sh` - проверка чисто тестов
- Можно и `ant all`, он тоже работает

Описание Selenium-сценариев: `docs/system-test-scenarios.md`.

Полный отчет и подробная инструкция запуска: `docs/report-and-run.md`.
