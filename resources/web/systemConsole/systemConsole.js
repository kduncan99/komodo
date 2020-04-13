//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

//  Globals ------------------------------------------------------------------------------------------------------------------------

const maxLoggingRows = 200;
const credentialUsername = 'admin';   //  TODO solicit these from the user
const credentialPassword = 'admin';   //  TODO as above
let clientIdent = '';
let polling = false;                    //  poll in progress
let statusMessageLines = 0;             //  Number of status messages lines at top of console
let validating = false;                 //  validation in progress

const consoleSizeRows = 24;
const consoleSizeColumns = 80;

//  Table of message ids, keyed by row.  E.g., messageIds[4] == 2 means row with index 4, that is, row 5, contains
//  a read-reply message with an id of 5.  messageIds[7] == -1 means row 7 does not contain a read-reply message.
let messageIds = [];
for (let mx = 0; mx < consoleSizeRows; mx++){
    messageIds[mx] = -1;
}

//  Colors for OS console (presented on a black background, so they should be lighter)
//  constants for now; later we might make these configurable.
const consoleInputBackground = '#000000';
const consoleInputColor = '#ffffff';
const consoleInputSpan = '<span style="color:' + consoleInputColor + '; background-color:' + consoleInputBackground + '">';
const consoleInputReverseSpan = '<span style="color:' + consoleInputBackground + '; background-color:' + consoleInputColor + '">';

const consoleReadOnlyBackground = '#000000';
const consoleReadOnlyColor = '#00ff00';
const consoleReadOnlySpan = '<span style="color:' + consoleReadOnlyColor + '; background-color:' + consoleReadOnlyBackground + '">';

const consoleReadReplyBackground = '#000000';
const consoleReadReplyColor = '#ff3f3f';
const consoleReadReplySpan = '<span style="color:' + consoleReadReplyColor + '; background-color:' + consoleReadReplyBackground + '">';

const consoleSystemStatusBackground = '#000000';
const consoleSystemStatusColor = '#00ffff';
const consoleSystemStatusSpan = '<span style="color:' + consoleSystemStatusColor + '; background-color:' + consoleSystemStatusBackground + '">';

let consoleInputCursorColumn = 0;       //  location of cursor in input area
let consoleInputCursorInverted = false; //  used for flashing the cursor

//  Colors for log pane (presented on a white background, so they should be darker)
const logErrorColor = '#ff0000';
const logDebugColor = '#ffff00';
const logTraceColor = '#00ff00';
const logInfoColor = '#0000ff';

const spaceRegexPattern = new RegExp(' ', 'g');


$(function() {
    $( "#consoleTabs" ).tabs();
});


//  Schedule the given polling function every 1/2 second (cursor flashing is the most periodic, and requires this)
window.setInterval(function(){
    flashCursor();
    if (!validating && !polling){
        pollServer();
    }
}, 500);


//  Every .5 seconds, we invert the cursor-inverted flag, then redraw the input text accordingly
function flashCursor(){
    consoleInputCursorInverted = !consoleInputCursorInverted;
    const row = document.getElementById('consoleInputRow');
    const originalText = row.textContent;
    if (consoleInputCursorInverted){
        html = '';
        if (consoleInputCursorColumn > 0){
            html = html + consoleInputSpan + originalText.substring(0, consoleInputCursorColumn) + '</span>';
        }
        html = html + consoleInputReverseSpan + originalText.substring(consoleInputCursorColumn, consoleInputCursorColumn + 1) + '</span>';
        if (consoleInputCursorColumn < consoleSizeColumns - 1){
            html = html + consoleInputSpan + originalText.substring(consoleInputCursorColumn + 1) + '</span>';
        }
    } else {
        html = consoleInputSpan + originalText + '</span>';
    }
    row.innerHTML = html;
}


//  Poll the REST server, unless we're not validated in which case, validate
function pollServer(){
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
            $.ajax({
                url: '/poll',
                type: 'GET',
                dataType: 'json',
                headers: {
                    "Client": clientIdent
                },
                success: function (data, textStatus, jqXHR) {
                    console.debug("POLL SUCCESS");//TODO do something with the data
                    console.debug(jqXHR.responseJSON);

                    pollCheckLogEntries(jqXHR);
                    //  TODO check for master status changed
                    //  TODO check for input accepted flag
                    pollCheckStatusMessage(jqXHR);
                    pollCheckReadOnlyMessages(jqXHR);
                    //  TODO check for new read-reply console output
                    //  TODO check for updated jumpkeys
                    //  TODO check for updated system config
                    //  TODO check for updated hardware config
                    polling = false;
                },
                error: function (jqXHR, textStatus /*, errorThrown*/) {
                    console.debug("POLL FAILED:" + jqXHR.status + ":" + textStatus);
                    polling = false;
                    //TODO if it's a 401 (unlikely) or a 403, change some stuff around so that we re-validate
                    //TODO if it's a 403, re-validate
                    //TODO any other status, or nothing at all, we complain then shut down
                }
            })
        }
    }
}


//  after a poll comes back, see if there are any new log entries
function pollCheckLogEntries(jqXHR){
    const newLogEntries = jqXHR.responseJSON.newLogEntries;
    if (newLogEntries != null) {
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
                categoryStr = '<span style="color: ' + logErrorColor + '">' + category + '</span>'
            } else if (category === 'DEBUG') {
                categoryStr = '<span style="color: ' + logDebugColor + '">' + category + '</span>'
            } else if (category === 'TRACE') {
                categoryStr = '<span style="color: ' + logTraceColor + '">' + category + '</span>'
            } else if (category === 'INFO') {
                categoryStr = '<span style="color: ' + logInfoColor + '">' + category + '</span>'
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
}


//  after a poll comes back, check whether there are any new read-only messages
function pollCheckReadOnlyMessages(jqXHR){
    const newReadOnlyMessages = jqXHR.responseJSON.newReadOnlyMessages;
    if (newReadOnlyMessages != null) {
        const rows = document.getElementById('ConsoleOutput').getElementsByTagName('li');
        for (let mx = 0; mx < newReadOnlyMessages.length; mx++){
            scrollConsole(rows);
            const romText = newReadOnlyMessages[mx].text;
            const lastRow = rows[consoleSizeRows - 1];
            const text = romText.replace(spaceRegexPattern, '&nbsp;');
            lastRow.innerHTML = consoleReadOnlySpan + text + '</span>';
        }
    }
}


//  after a poll comes back, see if there's an updated status message
function pollCheckStatusMessage(jqXHR){
    const latestStatusMessage = jqXHR.responseJSON.latestStatusMessage;
    if (latestStatusMessage != null) {
        const rows = document.getElementById('ConsoleOutput').getElementsByTagName('li');
        const newStatusMessages = latestStatusMessage.text;
        if (newStatusMessages.length < statusMessageLines) {
            //  If there fewer status message lines in the new message, erase the bottom existing message(s)
            //  TODO
        } else if (newStatusMessages.length > statusMessageLines) {
            //  If there are more status messages lines in the new message, make sure we don't overwrite any
            //  existing read-reply messages
            //  TODO
        }

        statusMessageLines = newStatusMessages.length;
        for (let mx = 0; mx < statusMessageLines; mx++) {
            const row = rows[mx];
            const text = newStatusMessages[mx].substring(0, consoleSizeColumns).replace(spaceRegexPattern, '&nbsp;');
            row.innerHTML = consoleSystemStatusSpan + text + '</span>';
        }
    }
}


//  Scrolls the console up by one line, presumably in preparation for inserting a new line at the bottom.
//  Some trickery involved - we cannot scroll the system status area which comprises the top {n} lines
//  where {n} >= 0, nor can we scroll a read-reply message off the top. So, we must find the first row
//  below the system status messages which does not contain a read-reply message, and scroll everything else
//  below that row, up by one row.
//  This does raise an interesting question, in that if the system status area + number of read-reply rows
//  is equal to the number of rows on the screen, we're pooched.
//  Since the standard OS only does 2 status lines and a max of 10 read-reply lines, we don't worry about it.
function scrollConsole(rows){
    let rx = statusMessageLines;
    while (!(messageIds[rx++] === -1)){}

    //  Now rx is the index of the first row to be scrolled
    while (rx < consoleSizeRows) {
        const prevRow = rows[rx - 1];
        const thisRow = rows[rx];
        prevRow.innerHTML = thisRow.innerHTML;
        messageIds[rx - 1] = messageIds[rx];
        rx++;
    }

    //  Clear the final row, and we're done
    rows[consoleSizeRows - 1].innerHTML = consoleReadOnlySpan + '</span>';
    messageIds[consoleSizeRows - 1] = -1;
}


//  Initiates a validation sequence with the REST server
function validate(){
    validating = true;
    console.debug("validating...");
    $.ajax({
        url:        '/session',
        type:       'POST',
        dataType:   'json',
        headers: {
            "Authorization": "Basic " + btoa(credentialUsername + ":" + credentialPassword)
        },
        success: function(data /*, textStatus, jqXHR */){
            console.debug("VALIDATE SUCCESS:" + data);
            clientIdent = data;
            validating = false;
        },
        error: function(jqXHR, textStatus /*, errorThrown*/){
            console.debug("VALIDATE FAILED:" + jqXHR.status + ":" + textStatus);
            validating = false;
            //TODO if it's a 401, go back and re-invoke the credential dialog
            //TODO anything else, complain and shut down
        }
    })
}
