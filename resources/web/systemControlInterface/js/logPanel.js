//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

const maxLoggingRows = 200;
const logBackgroundColor = 0xffffff;
const logDebugColor = 0xffff00;
const logErrorColor = 0xff0000;
const logInfoColor = 0x0000ff;
const logTraceColor = 0x00ff00;
let pollingLogs = false;

//  Schedule the given polling function every 1 second - this might be too frequent, keep an eye on it.
window.setInterval(function() {
    if (clientIdent !== '' && !pollingLogs) {
        pollLogs();
    }
}, 1000);


//  Poll the REST server, unless we're not validated in which case, validate
function pollLogs() {
    pollingLogs = true;
    console.debug("pullLogs polling...");//TODO remove later
    let xhr = new XMLHttpRequest();
    xhr.open('GET', '/poll/logs', true);
    xhr.responseType = "json";
    xhr.setRequestHeader('Content-Type', 'application/json; charset=utf-8');
    xhr.setRequestHeader('Client', clientIdent);
    xhr.onload = function () {
        if (xhr.status === 200 || xhr.status === 201) {
            //  input was accepted - should be 200, but we'll take 201
            console.debug("pollLogs POLL SUCCESS:" + xhr.response);  //  TODO remove later
            const newLogEntries = xhr.response.newLogEntries;
            if ((newLogEntries != null) && (newLogEntries.length > 0)) {
                processNewLogEntries(newLogEntries);
            }
        } else {
            console.debug("pollLogs POLL FAILED:" + xhr.statusText);
        }
        pollingLogs = false;
    };
    xhr.onerror = function () {
        //  Some sort of network error occurred - complain and unclog input
        pollingLogs = false;
    };
    xhr.send();
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
