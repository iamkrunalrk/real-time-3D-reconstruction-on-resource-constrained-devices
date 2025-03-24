package com.andrecadgarcia.sfm.model3D;

import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;


public class Feature3D {

    // color of the pixel first found int
    int color;
    // estimate 3D position of the feature
    public Point3D_F64 worldPt = new Point3D_F64();
    // observations in each frame that it's visible
    FastQueue<Point2D_F64> obs = new FastQueue<Point2D_F64>(Point2D_F64.class, true);
    // index of each frame its visible in
    GrowQueue_I32 frame = new GrowQueue_I32();

}
