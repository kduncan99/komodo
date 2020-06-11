/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.net;

public class RequestLineDataException extends DataException {

    public RequestLineDataException(
        final String message
    ) {
        super("Error in Request Line:" + message);
    }
}
