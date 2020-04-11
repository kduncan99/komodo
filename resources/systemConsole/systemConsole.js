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
                    var newLogEntries = jqXHR.responseJSON.newLogEntries;
                    if (newLogEntries != null) {
                        console.debug(newLogEntries.toString() + " " + newLogEntries.length);
                        for (var i = 0; i < newLogEntries.length; i++) {
                            console.debug("Log Entry:" + newLogEntries[i].timestamp + " " + newLogEntries[i].entity + " " + newLogEntries[i].message);
                        }
                    }
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

$('#consoleTabs').click(function(){
    poll(); //  TODO a hack to get things rolling - replace this intelligently later
});
