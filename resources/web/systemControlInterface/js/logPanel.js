//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

"use strict"

const LOG_MAX_ROWS = 200;
const LOG_BACKGROUND_COLOR = 0xffffff;
const LOG_DEBUG_COLOR = 0xffff00;
const LOG_ERROR_COLOR = 0xff0000;
const LOG_INFO_COLOR = 0x0000ff;
const LOG_TRACE_COLOR = 0x00ff00;

//let pollingLogs = false;

//  Schedule the given polling function every 1 second - this might be too frequent, keep an eye on it.
// window.setInterval(function() {
//     if (clientIdent !== '' && !pollingLogs) {
//         logsPoll();
//     }
// }, 1000);


//  Poll the REST server, unless we're not validated in which case, validate
// function logsPoll() {
//     pollingLogs = true;
//     let xhr = new XMLHttpRequest();
//     xhr.open('GET', '/poll/logs', true);
//     xhr.responseType = "json";
//     xhr.setRequestHeader('Content-Type', 'application/json; charset=utf-8');
//     xhr.setRequestHeader('Client', clientIdent);
//     xhr.onload = function () {
//         if (xhr.status === 200 || xhr.status === 201) {
//             //  input was accepted - should be 200, but we'll take 201
//             const newLogEntries = xhr.response.newLogEntries;
//             if ((newLogEntries != null) && (newLogEntries.length > 0)) {
//                 logsProcessNewEntries(newLogEntries);
//             }
//         } else {
//             console.debug("logsPoll POLL FAILED:" + xhr.statusText);
//         }
//         pollingLogs = false;
//     };
//     xhr.onerror = function () {
//         //  Some sort of network error occurred - complain and unclog input
//         pollingLogs = false;
//     };
//     xhr.send();
// }


//  Processes log entries we've retrieved from a poll
function logsProcessNewEntries(newLogEntries) {
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
            categoryStr = createSpan(LOG_ERROR_COLOR, LOG_BACKGROUND_COLOR, category);
        } else if (category === 'DEBUG') {
            categoryStr = createSpan(LOG_DEBUG_COLOR, LOG_BACKGROUND_COLOR, category);
        } else if (category === 'TRACE') {
            categoryStr = createSpan(LOG_TRACE_COLOR, LOG_BACKGROUND_COLOR, category);
        } else if (category === 'INFO') {
            categoryStr = createSpan(LOG_INFO_COLOR, LOG_BACKGROUND_COLOR, category);
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

    while (tableBody.rows.length > LOG_MAX_ROWS) {
        tableBody.deleteRow(0);
    }
}
