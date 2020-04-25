//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

const credentialUsername = 'admin';     //  TODO solicit these from the user
const credentialPassword = 'admin';     //  TODO as above
const spaceRegexPattern = new RegExp(' ', 'g');

const attributes = {
    screenSizeRows: 24,
    screenSizeColumns: 80
};

let clientIdent = '';
let pollFailed = false;                 //  previous poll failed
let validating = false;                 //  validation in progress


//  Do tab panels
$(function() {
    $( "#sciTabs" ).tabs();
});


window.setInterval(function() {
    if (clientIdent === '' && !validating) {
        validate();
    }
}, 1000);


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
    };
    xhr.onerror = function () {
        //  Some sort of network error occurred - complain and unclog input
        alert('Network error - cannot contact indicated server');
        //TODO set validating = false
        //TODO go back to login dialog
    };
    xhr.send(json);
}
