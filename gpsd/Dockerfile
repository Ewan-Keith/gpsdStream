FROM alpine:3.8

RUN apk add --no-cache gpsd
RUN addgroup -g 1000 -S gpsd && \
    adduser -u 1000 -S gpsd -G gpsd

USER gpsd

COPY ./run.sh .

ENV outputPort="2948"
ENV inputPort="5001"
ENV inputProtocol="udp"
ENV debugLevel="7"

CMD ./run.sh
