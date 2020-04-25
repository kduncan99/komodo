//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

const consoleDefaultOutputColor = 0x00ff00;
const consoleDefaultOutputBackgroundColor = 0x000000;
const consoleDefaultInputColor = 0xffffff;
const consoleDefaultInputBackgroundColor = 0x000000;
const consoleBlankLine = ''.padStart(attributes.screenSizeColumns, ' ');

let pollingConsole = false;             //  console poll in progress
let consolePendingPostMessageXHR = null;


consoleReset();


window.setInterval(function() {
    if (clientIdent !== '' && !pollingLogs) {
        consolePoll();
    }
}, 500);


//  Responds to pressing the Enter or Return key for input, or the Esc key to unlock the keyboard
$('input[type=text]').on('keydown', function(e) {
    const inputRow = document.getElementById('consoleInputRow');
    if (e.which === 13 || e.keyCode === 13){
        e.preventDefault();
        if (consolePendingPostMessageXHR != null) {
            alert('Previous input not yet acknowledged');
        } else if (validating) {
            alert("Validation in progress - please wait");
        } else if (clientIdent === '') {
            alert("No session active - input ignored");
            inputRow.value = '';
        } else {
            const inputRow = document.getElementById('consoleInputRow');
            let xhr = new XMLHttpRequest();
            xhr.open('POST', '/message', true);
            xhr.setRequestHeader('Content-Type', 'application/json; charset=utf-8');
            xhr.setRequestHeader('Client', clientIdent);
            let json = JSON.stringify({text: inputRow.value.trim()});
            xhr.onload = function () {
                consolePendingPostMessageXHR = null;
                if (xhr.status === 200 || xhr.status === 201) {
                    //  input was accepted
                } else if (xhr.status === 400) {
                    //  input was rejected for content
                    alert('Input rejected:' + xhr.statusText);
                } else if (xhr.status === 401) {
                    alert('Session is no longer validated');
                } else {
                    alert('Server is refusing input:' + xhr.statusText);
                }
                inputRow.disabled = false;
                inputRow.value = '';
                consoleSetStatusRow(false);
            };
            xhr.onerror = function () {
                //  Some sort of network error occurred - complain and unclog input
                alert('Network error - cannot submit console input');
                inputRow.disabled = false;
                inputRow.value = '';
                consoleSetStatusRow(false);
            };
            xhr.send(json);
            consolePendingPostMessageXHR = xhr;
            inputRow.disabled = true;
            consoleSetStatusRow(false);
        }
    } else if (e.which === 27 || e.keyCode === 27) {
        e.preventDefault();
        const row = document.getElementById('consoleInputRow');
        consolePendingPostMessageXHR = false;
        row.disabled=false;
    }
});


function consoleClearInputArea() {
    const html = createSpan(consoleDefaultInputColor, consoleDefaultInputBackgroundColor, consoleBlankLine);
    const inputRow = document.getElementById('consoleInputRow');
    inputRow.disabled = false;
    inputRow.innerHTML = html;
}


function consoleClearOutputArea() {
    const rows = document.getElementById('ConsoleOutput').getElementsByTagName('li');
    const html = createSpan(consoleDefaultOutputColor, consoleDefaultOutputBackgroundColor, consoleBlankLine);
    for (let rx = 0; rx < attributes.screenSizeRows; rx++) {
        rows[rx].innerHTML = html;
    }
}


//  Poll the REST server, unless we're not validated in which case, validate
function consolePoll() {
    pollingConsole = true;
    let xhr = new XMLHttpRequest();
    xhr.open('GET', '/poll/console', true);
    xhr.responseType = "json";
    xhr.setRequestHeader('Content-Type', 'application/json; charset=utf-8');
    xhr.setRequestHeader('Client', clientIdent);
    xhr.onload = function () {
        if (xhr.status === 200 || xhr.status === 201) {
            //  input was accepted - should be 200, but we'll take 201
            pollFailed = false;
            const newMessages = xhr.response.outputMessages;
            if ((newMessages != null) && (newMessages.length > 0)) {
                consoleProcessNewMessages(newMessages);
            }
        } else {
            console.debug("POLL FAILED:" + xhr.statusText);
            pollFailed = true;
        }
        pollingConsole = false;
        consoleSetStatusRow(false);
    };
    xhr.onerror = function () {
        //  Some sort of network error occurred - complain and unclog input
        alert('Network error - cannot contact indicated server');
        pollingConsole = false;
        clientIdent = '';
        consoleSetStatusRow(false);
        //TODO go back to login dialog
    };
    xhr.send();
    consoleSetStatusRow(true);
}


//  Processes console messages we've retrieved from a poll
function consoleProcessNewMessages(newMessages) {
    for (let mx = 0; mx < newMessages.length; mx++) {
        const messageType = newMessages[mx].messageType;
        const rowIndex = newMessages[mx].rowIndex;
        const textColor = newMessages[mx].textColor;
        const backgroundColor = newMessages[mx].backgroundColor;
        const text = newMessages[mx].text;
        const rightJustified = newMessages[mx].rightJustified;
        let html = '';

        switch(messageType) {
            case 0:     //  CLEAR_SCREEN
                consoleClearOutputArea();
                break;

            case 1:     //  UNLOCK_KEYBOARD
                consolePendingPostMessageXHR = null;
                const inputRow = document.getElementById('consoleInputRow');
                inputRow.disabled = false;
                break;

            case 2:     //  DELETE_ROW
                if ((rowIndex >= 0) && (rowIndex < attributes.screenSizeRows)) {
                    const rows = document.getElementById('ConsoleOutput').getElementsByTagName('li');
                    let rx = rowIndex + 1;
                    while (rx < attributes.screenSizeRows) {
                        rows[rx - 1].innerHTML = rows[rx].innerHTML;
                        rx++;
                    }
                    html = createSpan(consoleDefaultOutputColor, consoleDefaultOutputBackgroundColor, consoleBlankLine);
                    rows[attributes.screenSizeRows - 1].innerHTML = html;
                }
                break;

            case 3:     //  WRITE_ROW
                if ((rowIndex >= 0) && (rowIndex < attributes.screenSizeRows)) {
                    const rows = document.getElementById('ConsoleOutput').getElementsByTagName('li');
                    let adjustedText = text.substring(0, attributes.screenSizeColumns);
                    if (rightJustified) {
                        adjustedText = adjustedText.padStart(attributes.screenSizeColumns, ' ');
                    } else {
                        adjustedText = adjustedText.padEnd(attributes.screenSizeColumns, ' ');
                    }
                    html = createSpan(textColor, backgroundColor, adjustedText.substring(0, attributes.screenSizeColumns));
                    rows[rowIndex].innerHTML = html;
                }
                break;
        }
    }
}


function consoleReset() {
    consoleClearOutputArea();
    consoleClearInputArea();
    consoleSetStatusRow();
}


//  Updates the status row
function consoleSetStatusRow(pollTrigger) {
    let html = ''.padStart(attributes.screenSizeColumns - 14, ' ');
    if (consolePendingPostMessageXHR) {
        html += createSpan(0x000000, 0xffff00, 'LOCK');
    } else {
        html += createSpan(0xbfbfbf, 0x3f3f00, 'LOCK');
    }

    html += ' '
    if (pollTrigger) {
        html += createSpan(0x000000, 0x00ffff, 'POLL');
    } else if (pollingConsole) {
        html += createSpan(0x000000, 0x00ff00, 'POLL');
    } else if (pollFailed) {
        html += createSpan(0x000000, 0xff0000, 'POLL');
    } else {
        html += createSpan(0xbfbfbf, 0x003f00, 'POLL');
    }
    html += ' ';

    if (validating) {
        html += createSpan(0x000000, 0xffff00, 'SESN');
    } else if (clientIdent === '') {
        html += createSpan(0xbfbfbf, 0x003f00, 'SESN');
    } else {
        html += createSpan(0x000000, 0x00ff00, 'SESN');
    }

    document.getElementById('consoleStatusRow').innerHTML = html;
}
