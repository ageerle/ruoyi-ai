# syntax=docker/dockerfile:1.7
# Both upstream stages are official images pinned to the linux/amd64 manifest digest observed
# on 2026-08-30. The final scratch stage intentionally drops inherited image metadata such as
# VOLUME, CMD, ENTRYPOINT, healthcheck and environment before adding a minimal allowlist.
FROM node:24-bookworm@sha256:9137a20e25879e0b557227b57e3ee4e9af4bde29eb3db66134cd1723e84f830b AS node
FROM maven:3.9.16-eclipse-temurin-21-noble@sha256:edf045813426842617b1667456ddec0026885146465e890b897655d877ba3386 AS assembly

USER root
COPY --from=node /usr/local/ /usr/local/
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates git python3 ripgrep \
    && rm -rf /var/lib/apt/lists/* \
    && rm -f /usr/local/bin/pnpm /usr/local/bin/pnpx \
    && corepack enable pnpm \
    && corepack prepare pnpm@10.14.0 --activate \
    && groupadd --gid 65532 harness \
    && useradd --uid 65532 --gid 65532 --no-create-home --home-dir /tmp \
        --shell /usr/sbin/nologin harness \
    && java -version \
    && mvn -version \
    && node --version \
    && npm --version \
    && pnpm --version \
    && python3 --version \
    && rg --version \
    && git --version

FROM scratch
COPY --from=assembly / /
ENV PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
    JAVA_HOME=/opt/java/openjdk \
    MAVEN_HOME=/usr/share/maven \
    HOME=/tmp \
    LANG=C.UTF-8
USER 65532:65532
WORKDIR /workspace
ENTRYPOINT []
CMD []
