
set -e
cd "$(dirname "$0")"

echo "1. Загрузка JAR в lib/..."
sh download-libs.sh

[ -f db.properties ] || {
  echo "2. Создание db.properties из примера..."
  cp db.properties.example db.properties
}

if command -v ant >/dev/null 2>&1; then
  echo "3. Полная Ant-сборка: БД, DAO-тесты, React/Tailwind, WAR, deploy..."
  ant all
elif [ -x ./gradlew ]; then
  echo "3. Инициализация БД и запуск DAO-тестов (./gradlew all)..."
  GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PWD/.gradle-userhome}" ./gradlew all
elif command -v gradle >/dev/null 2>&1; then
  echo "3. Инициализация БД и запуск DAO-тестов (gradle all)..."
  gradle all
else
  echo "3. Ant не найден, используем psql + javac + java..."
  DB_HOST=$(grep '^db.host=' db.properties 2>/dev/null | cut -d= -f2); DB_HOST=${DB_HOST:-localhost}
  DB_PORT=$(grep '^db.port=' db.properties 2>/dev/null | cut -d= -f2); DB_PORT=${DB_PORT:-5432}
  DB_USER=$(grep '^db.user=' db.properties 2>/dev/null | cut -d= -f2); DB_USER=${DB_USER:-postgres}
  DB_PASS=$(grep '^db.pass=' db.properties 2>/dev/null | cut -d= -f2); DB_PASS=${DB_PASS:-postgres}
  DB_NAME=$(grep '^db.name=' db.properties 2>/dev/null | cut -d= -f2); DB_NAME=${DB_NAME:-bankdb}
  export PGPASSWORD="$DB_PASS"
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -f sql/create_db.sql 2>/dev/null || true
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f sql/drop_schema.sql 2>/dev/null || true
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f sql/schema.sql
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f sql/seed.sql
  unset PGPASSWORD
  echo "4. Компиляция..."
  mkdir -p build/classes build/test-classes
  javac -cp "lib/*" -d build/classes -sourcepath src $(find src -name "*.java")
  cp src/hibernate.cfg.xml build/classes/ 2>/dev/null || true
  javac -cp "lib/*:build/classes" -d build/test-classes -sourcepath test $(find test -name "*.java")
  echo "5. Запуск тестов..."
  java -cp "lib/*:build/classes:build/test-classes" \
    -Dhibernate.connection.url="jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME" \
    -Dhibernate.connection.username="$DB_USER" \
    -Dhibernate.connection.password="$DB_PASS" \
    org.testng.TestNG -d build/test-reports test/testng.xml
fi

echo "Готово."
