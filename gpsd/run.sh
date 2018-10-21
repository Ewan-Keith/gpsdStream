#!/bin/sh
gpsd -G -n -N -F /tmp/gpsd.sock -D $debugLevel -S $outputPort $inputProtocol://0.0.0.0:$inputPort
