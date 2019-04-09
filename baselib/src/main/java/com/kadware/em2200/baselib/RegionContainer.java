/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

import java.util.LinkedList;
import java.util.List;

import com.kadware.em2200.baselib.types.Counter;
import com.kadware.em2200.baselib.types.Identifier;

/**
 * Maintains a collection of Region classes, the sum of which describe a contiguous set of something.
 * One possible use would be to track disk allocation by track identifier...
 */
public class RegionContainer {

    private final List<Region> _regions = new LinkedList<>();

    /**
     * Default constructor
     */
    public RegionContainer() {}

    /**
     * Standard constructor with initial discrete parameters from which we construct a single Region
     * <p>
     * @param initialFirstUnit
     * @param initialExtent
     */
    public RegionContainer(
        final Identifier initialFirstUnit,
        final Counter initialExtent
    ) {
        _regions.add(new Region(initialFirstUnit, initialExtent));
    }

    /**
     * Standard constructor with initial discrete parameter values from which we construct a single Region.
     * Be careful with parameter ordering.
     * <p>
     * @param initialFirstUnit
     * @param initialExtent
     */
    public RegionContainer(
        final long initialFirstUnit,
        final long initialExtent
    ) {
        _regions.add(new Region(initialFirstUnit, initialExtent));
    }

    /**
     * Standard constructor using a Region for our initial value
     * <p>
     * @param initialRegion
     */
    public RegionContainer(
        final Region initialRegion
    ) {
        //  Do a copy here, so the caller can't mess us up by modifying the initialRegion object
        _regions.add(new Region(initialRegion.getFirstUnit(), initialRegion.getExtent()));
    }

    /**
     * Retrieves a reference to the _regions container.
     * Should only be used for unit testing
     * <p>
     * @return
     */
    public List<Region> getRegions(
    ) {
        return _regions;
    }

    /**
     * Adds a Region to our list of regions.  Does not join contiguous regions, but it does
     * refuse to add a region which intersects with any existing regions.
     * <p>
     * @param firstUnit
     * @param extent
     * <p>
     * @return
     */
    public boolean append(
        final Identifier firstUnit,
        final Counter extent
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
     * <p>
     * @param region
     * <p>
     * @return
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
     * <p>
     * @param firstUnit
     * @param extent
     * <p>
     * @return true if we found any intersections, and thus took any actions
     */
    public boolean carve(
        final Identifier firstUnit,
        final Counter extent
    ) {
        boolean result = false;
        long requestedLimit = firstUnit.getValue() + extent.getValue();
        for (int rx = 0; rx < _regions.size(); ++rx) {
            Region region = _regions.get(rx);
            long ourLimit = region.getFirstUnit().getValue() + region.getExtent().getValue();
            if (firstUnit.getValue() <= region.getFirstUnit().getValue()) {
                //  Requested region starting point is less than or aligns with ours.  So...
                if (requestedLimit <= region.getFirstUnit().getValue()) {
                    //  Case: requested region entirely preceeds ours, so no intersection and no change
                    //  do nothing
                } else if (requestedLimit >= ourLimit) {
                    //  Case: requested region contains our complete region - we become empty
                    region.setExtent(new Counter(0));
                    region.setFirstUnit(new Identifier(0));
                    result = true;
                } else {
                    //  Case: requested region includes top of our region, but not the end.
                    //  Since the requested first word includes our first word, we just chop out
                    //  a portion of the front of our region, as appropriate.
                    region.setFirstUnit(new Identifier(requestedLimit));
                    region.setExtent(new Counter(ourLimit - requestedLimit));
                    result = true;
                }
            } else {
                //  Requested region starting point is somewhere beyond our starting point.
                if (firstUnit.getValue() >= ourLimit) {
                    //  Case: requested starting point is at or beyond our limit, so no intersection
                    //  Do nothing
                } else if (requestedLimit >= ourLimit) {
                    //  Case: Requested region ending point aligns with, or is beyond ours.
                    //  Truncate our region to remove the overlapping portion.
                    region.setExtent(new Counter(firstUnit.getValue() - region.getFirstUnit().getValue()));
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
                    long frontExtent = firstUnit.getValue() - region.getFirstUnit().getValue();
                    Region newRegion = new Region(region.getFirstUnit(), new Counter(frontExtent));
                    _regions.add(rx, newRegion);

                    region.setFirstUnit(new Identifier(requestedLimit));
                    region.setExtent(new Counter(ourLimit - requestedLimit));
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * As above, but using a Region object as a parameter
     * <p>
     * @param region
     * <p>
     * @return
     */
    public boolean carve(
        final Region region
    ) {
        return carve(region.getFirstUnit(), region.getExtent());
    }

    //  As above, but for carving out multiple regions defined by the provided container,
    //  from *this* container.
    public boolean carve(
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
     * <p>
     * @param firstUnit
     * @param extent
     * <p>
     * @return
     */
    public RegionContainer intersection(
        final Identifier firstUnit,
        final Counter extent
    ) {
        RegionContainer result = new RegionContainer();

        for (Region region : _regions) {
            Region intersection = region.intersection(firstUnit, extent);
            if (intersection.getExtent().getValue() > 0) {
                result.append(intersection);
            }
        }

        return result;
    }

    /**
     * As above, but using a Region as a parameter instead of discrete values
     * <p>
     * @param region
     * <p>
     * @return
     */
    public RegionContainer intersection(
        final Region region
    ) {
        return intersection(region.getFirstUnit(), region.getExtent());
    }

    /**
     * Indicates whether the given region defined by firstUnit and extent intersects any region in this container.
     * <p>
     * @param firstUnit
     * @param extent
     * <p>
     * @return
     */
    public boolean intersects(
        final Identifier firstUnit,
        final Counter extent
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
     * <p>
     * @param region
     * <p>
     * @return
     */
    public boolean intersects(
        final Region region
    ) {
        return intersects(region.getFirstUnit(), region.getExtent());
    }

    /**
     * If this container has no regions, or all regions it has are empty, then the container is considered empty.
     * <p>
     * @return
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
