//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

//  Globals ------------------------------------------------------------------------------------------------------------------------

const maxLoggingRows = 200;
const credentialUsername = 'admin';   //  TODO solicit these from the user
const credentialPassword = 'admin';   //  TODO as above
let clientIdent = '';
let polling = false;                    //  poll in progress
let statusMessageLines = 0;             //  Number of status messages lines at top of console
let validating = false;                 //  validation in progress

//  Colors for OS console (presented on a black background, so they should be lighter)
const consoleInputColor = '#ffffff';
const consoleReadOnlyColor = '#00ff00';
const consoleReadReplyColor = '#ff3f3f';
const consoleSystemStatusColor = '#00ffff';

//  Colors for log pane (presented on a white background, so they should be darker)
const logErrorColor = '#ff0000';
const logDebugColor = '#ffff00';
const logTraceColor = '#00ff00';
const logInfoColor = '#0000ff';


//  General stuff ------------------------------------------------------------------------------------------------------------------

$(function() {
    $( "#consoleTabs" ).tabs();
});

$('#consoleTabs').click(function(){
    poll(); //  TODO a hack to get things rolling - replace this intelligently later
});

function poll(){
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
                    //  TODO check for new read-only console output
                    //  TODO check for new read-reply console output
                    //  TODO check for updated jumpkeys
                    //  TODO check for updated system config
                    //  TODO check for updated hardware config
                    polling = false;
                    setTimeout(poll,10);
                },
                error: function (jqXHR, textStatus, errorThrown) {
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

function pollCheckLogEntries(jqXHR){
    const newLogEntries = jqXHR.responseJSON.newLogEntries;
    if (newLogEntries != null) {
        const tableBody = document.getElementById('LoggingRows');
        const tableRows = tableBody.getElementsByTagName('tr');
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

function pollCheckStatusMessage(jqXHR){
    var latestStatusMessage = jqXHR.responseJSON.latestStatusMessage;
    const regexPattern = new RegExp(' ', 'g');
    if (latestStatusMessage != null) {
        const tableBody = document.getElementById('ConsoleRows');
        const tableRows = tableBody.getElementsByTagName('tr');
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
        for (var mx = 0; mx < statusMessageLines; mx++) {
            const row = tableBody.rows.item(mx);
            row.innerHTML = '<span style="color: ' + consoleSystemStatusColor + '">' + newStatusMessages[mx].replace(regexPattern, '&nbsp;') + '</span>';
        }
    }
}

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
        success: function(data, textStatus, jqXHR){
            console.debug("VALIDATE SUCCESS:" + data);
            clientIdent = data;
            validating = false;
            setTimeout(poll, 100);
        },
        error: function(jqXHR, textStatus, errorThrown){
            console.debug("VALIDATE FAILED:" + jqXHR.status + ":" + textStatus);
            validating = false;
            //TODO if it's a 401, go back and re-invoke the credential dialog
            //TODO anything else, complain and shut down
        }
    })
}
