/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import java.util.LinkedList;
import java.util.List;

/**
 * Maintains a collection of Region classes, the sum of which describe a contiguous set of something.
 * One possible use would be to track disk allocation by track identifier...
 */
public class RegionContainer {

    private final List<Region> _regions = new LinkedList<>();

    /**
     * Default constructor
     */
    RegionContainer() {}

    /**
     * Standard constructor with initial discrete parameters from which we construct a single Region
     * @param initialFirstUnit first identifier of the space represented by all the regions in the container
     * @param initialExtent length of the space
     */
    RegionContainer(
        final long initialFirstUnit,
        final long initialExtent
    ) {
        _regions.add(new Region(initialFirstUnit, initialExtent));
    }

    /**
     * Standard constructor with initial discrete parameters from which we construct a single Region
     * @param initialFirstUnit first identifier of the space represented by all the regions in the container
     * @param initialExtent length of the space
     */
    RegionContainer(
        final int initialFirstUnit,
        final int initialExtent
    ) {
        _regions.add(new Region(initialFirstUnit, initialExtent));
    }

    /**
     * Standard constructor using a Region for our initial value
     */
    RegionContainer(
        final Region initialRegion
    ) {
        //  Do a copy here, so the caller can't mess us up by modifying the initialRegion object
        _regions.add(new Region(initialRegion.getFirstUnit(), initialRegion.getExtent()));
    }

    /**
     * Retrieves a reference to the _regions container.
     * Should only be used for unit testing
     */
    List<Region> getRegions(
    ) {
        return _regions;
    }

    /**
     * Adds a Region to our list of regions.  Does not join contiguous regions, but it does
     * refuse to add a region which intersects with any existing regions.
     * @return true if successful
     */
    public boolean append(
        final long firstUnit,
        final long extent
    ) {
        for (Region region : _regions) {
            if ( region.intersects(firstUnit, extent)) {
                return false;
            }
        }

        _regions.add(new Region(firstUnit, extent));
        return true;
    }

    /**
     * Convenient wrapper for above method
     */
    public boolean append(
        final Region region
    ) {
        return append(region.getFirstUnit(), region.getExtent());
    }

    /**
     * Removes whatever portion of the requested region which intersects the various regions,
     * from those regions.  In one case, said carving will produce two small regions out of one,
     * both of which need to remain in our list -- for this reason, carve() is implemented only
     * on this container, not on the Region class.
     * @return true if we found any intersections, and thus took any actions
     */
    boolean carve(
        final long firstUnit,
        final long extent
    ) {
        boolean result = false;
        long requestedLimit = firstUnit + extent;
        for (int rx = 0; rx < _regions.size(); ++rx) {
            Region region = _regions.get(rx);
            long ourLimit = region.getFirstUnit() + region.getExtent();
            if (firstUnit <= region.getFirstUnit()) {
                //  Requested region starting point is less than or aligns with ours.  So...
                if (requestedLimit <= region.getFirstUnit()) {
                    //  Case: requested region entirely preceeds ours, so no intersection and no change
                    //  do nothing
                } else if (requestedLimit >= ourLimit) {
                    //  Case: requested region contains our complete region - we become empty
                    region.setExtent(0);
                    region.setFirstUnit(0);
                    result = true;
                } else {
                    //  Case: requested region includes top of our region, but not the end.
                    //  Since the requested first word includes our first word, we just chop out
                    //  a portion of the front of our region, as appropriate.
                    region.setFirstUnit(requestedLimit);
                    region.setExtent(ourLimit - requestedLimit);
                    result = true;
                }
            } else {
                //  Requested region starting point is somewhere beyond our starting point.
                if (firstUnit >= ourLimit) {
                    //  Case: requested starting point is at or beyond our limit, so no intersection
                    //  Do nothing
                } else if (requestedLimit >= ourLimit) {
                    //  Case: Requested region ending point aligns with, or is beyond ours.
                    //  Truncate our region to remove the overlapping portion.
                    region.setExtent(firstUnit - region.getFirstUnit());
                    result = true;
                } else {
                    //  Case: Requested region ending point is inside of our ending point.
                    //  Since the requested starting point is also inside of our starting point,
                    //  it is clear that the caller wants to carve a region out of our region,
                    //  leaving something at the front, and something at the end.
                    //
                    //  We will create a new region to represent the portion at the front and
                    //  insert it into the container ahead of *this* region, then update *this*
                    //  region to represent the portion at the back end.
                    //
                    //  We do it this way specifically so that we don't iterate into a Region
                    //  which we know has no intersection with the requested region and waste time.
                    long frontExtent = firstUnit - region.getFirstUnit();
                    Region newRegion = new Region(region.getFirstUnit(), frontExtent);
                    _regions.add(rx, newRegion);

                    region.setFirstUnit(requestedLimit);
                    region.setExtent(ourLimit - requestedLimit);
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * As above, but using a Region object as a parameter
     */
    boolean carve(
        final Region region
    ) {
        return carve(region.getFirstUnit(), region.getExtent());
    }

    //  As above, but for carving out multiple regions defined by the provided container,
    //  from *this* container.
    boolean carve(
        final RegionContainer regions
    ) {
        boolean result = false;
        for (Region region : regions._regions) {
            if (carve(region)) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Produces a RegionContainer containing Region objects which represent the various intersectons of the given region
     * (defined by the firstUnit and extent parameters) and the regions in this container.
     * It is entirely possible that the resulting container might contain regions which are contiguous.
     */
    RegionContainer intersection(
        final long firstUnit,
        final long extent
    ) {
        RegionContainer result = new RegionContainer();

        for (Region region : _regions) {
            Region intersection = region.intersection(firstUnit, extent);
            if (intersection.getExtent() > 0) {
                result.append(intersection.getFirstUnit(), intersection.getExtent());
            }
        }

        return result;
    }

    /**
     * As above, but using a Region as a parameter instead of discrete values
     */
    RegionContainer intersection(
        final Region region
    ) {
        return intersection(region.getFirstUnit(), region.getExtent());
    }

    /**
     * Indicates whether the given region defined by firstUnit and extent intersects any region in this container.
     */
    boolean intersects(
        final long firstUnit,
        final long extent
    ) {
        for (Region region : _regions) {
            if (region.intersects(firstUnit, extent)) {
                return true;
            }
        }

        return false;
    }

    /**
     * As above, but using a Region parameter instead of discrete values
     */
    boolean intersects(
        final Region region
    ) {
        return intersects(region.getFirstUnit(), region.getExtent());
    }

    /**
     * If this container has no regions, or all regions it has are empty, then the container is considered empty.
     */
    public boolean isEmpty() {
        for (Region region : _regions) {
            if (!region.isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
