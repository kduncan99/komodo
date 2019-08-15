/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

/**
 * A class which allows one to track regions of consecutive somethings (such as disk track IDs).
 */
public class Region {

    /**
     * Number of units represented by this Region
     */
    private long _extent = 0;

    /**
     * Identifier or position of the first unit tracked by this Region
     */
    private long _firstUnit = 0;

    /**
     * Standard constructor
     */
    Region() {}

    /**
     * Initial value constructor
     * @param firstUnit index or other identifier of the first unit of the contiguous units represented by this region
     * @param extent number of contiguous units represented by this region
     */
    Region(
        final long firstUnit,
        final long extent
    ) {
        _firstUnit = firstUnit;
        _extent = extent;
    }

    long getExtent() { return _extent; }
    long getFirstUnit() { return _firstUnit; }

    void setExtent(long extent) { _extent = extent; }
    void setFirstUnit(long firstUnit) { _firstUnit = firstUnit; }

    /**
     * Creates a single Region object representing the intersection of this object's space
     * with the specified space.
     * @param firstUnit index or other identifier of the first unit of the contiguous units represented by the specified space
     * @param extent number of contiguous units represented by the specified space
     */
    Region intersection(
        final long firstUnit,
        final long extent
    ) {
        long requestedLimit = firstUnit + extent;
        long ourLimit = _firstUnit + _extent;

        //  Special cases for empty set - if the end of the requested region preceeds the beginning
        //  of ours, or the beginning of the requested region follows the end of ours...
        if ((requestedLimit <= _firstUnit) || (firstUnit >= ourLimit)) {
            return new Region();
        }

        //  Does the beginning of the requested region preceed or align with the beginning of ours?
        if (firstUnit <= _firstUnit) {
            //  If ending of requested region follows or aligns, just return (a copy of) ourself
            if (requestedLimit >= ourLimit) {
                return this;
            }

            //  So, the end of the requested region falls ahead of the end of our region.
            //  Construct a Region representing the intersection.
            long requestedPreceeding = _firstUnit - firstUnit;
            return new Region(_firstUnit, extent - requestedPreceeding);
        }

        //  The beginning of the requested region is within our region, not aligned with the
        //  beginning of our region.  If the end of the requested region falls within or is aligned
        //  with the end of our region, return the requested region.
        if (requestedLimit <= ourLimit) {
            return new Region(firstUnit, extent);
        }

        //  No, the requested end is beyond our end.  Create a region for the intersection.
        long requestedFollowing = requestedLimit - ourLimit;
        return new Region(firstUnit, extent - requestedFollowing);
    }

    /**
     * Convenient wrapper around the method above
     */
    Region intersection(
        final Region compRegion
    ) {
        return intersection(compRegion.getFirstUnit(), compRegion.getExtent());
    }

    /**
     * Tests to see if the region we are tracking intersects at all with the given region
     * @param firstUnit index or other identifier of the first unit of the contiguous units represented by the specified space
     * @param extent number of contiguous units represented by the specified space
     */
    boolean intersects(
        final long firstUnit,
        final long extent
    ) {
        if ((firstUnit >= _firstUnit)
                && (firstUnit < _firstUnit + _extent)) {
            return true;
        }

        if ((firstUnit + extent > _firstUnit)
                && (firstUnit + extent <= _firstUnit + _extent)) {
            return true;
        }

        return false;
    }

    /**
     * Convenient wrapper around the above method
     */
    boolean intersects(
        final Region compRegion
    ) {
        return intersects(compRegion._firstUnit, compRegion._extent);
    }

    /**
     * Convenient method to minalib whether the region this object is tracking, is empty.
     * Caller could just as easily check getExtent()... but we are all enterprisey and stuff.
     */
    boolean isEmpty() { return (_extent == 0); }
}
