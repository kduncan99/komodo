/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import java.util.TreeMap;
import java.util.Map;

/**
 * A replacement for Region/RegionContainer
 * Tracks a contiguous region of things - such as disk tracks or the like
 * Allows the client to manipulate the region by carving out sections with unique attributes.
 */
public class RegionTracker {

    public interface IAttributes {};

    public class BadSubRegionException extends Exception {}
    public class SubRegionNotAssignedException extends Exception {}
    public class OutOfSpaceException extends Exception {}

    public class SubRegion {
        public final boolean _assigned;
        public final long _position;
        public final long _extent;
        public final IAttributes _attributes;

        public SubRegion(
            final boolean assigned,
            final long position,
            final long extent,
            final IAttributes attributes
        ) {
            _assigned = assigned;
            _position = position;
            _extent = extent;
            _attributes = attributes;
        }
    }

    private final Map<Long, SubRegion> _content = new TreeMap<>();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates a RegionTracker with the specified extent, and a single unassigned subregion
     * @param totalExtent total area represented by this tracker
     */
    public RegionTracker(
        final long totalExtent
    ) {
        _content.put(0L, new SubRegion(false, 0, totalExtent, null));
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Assigns space from the overall region.
     * Internally, it locates an unassigned subregion containing an adequate amount of space, and splits it into an assigned
     * subregion of the requested extent, and a smaller unassigned subregion.  If we find an exact match on extent, then
     * we merely replace the unassigned region with an assigned region.
     * @param extent requested extent
     * @param attributes caller-specified attributes (null if n/a)
     * @return newly-created assigned region
     * @throws OutOfSpaceException if we cannot find any sufficient unassigned subregions
     */
    public SubRegion assign(
        final long extent,
        final IAttributes attributes
    ) throws OutOfSpaceException {
        synchronized (this) {
            //  Look for an exact match first
            for (SubRegion sub : _content.values()) {
                if ((!sub._assigned) && (sub._extent == extent)){
                    SubRegion newSub = new SubRegion(true, sub._position, sub._extent, attributes);
                    _content.put(newSub._position, newSub);
                    return newSub;
                }
            }

            //  Now look for any available subregion large enough
            for (SubRegion sub : _content.values()) {
                if ((!sub._assigned) && (sub._extent > extent)) {
                    SubRegion newAssigned = new SubRegion(true, sub._position, extent, attributes);
                    SubRegion newUnassigned = new SubRegion(false,
                                                            sub._position + extent,
                                                            sub._extent - extent,
                                                            null);
                    _content.put(newAssigned._position, newAssigned);
                    _content.put(newUnassigned._position, newUnassigned);
                    return newAssigned;
                }
            }
        }

        throw new OutOfSpaceException();
    }

    /**
     * Retrieves the SubRegion associated with the indicated position
     * @param position indicates the subregion of interest
     * @return SubRegion object
     * @throws BadSubRegionException if the position is not the starting position of any subregion
     */
    public SubRegion getSubRegion(
        final long position
    ) throws BadSubRegionException {
        synchronized (this) {
            SubRegion sub = _content.get(position);
            if (sub == null) {
                throw new BadSubRegionException();
            } else {
                return sub;
            }
        }
    }

    /**
     * Unassigns the subregion which begins at the indicated position
     * @param position indicates the subregion to be unassigned
     * @throws BadSubRegionException if the position is not the starting position of any subregion
     * @throws SubRegionNotAssignedException if the subregion indicated by the position is not assigned
     */
    public void unassign(
        final long position
    ) throws BadSubRegionException,
             SubRegionNotAssignedException {
        synchronized (this) {
            SubRegion sub = _content.get(position);
            if (sub == null) {
                throw new BadSubRegionException();
            } else if (!sub._assigned) {
                throw new SubRegionNotAssignedException();
            } else {
                _content.put(position, new SubRegion(false, position, sub._extent, null));
            }
        }
    }
}
