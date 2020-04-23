/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.net;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    public final byte[] _responseBody;
    public final Map<String, String> _responseHeaders;
    public final HttpStatus _responseStatus;
    public final String _responseHttpVersion;

    public HttpResponse(
        final String responseHttpVersion,
        final HttpStatus responseStatus,
        final Map<String, String> responseHeaders,
        final byte[] responseBody
    ) {
        _responseHeaders = new HashMap<>(responseHeaders);
        _responseBody = responseBody;
        _responseStatus = responseStatus;
        _responseHttpVersion = responseHttpVersion;
    }

    public static HttpResponse createBadRequest(
        final byte[] optionalContent
    ) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Server", "Komodo");
        if (optionalContent != null) {
            headers.put("Content-Length", String.valueOf(optionalContent.length));
        }
        return new HttpResponse("HTTP/1.1", HttpStatus.BAD_REQUEST, headers, optionalContent);
    }

    public static HttpResponse createInternalServerError(
        final byte[] optionalContent
    ) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Server", "Komodo");
        if (optionalContent != null) {
            headers.put("Content-Length", String.valueOf(optionalContent.length));
        }
        return new HttpResponse("HTTP/1.1", HttpStatus.INTERNAL_SERVER_ERROR, headers, optionalContent);
    }

    public static HttpResponse createNotFound(
        final byte[] optionalContent
    ) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Server", "Komodo");
        if (optionalContent != null) {
            headers.put("Content-Length", String.valueOf(optionalContent.length));
        }
        return new HttpResponse("HTTP/1.1", HttpStatus.NOT_FOUND, headers, optionalContent);
    }

    public void send(
        final Socket socket
    ) {
        //TODO
    }
}
