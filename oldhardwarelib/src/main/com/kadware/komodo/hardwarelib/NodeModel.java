/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Indicates what type of device this particular object is modelling
 */
public enum NodeModel {
    None(0),
    FileSystemDisk(1),      //  Disk drive which uses native host filesystem for storage
    FileSystemPrinter(2),   //  Virtual printer, storing output to the native host filesystem
    FileSystemPunch(3),     //  Virtual card punch, storing output to the native host filesystem
    FileSystemReader(4),    //  Virtual card reader, taking input from the native host filesystem
    FileSystemTape(5),      //  Tape drive which uses native host filesystem for volume persistence
    RAMDisk(6);             //  Disk drive implemented entirely in memory, with persisted backing storage

    private final int _code;

    NodeModel(int code) {
        _code = code;
    }

    public int getCode() {
        return _code;
    }

    public static NodeModel getValue(
        final int code
    ) {
        return switch (code) {
            case 1 -> FileSystemDisk;
            case 2 -> FileSystemPrinter;
            case 3 -> FileSystemPunch;
            case 4 -> FileSystemReader;
            case 5 -> FileSystemTape;
            case 6 -> RAMDisk;
            default -> None;
        };
    }
}
