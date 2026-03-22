# WEB PRAC

Работа выполнена Моисейкиным Андреем Денисовичем 328

## Этап 2: Hibernate, DAO, тесты

### Запуск всего в одном 

```bash
gradle all
```


- `cp db.properties.example db.properties` - настройка подключения к БД
- `gradle initDb` - создать БД/схему, очистить и заполнить данными
- `gradle compileJava` - компиляция проекта
- `gradle test` - запуск тест
- `gradle all` - всё в одном

Скрипты:
- `./run-all.sh ` - проверка всего с установкой
- `./run-tests.sh` - проверка чисто тестов
Можно и ant all он тоже работает