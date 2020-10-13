const socket = new WebSocket("ws://" + window.location.hostname + (window.location.port ? ":" + window.location.port : ""));

const streamPort = 8080;

const cameraStreamElement = document.getElementById("camera-stream");
cameraStreamElement.src = "http://" + window.location.hostname + ":" + streamPort + "/?action=stream";

const connectionErrorModal = document.getElementById("connection-error-modal");
const connectionErrorMsg = document.getElementById("connection-error-msg");

const confirmationModal = document.getElementById("confirmation-modal");
const confirmationModalQuestion = document.getElementById("confirmation-modal-question");
let confirmationModalCallback = null;

const batteryVoltageElement = document.getElementById("battery-voltage");
const boardTemperatureElement = document.getElementById("board-temperature");

const speedInput = document.getElementById("speed-input");

let webSocketOpened = false;

document.getElementById("confirmation-modal-cancel").addEventListener("click", hideConfirmationModal);
document.getElementById("confirmation-modal-confirm").addEventListener("click", function(e) {
    confirmationModalCallback();
    hideConfirmationModal();
});

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

const lightsToggle = document.getElementById("lights-toggle");
lightsToggle.addEventListener("click", function(e) {
    let checked = lightsToggle.checked;
    socket.send(JSON.stringify({type: "process_toggle", processType: "lighting", enabled: checked}));
});

const streamToggle = document.getElementById("stream-toggle");
streamToggle.addEventListener("click", function(e) {
    let checked = streamToggle.checked;
    socket.send(JSON.stringify({type: "process_toggle", processType: "stream", enabled: checked}));

    cameraStreamElement.src = checked ? "http://" + window.location.hostname + ":" + streamPort + "/?action=stream" : "img-fail.png";
});

setInterval(function() {
    if(socket.readyState == WebSocket.OPEN) {
        socket.send(JSON.stringify({type: "heartbeat"}));
    }
}, 3000);

socket.addEventListener("open", function(event) {
    console.log("WebSocket opened");
    hideConnectionError();
    webSocketOpened = true;
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
        setCanvasInfo(json.x, json.y, json.h, json.th);
        console.log(json.d);
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
        let systemStateElement = document.getElementById("system-state");
        systemStateElement.innerHTML = "";

        let length = json.services.length;
        for(let i = 0; i < length; i++) {
            let service = json.services[i];

            let spanElement = document.createElement("span");
            spanElement.innerHTML = service.name;
            spanElement.classList.add("service");
            
            let iconElement = document.createElement("i");
            iconElement.classList.add("fas");
            iconElement.classList.add("fa-" + healthIcons[service.status]);
            iconElement.classList.add("service-color-" + healthColors[service.status]);
            spanElement.prepend(iconElement);

            systemStateElement.appendChild(spanElement);
        }

        return;
    }

    if(msgType == "route") {
        curvePoints = json.curve_points;
        return;
    }
});

const healthIcons = {
    "UNKNOWN": "question",
    "DISABLED": "circle",
    "INITIALIZING": "sync",
    "RUNNING": "check",
    "STOPPING": "sync",
    "FAULT": "times"
};

const healthColors = {
    "UNKNOWN": "gray",
    "DISABLED": "gray",
    "INITIALIZING": "green",
    "RUNNING": "green",
    "STOPPING": "orange",
    "FAULT": "red"
};

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

let modalActive = false;

function setConnectionError(errorMsg) {
    connectionErrorModal.style.display = "block";
    connectionErrorMsg.innerHTML = errorMsg;
    modalActive = true;
}

function hideConnectionError() {
    connectionErrorModal.style.display = "none";
    modalActive = false;
}

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

const controlKeyCodes = [87, 38, 65, 37, 83, 40, 68, 39];

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

function onImgError(e) {
    e.src = "img-fail.png";
}

const navToggleButton = document.getElementById("nav-toggle");
const waypointBtn = document.getElementById("waypoint-btn");
const movementToggleButton = document.getElementById("movement-toggle");
let navigationEnabled = false;
let movementEnabled = false;

function setNavigationEnabled(enabled) {
    navigationEnabled = enabled;
    navToggleButton.innerHTML = enabled ? "Hide map" : "Show map";

    cameraStreamElement.height = enabled ? 580 : 720;
    mapCanvas.style.display = enabled ? "block" : "none";
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

//
// Start of canvas stuff
//

const mapCanvas = document.getElementById("map-canvas");
const ctx = mapCanvas.getContext("2d");
ctx.font = "12px Arial";
ctx.textAlign = "center";

var x = 0, y = 0, h = 0, th = 0;
var waypoints = {}, waypointLength = 0;

var curvePoints = [];

function draw() {
    ctx.clearRect(0, 0, mapCanvas.width, mapCanvas.height);

    let drawX = this.x * 40;
    let drawY = this.y * 40;
    let hRadians = degToRad(h - 90);
    let thRadians = degToRad(th - 90);

    ctx.fillText(this.x + ", " + this.y, drawX, drawY + 15);

    ctx.beginPath();
    ctx.arc(drawX, drawY, 3, 0, 2 * Math.PI);
    ctx.fill();

    ctx.strokeStyle = "#808080";
    curvePoints.forEach((curvePoint) => {
        ctx.beginPath();
        ctx.arc(curvePoint[0] * 40, curvePoint[1] * 40, 0.5, 0, 2 * Math.PI);
        ctx.stroke();
    });

    for(let i = 0; i < waypointLength; i++) {
        let waypoint = waypoints[i];
        
        ctx.beginPath();
        ctx.arc(waypoint.x * 40, waypoint.y * 40, 5, 0, 2 * Math.PI);
        ctx.fillStyle = waypoint.targeted ? "#ff0000" : "#000000";
        ctx.fill();
    }
    ctx.fillStyle = "#000000";

    ctx.beginPath();
    ctx.moveTo(drawX, drawY);
    ctx.lineTo(drawX + Math.cos(hRadians) * 15, drawY + Math.sin(hRadians) * 15);
    ctx.strokeStyle = "#000000";
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(drawX, drawY);
    ctx.lineTo(drawX + Math.cos(thRadians) * 15, drawY + Math.sin(thRadians) * 15);
    ctx.strokeStyle = "#ff0000";
    ctx.stroke();
}
setInterval(draw, 50);

var draggingWaypoint = null, draggingWaypointIndex = -1;

function handleMouseMove(e) {
    if(!draggingWaypoint) {
        return;
    }

    let mousePos = getCursorPosition(mapCanvas, e);
    draggingWaypoint.x = mousePos.x / 40;
    draggingWaypoint.y = mousePos.y / 40;
}
mapCanvas.onmousemove = handleMouseMove;

function handleMouseDown(e) {
    let mousePos = getCursorPosition(mapCanvas, e);
    for(let i = 0; i < waypointLength; i++) {
        let waypoint = waypoints[i];
        let waypointX = waypoint.x * 40;
        let waypointY = waypoint.y * 40;

        console.log(Math.abs(waypointX - mousePos.x) + " " + Math.abs(waypointY - mousePos.y));

        if(Math.abs(waypointX - mousePos.x) < 5 && Math.abs(waypointY - mousePos.y) < 5) {
            draggingWaypoint = waypoint;
            draggingWaypointIndex = i;
            break;
        }
    }
    console.log("Clicked waypoint not found");
}
mapCanvas.onmousedown = handleMouseDown;

function handleMouseUp(e) {
    socket.send(JSON.stringify({
        type: "nav_waypoints",
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

function setCanvasInfo(x, y, h, th) {
    this.h = h;
    this.th = th;
    this.x = x;
    this.y = y;
}

function degToRad(degrees) {
    return degrees * (Math.PI / 180);
}