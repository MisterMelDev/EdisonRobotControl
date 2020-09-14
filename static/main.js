const socket = new WebSocket("ws://127.0.0.1:8888");
const connectionErrorModal = document.getElementById("connection-error-modal");

socket.addEventListener("open", function(event) {
    console.log("WebSocket opened");
    connectionErrorModal.style.display = "none";
});

socket.addEventListener("message", function(event) {
    let json = JSON.parse(event.data);
    console.log(json);
});

socket.addEventListener("close", function(event) {
    console.log("WebSocket closed");
    connectionErrorModal.style.display = "block";
});

socket.addEventListener("error", function(event) {
    console.log("WebSocket error");
    connectionErrorModal.style.display = "block";
});