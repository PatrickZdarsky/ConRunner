# =========================================
# Stage 1: Build the Angular Application
# =========================================
ARG NODE_VERSION=24.7.0-alpine
ARG NGINX_VERSION=alpine3.22

# Use a lightweight Node.js image for building (customizable via ARG)
FROM node:${NODE_VERSION} AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy package-related files first to leverage Docker's caching mechanism
COPY package.json package-lock.json ./

ENV CYPRESS_INSTALL_BINARY=0
# Install project dependencies using npm ci (ensures a clean, reproducible install)
RUN --mount=type=cache,target=/root/.npm npm ci

# Copy the rest of the application source code into the container
COPY . .

# Build the Angular application
RUN npm run build

# =========================================
# Stage 2: Prepare Nginx to Serve Static Files
# =========================================

FROM nginxinc/nginx-unprivileged:${NGINX_VERSION} AS runner

USER root

# Copy entrypoint
COPY entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

# Install sed only
RUN apk add --no-cache sed bash
# Fix permissions for runtime replacement
RUN chown -R nginx:nginx /usr/share/nginx/html \
    && chmod -R u+w /usr/share/nginx/html

# Change to nginx user
USER 101

# Copy nginx config
COPY nginx.conf /etc/nginx/nginx.conf

# Copy Angular build
COPY --from=builder /app/dist/*/browser /usr/share/nginx/html


EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]

CMD ["nginx", "-c", "/etc/nginx/nginx.conf", "-g", "daemon off;"]
