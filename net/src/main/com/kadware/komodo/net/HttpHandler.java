/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.net;

import java.net.Socket;

public interface HttpHandler {

    void handle(
        final Socket socket,
        final HttpRequest exchange
    );
}
