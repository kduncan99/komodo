//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

//  Globals ------------------------------------------------------------------------------------------------------------------------
const maxLoggingRows = 200;
const credentialUsername = 'admin';     //  TODO solicit these from the user
const credentialPassword = 'admin';     //  TODO as above
let clientIdent = '';
let polling = false;                    //  poll in progress
let pollFailed = false;                 //  previous poll failed
let validating = false;                 //  validation in progress

const attributes = {
    screenSizeRows: 24,
    screenSizeColumns: 80
};

const logBackgroundColor = 0xffffff;
const logDebugColor = 0xffff00;
const logErrorColor = 0xff0000;
const logInfoColor = 0x0000ff;
const logTraceColor = 0x00ff00;
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


//  Because browsers are designed to act like a magazine of indeterminate size for displaying content,
//  it's bloody difficult to set up (even using monospaced fonts) something resembling a terminal screen with a reliable
//  width and height for the screen, and for the content thereof.
// $(function() {
//     const textContent = ''.padStart(80, 'M');
//     const inputRow = document.getElementById('consoleInputRow');
//     inputRow.textContent = textContent;
//     const lineWidthPixels = inputRow.offsetWidth;
//     const lineHeightPixels = inputRow.offsetHeight;
//
//     const rows = document.getElementById('ConsoleOutput').getElementsByTagName('li');
//     for (let rx = 0; rx < attributes.screenSizeRows; ++rx) {
//         rows[rx].attributes.height = lineHeightPixels;
//         rows[rx].attributes.width = lineWidthPixels;
//     }
//
//     const statusRow = document.getElementById('statusRow');
//     statusRow.height = lineHeightPixels;
//     statusRow.width = lineWidthPixels;
//
//     inputRow.innerHTML = blankLine.replace(spaceRegexPattern, '&nbsp;');
//     inputRow.height = lineHeightPixels;
//     inputRow.width = lineWidthPixels;
//
//     console.debug("line height " + lineHeightPixels + "px");//TODO remove
//     console.debug("line width  " + lineWidthPixels + "px");//TODO remove this also
// });


//  Schedule the given polling function every 1 second - this might be too frequent, keep an eye on it.
window.setInterval(function(){
    if (!validating && !polling){
        pollServer();
    } else {
        setStatusRow(false);
    }
}, 1000);


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
function pollServer() {
    if (clientIdent === '') {
        if (validating) {
            console.debug("waiting for validation...");
        } else {
            validate();
        }
    } else {
        if (polling) {
            console.debug("waiting on poll...");
        } else {
            polling = true;
            console.debug("polling...");
            let xhr = new XMLHttpRequest();
            xhr.open('GET', '/poll', true);
            xhr.responseType = "json";
            xhr.setRequestHeader('Content-Type', 'application/json; charset=utf-8');
            xhr.setRequestHeader('Client', clientIdent);
            xhr.onload = function () {
                if (xhr.status === 200 || xhr.status === 201) {
                    //  input was accepted - should be 200, but we'll take 201
                    pollFailed = false;
                    console.debug("POLL SUCCESS:" + xhr.response);  //  TODO remove later
                    const jumpKeySettings = xhr.response.jumpKeySettings;
                    const newLogEntries = xhr.response.newLogEntries;
                    const newMessages = xhr.response.outputMessages;

                    if (jumpKeySettings != null) {
                        //TODO update jump key settings panel
                    }

                    if ((newLogEntries != null) && (newLogEntries.length > 0)) {
                        processNewLogEntries(newLogEntries);
                    }

                    if ((newMessages != null) && (newMessages.length > 0)) {
                        processNewConsoleMessages(newMessages);
                    }
                } else {
                    console.debug("POLL FAILED:" + xhr.statusText);
                    pollFailed = true;
                }
                polling = false;
                setStatusRow(false);
            };
            xhr.onerror = function () {
                //  Some sort of network error occurred - complain and unclog input
                alert('Network error - cannot contact indicated server');
                polling = false;
                clientIdent = '';
                setStatusRow(false);
                //TODO go back to login dialog
            };
            xhr.send();
            //TODO use XMLHttpRequest directly
            // $.ajax({
            //     url: '/poll',
            //     type: 'GET',
            //     dataType: 'json',
            //     headers: {
            //         "Client": clientIdent
            //     },
            //     success: function (data, textStatus, jqXHR) {
            //         console.debug(jqXHR.responseJSON);
            //         const jumpKeySettings = jqXHR.responseJSON.jumpKeySettings;
            //         const newLogEntries = jqXHR.responseJSON.newLogEntries;
            //         const newMessages = jqXHR.responseJSON.outputMessages;
            //
            //         if (jumpKeySettings != null) {
            //             //TODO update jump key settings panel
            //         }
            //
            //         if ((newLogEntries != null) && (newLogEntries.length > 0)) {
            //             processNewLogEntries(newLogEntries);
            //         }
            //
            //         if ((newMessages != null) && (newMessages.length > 0)) {
            //             processNewConsoleMessages(newMessages);
            //         }
            //
            //         polling = false;
            //     },
            // });
            setStatusRow(true);
        }
    }
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
                    console.debug('WRITE_ROW');//TODO remove
                    console.debug('  text:[' + text + '] len=' + text.length);  //TODO remove
                    console.debug('  adj: [' + adjustedText + '] len=' + adjustedText.length);  // TODO remove
                    console.debug('  rightJust = ' + rightJustified);   //  TODO remove
                    if (rightJustified) {
                        adjustedText = adjustedText.padStart(attributes.screenSizeColumns, ' ');
                    } else {
                        adjustedText = adjustedText.padEnd(attributes.screenSizeColumns, ' ');
                    }
                    console.debug('  adj: [' + adjustedText + '] len=' + adjustedText.length); //TODO remove
                    html = createSpan(textColor, backgroundColor, adjustedText.substring(0, attributes.screenSizeColumns));
                    rows[rowIndex].innerHTML = html;
                }
                break;
        }
    }
}


//  Processes log entries we've retrieved from a poll
function processNewLogEntries(newLogEntries) {
    const tableBody = document.getElementById('LoggingRows');
    for (let i = 0; i < newLogEntries.length; i++) {
        const timestamp = newLogEntries[i].timestamp;
        const category = newLogEntries[i].category;
        const entity = newLogEntries[i].entity;
        const message = newLogEntries[i].message;

        const dateStr = new Date(timestamp).toISOString();
        const dateElement = document.createElement('td');
        dateElement.innerHTML = dateStr;

        let categoryStr = category.padStart(5, ' ');
        if (category === 'ERROR') {
            categoryStr = createSpan(logErrorColor, logBackgroundColor, category);
        } else if (category === 'DEBUG') {
            categoryStr = createSpan(logDebugColor, logBackgroundColor, category);
        } else if (category === 'TRACE') {
            categoryStr = createSpan(logTraceColor, logBackgroundColor, category);
        } else if (category === 'INFO') {
            categoryStr = createSpan(logInfoColor, logBackgroundColor, category);
        }
        const catElement = document.createElement('td');
        catElement.innerHTML = categoryStr;

        const entityElement = document.createElement('td');
        entityElement.innerHTML = entity;

        const messageStr = message.replace(new RegExp(' ', 'g'), '&nbsp;');
        const msgElement = document.createElement('td');
        msgElement.innerHTML = messageStr;

        const newRow = tableBody.insertRow(0);
        newRow.appendChild(dateElement);
        newRow.appendChild(catElement);
        newRow.appendChild(entityElement);
        newRow.appendChild(msgElement);

        tableBody.appendChild(newRow);
    }

    while (tableBody.rows.length > maxLoggingRows) {
        tableBody.deleteRow(0);
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
    } else if (polling) {
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
