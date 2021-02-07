const socket = new WebSocket("ws://" + window.location.hostname + (window.location.port ? ":" + window.location.port : ""));

setInterval(function() {
    if(socket.readyState == WebSocket.OPEN) {
        socket.send(JSON.stringify({type: "heartbeat"}));
    }
}, 3000);

socket.addEventListener("open", function(event) {
    setConnectionState("Connected");
});

socket.addEventListener("close", function(event) {
    setConnectionState("Disconnected");
});

socket.addEventListener("message", function(event) {
    let json = JSON.parse(event.data);
    let msgType = json.type;

    console.log(msgType);

    if(msgType == "telemetry") {
        let batteryMain = json.voltage.main.toFixed(2) + "v";
        let batterySmall = json.voltage.small.toFixed(2) + "v";
        let boardTemp = json.temp.toFixed(1) + " &#8451;";
        document.getElementById("telemetry").innerHTML = batteryMain + " &middot; " + batterySmall + " &middot; " + boardTemp;
        return;
    }
});

function setConnectionState(state) {
    document.getElementById("connection-state").innerHTML = state;
}

/* Controls */

var joy = new JoyStick("joyDiv");

function sendControlPacket() {
    if(socket.readyState == WebSocket.OPEN) {
        let speedInput = document.getElementById("speed-input");
        let maxSpeed = speedInput.value;
        if(!maxSpeed) {
            speedInput.value = 300;
            maxSpeed = 300;
        }

        let json = {
            type: "control",
            speed: joy.GetY() * (maxSpeed / 100),
            steer: joy.GetX() * (maxSpeed / 100)
        };
        socket.send(JSON.stringify(json));
    }
}
setInterval(sendControlPacket, 100);