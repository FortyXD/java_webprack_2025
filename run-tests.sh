set -e
cd "$(dirname "$0")"

if ! ls lib/*.jar >/dev/null 2>&1; then
  echo "No JAR dependencies found in lib/, downloading..."
  sh download-libs.sh
fi

if [ -x ./gradlew ]; then
  exec env GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PWD/.gradle-userhome}" ./gradlew test
fi

if command -v gradle >/dev/null 2>&1; then
  exec gradle test
fi

DB_HOST=$(grep '^db.host=' db.properties 2>/dev/null | cut -d= -f2); DB_HOST=${DB_HOST:-localhost}
DB_PORT=$(grep '^db.port=' db.properties 2>/dev/null | cut -d= -f2); DB_PORT=${DB_PORT:-5432}
DB_USER=$(grep '^db.user=' db.properties 2>/dev/null | cut -d= -f2); DB_USER=${DB_USER:-postgres}
DB_PASS=$(grep '^db.pass=' db.properties 2>/dev/null | cut -d= -f2); DB_PASS=${DB_PASS:-postgres}
DB_NAME=$(grep '^db.name=' db.properties 2>/dev/null | cut -d= -f2); DB_NAME=${DB_NAME:-bankdb}

mkdir -p build/classes build/test-classes build/test-reports

javac -cp "lib/*" -d build/classes -sourcepath src $(find src -name "*.java")
cp src/hibernate.cfg.xml build/classes/ 2>/dev/null || true
javac -cp "lib/*:build/classes" -d build/test-classes -sourcepath test $(find test -name "*.java")

exec java -cp "lib/*:build/classes:build/test-classes" \
  -Dhibernate.connection.url="jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME" \
  -Dhibernate.connection.username="$DB_USER" \
  -Dhibernate.connection.password="$DB_PASS" \
  org.testng.TestNG -d build/test-reports test/testng.xml
