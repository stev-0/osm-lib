package com.conveyal.osmlib;

import java.io.InputStream;

/**
 * A parser that keeps track of intersections in the file being loaded.
 *
 * FIXME just move this functionality into OSM class
 */
public class VexPbfParser extends PBFInput {

    /** The nodes that are referenced at least once by ways in this OSM. */
    NodeTracker referenced = new NodeTracker();

    /** The nodes which are referenced more than once by ways in this OSM. */
    NodeTracker intersections = new NodeTracker();

    public VexPbfParser (InputStream inputStream, OSM osm) {
        super(inputStream, osm);
    }

    public void handleWay(long wayId, Way way) {

        /* Skip ways with no nodes. */
        if (way.nodes.length == 0) return;

        /* Detect intersections. */
        for (long nid : way.nodes) {
            if (referenced.contains(nid)) {
                intersections.add(nid); // seen more than once
            } else {
                referenced.add(nid); // seen for the first time
            }
        }

        /* Insert the way in the spatial index. */
        long firstNodeId = way.nodes[0];
        Node firstNode = null; // FIXME osm.nodes.get(firstNodeId);
        if (firstNode == null) {
            LOG.error("A way referenced a node that was not yet included in the input.");
        } else {
            WebMercatorTile tile = new WebMercatorTile(firstNode.getLat(), firstNode.getLon());
            // We could also insert using ((float)lat, (float)lon) as a key
            // but depending on whether MapDB does tree path compression this might take more space
            // FIXME osm.index.add(new Tuple3(tile.xtile, tile.ytile, wayId));
        }

        /* Defer to the superclass to store the node in the map. */
        // FIXME super.handleWay(wayId, way);
    };

    public static class WebMercatorTile {

        //http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/
        public final int ZOOM = 12;
        public final int xtile, ytile;

        /** Tile definition equations from: TODO URL */
        public WebMercatorTile(double lat, double lon) {
            xtile = (int) Math.floor((lon + 180) / 360 * (1 << ZOOM));
            ytile = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat))
                    + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << ZOOM));
        }

    }

}
