/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import java.util.LinkedList;

public class TapeFileCycleInfo extends FileCycleInfo {

    private LinkedList<String> _reelTable;
    private TapeDensity _density;
    private TapeFormat _format;
    private TapeFeatures _features;
    private TapeMTAPOP _mtaPop;
    private int _noiseConstant;
    private int _numberOfReels; // only used for assistance in serializing/deserializing the reel table
    private String _processorTranslator;
    private String _tapeTranslator;
    private String _ctlPool;

    public TapeFileCycleInfo(
        final MFDSector leadItem0
    ) {
        super(leadItem0);
    }

    public LinkedList<String> getReelTable() { return _reelTable; }
    public TapeDensity getDensity() { return _density; }
    public TapeFormat getFormat() { return _format; }
    public TapeFeatures getFeatures() { return _features; }
    public TapeMTAPOP getMTAPOP() { return _mtaPop; }
    public int getNoiseConstant() { return _noiseConstant; }
    public String getProcessorTranslator() { return _processorTranslator; }
    public String getTapeTranslator() { return _tapeTranslator; }
    public String getCTLPool() { return _ctlPool; }

    public TapeFileCycleInfo addReelTableEntry(final String value) { _reelTable.add(value); return this; }
    public TapeFileCycleInfo setDensity(final TapeDensity value) { _density = value; return this; }
    public TapeFileCycleInfo setFormat(final TapeFormat value) { _format = value; return this; }
    public TapeFileCycleInfo setFeatures(final TapeFeatures value) { _features = value; return this; }
    public TapeFileCycleInfo setMTAPOP(final TapeMTAPOP value) { _mtaPop = value; return this; }
    public TapeFileCycleInfo setNoiseConstant(final int value) { _noiseConstant = value; return this; }
    public TapeFileCycleInfo setReelTable(final LinkedList<String> list) { _reelTable = list; return this; }
    public TapeFileCycleInfo setProcessorTranslator(final String value) { _processorTranslator = value; return this; }
    public TapeFileCycleInfo setTapeTranslator(final String value) { _tapeTranslator = value; return this; }
    public TapeFileCycleInfo setCTLPool(final String value) { _ctlPool = value; return this; }

    /**
     * Loads this object from the content in the given main item MFD sector chain.
     * @param mfdSectors main item chain
     */
    public void loadFromMainItemChain(
        final LinkedList<MFDSector> mfdSectors
    ) {
        super.loadFromMainItemChain(mfdSectors);
        var sector0 = mfdSectors.getFirst().getSector();
        _density = TapeDensity.getTapeDensity(sector0.getS1(024));
        _format = new TapeFormat().extract(sector0.getS2(024));
        _features = new TapeFeatures().extract(sector0.getS3(024))
                                      .extractExtension(sector0.getT1(025))
                                      .extractExtension1(sector0.getS4(025));
        _mtaPop = new TapeMTAPOP().extract(sector0.getS3(025));
        _noiseConstant = (int)sector0.getT3(025);
        _numberOfReels = (int)sector0.getS2(024);

        var ptVal = sector0.get(026);
        _processorTranslator = ptVal == 0 ? "" : Word36.toStringFromFieldata(ptVal);

        var ttVal = sector0.get(027);
        _tapeTranslator = ttVal == 0 ? "" : Word36.toStringFromFieldata(ttVal);

        if (sector0.get(030) == 0) {
            _ctlPool = "";
        } else {
            _ctlPool = (Word36.toStringFromFieldata(sector0.get(030)) + Word36.toStringFromFieldata(sector0.get(031))).trim();
        }

        // First two reels for the cataloged tape files are stored in main item sector 0.
        int reelCount = (int)sector0.getH2(024);
        _reelTable = new LinkedList<>();
        if (reelCount > 0) {
            _reelTable.add(Word36.toStringFromFieldata(sector0.get(032)));
            if (reelCount > 1) {
                _reelTable.add(Word36.toStringFromFieldata(sector0.get(033)));
            }
        }
    }

    /**
     * Loads the reel table from the reel item table sectors.
     * The first two reels should already have been populated from main item sector 0.
     * @param mfdSectors chain of reel table sectors describing reels 3+
     */
    public void loadFromReelItemTables(
        final LinkedList<MFDSector> mfdSectors
    ) {
        for (var ms : mfdSectors) {
            var sector = ms.getSector();
            for (int ex = 0; (ex < 25) && (_reelTable.size() >= _numberOfReels); ex++) {
                _reelTable.add(Word36.toStringFromFieldata(sector.get(2 + ex)));
            }
        }
    }

    /**
     * Populates cataloged file main item sectors 0 and 1
     * Invokes super class to do the most common things, then fills in anything related to mass storage
     * @param mfdSectors enough MFDSectors to store all of the information required for this file cycle.
     */
    @Override
    public void populateMainItems(
        final LinkedList<MFDSector> mfdSectors
    ) throws ExecStoppedException {
        super.populateMainItems(mfdSectors);
        var sector0 = mfdSectors.getFirst().getSector();
        sector0.setS1(024, _density == null ? 0 : _density.getValue());
        sector0.setS2(024, _format == null ? 0 : _format.compose());
        sector0.setS3(024, _features == null ? 0 : _features.compose());
        sector0.setH2(024, _reelTable.size());
        sector0.setT1(025, _features == null ? 0 : _features.composeExtension());
        sector0.setS3(025, _mtaPop == null ? 0 : _mtaPop.compose());
        sector0.setS4(025, _features == null ? 0 : _features.composeExtension1());
        sector0.setT3(025, _noiseConstant);
        if (_processorTranslator != null) {
            sector0.set(026, Word36.stringToWordFieldata(_processorTranslator));
        }
        if (_tapeTranslator != null) {
            sector0.set(027, Word36.stringToWordFieldata(_tapeTranslator));
        }

        if (_ctlPool != null) {
            String paddedCTLPool = String.format("%-12s", _ctlPool);
            sector0.set(030, Word36.stringToWordFieldata(paddedCTLPool.substring(0, 6)));
            sector0.set(031, Word36.stringToWordFieldata(paddedCTLPool.substring(6)));
        }

        // First two reels for the cataloged tape files are stored in main item sector 0.
        var iter = _reelTable.iterator();
        if (iter.hasNext()) {
            var reel = iter.next();
            sector0.set(032, Word36.stringToWordFieldata(reel));
            if (iter.hasNext()) {
                reel = iter.next();
                sector0.set(033, Word36.stringToWordFieldata(reel));
            }
        }
    }
}
