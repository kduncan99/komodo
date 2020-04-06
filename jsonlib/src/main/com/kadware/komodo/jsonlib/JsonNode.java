/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.jsonlib;

import com.kadware.komodo.jsonlib.exceptions.NotFoundJsonException;
import com.kadware.komodo.parserlib.Parser;

public interface JsonNode {

    boolean isArray();
    boolean isComponent();
    boolean isComposite();
    boolean isNull();
    boolean isNumber();
    boolean isObject();
    boolean isString();

    void deserialize(final Parser parser)
        throws NotFoundJsonException;
    String serialize();
}
