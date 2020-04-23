/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.net;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    public final byte[] _requestBody;
    public final Map<String, String> _requestHeaders;
    public final String _requestHttpVersion;
    public final HttpMethod _requestMethod;
    public final String _requestURI;

    public HttpRequest(
        final HttpMethod requestMethod,
        final String requestURI,
        final String requestHttpVersion,
        final Map<String, String> requestHeaders,
        final byte[] requestBody
    ) {
        _requestMethod = requestMethod;
        _requestURI = requestURI;
        _requestHttpVersion = requestHttpVersion;
        _requestHeaders = requestHeaders;
        _requestBody = requestBody;
    }

    public static HttpRequest create(
        final byte[] rawData
    ) throws DataException {
        //  The rawData will have a request component and a body component.
        //  The request component is a set of strings, each terminated with \r\n.
        //  The end of the request component is signified by an empty string, e.g., \r\n\r\n.
        //  Following this \r\n\r\n sentinel is optionally a stream of bytes which may represent
        //  just about anything - possibly more lines of text, but also maybe binary data.

        String[] requestStrings = null;
        byte[] body = new byte[0];
        for (int rdx = 0; rdx < rawData.length - 3; ++rdx) {
            if (rawData[rdx] == '\r'
                && rawData[rdx + 1] == '\n'
                && rawData[rdx + 2] == '\r'
                && rawData[rdx + 3] == '\n' ) {
                requestStrings = new String(rawData, 0, rdx, StandardCharsets.UTF_8).split("\r\n");
                body = Arrays.copyOfRange(rawData, rdx + 4, rawData.length);
                break;
            }
        }

        if (requestStrings == null) {
            requestStrings = new String(rawData, StandardCharsets.UTF_8).split("\r\n");
        }

        if (requestStrings.length == 0) {
            throw new DataException("Missing Request-Line");
        }

        String requestLine = requestStrings[0];
        String[] requestLineComponents = requestLine.split(" ");
        if (requestLineComponents.length != 3) {
            throw new RequestLineDataException("Incorrectly-formatted Request-Line:'" + requestLine + "'");
        }

        String reqMethodStr = requestLineComponents[0];
        String reqURI = requestLineComponents[1];
        String reqHttpVersion = requestLineComponents[2];

        HttpMethod reqMethod = HttpMethod.create(requestLineComponents[0]);
        if (reqMethod == null) {
            throw new RequestLineDataException("Invalid Request Method:'" + reqMethodStr + "'");
        }

        Map<String, String> headers = new HashMap<>();
        for (int rsx = 1; rsx < requestStrings.length; ++rsx) {
            String header = requestStrings[rsx];
            String[] split = header.split(":", 2);
            if (split.length != 2) {
                throw new RequestHeadersDataException("Badly formatted header:'" + header + ";");
            }

            String key = split[0].trim();
            String value = split[1].trim();
            headers.put(key, value);
        }

        return new HttpRequest(reqMethod, reqURI, reqHttpVersion, headers, body);
    }
}
