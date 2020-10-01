# EdisonRobotControl
Can be used on a RPi to control a hoverboard motherboard running [hacked firmware](https://github.com/EmanuelFeru/hoverboard-firmware-hack-FOC).

## Peripherals
The system is built around a Raspberry Pi 3B+ with the following peripherals:
- A hoverboard motherboard, with:
  - the original wheels and battery
  - a serial connection to the RPi
  - [EmanuelFeru's firmware hack](https://github.com/EmanuelFeru/hoverboard-firmware-hack-FOC)
- Raspberry Pi Cam
- 18x WS2812b
  - The first 2 are attached to the side of the robot (flashing warning lights)
  - The other 16 are attached to the front of the robot (headlights)
- 4x DWM1001-DEV
  - 3 of these are placed around the room and configured as anchors
  - 1 goes on the robot and is plugged into the RPi
- HMC5883L magnetometer
  - I2C connecton to the RPi
  
The RPi is powered by a seperate 3S LiPo connected to a buck converter set to 5v. The buck converter has an additional 1000uF capacitor added on the output. The battery also connects to a voltage divider, so the voltage can be also be monitored by the RPi. The main battery voltage is monitored by the motherboard and sent over serial.
