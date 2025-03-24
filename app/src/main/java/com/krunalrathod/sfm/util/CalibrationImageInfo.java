package com.krunalrathod.sfm.util;

import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.image.GrayF32;


public class CalibrationImageInfo {
    GrayF32 image;
    CalibrationObservation calibPoints = new CalibrationObservation();

    public CalibrationImageInfo(GrayF32 image, CalibrationObservation observations) {
        this.image = image.clone();
        this.calibPoints.setTo(observations);
    }

    public CalibrationObservation getCalibPoints() {
        return this.calibPoints;
    }
}