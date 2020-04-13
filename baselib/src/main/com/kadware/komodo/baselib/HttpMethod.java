/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

/**
 * Describes a particular REST operation
 */
public enum HttpMethod {
    DELETE("DELETE"),
    GET("GET"),
    HEAD("HEAD"),
    POST("POST"),
    PUT("PUT"),
    TRACE("TRACE");

    public final String _value;
    HttpMethod(String value) { _value = value; }
}
