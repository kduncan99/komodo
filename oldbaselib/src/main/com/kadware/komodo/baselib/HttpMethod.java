/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import java.util.HashMap;
import java.util.Map;

/**
 * Describes a particular HTTP operation
 */
public enum HttpMethod {

    CONNECT("CONNECT"),
    DELETE("DELETE"),
    GET("GET"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS"),
    POST("POST"),
    PUT("PUT"),
    TRACE("TRACE");

    public final String _value;
    HttpMethod(String value) { _value = value; }

    private static final Map<String, HttpMethod> _lookup = new HashMap<>();
    static {
        _lookup.put(CONNECT._value, CONNECT);
        _lookup.put(DELETE._value, DELETE);
        _lookup.put(GET._value, GET);
        _lookup.put(HEAD._value, HEAD);
        _lookup.put(OPTIONS._value, OPTIONS);
        _lookup.put(POST._value, POST);
        _lookup.put(PUT._value, PUT);
        _lookup.put(TRACE._value, TRACE);
    }

    public static HttpMethod create(
        final String value
    ) {
        return _lookup.get(value.toUpperCase());
    }
}
