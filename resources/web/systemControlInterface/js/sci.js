//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

//  Globals ------------------------------------------------------------------------------------------------------------------------
const credentialUsername = 'admin';     //  TODO solicit these from the user
const credentialPassword = 'admin';     //  TODO as above
let clientIdent = '';
let pollingConsole = false;             //  console poll in progress
let pollFailed = false;                 //  previous poll failed
let validating = false;                 //  validation in progress

const attributes = {
    screenSizeRows: 24,
    screenSizeColumns: 80
};

const defaultConsoleColor = 0x00ff00;
const defaultConsoleBackground = 0x000000;
const defaultInputColor = 0xffffff;
const defaultInputBackground = 0x000000;

let pendingPostMessageXHR = null;
const spaceRegexPattern = new RegExp(' ', 'g');


const blankLine = ''.padStart(attributes.screenSizeColumns, ' ');


//  Do tab panels
$(function() {
    $( "#consoleTabs" ).tabs();
});

resetConsole();


window.setInterval(function() {
    if (clientIdent === '' && !validating) {
        validate();
        setStatusRow(false);
    } else if (clientIdent !== '' && !pollingConsole){
        pollConsole();
        setStatusRow(true);
    } else {
        setStatusRow(false);
    }
}, 500);


//  Responds to pressing the Enter or Return key for input, or the Esc key to unlock the keyboard
$('input[type=text]').on('keydown', function(e) {
    const inputRow = document.getElementById('consoleInputRow');
    if (e.which === 13 || e.keyCode === 13){
        e.preventDefault();
        if (pendingPostMessageXHR != null) {
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
                pendingPostMessageXHR = null;
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
                setStatusRow(false);
            };
            xhr.onerror = function () {
                //  Some sort of network error occurred - complain and unclog input
                alert('Network error - cannot submit console input');
                inputRow.disabled = false;
                inputRow.value = '';
                setStatusRow(false);
            };
            xhr.send(json);
            pendingPostMessageXHR = xhr;
            inputRow.disabled = true;
            setStatusRow(false);
        }
    } else if (e.which === 27 || e.keyCode === 27) {
        e.preventDefault();
        const row = document.getElementById('consoleInputRow');
        pendingPostMessageXHR = false;
        row.disabled=false;
    }
});


function clearInputArea() {
    const html = createSpan(defaultInputColor, defaultInputBackground, blankLine);
    const inputRow = document.getElementById('consoleInputRow');
    inputRow.disabled = false;
    inputRow.innerHTML = html;
}


function clearOutputArea() {
    const rows = document.getElementById('ConsoleOutput').getElementsByTagName('li');
    const html = createSpan(defaultConsoleColor, defaultConsoleBackground, blankLine);
    for (let rx = 0; rx < attributes.screenSizeRows; rx++) {
        rows[rx].innerHTML = html;
    }
}


//  Creates a span of html consisting of a <span> leading tag for specifying color and background color,
//  the given text where literal spaces are replaced by '&nbsp', and a final </span> tag.
//  Parameters should be integers values constructed as 0x{rr}{gg}{bb}
function createSpan(fgcolor, bgcolor, text) {
    const fgStr = '#' + (Number(fgcolor).toString(16).padStart(6, '0'));
    const bgStr = '#' + (Number(bgcolor).toString(16).padStart(6, '0'));
    const openTag = '<span style="color:' + fgStr + '; background-color:' + bgStr + '">'
    const html = text.replace(spaceRegexPattern, '&nbsp;');
    return openTag + html + '</span>';
}


//  Poll the REST server, unless we're not validated in which case, validate
function pollConsole() {
    pollingConsole = true;
    console.debug("polling...");
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
                processNewConsoleMessages(newMessages);
            }
        } else {
            console.debug("POLL FAILED:" + xhr.statusText);
            pollFailed = true;
        }
        pollingConsole = false;
        setStatusRow(false);
    };
    xhr.onerror = function () {
        //  Some sort of network error occurred - complain and unclog input
        alert('Network error - cannot contact indicated server');
        pollingConsole = false;
        clientIdent = '';
        setStatusRow(false);
        //TODO go back to login dialog
    };
    xhr.send();
    setStatusRow(true);
}


//  Processes console messages we've retrieved from a poll
function processNewConsoleMessages(newMessages) {
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
                clearOutputArea();
                break;

            case 1:     //  UNLOCK_KEYBOARD
                pendingPostMessageXHR = null;
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
                    html = createSpan(defaultConsoleColor, defaultConsoleBackground, blankLine);
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


function resetConsole() {
    clearOutputArea();
    clearInputArea();
    setStatusRow();
}


//  Updates the status row
function setStatusRow(pollTrigger) {
    let html = ''.padStart(attributes.screenSizeColumns - 14, ' ');
    if (pendingPostMessageXHR) {
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

    document.getElementById('statusRow').innerHTML = html;
}


//  Initiates a validation sequence with the REST server
function validate() {
    validating = true;
    console.debug("validating...");
    let xhr = new XMLHttpRequest();
    xhr.open('POST', '/session', true);
    xhr.responseType = "json";
    xhr.setRequestHeader('Content-Type', 'application/json; charset=utf-8');
    xhr.setRequestHeader('Authorization', 'Basic ' + btoa(credentialUsername + ':' + credentialPassword));
    let json = JSON.stringify(attributes);
    xhr.onload = function () {
        pendingPostMessageXHR = null;
        if (xhr.status === 200 || xhr.status === 201) {
            //  input was accepted - should be 201, but we'll take 200
            console.debug("VALIDATE SUCCESS:" + xhr.response);
            clientIdent = xhr.response;
            validating = false;
        } else {
            //  input was rejected for content
            console.debug("VALIDATE FAILED:" + xhr.statusText);
            alert('Validation failure:' + xhr.statusText);
            //TODO go back to login dialog
        }
        setStatusRow(false);
    };
    xhr.onerror = function () {
        //  Some sort of network error occurred - complain and unclog input
        alert('Network error - cannot contact indicated server');
        //TODO set validating = false
        //TODO go back to login dialog
        setStatusRow(false);
    };
    xhr.send(json);
    setStatusRow(false);
}
