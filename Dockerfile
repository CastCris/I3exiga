# I3exiga IFSP - Tomcat 10.1 (Jakarta EE 10) + JDK 17.
# O build usa apenas o JDK da própria imagem do Tomcat: não precisa de Maven, Gradle nem de bibliotecas externas.
ARG TOMCAT_IMAGE=tomcat:10.1-jdk17

# ---------- Etapa 1: compila as classes Java ----------
FROM ${TOMCAT_IMAGE} AS build
WORKDIR /build
COPY I3exiga/src/main/java ./java
COPY I3exiga/src/main/resources ./resources
COPY I3exiga/src/main/webapp ./webapp
RUN mkdir -p webapp/WEB-INF/classes \
 && javac -encoding UTF-8 --release 17 \
      -cp "/usr/local/tomcat/lib/servlet-api.jar:/usr/local/tomcat/lib/jsp-api.jar:/usr/local/tomcat/lib/el-api.jar" \
      -d webapp/WEB-INF/classes $(find java -name '*.java') \
 && cp -r resources/. webapp/WEB-INF/classes/

# ---------- Etapa 2: imagem final ----------
FROM ${TOMCAT_IMAGE}

# Compiladores e interpretadores usados pelo avaliador (C, C++, Python; o JDK já vem na imagem),
# curl para o healthcheck e o usuário sem privilégios "judge", que executa o código dos usuários.
RUN apt-get update \
 && apt-get install -y --no-install-recommends gcc g++ libc6-dev python3 curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --no-create-home --shell /usr/sbin/nologin judge \
 && rm -rf /usr/local/tomcat/webapps/* \
 && mkdir -p /data /opt/i3exiga \
 && chmod 700 /data /usr/local/tomcat

COPY I3exiga/docker/sandbox.sh /opt/i3exiga/sandbox.sh
RUN chmod 755 /opt/i3exiga/sandbox.sh
COPY --from=build /build/webapp /usr/local/tomcat/webapps/ROOT

ENV I3EXIGA_DATA_DIR=/data \
    I3EXIGA_JUDGE_DIR=/tmp/i3exiga-judge \
    I3EXIGA_SANDBOX_USER=judge \
    I3EXIGA_SANDBOX_SCRIPT=/opt/i3exiga/sandbox.sh

VOLUME /data
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/login > /dev/null || exit 1
