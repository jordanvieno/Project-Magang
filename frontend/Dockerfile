FROM nginx:alpine
# ponytail: nginx needs root to bind port 80; switch to nginxinc/nginx-unprivileged if non-root needed
COPY index.html /usr/share/nginx/html/index.html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
ENV BACKEND_HOST=agen46-dev
ENV BACKEND_PORT=8080
EXPOSE 80
