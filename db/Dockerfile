FROM mysql/mysql-server:latest
COPY world.sql /docker-entrypoint-initdb.d
ENV MYSQL_ROOT_PASSWORD example
#Yet another Mac Fix
ENV MYSQL_ROOT_HOST=%