//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

"use strict"

const POLL_FAILURE_LIMIT = 5;               //  Max consecutive poll failures allowed before we give up
const SPACE_REGEX_PATTERN = new RegExp(' ', 'g');

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
    $( "#sciTabs" ).tabs();
});


//  Raise credentials window, then schedule the periodic scan
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
    if (polling) {
        if (pollFlashed) {
            //  poll indicator was flashed - dim it now, but leave it lit
            //  TODO
        }
        return;
    }

    if (validating) {
        return;
    }

    if (solicitingCredentials) {
        return;
    }

    if (clientIdent === '') {
        //  we're not validated
        if (validationFailed) {
            //  TODO start solicit credentials
            solicitingCredentials = true;
        } else {
            startValidation();
        }
    } else {
        //  we are validated
        if (pollFailedCounter < POLL_FAILURE_LIMIT) {
            startPoll();
        }
    }
}


function solicitCredentials(prompt) {
    //TODO 'raise' the dialog - however that works
    solicitingCredentials = true;

    //TODO remove the following temporary cruft
    credentialsUserName = 'admin'
    credentialsPassword = 'admin'
    solicitingCredentials = false;
}


//  Poll the REST server
function startPoll() {
    polling = true;
    //  TODO flash poll indicator
    pollFlashed = true;

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
                //  TODO update poll indicator
                alert('Poll Failure Limit Reached - Server may have been shut down')
            }
        }
        polling = false;
    };
    xhr.onerror = function () {
        //  Some sort of network error occurred - complain
        alert('Network error - Cannot contact indicated server');
        validating = false;
        validationFailed = false;
    };
    xhr.send();
}


//  Initiates a validation sequence with the REST server
function startValidation() {
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
            validating = false;
        } else {
            //  input was rejected for content
            console.debug("VALIDATE FAILED:" + xhr.statusText);
            validationFailed = true;
            solicitCredentials('Validation failure - re-enter credentials');
        }
    };
    xhr.onerror = function () {
        //  Some sort of network error occurred - complain
        alert('Network error - cannot contact indicated server - try refreshing the page');
        validating = false;
        validationFailed = false;
    };
    xhr.send(json);
}
