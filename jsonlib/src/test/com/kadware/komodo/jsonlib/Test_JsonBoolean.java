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

public class Test_JsonBoolean {

    @Test
    public void basic_1() {
        JsonBoolean entity = new JsonBoolean(true);
        assertFalse(entity.isArray());
        assertTrue(entity.isBoolean());
        assertTrue(entity.isComponent());
        assertFalse(entity.isComposite());
        assertFalse(entity.isNull());
        assertFalse(entity.isNumber());
        assertFalse(entity.isString());
    }

    @Test
    public void basic_2() {
        JsonBoolean entity = new JsonBoolean(false);
        assertFalse(entity.isArray());
        assertTrue(entity.isBoolean());
        assertTrue(entity.isComponent());
        assertFalse(entity.isComposite());
        assertFalse(entity.isNull());
        assertFalse(entity.isNumber());
        assertFalse(entity.isString());
    }

    @Test
    public void deserialize_good_1() {
        String text = "TrUe";
        Parser parser = new Parser(text);
        JsonBoolean jb = JsonBoolean.deserialize(parser);
        assertNotNull(jb);
        assertTrue(jb._value);
    }

    @Test
    public void deserialize_good_2() {
        String text = "fALSe";
        Parser parser = new Parser(text);
        JsonBoolean jb = JsonBoolean.deserialize(parser);
        assertNotNull(jb);
        assertFalse(jb._value);
    }

    @Test
    public void deserialize_good_3() {
        String text = "truelies";
        Parser parser = new Parser(text);
        assertNotNull(JsonBoolean.deserialize(parser));
    }

    @Test
    public void deserialize_bad_1() {
        String text = "null false true ";
        Parser parser = new Parser(text);
        assertNull(JsonBoolean.deserialize(parser));
    }

    @Test
    public void deserialize_bad_2() {
        String text = "  false";
        Parser parser = new Parser(text);
        assertNull(JsonBoolean.deserialize(parser));
    }

    @Test
    public void deserialize_bad_3() {
        String text = "";
        Parser parser = new Parser(text);
        assertNull(JsonBoolean.deserialize(parser));
    }
}
