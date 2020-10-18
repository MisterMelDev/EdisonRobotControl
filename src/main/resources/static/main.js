/* WebSocket */
const socket = new WebSocket("ws://" + window.location.hostname + (window.location.port ? ":" + window.location.port : ""));
let webSocketOpened = false;

socket.addEventListener("open", function(event) {
    console.log("WebSocket opened");
    hideConnectionError();
    webSocketOpened = true;
});

socket.addEventListener("close", function(event) {
    console.log("WebSocket closed");
    console.log(event);

    if(!webSocketOpened) {
        setConnectionError("Failed to connect to server");
        return;
    }

    setConnectionError(event.reason == "" ? "Connection to server was interrupted" : "Connection to server was interrupted: " + event.reason);
});

socket.addEventListener("error", function(event) {
    console.log("WebSocket error");
    setConnectionError("A WebSocket error occurred");
});

socket.addEventListener("message", function(event) {
    let json = JSON.parse(event.data);
    let msgType = json.type;

    if(msgType == "telemetry") {
        batteryVoltageElement.innerHTML = json.voltage.toFixed(2) + "v";
        boardTemperatureElement.innerHTML = json.temp.toFixed(1) + " &#8451;";
        return;
    }

    if(msgType == "checkboxes") {
        lightsToggle.checked = json.isLightingEnabled;
        streamToggle.checked = json.isStreamEnabled;
        return;
    }

    if(msgType == "nav") {
        setCanvasInfo(json.pos[0], json.pos[1], json.pos[2], json.t);
        setRawAcceleration(json.acc);
        setRawHeading(json.pos[2]);
        setRawNavigationParams(json.params);
        return;
    }

    if(msgType == "imu_calib") {
        setRawCalib(json.calibStatuses);
        return;
    }

    if(msgType == "nav_toggle") {
        setMovementEnabled(json.enabled, false);
        return;
    }

    if(msgType == "nav_waypoints") {
        waypoints = json.waypoints;
        waypointLength = waypoints.size;
        return;
    }

    if(msgType == "health") {
        handleHealthPacket(json);
        return;
    }

    if(msgType == "route") {
        curvePoints = json.curve_points;
        return;
    }
});

setInterval(function() {
    if(socket.readyState == WebSocket.OPEN) {
        socket.send(JSON.stringify({type: "heartbeat"}));
    }
}, 3000);

/* Raw data section */

const rawHeading = document.getElementById("raw-heading");
const rawCalib = document.getElementById("raw-calib");

const rawAccX = document.getElementById("raw-acc-x");
const rawAccY = document.getElementById("raw-acc-y");
const rawAccZ = document.getElementById("raw-acc-z");

const rawCte = document.getElementById("raw-cte");
const rawSteerFactor = document.getElementById("raw-steer-factor");
const rawSteer = document.getElementById("raw-steer");
const rawSpeed = document.getElementById("raw-speed");

function setRawHeading(heading) {
    rawHeading.innerHTML = heading;
}

function setRawCalib(calibData) {
    rawCalib.innerHTML = "sys: " + calibData[0] + ", gyro: " + calibData[1] + ", acc: " + calibData[2] + ", mag: " + calibData[3];
}

function setRawAcceleration(rawAcc) {
    rawAccX.innerHTML = rawAcc ? awAcc[0].toFixed(1) : 0;
    rawAccY.innerHTML = rawAcc ? awAcc[1].toFixed(1) : 0;
    rawAccZ.innerHTML = rawAcc ? awAcc[2].toFixed(1) : 0;
}

function setRawNavigationParams(params) {
    rawCte.innerHTML = params[0].toFixed(3);
    rawSteerFactor.innerHTML = params[1].toFixed(2);
    rawSteer.innerHTML = params[2];
    rawSpeed.innerHTML = params[3];
}

/* Stream */
const streamPort = 8080;
const cameraStreamElement = document.getElementById("camera-stream");

function showStream(enabled) {
    cameraStreamElement.src = enabled ? "http://" + window.location.hostname + ":" + streamPort + "/?action=stream" : "img-fail.png";
}

function setStreamSmall(enabled) {
    cameraStreamElement.width = enabled ? 1040 : 1280;
    cameraStreamElement.height = enabled ? 585 : 720;
}

showStream(true);
setStreamSmall(false);

/* Modals */
let modalActive = false;

const connectionErrorModal = document.getElementById("connection-error-modal");
const connectionErrorMsg = document.getElementById("connection-error-msg");

function setConnectionError(errorMsg) {
    connectionErrorModal.style.display = "block";
    connectionErrorMsg.innerHTML = errorMsg;
    modalActive = true;
}

function hideConnectionError() {
    connectionErrorModal.style.display = "none";
    modalActive = false;
}

const confirmationModal = document.getElementById("confirmation-modal");
const confirmationModalQuestion = document.getElementById("confirmation-modal-question");
let confirmationModalCallback = null;

function showConfirmationModal(question, callback) {
    confirmationModal.style.display = "block";
    confirmationModalQuestion.innerHTML = question;
    confirmationModalCallback = callback;
    modalActive = true;
}

function hideConfirmationModal() {
    confirmationModal.style.display = "none";
    modalActive = false;
}

document.getElementById("confirmation-modal-cancel").addEventListener("click", hideConfirmationModal);
document.getElementById("confirmation-modal-confirm").addEventListener("click", function(e) {
    confirmationModalCallback();
    hideConfirmationModal();
});

const wifiModal = document.getElementById("wifi-modal");
const wifiModalConfigSelect = document.getElementById("wifi-modal-configuration-select");
const wifiModalSsid = document.getElementById("wifi-modal-ssid");
const wifiModalPassword = document.getElementById("wifi-modal-password");

let wifiConfigurations = null;

document.getElementById("wifi-btn").addEventListener("click", function(e) {
    modalActive = true;
    wifiModal.style.display = "block";
});

fetch("/wifiConfigs").then(r => r.json()).then(response => {
    wifiConfigurations = response.configurations;
    for(let i = 0; i < wifiConfigurations.length; i++) {
        let option = document.createElement("option");
        option.text = wifiConfigurations[i].name;
        option.value = i.toString();
        wifiModalConfigSelect.add(option);
    }
});

wifiModalConfigSelect.addEventListener("change", function(e) {
    let value = wifiModalConfigSelect.value;
    if(value == "none") {
        wifiModalSsid.value = "";
        wifiModalPassword.value = "";
        return;
    }

    wifiModalSsid.value = wifiConfigurations[value].ssid;
    wifiModalPassword.value = wifiConfigurations[value].password;
});

wifiModalSsid.addEventListener("input", (e) => wifiModalConfigSelect.value = "none");
wifiModalPassword.addEventListener("input", (e) => wifiModalConfigSelect.value = "none");

document.getElementById("wifi-modal-cancel").addEventListener("click", function(e) {
    modalActive = false;
    wifiModal.style.display = "none";
});

document.getElementById("wifi-modal-confirm").addEventListener("click", function(e) {
    modalActive = false;
    wifiModal.style.display = "none";
    socket.send(JSON.stringify({
        type: "wifi",
        ssid: wifiModalSsid.value,
        password: wifiModalPassword.value
    }));
});

/* System health bar */
const healthIcons = {
    "UNKNOWN": "question",
    "DISABLED": "circle",
    "INITIALIZING": "sync",
    "RUNNING": "check",
    "STOPPING": "sync",
    "FAULT": "times",
    "REQUIRES_ATTENTION": "exclamation"
};

const healthColors = {
    "UNKNOWN": "gray",
    "DISABLED": "gray",
    "INITIALIZING": "green",
    "RUNNING": "green",
    "STOPPING": "orange",
    "FAULT": "red",
    "REQUIRES_ATTENTION": "orange"
};

function handleHealthPacket(json) {
    let systemStateElement = document.getElementById("system-state");
    systemStateElement.innerHTML = "";

    let length = json.services.length;
    for(let i = 0; i < length; i++) {
        let service = json.services[i];

        let spanElement = document.createElement("span");
        spanElement.innerHTML = service.name;
        spanElement.title = service.extraInfo ? service.extraInfo : "No extra info";
        spanElement.classList.add("service");
        
        let iconElement = document.createElement("i");
        iconElement.classList.add("fas");
        iconElement.classList.add("fa-" + healthIcons[service.status]);
        iconElement.classList.add("service-color-" + healthColors[service.status]);
        spanElement.prepend(iconElement);

        systemStateElement.appendChild(spanElement);
    }
}

/* Telemetry bar */
const batteryVoltageElement = document.getElementById("battery-voltage");
const boardTemperatureElement = document.getElementById("board-temperature");

/* Controls bar */
const speedInput = document.getElementById("speed-input");

document.getElementById("shutdown-btn").addEventListener("click", function(e) {
    showConfirmationModal("Are you sure you want to shut down?", function() {
        socket.send(JSON.stringify({type: "system_cmd", action: "shutdown"}));
    });
});

document.getElementById("reboot-btn").addEventListener("click", function(e) {
    showConfirmationModal("Are you sure you want to reboot?", function() {
        socket.send(JSON.stringify({type: "system_cmd", action: "reboot"}));
    });
});

const lightsToggle = document.getElementById("lights-toggle");
lightsToggle.addEventListener("click", function(e) {
    let checked = lightsToggle.checked;
    socket.send(JSON.stringify({type: "process_toggle", processType: "lighting", enabled: checked}));
});

const streamToggle = document.getElementById("stream-toggle");
streamToggle.addEventListener("click", function(e) {
    let checked = streamToggle.checked;
    socket.send(JSON.stringify({type: "process_toggle", processType: "stream", enabled: checked}));

    showStream(checked);
});

const navToggleButton = document.getElementById("nav-toggle");
const waypointBtn = document.getElementById("waypoint-btn");
const movementToggleButton = document.getElementById("movement-toggle");
const mapSection = document.getElementById("map-section");
let navigationEnabled = false;
let movementEnabled = false;

function setNavigationEnabled(enabled) {
    navigationEnabled = enabled;
    navToggleButton.innerHTML = enabled ? "Hide map" : "Show map";

    setStreamSmall(enabled);
    mapSection.style.display = enabled ? "block" : "none";
    waypointBtn.style.display = enabled ? "inline-block" : "none";
    movementToggleButton.style.display = enabled ? "inline-block" : "none";
}

navToggleButton.addEventListener("click", function(e) {
    setNavigationEnabled(!navigationEnabled);
    if(!navigationEnabled) {
        setMovementEnabled(false, true);
    }
});

function setMovementEnabled(enabled, sendPacket) {
    movementEnabled = enabled;
    movementToggleButton.innerHTML = enabled ? "Disable movement" : "Enable movement";

    if(sendPacket) {
        let json = {
            type: "nav_toggle",
            enabled: movementEnabled
        };
        socket.send(JSON.stringify(json));
    }
}

movementToggleButton.addEventListener("click", function(e) {
    setMovementEnabled(!movementEnabled, true);
});

waypointBtn.addEventListener("click", function(e) {
    let json = {
        type: "nav_createwaypoint",
        x: x,
        y: y
    };
    socket.send(JSON.stringify(json));
});

/* Keyboard input handling */
const controlKeyCodes = [87, 38, 65, 37, 83, 40, 68, 39];
let pressedKeys = {};

window.onkeyup = function(e) {
    if(!controlKeyCodes.includes(e.keyCode) || !pressedKeys[e.keyCode]) {
        return;
    }

    pressedKeys[e.keyCode] = false;
    sendControlPacket();
};

window.onkeydown = function(e) {
    if(e.keyCode == 32 && navigationEnabled) {
        setMovementEnabled(false, true);
        return;
    }

    if(!controlKeyCodes.includes(e.keyCode) || pressedKeys[e.keyCode]) {
        return;
    }

    pressedKeys[e.keyCode] = true;
    sendControlPacket();
}

function sendControlPacket() {
    let maxSpeed = speedInput.value;
    if(!maxSpeed) {
        speedInput.value = 300;
        maxSpeed = 300;
    }

    let speed = 0;
    let steer = 0;

    if(pressedKeys[87] || pressedKeys[38]) {
        speed = maxSpeed;
    } else if(pressedKeys[83] || pressedKeys[40]) {
        speed = -maxSpeed;
    }

    if(pressedKeys[68] || pressedKeys[39]) {
        steer = maxSpeed;
    } else if(pressedKeys[65] || pressedKeys[37]) {
        steer = -maxSpeed;
    }

    let json = {
        type: "control",
        speed: speed,
        steer: steer
    };
    socket.send(JSON.stringify(json));
}



//
// Start of canvas stuff
//

const mapRatio = 40.0;
const dotSize = 3.5;
const headingIndicatorLength = 15.0;

const mapCanvas = document.getElementById("map-canvas");
const ctx = mapCanvas.getContext("2d");
ctx.font = "12px Arial";
ctx.textAlign = "center";

var x = 0, y = 0, h = 0;
var waypoints = {}, waypointLength = 0;

var curvePoints = [];
var closestSplinePoint = null;

function draw() {
    ctx.clearRect(0, 0, mapCanvas.width, mapCanvas.height);

    let drawX = this.x * mapRatio;
    let drawY = this.y * mapRatio;
    let hRadians = degToRad(h - 90);

    ctx.fillText(this.x + ", " + this.y, drawX, drawY + 15);

    ctx.beginPath();
    ctx.arc(drawX, drawY, dotSize, 0, 2 * Math.PI);
    ctx.fill();

    ctx.strokeStyle = "#808080";
    curvePoints.forEach((curvePoint) => {
        ctx.beginPath();
        ctx.arc(curvePoint[0] * mapRatio, curvePoint[1] * mapRatio, 0.5, 0, 2 * Math.PI);
        ctx.stroke();
    });

    ctx.fillStyle = "#000000";
    for(let i = 0; i < waypointLength; i++) {
        let waypoint = waypoints[i];
        
        ctx.beginPath();
        ctx.arc(waypoint.x * mapRatio, waypoint.y * mapRatio, dotSize, 0, 2 * Math.PI);
        ctx.fill();

        ctx.fillText("#" + (i + 1), waypoint.x * mapRatio, waypoint.y * mapRatio - 10);
    }

    ctx.beginPath();
    ctx.moveTo(drawX, drawY);
    ctx.lineTo(drawX + Math.cos(hRadians) * headingIndicatorLength, drawY + Math.sin(hRadians) * headingIndicatorLength);
    ctx.strokeStyle = "#000000";
    ctx.stroke();

    if(closestSplinePoint != null) {
        ctx.beginPath();
        ctx.moveTo(drawX, drawY);
        ctx.lineTo(closestSplinePoint[0] * mapRatio, closestSplinePoint[1] * mapRatio);
        ctx.strokeStyle = "#ff0000";
        ctx.stroke();
    }
}
setInterval(draw, 50);

var draggingWaypoint = null, draggingWaypointIndex = -1;

function handleMouseMove(e) {
    if(!draggingWaypoint) {
        return;
    }

    let mousePos = getCursorPosition(mapCanvas, e);
    draggingWaypoint.x = mousePos.x / mapRatio;
    draggingWaypoint.y = mousePos.y / mapRatio;
}
mapCanvas.onmousemove = handleMouseMove;

function handleMouseDown(e) {
    let mousePos = getCursorPosition(mapCanvas, e);
    for(let i = 0; i < waypointLength; i++) {
        let waypoint = waypoints[i];
        let waypointX = waypoint.x * mapRatio;
        let waypointY = waypoint.y * mapRatio;

        if(Math.abs(waypointX - mousePos.x) < 5 && Math.abs(waypointY - mousePos.y) < 5) {
            if(e.ctrlKey) {
                socket.send(JSON.stringify({
                    type: "nav_waypoints",
                    action: "remove",
                    index: i,
                }));
                return;
            }

            draggingWaypoint = waypoint;
            draggingWaypointIndex = i;
            break;
        }
    }
}
mapCanvas.onmousedown = handleMouseDown;

function handleMouseUp(e) {
    if(!draggingWaypoint) {
        return;
    }

    socket.send(JSON.stringify({
        type: "nav_waypoints",
        action: "move",
        index: draggingWaypointIndex,
        x: draggingWaypoint.x,
        y: draggingWaypoint.y
    }));

    draggingWaypoint = null;
}
mapCanvas.onmouseup = handleMouseUp;
mapCanvas.onmouseout = handleMouseUp;

function getCursorPosition(canvas, event) {
    const rect = canvas.getBoundingClientRect()
    const x = event.clientX - rect.left
    const y = event.clientY - rect.top
    return {x, y};
}

function setCanvasInfo(x, y, h, t) {
    this.h = h;
    this.closestSplinePoint = t == -1 ? null : curvePoints[t];
    this.x = x;
    this.y = y;
}

function degToRad(degrees) {
    return degrees * (Math.PI / 180);
}