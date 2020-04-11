//  Copyright(c) 2020 by Kurt Duncan - All Rights Reserved

//  General stuff ------------------------------------------------------------------------------------------------------------------

$(function() {
    $( "#consoleTabs" ).tabs();
});

$('#consoleTabs').click(function(){
    poll(); //  TODO a hack to get things rolling - replace this intelligently later
});

var credentialUsername = 'admin';   //  TODO solicit these from the user
var credentialPassword = 'admin';   //  TODO as above
var clientIdent = '';
var polling = false;
var validating = false;

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
                        postLogEntries(newLogEntries);
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

//  Code specific to the log pane --------------------------------------------------------------------------------------------------

function postLogEntries(newLogEntries){
    var tableBody = document.getElementById('LoggingRows');
    var tableRows = tableBody.getElementsByTagName('tr');

    for (var i = 0; i < newLogEntries.length; i++) {
        var timestamp = newLogEntries[i].timestamp;
        var category = newLogEntries[i].category;
        var entity = newLogEntries[i].entity;
        var message = newLogEntries[i].message;

        var dateStr = new Date(timestamp).toISOString();
        var dateElement = document.createElement('td');
        dateElement.innerHTML = dateStr;

        var categoryStr = category.padStart(5, ' ');
        if (category === 'ERROR') {
            categoryStr = '<span style="color: #ff0000">' + category + '</span>'
        } else if (category === 'DEBUG') {
            categoryStr = '<span style="color: #ffff00">' + category + '</span>'
        } else if (category === 'TRACE') {
            categoryStr = '<span style="color: #00ff00">' + category + '</span>'
        } else if (category === 'INFO') {
            categoryStr = '<span style="color: #0000ff">' + category + '</span>'
        }
        var catElement = document.createElement('td');
        catElement.innerHTML = categoryStr;

        var entityElement = document.createElement('td');
        entityElement.innerHTML = entity;

        var messageStr = message.replace(new RegExp(' ', 'g'), '&nbsp;');
        var msgElement = document.createElement('td');
        msgElement.innerHTML = messageStr;

        var newRow = tableBody.insertRow(0);
        newRow.appendChild(dateElement);
        newRow.appendChild(catElement);
        newRow.appendChild(entityElement);
        newRow.appendChild(msgElement);

        tableBody.appendChild(newRow);
    }
}
