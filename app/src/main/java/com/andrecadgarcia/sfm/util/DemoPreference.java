package com.andrecadgarcia.sfm.util;

import boofcv.struct.calib.IntrinsicParameters;

/**
 * @author Peter Abeles
 */
public class DemoPreference {
    public int cameraId;
    public int preview;
    public int picture;
    public boolean showFps;
    public IntrinsicParameters intrinsic;
}