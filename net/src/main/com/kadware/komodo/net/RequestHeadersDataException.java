/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.net;

public class RequestHeadersDataException extends DataException {

    public RequestHeadersDataException(
        final String message
    ) {
        super("Error in Request Headers:" + message);
    }
}
