FROM ubuntu:24.04

RUN apt-get update && apt-get install -y openjdk-11-jre

WORKDIR /opt/domibus

COPY . .

EXPOSE 8080

ENTRYPOINT [ "sh", "-c","/opt/domibus/bin/startup.sh && touch /opt/domibus/logs/domibus.log && tail -f /opt/domibus/logs/domibus.log" ]
