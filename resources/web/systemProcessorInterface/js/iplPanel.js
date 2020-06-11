//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

"use strict"

let jumpKeyState = new Array(36);
let jumpKeys = new Array(36);

//  Catch mouse clicks on the various jump keys.  Each click will send a PUT to /jumpkeys.
//  Also set up dump button click handler.
$(function() {
    for (let jkx = 0; jkx < jumpKeys.length; jkx++) {
        jumpKeyState[jkx] = false;
        const jknumber = jkx + 1;
        const jkname = 'jk' + jknumber;
        jumpKeys[jkx] = document.getElementById(jkname);
        jumpKeys[jkx].onclick = function (e) {
            const jkname = e.target.id;
            const jknumber = Number(jkname.substring(2));
            const jkindex = jknumber - 1;

            const currentState = jumpKeyState[jkindex];
            const desiredState = !currentState;

            //  For now, we'll do a set-and-forget, and see how that works out.
            let xhr = new XMLHttpRequest();
            xhr.open('PUT', '/jumpkeys', true);
            xhr.setRequestHeader('Content-Type', 'application/json; charset=utf-8');
            xhr.setRequestHeader('Client', clientIdent);

            let data = {};
            data[jknumber] = desiredState;
            let json = JSON.stringify({componentValues: data});
            xhr.send(json);
            return false;
        }
    }

    const dumpButton = document.getElementById("dumpButton");
    dumpButton.onclick = function (e) {
        let xhr = new XMLHttpRequest();
        xhr.open('GET', '/dump', true);
        xhr.setRequestHeader('Content-Type', 'text/text; charset=utf-8');
        xhr.setRequestHeader('Client', clientIdent);
        xhr.onload = function () {
            consolePendingPostMessageXHR = null;
            if (xhr.status === 200 || xhr.status === 201) {
                //  input was accepted
                alert('Dump file created:' + xhr.response);
            } else if (xhr.status === 400) {
                //  input was rejected for content
                alert('Input rejected:' + xhr.statusText);
            } else if (xhr.status === 401) {
                alert('Session is no longer validated');
            } else {
                alert('Server is refusing input:' + xhr.statusText);
            }
        };
        xhr.onerror = function () {
            //  Some sort of network error occurred - complain and unclog input
            alert('Network error - cannot submit console input');
        };
        xhr.send();
        return false;
    }
})


function iplUpdateJumpKeys(componentValues) {
    for (const jknumber in componentValues) {
        const jkindex = jknumber - 1;
        jumpKeyState[jkindex] = componentValues[jknumber];
        if (jumpKeyState[jkindex]) {
            jumpKeys[jkindex].className = "JumpKeyLit";
        } else {
            jumpKeys[jkindex].className = "JumpKeyDim";
        }
    }
}
