const socket = new WebSocket("ws://" + window.location.hostname + (window.location.port ? ":" + window.location.port : ""));

const streamPort = 8080;
document.getElementById("camera-stream").src = "http://" + window.location.hostname + ":" + streamPort + "/?action=stream";

const connectionErrorModal = document.getElementById("connection-error-modal");
const connectionErrorMsg = document.getElementById("connection-error-msg");

const confirmationModal = document.getElementById("confirmation-modal");
const confirmationModalQuestion = document.getElementById("confirmation-modal-question");
let confirmationModalCallback = null;

const batteryVoltageElement = document.getElementById("battery-voltage");
const boardTemperatureElement = document.getElementById("board-temperature");

const speedInput = document.getElementById("speed-input");

document.getElementById("confirmation-modal-cancel").addEventListener("click", hideConfirmationModal);
document.getElementById("confirmation-modal-confirm").addEventListener("click", function(e) {
    confirmationModalCallback();
    hideConfirmationModal();
});

document.getElementById("shutdown-btn").addEventListener("click", function(e) {
    showConfirmationModal("Are you sure you want to shut down?", function() {
        socket.send(JSON.stringify({"type": "shutdown"}));
    });
});

setInterval(function() {
    if(socket.readyState == WebSocket.OPEN) {
        socket.send(JSON.stringify({"type": "heartbeat"}));
    }
}, 3000);

socket.addEventListener("open", function(event) {
    console.log("WebSocket opened");
    hideConnectionError();
});

socket.addEventListener("message", function(event) {
    let json = JSON.parse(event.data);
    let msgType = json.type;

    if(msgType == "telemetry") {
        if(!json.isConnected) {
            setConnectionError("Unable to communicate with motherboard, is it turned on?");
        } else {
            hideConnectionError();
        }

        batteryVoltageElement.innerHTML = json.battVoltage.toFixed(2) + "v";
        boardTemperatureElement.innerHTML = json.boardTemp.toFixed(1) + " &#8451;";
    }
});

socket.addEventListener("close", function(event) {
    console.log("WebSocket closed");
    setConnectionError("WebSocket connection was interrupted");
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