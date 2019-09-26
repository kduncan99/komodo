/*
 * Copyright (c) 2019 by Kurt Duncan
 */

package com.kadware.komodo.commlib;

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
