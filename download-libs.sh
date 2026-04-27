set -e
cd "$(dirname "$0")"
mkdir -p lib

BASE="https://repo1.maven.org/maven2"
get() {
  g="$1"; a="$2"; v="$3"
  path=$(echo "$g" | tr . /)
  if [ -s "lib/$a-$v.jar" ]; then
    echo "SKIP $a"
    return
  fi
  curl -fsSL -o "lib/$a-$v.jar" "$BASE/$path/$a/$v/$a-$v.jar"
  echo "OK $a"
}

get "org.postgresql" "postgresql" "42.7.3"
get "org.hibernate.orm" "hibernate-core" "6.2.33.Final"
get "jakarta.persistence" "jakarta.persistence-api" "3.1.0"
get "jakarta.transaction" "jakarta.transaction-api" "2.0.1"
get "net.bytebuddy" "byte-buddy" "1.14.10"
get "org.jboss.logging" "jboss-logging" "3.5.3.Final"
get "com.fasterxml" "classmate" "1.6.0"
get "org.antlr" "antlr4-runtime" "4.13.1"
get "io.smallrye" "jandex" "3.0.5"
get "org.hibernate.common" "hibernate-commons-annotations" "6.0.6.Final"
get "org.testng" "testng" "7.10.2"
get "com.beust" "jcommander" "1.82"
get "org.slf4j" "slf4j-api" "2.0.9"
get "org.slf4j" "slf4j-simple" "2.0.9"
get "org.yaml" "snakeyaml" "2.2"
get "jakarta.xml.bind" "jakarta.xml.bind-api" "4.0.2"
get "org.glassfish.jaxb" "jaxb-runtime" "4.0.2"
get "org.glassfish.jaxb" "jaxb-core" "4.0.2"
get "com.sun.istack" "istack-commons-runtime" "4.1.2"
get "org.eclipse.angus" "angus-activation" "2.0.1"
get "jakarta.activation" "jakarta.activation-api" "2.1.3"

# Spring MVC / Jakarta Web / embedded Tomcat for the JSP web interface.
get "org.springframework" "spring-core" "6.1.14"
get "org.springframework" "spring-jcl" "6.1.14"
get "org.springframework" "spring-beans" "6.1.14"
get "org.springframework" "spring-context" "6.1.14"
get "org.springframework" "spring-aop" "6.1.14"
get "org.springframework" "spring-expression" "6.1.14"
get "org.springframework" "spring-web" "6.1.14"
get "org.springframework" "spring-webmvc" "6.1.14"
get "io.micrometer" "micrometer-observation" "1.12.11"
get "io.micrometer" "micrometer-commons" "1.12.11"
get "jakarta.annotation" "jakarta.annotation-api" "2.1.1"
get "jakarta.servlet" "jakarta.servlet-api" "6.0.0"
get "jakarta.servlet.jsp" "jakarta.servlet.jsp-api" "3.1.1"
get "org.apache.tomcat.embed" "tomcat-embed-core" "10.1.34"
get "org.apache.tomcat.embed" "tomcat-embed-jasper" "10.1.34"
get "org.apache.tomcat.embed" "tomcat-embed-el" "10.1.34"
get "org.apache.tomcat.embed" "tomcat-embed-websocket" "10.1.34"
get "org.apache.tomcat" "tomcat-annotations-api" "10.1.34"
get "org.eclipse.jdt" "ecj" "3.39.0"

echo "Done. lib/ contains $(ls lib/*.jar 2>/dev/null | wc -l) JARs"
