FROM nginx:alpine

COPY index.html /usr/share/nginx/html/index.html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

ENV BACKEND_HOST=agen46-dev

EXPOSE 80
