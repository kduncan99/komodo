/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.jsonlib;

import com.kadware.komodo.jsonlib.exceptions.NotFoundJsonException;
import com.kadware.komodo.parserlib.Parser;

public interface JsonEntity {

    boolean isArray();
    boolean isBoolean();
    boolean isComponent();
    boolean isComposite();
    boolean isNull();
    boolean isNumber();
    boolean isObject();
    boolean isString();

    String serialize();
    String[] serializeForDisplay();
}
