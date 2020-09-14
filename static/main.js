const socket = new WebSocket("ws://127.0.0.1:8888");

const connectionErrorModal = document.getElementById("connection-error-modal");
const connectionErrorMsg = document.getElementById("connection-error-msg");

const batteryVoltageElement = document.getElementById("battery-voltage");
const boardTemperatureElement = document.getElementById("board-temperature");

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

function setConnectionError(errorMsg) {
    connectionErrorModal.style.display = "block";
    connectionErrorMsg.innerHTML = errorMsg;
}

function hideConnectionError() {
    connectionErrorModal.style.display = "none";
}