# Chen

Chen is the WebDB connection component of JumpServer, supporting multiple database protocols.

Chen is implemented in Java, and its name is derived from the Dota hero [Chen](https://www.dota2.com/hero/chen).

# UI Showcase

![UI Showcase](https://download.jumpserver.org/images/chen.png)

## Repository Layout

- **`backend/`** — Java 21, Maven multi-module (root `pom.xml` has a single module,
  `backend`, which in turn has four sub-modules):
  - `wisp` — a generated gRPC client (`ServiceGrpc`, `ServiceOuterClass`) for the
    external [wisp](https://github.com/jumpserver/wisp) session-recording/audit
    service. Not something this fork builds — just a protocol client bundled in.
  - `framework` — the core engine: session lifecycle (`SessionManager`, primary
    WebSocket claim/release), SQL execution and paging (`BaseSQLActuator`,
    `PageUtils`), data masking, the query console, and export/code utilities.
  - `modules` — one package per supported database engine (`oracle`, `sqlserver`,
    `mysql`, …), plus `base/ssl` for client-certificate/JKS handling shared across
    engines.
  - `web` — the Spring Boot web layer: REST controllers (`AuthController`,
    `ConsoleController`), the WebSocket handshake/session interceptor, and
    i18n/locale config. Runs on port `8082` under context path `/chen`
    (`config/application.yml`).
- **`frontend/`** — a Vue 2 SPA (`vue-cli-service`, `yarn serve` / `yarn build`):
  the query console UI (`components/Main/Explore` — SQL editor, result grid,
  data view), the resource tree (`components/Main/ResourceManage`), and i18n
  support (`en` default, plus `zh-CN`/`zh-Hant`/`ja` catalogs under `src/i18n`).
- **`drivers/`** — JDBC driver JARs bundled per supported database engine (e.g.
  `mysql-connector-j-*.jar`).
- **`Dockerfile` / `Dockerfile-base` / `Dockerfile-ee`** — `Dockerfile-base` builds
  a dependency-cached base image; `Dockerfile` (Community Edition) builds `FROM`
  that base; `Dockerfile-ee` is the Enterprise Edition variant and requires a
  private FIT2CLOUD registry this fork has no access to — left as-is, not usable
  here.
- **`.github/workflows/`** — CI publishes to GHCR under this fork's own namespace:
  `build-base-image.yml` → `ghcr.io/matheus-marques-ft/chen-base`,
  `build-release-image.yml` (tag-triggered, `v*`) → `ghcr.io/matheus-marques-ft/chen`.
  That final image is what `js-installer`'s `compose/chen.yml` pulls as
  `${NAMESPACE}/chen:${VERSION}`.

## Supported Features

- [x] Security Authentication
- [x] SQL Filtering
- [x] SQL Recording
- [x] SQL Blocking

## Supported Databases

- [x] MySQL 5.7/8.0+
- [x] MariaDB
- [x] PostgreSQL (X-Pack)
- [x] SQL Server (X-Pack)
- [x] Oracle (X-Pack)
- [x] DB2 (X-Pack)


