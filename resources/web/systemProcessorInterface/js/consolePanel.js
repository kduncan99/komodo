//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

"use strict"

const CONSOLE_DEFAULT_OUTPUT_FG_COLOR = 0x00ff00;
const CONSOLE_DEFAULT_OUTPUT_BG_COLOR = 0x000000;
const CONSOLE_DEFAULT_INPUT_FG_COLOR = 0xffffff;
const CONSOLE_DEFAULT_INPUT_BG_COLOR = 0x000000;
const CONSOLE_BLANK_LINE = ''.padStart(ATTRIBUTES.consoleScreenSizeColumns, ' ');
const CONSOLE_FORM = document.getElementById('consoleInput');
const CONSOLE_INPUT_FIELD = document.getElementById('consoleInputRow');

let consolePendingPostMessageXHR = null;

//  Set up input submit handling
CONSOLE_FORM.onsubmit = function() {
    if (consolePendingPostMessageXHR != null) {
        alert('Previous input not yet acknowledged');
    } else if (validating) {
        alert("Validation in progress - please wait");
    } else if (clientIdent === '') {
        alert("No session active - input ignored");
        CONSOLE_INPUT_FIELD.value = '';
    } else {
        let xhr = new XMLHttpRequest();
        xhr.open('POST', '/message', true);
        xhr.setRequestHeader('Content-Type', 'application/json; charset=utf-8');
        xhr.setRequestHeader('Client', clientIdent);
        let json = JSON.stringify({text: CONSOLE_INPUT_FIELD.value.trim()});
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
            CONSOLE_INPUT_FIELD.disabled = false;
            CONSOLE_INPUT_FIELD.value = '';
            consoleSetStatusRow();
        };
        xhr.onerror = function () {
            //  Some sort of network error occurred - complain and unclog input
            alert('Network error - cannot submit console input');
            CONSOLE_INPUT_FIELD.disabled = false;
            CONSOLE_INPUT_FIELD.value = '';
            consoleSetStatusRow();
        };
        xhr.send(json);
        consolePendingPostMessageXHR = xhr;
        CONSOLE_INPUT_FIELD.disabled = true;
    }

    consoleSetStatusRow();
    return false;
};

consoleReset();


// Responds to pressing the Esc key to unlock the keyboard
$('input[type=text]').on('keydown', function(e) {
    if (e.which === 27 || e.keyCode === 27) {
        e.preventDefault();
        consolePendingPostMessageXHR = false;
        CONSOLE_INPUT_FIELD.disabled=false;
    }
});


//  Called whenever the console tab gets activated
function consoleActivate() {
    CONSOLE_INPUT_FIELD.focus();
}


function consoleClearInputArea() {
    const html = createSpan(CONSOLE_DEFAULT_INPUT_FG_COLOR, CONSOLE_DEFAULT_INPUT_BG_COLOR, CONSOLE_BLANK_LINE);
    const inputRow = document.getElementById('consoleInputRow');
    inputRow.disabled = false;
    inputRow.innerHTML = html;
}


function consoleClearOutputArea() {
    const rows = document.getElementById('consoleOutput').getElementsByTagName('li');
    const html = createSpan(CONSOLE_DEFAULT_OUTPUT_FG_COLOR, CONSOLE_DEFAULT_OUTPUT_BG_COLOR, CONSOLE_BLANK_LINE);
    const width = CONSOLE_INPUT_FIELD.clientWidth;
    for (let rx = 0; rx < ATTRIBUTES.consoleScreenSizeRows; rx++) {
        rows[rx].style.width = width;
        rows[rx].style.maxWidth = width;
        rows[rx].innerHTML = html;
    }
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
                if ((rowIndex >= 0) && (rowIndex < ATTRIBUTES.consoleScreenSizeRows)) {
                    const rows = document.getElementById('consoleOutput').getElementsByTagName('li');
                    let rx = rowIndex + 1;
                    while (rx < ATTRIBUTES.consoleScreenSizeRows) {
                        rows[rx - 1].innerHTML = rows[rx].innerHTML;
                        rx++;
                    }
                    html = createSpan(CONSOLE_DEFAULT_OUTPUT_FG_COLOR, CONSOLE_DEFAULT_OUTPUT_BG_COLOR, CONSOLE_BLANK_LINE);
                    rows[ATTRIBUTES.consoleScreenSizeRows - 1].innerHTML = html;
                }
                break;

            case 3:     //  WRITE_ROW
                if ((rowIndex >= 0) && (rowIndex < ATTRIBUTES.consoleScreenSizeRows)) {
                    const rows = document.getElementById('consoleOutput').getElementsByTagName('li');
                    let adjustedText = text.substring(0, ATTRIBUTES.consoleScreenSizeColumns);
                    if (rightJustified) {
                        adjustedText = adjustedText.padStart(ATTRIBUTES.consoleScreenSizeColumns, ' ');
                    } else {
                        adjustedText = adjustedText.padEnd(ATTRIBUTES.consoleScreenSizeColumns, ' ');
                    }
                    html = createSpan(textColor, backgroundColor, adjustedText.substring(0, ATTRIBUTES.consoleScreenSizeColumns));
                    rows[rowIndex].innerHTML = html;
                }
                break;
        }
    }
}


function consoleReset() {
    consoleClearInputArea();
    consoleClearOutputArea();
    consoleSetStatusRow();
}


//  Updates the status row...
function consoleSetStatusRow() {
    let html = ''.padStart(ATTRIBUTES.consoleScreenSizeColumns - 14, ' ').replace(SPACE_REGEX_PATTERN, '&nbsp;')
    let colors;
    if (consolePendingPostMessageXHR) {
        colors = [0x000000, 0xffff00];
    } else {
        colors = [0xbfbfbf, 0x3f3f00];
    }
    html += createSpan(colors[0], colors[1], 'LOCK');

    html += ' '
    if (pollFlashed) {
        colors = [0x000000, 0x00ffff];
    } else if (polling) {
        colors = [0x000000, 0x00ff00];
    } else if (pollFailedCounter > 0) {
        colors = [0x000000, 0xff0000];
    } else {
        colors = [0xbfbfbf, 0x003f00];
    }
    html += createSpan(colors[0], colors[1], 'POLL');

    html += ' ';
    if (validating) {
        colors = [0x000000, 0xffff00];
    } else if (clientIdent === '') {
        colors = [0xbfbfbf, 0x003f00];
    } else {
        colors = [0x000000, 0x00ff00];
    }
    html += createSpan(colors[0], colors[1], 'SESN');

    document.getElementById('consoleStatusRow').innerHTML = html;
}
