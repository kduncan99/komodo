/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.configuration.Configuration;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.ScheduleManagerException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestScheduleManager extends ScheduleManager {

    private static class TestExec extends Exec {

        private static final boolean[] JUMP_KEY_TABLE = new boolean[36];

        private TestExec() {
            super(JUMP_KEY_TABLE);
            setConfiguration(new Configuration());
        }

        @Override
        public void sendExecReadOnlyMessage(final String message, final ConsoleType consoleType) {
            System.out.printf("[%s][%s]\n", consoleType, message);
        }

        @Override
        public void sendExecReadOnlyMessage(final String message, final ConsoleId consoleId) {
            System.out.printf("[%s][%s]\n", consoleId, message);
        }

        @Override
        public void stop(final StopCode code) {}
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new TestExec();
    }

    // rotateString ------------------------------------------------------------

    @Test
    public void testRotateString_1() {
        assertEquals("B", rotateString("A"));
        assertEquals("0", rotateString("Z"));
    }

    @Test
    public void testRotateString_2() {
        assertEquals("0", rotateString("Z"));
    }

    @Test
    public void testRotateString_3() {
        assertNull(rotateString("9"));
    }

    @Test
    public void testRotateString_4() {
        assertEquals("AB", rotateString("AA"));
    }

    @Test
    public void testRotateString_5() {
        assertEquals("BA", rotateString("A9"));
    }

    @Test
    public void testRotateString_6() {
        assertEquals("0A", rotateString("Z9"));
    }

    @Test
    public void testRotateString_7() {
        assertEquals("90", rotateString("9Z"));
    }

    @Test
    public void testRotateString_8() {
        assertNull(rotateString("99"));
    }

    @Test
    public void testRotateString_9() {
        assertEquals("ABDAA", rotateString("ABC99"));
    }

    // createUniqueRunid -------------------------------------------------------

    @Test
    public void testCreateUniqueRunidNormal_SYS() throws ExecStoppedException, ScheduleManagerException {
        createBatchRun(new RunCardInfo("@RUN SYS,/USER").setRunId("SYS").setUserId("USER"));
        var runid = createUniqueRunid("SYS");
        assertEquals("SYSA", runid);
    }

    @Test
    public void testCreateUniqueRunidNormal_SYS2() throws ExecStoppedException, ScheduleManagerException {
        createBatchRun(new RunCardInfo("@RUN SYS,/USER").setRunId("SYS").setUserId("USER"));
        createBatchRun(new RunCardInfo("@RUN SYS,/USER").setRunId("SYS").setUserId("USER"));
        var runid = createUniqueRunid("SYS");
        assertEquals("SYSB", runid);
    }

    @Test
    public void testCreateUniqueRunidNormal_SYS26() throws ExecStoppedException, ScheduleManagerException {
        createBatchRun(new RunCardInfo("@RUN SYS,/USER").setRunId("SYS").setUserId("USER"));
        for (int a = 0; a < 26; a++) {
            createBatchRun(new RunCardInfo("@RUN SYS,/USER").setRunId("SYS").setUserId("USER"));
        }

        var runid = createUniqueRunid("SYS");
        assertEquals("SYS0", runid);
    }

    @Test
    public void testCreateUniqueRunidNormal_SYS35() throws ExecStoppedException, ScheduleManagerException {
        createBatchRun(new RunCardInfo("@RUN SYS,/USER").setRunId("SYS").setUserId("USER"));
        for (int a = 0; a < 35; a++) {
            createBatchRun(new RunCardInfo("@RUN SYS,/USER").setRunId("SYS").setUserId("USER"));
        }

        var runid = createUniqueRunid("SYS");
        assertEquals("SYS9", runid);
    }

    @Test
    public void testCreateUniqueRunidNormal_SYS36() throws ExecStoppedException, ScheduleManagerException {
        createBatchRun(new RunCardInfo("@RUN SYS,/USER").setRunId("SYS").setUserId("USER"));
        for (int a = 0; a < 36; a++) {
            createBatchRun(new RunCardInfo("@RUN SYS,/USER").setRunId("SYS").setUserId("USER"));
        }

        var runid = createUniqueRunid("SYS");
        assertEquals("SYTA", runid);
    }

    @Test
    public void testCreateUniqueRunidNormal_MAPPER() throws ExecStoppedException, ScheduleManagerException {
        createBatchRun(new RunCardInfo("@RUN MAPPER").setRunId("MAPPER").setUserId("USER"));
        var runid = createUniqueRunid("MAPPER");
        assertEquals("MAPPES", runid);
    }

    @Test
    public void testCreateUniqueRunidNormal_99() throws ExecStoppedException, ScheduleManagerException {
        createBatchRun(new RunCardInfo("@RUN 99").setRunId("99").setUserId("USER"));
        var runid = createUniqueRunid("99");
        assertEquals("99A", runid);
    }

    @Test(expected = ExecStoppedException.class)
    public void testCreateUniqueRunidNormal_999999() throws ExecStoppedException, ScheduleManagerException {
        createBatchRun(new RunCardInfo("@RUN 999999").setRunId("999999").setUserId("USER"));
        createUniqueRunid("999999");
    }
}
