FROM maven:3.6.3-jdk-8 AS build

MAINTAINER mjj

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:UTF-8
ENV LC_ALL en_US.UTF-8

WORKDIR /src
RUN git clone https://github.com/6mb/Microsoft-365-Admin.git \
    && cd Microsoft-365-Admin \
    && mvn package -Dmaven.test.skip=true

# microsoft
FROM logr/8-jre-alpine
COPY --from=build /src/Microsoft-365-Admin/target/microsoft-365-admin-*-RELEASE.jar .
RUN mv microsoft-365-admin-*-RELEASE.jar microsoft.jar

#执行
CMD java -jar microsoft.jar --spring.profile.active=dev
