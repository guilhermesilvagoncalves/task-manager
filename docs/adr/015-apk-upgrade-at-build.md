# ADR 015 - Apply Alpine security fixes at image build time

## Context
The runtime images use `eclipse-temurin:25-jre-alpine`. On delivery day the image scan found a critical
vulnerability in `brotli-libs` 1.2.0-r1 from that base image; Alpine already shipped the fix (1.2.1-r0),
but the published base image had not been rebuilt yet. Spec 004 requires every finding fixed or accepted
with a reason and an expiry.

## Decision
The runtime stage of every Dockerfile runs `apk upgrade --no-cache` right after `FROM`, so each build
installs the Alpine security fixes available at that moment.

## Alternatives
- **Wait for a new base image and accept the finding in `osv-scanner.toml`**: reproducible builds, but the
  image ships a known critical vulnerability until upstream rebuilds.
- **Upgrade only the affected package** (`apk add --upgrade brotli-libs`): smallest change, but every new
  finding needs a Dockerfile edit.
- **Pin the base image by digest**: fully reproducible, but OS fixes wait for a manual digest bump.
- **Distroless or another base image**: fewer OS packages to patch, but a different image family to adopt
  and evaluate; out of scope for delivery.

## Consequences
- Builds are not reproducible: two builds of the same commit can have different OS patch levels.
- A rebuild picks up fixes without any code change; `docker compose build --pull` also refreshes the base image.
- An upstream Alpine package change could break the image at build time instead of at the next base update.
