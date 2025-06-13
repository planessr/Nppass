FROM node:20.19.0 AS builder
ARG VUE_APP_API_URL
ARG VUE_APP_WS_URL
ENV WORKDIR=/app
ENV VUE_APP_API_URL=${VUE_APP_API_URL}
ENV VUE_APP_WS_URL=${VUE_APP_WS_URL}
WORKDIR $WORKDIR
COPY . .
RUN npm install && \
    npm run build && \
    echo "window.APP_CONFIG={VUE_APP_API_URL:\"$VUE_APP_API_URL\",VUE_APP_WS_URL:\"$VUE_APP_WS_URL\"}" > dist/config.js && \
    echo "Compilation stage completed"

FROM nginx AS runner
COPY --from=builder /app/dist /app/dist
COPY --from=builder /app/nginx.conf /etc/nginx/conf/nginx.conf
COPY --from=builder /app/default.conf /etc/nginx/conf.d/default.conf
RUN apt install tzdata && \
    mkdir -p /var/log && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    chmod -R 755 /app
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]