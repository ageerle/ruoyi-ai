# FFmpeg-enabled variant of ruoyi-admin/Dockerfile.
FROM bellsoft/liberica-openjdk-rocky:17.0.16-cds

LABEL maintainer="Lion Li"

# Rocky base repositories do not provide the full codec build. RPM Fusion Free
# supplies ffmpeg with libx264, while the checks below prevent a reduced build
# from reaching production unnoticed.
RUN set -eux; \
    dnf -y install dnf-plugins-core epel-release; \
    . /etc/os-release; \
    rocky_major="${VERSION_ID%%.*}"; \
    if [ "${rocky_major}" -ge 9 ]; then \
        dnf config-manager --set-enabled crb; \
    else \
        dnf config-manager --set-enabled powertools; \
    fi; \
    dnf -y install "https://mirrors.rpmfusion.org/free/el/rpmfusion-free-release-${rocky_major}.noarch.rpm"; \
    dnf -y install ffmpeg; \
    for filter in scale pad fps setsar format tpad trim settb setpts xfade \
        aresample aformat apad atrim asetpts anullsrc concat acrossfade; do \
        ffmpeg -hide_banner -filters 2>/dev/null | grep -Eq "[[:space:]]${filter}[[:space:]]"; \
    done; \
    ffmpeg -hide_banner -h filter=xfade 2>&1 | grep -q dissolve; \
    ffmpeg -hide_banner -h filter=xfade 2>&1 | grep -q fadeblack; \
    ffmpeg -hide_banner -h filter=xfade 2>&1 | grep -q slideleft; \
    ffmpeg -hide_banner -encoders 2>/dev/null | grep -Eq '[[:space:]]libx264[[:space:]]'; \
    ffmpeg -hide_banner -encoders 2>/dev/null | grep -Eq '[[:space:]]aac[[:space:]]'; \
    ffprobe -hide_banner -version >/dev/null; \
    dnf clean all; \
    rm -rf /var/cache/dnf

RUN mkdir -p /ruoyi/server/logs \
    /ruoyi/server/temp \
    /ruoyi/skywalking/agent

WORKDIR /ruoyi/server

ENV SERVER_PORT=8080 \
    SNAIL_PORT=28080 \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_OPTS="" \
    FFMPEG_PATH=/usr/bin/ffmpeg \
    FFPROBE_PATH=/usr/bin/ffprobe

EXPOSE ${SERVER_PORT}
EXPOSE ${SNAIL_PORT}

ADD ./target/ruoyi-admin.jar ./app.jar

SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom \
           -Djava.io.tmpdir=/ruoyi/server/temp \
           -Dserver.port=${SERVER_PORT} \
           -Dsnail-job.port=${SNAIL_PORT} \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar
