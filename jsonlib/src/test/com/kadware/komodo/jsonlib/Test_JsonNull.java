/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.jsonlib;

import com.kadware.komodo.parserlib.Parser;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class Test_JsonNull {

    @Test
    public void basic() {
        JsonNull entity = new JsonNull();
        assertFalse(entity.isArray());
        assertFalse(entity.isBoolean());
        assertTrue(entity.isComponent());
        assertFalse(entity.isComposite());
        assertTrue(entity.isNull());
        assertFalse(entity.isNumber());
        assertFalse(entity.isString());
    }

    @Test
    public void deserialize_good_1() {
        String text = "null";
        Parser parser = new Parser(text);
        assertNotNull(JsonNull.deserialize(parser));
    }

    @Test
    public void deserialize_good_2() {
        String text = "null";
        Parser parser = new Parser(text);
        assertNotNull(JsonNull.deserialize(parser));
    }

    @Test
    public void deserialize_good_3() {
        String text = "null  false";
        Parser parser = new Parser(text);
        assertNotNull(JsonNull.deserialize(parser));
    }

    @Test
    public void deserialize_bad_1() {
        String text = "  false true ";
        Parser parser = new Parser(text);
        assertNull(JsonNull.deserialize(parser));
    }

    @Test
    public void deserialize_bad_2() {
        String text = "  null";
        Parser parser = new Parser(text);
        assertNull(JsonNull.deserialize(parser));
    }
}
