# Edison Robot Control
[![Build Status](https://github.com/MisterMelDev/EdisonRobotControl/workflows/Build/badge.svg)](https://github.com/MisterMelDev/EdisonRobotControl/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=MisterMelDev_EdisonRobotControl&metric=alert_status)](https://sonarcloud.io/dashboard?id=MisterMelDev_EdisonRobotControl)

Control system for a robot built on components from an old hoverboard

## Features
- Web interface
- Camera stream
- Lighting system
- Manual control
- Autonomous control

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
- BNO055 9-DoF IMU
  - I2C connecton to the RPi
  
The RPi is powered by a seperate 3S LiPo connected to a buck converter set to 5v. The buck converter has an additional 1000uF capacitor added on the output. The main battery voltage is monitored by the motherboard and sent over serial.
