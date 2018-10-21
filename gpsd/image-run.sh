#!/bin/sh
docker run -d -p 2948:2948 -p 5001:5001/udp --rm gpsd:testing
