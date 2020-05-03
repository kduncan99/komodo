//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

"use strict"

let jumpKeyState = new Array(36);
let jumpKeys = new Array(36);

//  Catch mouse clicks on the various jump keys.
//  Each click will send a PUT to /jumpkeys.
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
