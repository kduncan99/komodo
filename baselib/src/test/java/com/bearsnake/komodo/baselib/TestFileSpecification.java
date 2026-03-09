/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestFileSpecification {

    @Test
    public void testConstructor_Basic() {
        FileSpecification fs = new FileSpecification("QUAL", "FILE");
        assertEquals("QUAL", fs.getQualifier());
        assertEquals("FILE", fs.getFilename());
        assertNull(fs.getFileCycleSpecification());
        assertNull(fs.getReadKey());
        assertNull(fs.getWriteKey());
        assertTrue(fs.hasQualifier());
        assertFalse(fs.hasFileCycleSpecification());
    }

    @Test
    public void testConstructor_Full() {
        FileCycleSpecification fcs = FileCycleSpecification.newAbsoluteSpecification(10);
        FileSpecification fs = new FileSpecification("qual", "file", fcs, "read", "write");
        assertEquals("QUAL", fs.getQualifier());
        assertEquals("FILE", fs.getFilename());
        assertSame(fcs, fs.getFileCycleSpecification());
        assertEquals("READ", fs.getReadKey());
        assertEquals("WRITE", fs.getWriteKey());
        assertTrue(fs.hasQualifier());
        assertTrue(fs.hasFileCycleSpecification());
    }

    @Test
    public void testCouldBeInternalName() {
        assertTrue(new FileSpecification(null, "INTERNAL").couldBeInternalName());
        assertFalse(new FileSpecification("QUAL", "FILE").couldBeInternalName());
        assertFalse(new FileSpecification(null, "FILE", FileCycleSpecification.newAbsoluteSpecification(1), null, null).couldBeInternalName());
        assertFalse(new FileSpecification(null, "FILE", null, "READ", null).couldBeInternalName());
        assertFalse(new FileSpecification(null, "FILE", null, null, "WRITE").couldBeInternalName());
    }

    @Test
    public void testParse_Simple() throws FileSpecification.Exception {
        Parser parser = new Parser("MYFILE");
        FileSpecification fs = FileSpecification.parse(parser, " ");
        assertNotNull(fs);
        assertEquals("MYFILE", fs.getFilename());
        assertNull(fs.getQualifier());
    }

    @Test
    public void testParse_WithQualifier() throws FileSpecification.Exception {
        Parser parser = new Parser("MYQUAL*MYFILE");
        FileSpecification fs = FileSpecification.parse(parser, " ");
        assertNotNull(fs);
        assertEquals("MYQUAL", fs.getQualifier());
        assertEquals("MYFILE", fs.getFilename());
    }

    @Test
    public void testParse_WithAsteriskOnly() throws FileSpecification.Exception {
        Parser parser = new Parser("*MYFILE");
        FileSpecification fs = FileSpecification.parse(parser, " ");
        assertNotNull(fs);
        assertEquals("", fs.getQualifier());
        assertEquals("MYFILE", fs.getFilename());
    }

    @Test
    public void testParse_WithCycle() throws FileSpecification.Exception {
        Parser parser = new Parser("MYFILE(123)");
        FileSpecification fs = FileSpecification.parse(parser, " ");
        assertNotNull(fs);
        assertEquals("MYFILE", fs.getFilename());
        assertTrue(fs.hasFileCycleSpecification());
        assertEquals(123, fs.getFileCycleSpecification().getCycle());
    }

    @Test
    public void testParse_WithKeys() throws FileSpecification.Exception {
        Parser parser = new Parser("MYFILE/RKEY/WKEY");
        FileSpecification fs = FileSpecification.parse(parser, " ");
        assertNotNull(fs);
        assertEquals("MYFILE", fs.getFilename());
        assertEquals("RKEY", fs.getReadKey());
        assertEquals("WKEY", fs.getWriteKey());
    }

    @Test
    public void testParse_WithReadKeyOnly() throws FileSpecification.Exception {
        Parser parser = new Parser("MYFILE/RKEY");
        FileSpecification fs = FileSpecification.parse(parser, " ");
        assertNotNull(fs);
        assertEquals("MYFILE", fs.getFilename());
        assertEquals("RKEY", fs.getReadKey());
        assertNull(fs.getWriteKey());
    }

    @Test
    public void testParse_WithEmptyReadKey() throws FileSpecification.Exception {
        Parser parser = new Parser("MYFILE//WKEY");
        FileSpecification fs = FileSpecification.parse(parser, " ");
        assertNotNull(fs);
        assertEquals("MYFILE", fs.getFilename());
        assertNull(fs.getReadKey());
        assertEquals("WKEY", fs.getWriteKey());
    }

    @Test
    public void testParse_Full() throws FileSpecification.Exception {
        Parser parser = new Parser("QUAL*FILE(5)/R/W");
        FileSpecification fs = FileSpecification.parse(parser, " ");
        assertNotNull(fs);
        assertEquals("QUAL", fs.getQualifier());
        assertEquals("FILE", fs.getFilename());
        assertEquals(5, fs.getFileCycleSpecification().getCycle());
        assertEquals("R", fs.getReadKey());
        assertEquals("W", fs.getWriteKey());
    }

    @Test
    public void testParse_InvalidQualifier() {
        Parser parser = new Parser("INVALID_QUALIFIER_TOO_LONG*FILE");
        assertThrows(FileSpecification.InvalidQualifierException.class, () -> FileSpecification.parse(parser, " "));
    }

    @Test
    public void testParse_InvalidFilename() {
        Parser parser = new Parser("INVALID_FILENAME_TOO_LONG_12345");
        assertThrows(FileSpecification.InvalidFilenameException.class, () -> FileSpecification.parse(parser, " "));
    }

    @Test
    public void testParse_InvalidCycle() {
        Parser parser = new Parser("FILE(1000)");
        assertThrows(FileSpecification.InvalidFileCycleException.class, () -> FileSpecification.parse(parser, " "));
    }

    @Test
    public void testParse_InvalidReadKey() {
        Parser parser = new Parser("FILE/INVALID_KEY_TOO_LONG");
        assertThrows(FileSpecification.InvalidReadKeyException.class, () -> FileSpecification.parse(parser, " "));
    }

    @Test
    public void testParse_InvalidWriteKey() {
        Parser parser = new Parser("FILE/R/INVALID_KEY_TOO_LONG");
        assertThrows(FileSpecification.InvalidWriteKeyException.class, () -> FileSpecification.parse(parser, " "));
    }

    @Test
    public void testToString() {
        FileSpecification fs = new FileSpecification("QUAL", "FILE", FileCycleSpecification.newAbsoluteSpecification(5), "READ", "WRITE");
        assertEquals("QUAL*FILE(5)/READ/WRITE", fs.toString());

        fs = new FileSpecification(null, "FILE");
        assertEquals("FILE", fs.toString());

        fs = new FileSpecification("", "FILE");
        assertEquals("*FILE", fs.toString());

        fs = new FileSpecification(null, "FILE", null, "READ", null);
        assertEquals("FILE/READ", fs.toString());

        fs = new FileSpecification(null, "FILE", null, null, "WRITE");
        assertEquals("FILE//WRITE", fs.toString());
    }
}
