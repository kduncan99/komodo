/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.hardwarelib.IoFunction;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.hardwarelib.channels.ChannelIoPacket;
import com.bearsnake.komodo.hardwarelib.channels.DiskChannel;
import com.bearsnake.komodo.hardwarelib.channels.TransferFormat;
import com.bearsnake.komodo.hardwarelib.devices.DiskIoPacket;
import com.bearsnake.komodo.hardwarelib.devices.FileSystemDiskDevice;

public class Main {

    private static void displaySector(final Context ctx,
                                      final long wordAddress,
                                      final byte[] byteBuffer,
                                      final int bufferOffset) {
        long sectorAddress = wordAddress / 64;
        long offsetFromSector = wordAddress % 64;
        long trackAddress = wordAddress / 1792;
        long offsetFromTrack = wordAddress % 1792;

        System.out.printf("WordAddress %012o (%d) - Sector %012o (%d) offset (%012o) %d - Track %012o (%d) offset (%012o) %d\n",
                          wordAddress, wordAddress,
                          sectorAddress, sectorAddress,
                          offsetFromSector, offsetFromSector,
                          trackAddress, trackAddress,
                          offsetFromTrack, offsetFromTrack);

        for (int wx = 0, bx = 0; wx < 28; wx += 4, bx += 18) {
            if (ctx.displayRaw()) {
                for (int by = 0; by < 18; by++) {
                    System.out.printf(" %02X", byteBuffer[bufferOffset + bx + by] & 0xFF);
                }
                System.out.print(" :");
            }

            var slice = new ArraySlice(new long[4]);
            slice.unpack(byteBuffer, bufferOffset + bx, 18);
            for (int wy = 0; wy < 4; wy++) {
                System.out.print(" " + Word36.toOctal(slice.get(wy)));
            }

            if (ctx.displayFieldata()) {
                System.out.print("  ");
                for (int wy = 0; wy < 4; wy++) {
                    System.out.print(" " + Word36.toStringFromFieldata(slice.get(wy)));
                }
            }

            if (ctx.displayASCII()) {
                System.out.print("  ");
                for (int wy = 0; wy < 4; wy++) {
                    System.out.print(" " + Word36.toStringFromASCII(slice.get(wy)));
                }
            }

            System.out.println();
        }
    }

    private static void displaySector(final Context ctx,
                                      final long wordAddress,
                                      final ArraySlice wordBuffer) {
        long sectorAddress = wordAddress / 64;
        long offsetFromSector = wordAddress % 64;
        long trackAddress = wordAddress / 1792;
        long offsetFromTrack = wordAddress % 1792;

        System.out.printf("WordAddress %012o (%d) - Sector %012o (%d) offset (%012o) %d - Track %012o (%d) offset (%012o) %d\n",
                          wordAddress, wordAddress,
                          sectorAddress, sectorAddress,
                          offsetFromSector, offsetFromSector,
                          trackAddress, trackAddress,
                          offsetFromTrack, offsetFromTrack);

        for (int wx = 0; wx < 28; wx += 4) {
            for (int wy = 0; wy < 4; wy++) {
                System.out.print(" " + Word36.toOctal(wordBuffer.get(wx + wy)));
            }

            if (ctx.displayFieldata()) {
                System.out.print("  ");
                for (int wy = 0; wy < 4; wy++) {
                    System.out.print(" " + Word36.toStringFromFieldata(wordBuffer.get(wx + wy)));
                }
            }

            if (ctx.displayASCII()) {
                System.out.print("  ");
                for (int wy = 0; wy < 4; wy++) {
                    System.out.print(" " + Word36.toStringFromASCII(wordBuffer.get(wx + wy)));
                }
            }

            System.out.println();
        }
    }

    private static void interpretLabel(final ArraySlice wordBuffer) {
        var sentinel = Word36.toStringFromASCII(wordBuffer.get(0));
        if (!sentinel.equals("VOL1")) {
            System.out.printf("Label Information is not valid - expected 'VOL1', found '%s'\n", sentinel);
            return;
        }

        var packName = (Word36.toStringFromASCII(wordBuffer.get(1)) + Word36.toStringFromASCII(wordBuffer.get(2))).substring(0, 6).trim();
        System.out.println("Label Information");
        System.out.printf("  Pack Name:                 %s\n", packName);
        System.out.printf("  First Dir Trk Word Addr:   %012o (%d)\n", wordBuffer.get(3), wordBuffer.get(3));
        System.out.printf("  Records Per Track:         %d\n", wordBuffer.getH1(4));
        System.out.printf("  Words Per Record:          %d\n", wordBuffer.getH2(4));
        System.out.printf("  Disk Capacity:             %d tracks\n", wordBuffer.get(016));
        System.out.printf("  Words Per Physical Record: %d\n", wordBuffer.getH1(017));
    }

    public static void main(final String[] args) {
        var ctx = new Context();
        if (!ctx.setup(args) || ctx.displayUsage()) {
            Context.showUsage();
            return;
        }

        var disk = new FileSystemDiskDevice("DISK0", ctx.getPackName(), true);
        var diskInfo = disk.getInfo();
        var bytesPerBlock = diskInfo.getBlockSize();
        if (ctx.displayInfo()) {
            System.out.printf("Block Size:  %d bytes = %d sectors\n", bytesPerBlock, bytesPerBlock / 128);
            System.out.printf("Block Count: 0%o (%d) blocks\n", diskInfo.getBlockCount(), diskInfo.getBlockCount());
            System.out.printf("Max BLocks:  0%o (%d) blocks\n", diskInfo.getMaxBlockCount(), diskInfo.getMaxBlockCount());
        }

        Long wordAddress = ctx.getWordAddress();
        if (wordAddress != null) {
            if (ctx.doChannelIO()) {
                DiskChannel channel = new DiskChannel("CHDISK");
                channel.attach(disk);

                var chPacket = new ChannelIoPacket().setNodeIdentifier(disk.getNodeIdentifier())
                                                    .setIoFunction(IoFunction.Read)
                                                    .setFormat(TransferFormat.Packed)
                                                    .setBuffer(new ArraySlice(new long[28]))
                                                    .setDeviceWordAddress(wordAddress);
                channel.routeIo(chPacket);

                System.out.printf("IO Packet: %s\n", chPacket);
                if (chPacket.getIoStatus() == IoStatus.Successful) {
                    displaySector(ctx, wordAddress, chPacket.getBuffer());
                    if (ctx.interpretLabel()) {
                        interpretLabel(chPacket.getBuffer());
                    }
                }
            } else {
                var sectorAddress = ctx.getWordAddress() / 28;
                var byteAddress = sectorAddress * 128;
                var blockId = byteAddress / bytesPerBlock;
                var blockOffset = (int)(byteAddress % bytesPerBlock);

                var ioPacket = new DiskIoPacket();
                ioPacket.setBlockId(blockId)
                        .setBlockCount(1)
                        .setFunction(IoFunction.Read);
                disk.performIo(ioPacket);

                System.out.printf("IO Packet: %s\n", ioPacket);
                if (ioPacket.getStatus() == IoStatus.Successful) {
                    var byteBuffer = ioPacket.getBuffer().array();
                    displaySector(ctx, wordAddress, byteBuffer, blockOffset);
                    if (ctx.interpretLabel()) {
                        var slice = new ArraySlice(new long[4]);
                        slice.unpack(byteBuffer, blockOffset, 126);
                        interpretLabel(slice);
                    }
                }
            }
        }
    }
}
