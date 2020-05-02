//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

"use strict"

const POLL_FAILURE_LIMIT = 5;               //  Max consecutive poll failures allowed before we give up
const SPACE_REGEX_PATTERN = new RegExp(' ', 'g');
const CREDENTIALS_DIV = document.getElementById('credentials');
const CREDENTIALS_FORM = document.getElementById('credentialsForm');
const CREDENTIALS_USERNAME_FIELD = document.getElementById('credentialsUsername');
const CREDENTIALS_PASSWORD_FIELD = document.getElementById('credentialsPassword');
const CREDENTIALS_PROMPT = document.getElementById('credentialsPrompt');

const ATTRIBUTES = {
    consoleScreenSizeRows: 24,
    consoleScreenSizeColumns: 80
};

let clientIdent = '';               //  if empty, we're not currently validated
let credentialsUserName;
let credentialsPassword;
let pollFailedCounter = 0;          //  consecutive poll failures
let pollFlashed = false;            //  for managing the poll indicator
let polling = false;                //  poll in progress
let solicitingCredentials = false;  //  user is currently being asked for credentials
let validating = false;             //  validation in progress
let validationFailed = false;       //  previous validation attempt failed


//  Set up tab panels
$(function() {
    $( "#sciTabs" ).tabs({
        activate: function(event, ui) {
            if (ui.newPanel[0].id === 'Console') {
                consoleActivate();
            }
        }
    });
});


//  Set up credentials submit handling, raise credentials window, then schedule the periodic scan
CREDENTIALS_FORM.onsubmit = function() {
    credentialsUserName = CREDENTIALS_USERNAME_FIELD.value;
    credentialsPassword = CREDENTIALS_PASSWORD_FIELD.value;
    CREDENTIALS_USERNAME_FIELD.value = '';
    CREDENTIALS_PASSWORD_FIELD.value = '';
    solicitingCredentials = false;
    CREDENTIALS_DIV.style.display = "none";
    return false;
};

solicitCredentials("Enter credentials for managing Komodo");
window.setInterval(periodicScan, 1000);


//  Creates a span of html consisting of a <span> leading tag for specifying color and background color,
//  the given text where literal spaces are replaced by '&nbsp', and a final </span> tag.
//  Parameters should be integers values constructed as 0x{rr}{gg}{bb}
function createSpan(fgcolor, bgcolor, text) {
    const fgStr = '#' + (Number(fgcolor).toString(16).padStart(6, '0'));
    const bgStr = '#' + (Number(bgcolor).toString(16).padStart(6, '0'));
    const openTag = '<span style="color:' + fgStr + '; background-color:' + bgStr + '">'
    const html = text.replace(SPACE_REGEX_PATTERN, '&nbsp;');
    return openTag + html + '</span>';
}


//  Periodically check to see if anything needs to be done.
//  If we're soliciting credentials, validating, or polling, don't do anything.
//  If none of the above are true:
//      if we are not validated
//          If previous validation failed, solicit credentials
//          otherwise start validation
//      otherwise
//          If we've reached the limit of polling failures do nothing (user already has been notified)
//          otherwise start poll
function periodicScan() {
    if (polling | validating || solicitingCredentials || (pollFailedCounter >= POLL_FAILURE_LIMIT)) {
        if (pollFlashed) {
            //  poll indicator was flashed - dim it now, but leave it lit
            pollFlashed = false;
            consoleSetStatusRow()
        }
        return;
    }

    if (clientIdent === '') {
        //  we're not validated
        if (validationFailed) {
            solicitCredentials('Validation failed - re-enter credentials');
        } else {
            startValidation();
        }
    } else {
        startPoll();
    }
}


function solicitCredentials(prompt) {
    validationFailed = false;
    solicitingCredentials = true;
    CREDENTIALS_USERNAME_FIELD.value = '';
    CREDENTIALS_PASSWORD_FIELD.value = '';
    CREDENTIALS_PROMPT.innerText = prompt;
    CREDENTIALS_DIV.style.display = "block";
    CREDENTIALS_USERNAME_FIELD.focus();
}


//  Poll the REST server
function startPoll() {
    polling = true;
    pollFlashed = true;
    consoleSetStatusRow();

    let xhr = new XMLHttpRequest();
    xhr.open('GET', '/poll', true);
    xhr.responseType = "json";
    xhr.setRequestHeader('Content-Type', 'application/json; charset=utf-8');
    xhr.setRequestHeader('Client', clientIdent);
    xhr.onload = function () {
        if (xhr.status === 200 || xhr.status === 201) {
            //  input was accepted - should be 200, but we'll take 201
            const newLogEntries = xhr.response.newLogEntries;
            if ((newLogEntries != null) && (newLogEntries.length > 0)) {
                logsProcessNewEntries(newLogEntries);
            }

            const newMessages = xhr.response.outputMessages;
            if ((newMessages != null) && (newMessages.length > 0)) {
                consoleProcessNewMessages(newMessages);
            }

            //  TODO jumpkey entries
        } else if (xhr.status === 401) {
            console.debug("Session is no longer validated:" + xhr.statusText);
            clientIdent = '';
            solicitCredentials('Session is no longer validated - re-enter credentials');
        } else {
            console.debug("POLL FAILED:" + xhr.statusText);
            pollFailedCounter++;
            if (pollFailedCounter >= POLL_FAILURE_LIMIT) {
                alert('Poll Failure Limit Reached - Server may have been shut down')
            }
        }
        pollFlashed = false;
        polling = false;
        consoleSetStatusRow();
    };
    xhr.onerror = function () {
        //  Some sort of network error occurred - complain
        validating = false;
        validationFailed = false;
        pollFailedCounter++;
        pollFlashed = false;
        polling = false;
        if (pollFailedCounter >= POLL_FAILURE_LIMIT) {
            alert('Poll Failure Limit Reached - Server may have been shut down')
        }
        consoleSetStatusRow();
    };
    xhr.send();
}


//  Initiates a validation sequence with the REST server
function startValidation() {
    validationFailed = false;
    validating = true;
    pollFailedCounter = 0;
    console.debug("validating...");
    let xhr = new XMLHttpRequest();
    xhr.open('POST', '/session', true);
    xhr.responseType = "json";
    xhr.setRequestHeader('Content-Type', 'application/json; charset=utf-8');
    xhr.setRequestHeader('Authorization', 'Basic ' + btoa(credentialsUserName + ':' + credentialsPassword));
    let json = JSON.stringify(ATTRIBUTES);
    xhr.onload = function () {
        if (xhr.status === 200 || xhr.status === 201) {
            //  input was accepted - should be 201, but we'll take 200
            console.debug("VALIDATE SUCCESS:" + xhr.response);
            clientIdent = xhr.response;
        } else {
            //  input was rejected for content
            console.debug("VALIDATE FAILED:" + xhr.statusText);
            validationFailed = true;
        }

        validating = false;
        consoleSetStatusRow();
    };
    xhr.onerror = function () {
        //  Some sort of network error occurred - complain
        alert('Network error - cannot contact indicated server - try refreshing the page');
        validationFailed = false;
        validating = false;
        consoleSetStatusRow();
    };

    xhr.send(json);
    consoleSetStatusRow();
}
