#! /bin/bash

# Set the RPi into it's lowest possible power state. 
# If you want to use HDMI or USB then you probably want to comment this out
/opt/vc/bin/tvservice -o
#echo '1-1' | sudo tee /sys/bus/usb/drivers/usb/unbind
echo 'auto' > '/sys/bus/usb/devices/usb1/power/control'

# Turn off the RPi leds on the board as we have our own.
echo 0 | sudo tee /sys/class/leds/led1/brightness
echo none | sudo tee /sys/class/leds/led0/trigger

# Start the client
cd /home/pi/distribbs || { echo "Failure to change directory"; exit 1; }

# EXPERIMENTAL FOR LoRA hardware only (not required) to allow for TCP/IP networking create the relevant sockets.
#socat pty,raw,echo=0,ignoreof,link=./lax0,iexten=0,nonblock pty,raw,echo=0,ignoreof,link=./rax0,iexten=0,nonblock &
#sleep 3
#kissattach -i 192.168.5.88 -m 192 rax0 ax0

# Finally start the server
java -cp distribbs-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.prowl.distribbs.DistriBBS

