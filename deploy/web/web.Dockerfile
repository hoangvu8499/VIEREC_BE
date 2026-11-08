# Builds the React app, then serves it with nginx (which also terminates HTTPS and proxies /api).
# Build context: the VIEREC-_FE repo cloned next to VIEREC_BE (see docker-compose.yml). nginx.conf is mounted, not baked in.

# ---------- build ----------
FROM node:24-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
# --ignore-scripts: skips the husky "prepare" hook, which needs a .git directory.
RUN npm ci --ignore-scripts
COPY . .
RUN npm run build

# ---------- runtime ----------
FROM nginx:1.28-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist /usr/share/nginx/html
