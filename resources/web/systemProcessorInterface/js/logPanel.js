//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

"use strict"

const LOG_MAX_ROWS = 200;
const LOG_BACKGROUND_COLOR = 0xffffff;
const LOG_DEBUG_COLOR = 0xffff00;
const LOG_ERROR_COLOR = 0xff0000;
const LOG_INFO_COLOR = 0x0000ff;
const LOG_TRACE_COLOR = 0x00ff00;


//  Processes log entries we've retrieved from a poll
function logsProcessNewEntries(newLogEntries) {
    const tableBody = document.getElementById('loggingRows');
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
