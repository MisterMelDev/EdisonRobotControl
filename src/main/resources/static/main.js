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

const motherboardStateElement = document.getElementById("motherboard-connection-state");
let lastMotherboardState = false;

let webSocketOpened = false;

function setMotherboardState(motherboardState) {
    if(motherboardState == lastMotherboardState) {
        return;
    }
    lastMotherboardState = motherboardState;

    console.log("Motherboard state changed, new state is " + motherboardState);

    motherboardStateElement.innerHTML = "Motherboard " + (motherboardState ? "connected" : "disconnected");
    
    if(!motherboardState) {
        motherboardStateElement.classList.add("text-red");
    } else {
        motherboardStateElement.classList.remove("text-red");
    }
}

document.getElementById("confirmation-modal-cancel").addEventListener("click", hideConfirmationModal);
document.getElementById("confirmation-modal-confirm").addEventListener("click", function(e) {
    confirmationModalCallback();
    hideConfirmationModal();
});

document.getElementById("shutdown-btn").addEventListener("click", function(e) {
    showConfirmationModal("Are you sure you want to shut down?", function() {
        socket.send(JSON.stringify({type: "shutdown"}));
    });
});

document.getElementById("reboot-btn").addEventListener("click", function(e) {
    showConfirmationModal("Are you sure you want to reboot?", function() {
        socket.send(JSON.stringify({type: "reboot"}));
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
    socket.send(JSON.stringify({type: "lighting", enabled: checked}));
});

const streamToggle = document.getElementById("stream-toggle");
streamToggle.addEventListener("click", function(e) {
    let checked = streamToggle.checked;
    socket.send(JSON.stringify({type: "stream", enabled: checked}));

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
        setMotherboardState(json.isConnected);

        batteryVoltageElement.innerHTML = json.battVoltage.toFixed(2) + "v";
        boardTemperatureElement.innerHTML = json.boardTemp.toFixed(1) + " &#8451;";
        return;
    }

    if(msgType == "checkboxes") {
        lightsToggle.checked = json.isLightingEnabled;
        streamToggle.checked = json.isStreamEnabled;
        return;
    }

    if(msgType == "pos") {
        setCanvasInfo(json.x, json.y, json.h);
        return;
    }

    if(msgType == "nav_toggle") {
        setNavigationEnabled(json.enabled, false);
        return;
    }
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
        setNavigationEnabled(false, true);
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
let navigationEnabled = false;

function setNavigationEnabled(enabled, sendPacket) {
    if(sendPacket) {
        let json = {
            type: "nav_toggle",
            enabled: enabled
        };
        socket.send(JSON.stringify(json));
    }
    
    navigationEnabled = enabled;
    navToggleButton.innerHTML = enabled ? "Stop navigation" : "Start navigation";

    cameraStreamElement.height = enabled ? 580 : 720;
    mapCanvas.style.display = enabled ? "block" : "none";
}

navToggleButton.addEventListener("click", function(e) {
    setNavigationEnabled(!navigationEnabled, true);
});

//
// Start of canvas stuff
//

const mapCanvas = document.getElementById("map-canvas");
const ctx = mapCanvas.getContext("2d");
ctx.font = "12px Arial";
ctx.textAlign = "center";

const compassOffsetElement = document.getElementById("compass-offset");

var x = 0, y = 0, h = 0;

function draw() {
    ctx.clearRect(0, 0, mapCanvas.width, mapCanvas.height);

    let drawX = this.x * 20 + mapCanvas.width / 2;
    let drawY = this.y * 20 + mapCanvas.height / 2;
    let hRadians = degToRad(h - 90);

    ctx.fillText(this.x + ", " + this.y, drawX, drawY + 15);

    ctx.beginPath();
    ctx.arc(drawX, drawY, 5, 0, 2 * Math.PI);
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(drawX, drawY);
    ctx.lineTo(drawX + Math.cos(hRadians) * 15, drawY + Math.sin(hRadians) * 15);
    ctx.stroke();
}
setInterval(draw, 50);

function setCanvasInfo(x, y, h) {
    this.h = h + parseInt(compassOffsetElement.value);
    this.x = x;
    this.y = y;
}

function degToRad(degrees) {
    return degrees * (Math.PI / 180);
}