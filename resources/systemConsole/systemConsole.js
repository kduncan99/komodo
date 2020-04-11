//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

$(function() {
    $( "#consoleTabs" ).tabs();
});

var credentialUsername = 'admin';   //  TODO solicit these from the user
var credentialPassword = 'admin';   //  TODO as above
var clientIdent = '';
var polling = false;
var validating = false;

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
        },
        error: function(jqXHR, textStatus, errorThrown){
            console.debug("VALIDATE FAILED:" + jqXHR.status + ":" + textStatus);
        },
        complete: function(){
            validating = false;
        }
    })
}

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
                    console.debug("POLL SUCCESS:" + data);
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    console.debug("POLL FAILED:" + jqXHR.status + ":" + textStatus);
                },
                complete: function(){
                    polling = false;
                }
            })
        }
    }
}

$('#consoleTabs').click(function(){
    poll();
});
