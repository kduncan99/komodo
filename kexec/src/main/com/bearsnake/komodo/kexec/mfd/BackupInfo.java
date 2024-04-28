/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import java.time.Instant;
import java.util.LinkedList;

public class BackupInfo {

    private Instant _timeBackupCreated;
    private long _fasBits;
    private long _numberOfTextBlocks;
    private long _startingFilePosition;
    private LinkedList<String> _backupReelNumbers = new LinkedList<>();

    public int getNumberOfBackupWords() {
        // we do not support FTA nor multiple backup levels.
        // therefore, the number of backup words is equivalent to the number of backup reels
        return _backupReelNumbers.size();
    }

    public Instant getTimeBackupCreated() { return _timeBackupCreated; }
    public long getFASBits() { return _fasBits; }
    public long getNumberOfTextBlocks() { return _numberOfTextBlocks; }
    public long getStartingFilePosition() { return _startingFilePosition; }
    public LinkedList<String> getBackupReelNumbers() { return _backupReelNumbers; }

    public BackupInfo addBackupReelNumber(final String value) { _backupReelNumbers.add(value); return this; }
    public BackupInfo setTimeBackupCreated(final Instant value) { _timeBackupCreated = value; return this; }
    public BackupInfo setFASBits(final long value) { _fasBits = value; return this; }
    public BackupInfo setNumberOfTextBlocks(final long value) { _numberOfTextBlocks = value; return this; }
    public BackupInfo setStartingFilePosition(final long value) { _startingFilePosition = value; return this; }
    public BackupInfo setBackupReelNumbers(final LinkedList<String> list) { _backupReelNumbers = list; return this; }
}
