package com.kadware.komodo.kex;

import com.kadware.komodo.baselib.ArraySlice;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.junit.Test;

/**
 * Temporary 'program' to analyse the content of a binary file
 */
public class Test_Foo {

    private static long TB_SENTINEL = 0_505031_075050L;
    private static long TF_SENTINEL = 0_505031_135050L;

    public ArraySlice readSector(
        RandomAccessFile raf,
        long sectorOffset
    ) throws IOException {
        long wordOffset = sectorOffset * 28;
        long byteOffset = wordOffset * 9 / 2;
        byte[] buffer = new byte[126];
        ArraySlice result = new ArraySlice(new long[28]);
        raf.seek(byteOffset);
        raf.read(buffer);
        result.unpack(buffer, 0, 126, false);
        return result;
    }

    /**
     * A VT volume is comprised of multiple blocks, the length of each block being divisible by 28.
     * If the information in the block does not constitue a multiple of 28, zeros are padded as necessary.
     * Each block consists of consecutive 36-bit words, packed into 9 8-bit bytes per word.
     * Each block has a block type coded in the first word, in fieldata as such:
     *      **VT**  This is the header block for the tape - header contains 10 words
     *          +1      volume number in fieldata, LJSF
     *          +3      site identifier in fieldata, LJSF
     *          +5      sector id of next file mark
     *          +6      sector id of EOT
     *          +9      run-id in fieldata
     *      **VB**  This is a block of data - header contains 6 words
     *          +1,H1   file sequence number, starting at 1 ?
     *          +1,H2   sector length of previous block
     *          +2,H2   block sequence number, starting at 1
     *          +3      data block size in words
     *          +6      first word of data
     *      **EOF*  Emulates a tape mark
     *          +1,H1   psuedo-file-seq# - start at previous file's seq#, consecutive EOF's increment this
     *          +1,H2   sector length of previous block
     *          +2,H2   ?
     *          +4      sector id of previous file mark, or BOT
     *          +5      sector id of next file mark, or zero for EOT
     */
    @Test
    public void show_headers(
    ) throws IOException {
        String fileName = "/home/kduncan/PS2200Share/VT_1000.bin";
        RandomAccessFile raf = new RandomAccessFile(fileName, "r");

        int sector = 0;
        boolean done = false;
        while (!done) {
            try {
                ArraySlice buffer = readSector(raf, sector);

                int dumpWords = 0;
                int blockLength = 0;
                if (buffer.get(0) == TB_SENTINEL) {
                    dumpWords = 7;
                    blockLength = 6 + (int)buffer.get(3);
                } else if (buffer.get(0) == TF_SENTINEL) {
                    dumpWords = 28;
                    blockLength = 10;
                } else if (buffer.get(0) == 0) {
                    break;
                } else {
                    dumpWords = 28;
                    blockLength = 28;
                }

                for (int wx = 0, bx = 0; wx < dumpWords; wx += 7) {
                    ArraySlice subSlice = new ArraySlice(buffer, wx, 7);
                    String disp = String.format("%012o.%03o:  %s  %s  %s",
                                                sector,
                                                wx,
                                                subSlice.toOctal(true),
                                                subSlice.toFieldata(true),
                                                subSlice.toASCII(true));
                    System.out.println(disp);
                }

                int sectorLength = blockLength / 28;
                if (blockLength % 28 > 0) {
                    sectorLength += 1;
                }
                sector += sectorLength;
            } catch (IOException ex) {
                done = true;
            }
        }

        raf.close();
    }

    @Test
    public void first_pass(
    ) throws IOException {
        //  A file has a 10(?)-word header
        //      +0      '**TF**' in fieldata
        //      +1      volume number in fieldata, LJSF
        //      +3      site identifier in fieldata, LJSF
        //      +9      run-id in fieldata
        //  A block has a 6-word header
        //      +0      '**TB**' in fieldata
        //      +1,H1   file sequence number, starting at 1 ?
        //      +1,H2   block sequence number, starting at 1 ?
        //      +2      block sequence number, starting at 1 ?
        //      +3      block size in words

        //  0       **TF** - one full sector
        //  00034   **TB** (034 block length) - wrapped in two full sectors
        //  00124   **TB** (03402 block length) - wrapped in 03434 words = 0101 (65) full sectors
        //  03560   **TB**
        int wordBufferSize = 128 * 28;
        int byteBufferSize = wordBufferSize / 2 * 9;
        String fileName = "/home/kduncan/PS2200Share/VT_1000.bin";

        byte[] byteBuffer = new byte[byteBufferSize];
        long[] wordBuffer = new long[wordBufferSize];

        FileInputStream fis = new FileInputStream(fileName);
        int bytes = fis.read(byteBuffer);

        int words = bytes * 9 / 2;
        ArraySlice as = new ArraySlice(wordBuffer);
        as.unpack(byteBuffer, 0, bytes, false);

        for (int wx = 0; wx < words; wx += 7) {
            ArraySlice subSlice = new ArraySlice(as, wx, 7);
            String msg = String.format("%06o:  %s  %s  %s",
                                       wx,
                                       subSlice.toOctal(true),
                                       subSlice.toFieldata(true),
                                       subSlice.toASCII(true));
            System.out.println(msg);
        }

        fis.close();
    }
}
