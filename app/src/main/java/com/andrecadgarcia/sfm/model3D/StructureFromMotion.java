package com.andrecadgarcia.sfm.model3D;

/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.TextView;

import com.andrecadgarcia.sfm.adapter.GalleryRecyclerAdapter;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.*;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.SurfFeatureQueue;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import boofcv.android.ConvertBitmap;

import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Demonstration on how to do 3D reconstruction from a set of unordered photos with known intrinsic camera calibration.
 * The code below is still a work in process and is very basic, but still require a solid understanding of
 * structure from motion to understand.  In other words, this is not for beginners and requires good clean set of
 * images to work.
 * <p>
 * One key element it is missing is bundle adjustment to improve the estimated camera location and 3D points.  The
 * current bundle adjustment in BoofCV is too inefficient.   Better noise removal and numerous other improvements
 * are needed before it can compete with commercial equivalents.
 *
 * @author Peter Abeles
 */
public class StructureFromMotion {

    // Converts a point from pixel to normalized image coordinates
    PointTransform_F64 pixelToNorm;

    // ratio of matching features to unmatched features for two images to be considered connected
    double connectThreshold = 0.3;

    // tolerance for inliers in pixels
    double inlierTol = 1.5;

    // Detects and describes image interest points
    DetectDescribePoint<GrayF32, BrightFeature> detDesc = FactoryDetectDescribe.surfStable(null, null, null, GrayF32.class);
    // score ans association algorithm
    ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class, true);
    AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, 1, true);

    // Triangulates the 3D coordinate of a point from two observations
    TriangulateTwoViewsCalibrated triangulate = FactoryMultiView.triangulateTwoGeometric();

    // List of visual features (e.g. SURF) descriptions in each image
    List<FastQueue<BrightFeature>> imageVisualFeatures = new ArrayList<FastQueue<BrightFeature>>();
    // List of visual feature locations as normalized image coordinates in each image
    List<FastQueue<Point2D_F64>> imagePixels = new ArrayList<FastQueue<Point2D_F64>>();
    // Color of the pixel at each feature location
    List<GrowQueue_I32> imageColors = new ArrayList<GrowQueue_I32>();
    // List of 3D features in each image
    List<List<Feature3D>> imageFeature3D = new ArrayList<List<Feature3D>>();

    // Transform from world to each camera image
    Se3_F64 motionWorldToCamera[];

    // indicates if an image has had its motion estimated yet
    boolean estimatedImage[];
    // if true the image has been processed.  Estimation could have failed. so this can be true but estimated false
    boolean processedImage[];

    // List of all 3D features
    List<Feature3D> featuresAll;

    // used to provide initial estimate of the 3D scene
    ModelMatcher<Se3_F64, AssociatedPair> estimateEssential;
    ModelMatcher<Se3_F64, Point2D3D> estimatePnP;
    ModelFitter<Se3_F64, Point2D3D> refinePnP = FactoryMultiView.refinePnP(1e-12, 40);

    BitmapFactory.Options opt = new BitmapFactory.Options();
    Context context;

    String result = "";

    GalleryRecyclerAdapter adapter;

    int progress = 0;
    int recon_steps = 0;

    /**
     * Process the images and reconstructor the scene as a point cloud using matching interest points between
     * images.
     */
    public List<Feature3D> process(IntrinsicParameters intrinsic, List<String> colorImages, Context context, GalleryRecyclerAdapter adapter) {

        this.adapter = adapter;

        featuresAll = new ArrayList<>();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;

        this.context = context;

        pixelToNorm = LensDistortionOps.transformPoint(intrinsic).undistort_F64(true, false);

        estimateEssential = FactoryMultiViewRobust.essentialRansac(
                new ConfigEssential(intrinsic), new ConfigRansac(4000, inlierTol));
        estimatePnP = FactoryMultiViewRobust.pnpRansac(
                new ConfigPnP(intrinsic), new ConfigRansac(4000, inlierTol));

        // find features in each image
        if (!detectImageFeatures(colorImages)) {
            return null;
        }

        // see which images are the most similar to each o ther
        double[][] matrix = computeConnections();

        if (matrix == null) {
            return null;
        }

        if (!printConnectionMatrix(matrix)) {
            return null;
        }

        // find the image which is connected to the most other images.  Use that as the origin of the arbitrary
        // coordinate system
        int bestImage = selectMostConnectFrame(colorImages, matrix);

        if (bestImage == -1) {
            return null;
        }

        // Use two images to initialize the scene reconstruction
        if (!initializeReconstruction(colorImages, matrix, bestImage)) {
            return null;
        }

        // Process rest of the images and compute 3D coordinates
        List<Integer> seed = new ArrayList<Integer>();
        seed.add(bestImage);
        if (!performReconstruction(seed, -1, matrix)) {
            return null;
        }

        return featuresAll;
    }

    /**
     * Initialize the reconstruction by finding the image which is most similar to the "best" image.  Estimate
     * its pose up to a scale factor and create the initial set of 3D features
     */
    private boolean initializeReconstruction(List<String> colorImages, double[][] matrix, int bestImage) {
        // Set all images, but the best one, as not having been estimated yet
        estimatedImage = new boolean[colorImages.size()];
        processedImage = new boolean[colorImages.size()];
        estimatedImage[bestImage] = true;
        processedImage[bestImage] = true;

        // declare stored for found motion of each image
        motionWorldToCamera = new Se3_F64[colorImages.size()];
        for (int i = 0; i < colorImages.size(); i++) {

            progress = 40 + ((i*10)/colorImages.size());
            if (!adapter.setProgress(progress)) {
                return false;
            }

            motionWorldToCamera[i] = new Se3_F64();
            imageFeature3D.add(new ArrayList<Feature3D>());
        }

        progress = 50;
        adapter.setProgress(progress);

        // pick the image most similar to the original image to initialize pose estimation
        int firstChild = findBestFit(matrix, bestImage);
        initialize(bestImage, firstChild);

        return true;
    }

    /**
     * Select the frame which has the most connections to all other frames.  The is probably a good location
     * to start since it will require fewer hops to estimate the motion of other frames
     */
    private int selectMostConnectFrame(List<String> colorImages, double[][] matrix) {
        int bestImage = -1;
        int bestCount = 0;
        for (int i = 0; i < colorImages.size(); i++) {

            progress = 30 + ((i*10)/colorImages.size());
            if (!adapter.setProgress(progress)) {
                return -1;
            }

            int count = 0;
            for (int j = 0; j < colorImages.size(); j++) {
                if (matrix[i][j] > connectThreshold) {
                    count++;
                }
            }
            System.out.println(i + "  count " + count);
            if (count > bestCount) {
                bestCount = count;
                bestImage = i;
            }
        }

        progress = 40;
        adapter.setProgress(progress);

        return bestImage;
    }

    /**
     * Detect image features in all the images.  Save location, description, and color
     */
    private boolean detectImageFeatures(List<String> colorImages) {
        System.out.println("Detecting Features in each image.  Total " + colorImages.size());
        Bitmap colorImage;
        File f;
        for (int i = 0; i < colorImages.size(); i++) {

            progress = (i*10/colorImages.size());
            if (!adapter.setProgress(progress)) {
                return false;
            }

            System.out.print("*");
            f = new File(colorImages.get(i));
            try {
                colorImage = BitmapFactory.decodeStream(new FileInputStream(f), null, opt);
                FastQueue<BrightFeature> features = new SurfFeatureQueue(64);
                FastQueue<Point2D_F64> pixels = new FastQueue<Point2D_F64>(Point2D_F64.class, true);
                GrowQueue_I32 colors = new GrowQueue_I32();
                detectFeatures(colorImage, features, pixels, colors);

                imageVisualFeatures.add(features);
                imagePixels.add(pixels);
                imageColors.add(colors);

                colorImage.recycle();
            } catch (Exception e) {
                Log.d("","Open Image");
            }
        }

        progress = 10;
        adapter.setProgress(progress);

        System.out.println();
        return true;
    }

    /**
     * Compute connectivity matrix based on fraction of matching image features
     */
    private double[][] computeConnections() {
        double matrix[][] = new double[imageVisualFeatures.size()][imageVisualFeatures.size()];

        for (int i = 0; i < imageVisualFeatures.size(); i++) {

            progress = 10 + (i*10/imageVisualFeatures.size());
            if (!adapter.setProgress(progress)) {
                return null;
            }

            for (int j = i + 1; j < imageVisualFeatures.size(); j++) {
                System.out.printf("Associated %02d %02d ", i, j);
                associate.setSource(imageVisualFeatures.get(i));
                associate.setDestination(imageVisualFeatures.get(j));
                associate.associate();

                matrix[i][j] = associate.getMatches().size() / (double) imageVisualFeatures.get(i).size();
                matrix[j][i] = associate.getMatches().size() / (double) imageVisualFeatures.get(j).size();

                System.out.println(" = " + matrix[i][j]);
            }
        }

        progress = 20;
        adapter.setProgress(progress);

        return matrix;
    }

    /**
     * Prints out which frames are connected to each other
     */
    private boolean printConnectionMatrix(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {

            progress = 20 + (i*10/imageVisualFeatures.size());
            if(!adapter.setProgress(progress)) {
                return false;
            }

            for (int j = 0; j < matrix.length; j++) {
                if (matrix[i][j] >= connectThreshold) {
                    System.out.print("#");
                }
                else{
                    System.out.print(".");
                }
            }
            System.out.println();
        }

        progress = 30;
        adapter.setProgress(progress);

        return true;
    }

    /**
     * Detects image features.  Saves their location, description, and pixel color
     */
    private void detectFeatures(Bitmap colorImage,
                                FastQueue<BrightFeature> features, FastQueue<Point2D_F64> pixels,
                                GrowQueue_I32 colors) {

        GrayF32 image = ConvertBitmap.bitmapToGray(colorImage, (GrayF32) null, null);

        features.reset();
        pixels.reset();
        colors.reset();
        detDesc.detect(image);
        for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
            Point2D_F64 p = detDesc.getLocation(i);

            features.grow().set(detDesc.getDescription(i));
            // store pixels are normalized image coordinates
            pixelToNorm.compute(p.x, p.y, pixels.grow());

            colors.add(colorImage.getPixel((int) p.x, (int) p.y));
        }
    }

    /**
     * Finds the frame which is the best match for the given target frame
     */
    private int findBestFit(double matrix[][], int target) {

        // find the image which is the closest fit
        int bestIndex = -1;
        double bestRatio = 0;

        for (int i = 0; i < estimatedImage.length; i++) {
            double ratio = matrix[target][i];
            if (ratio > bestRatio) {
                bestRatio = ratio;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    /**
     * Initialize the 3D world given these two images.  imageA is assumed to be the origin of the world.
     */
    private void initialize(int imageA, int imageB) {
        System.out.println("Initializing 3D world using " + imageA + " and " + imageB);
        // Compute the 3D pose and find valid image features
        Se3_F64 motionAtoB = new Se3_F64();
        List<AssociatedIndex> inliers = new ArrayList<AssociatedIndex>();

        if (!estimateStereoPose(imageA, imageB, motionAtoB, inliers))
            throw new RuntimeException("The first image pair is a bad keyframe!");

        motionWorldToCamera[imageB].set(motionAtoB);
        estimatedImage[imageB] = true;
        processedImage[imageB] = true;

        // create tracks for only those features in the inlier list
        FastQueue<Point2D_F64> pixelsA = imagePixels.get(imageA);
        FastQueue<Point2D_F64> pixelsB = imagePixels.get(imageB);
        List<Feature3D> tracksA = imageFeature3D.get(imageA);
        List<Feature3D> tracksB = imageFeature3D.get(imageB);

        GrowQueue_I32 colorsA = imageColors.get(imageA);

        for (int i = 0; i < inliers.size(); i++) {
            AssociatedIndex a = inliers.get(i);

            Feature3D t = new Feature3D();
            t.color = colorsA.get(a.src);
            t.obs.grow().set(pixelsA.get(a.src));
            t.obs.grow().set(pixelsB.get(a.dst));
            t.frame.add(imageA);
            t.frame.add(imageB);
            // compute the 3D coordinate of the feature
            Point2D_F64 pa = pixelsA.get(a.src);
            Point2D_F64 pb = pixelsB.get(a.dst);

            if (!triangulate.triangulate(pa, pb, motionAtoB, t.worldPt))
                continue;
            // the feature has to be in front of the camera
            if (t.worldPt.z > 0) {
                featuresAll.add(t);
                tracksA.add(t);
                tracksB.add(t);
            }
        }

        // adjust the scale so that it's not excessively large or small
        normalizeScale(motionWorldToCamera[imageB], tracksA);
    }

    /**
     * Perform a breadth first search through connection graph until the motion to all images has been found
     */
    private boolean performReconstruction(List<Integer> parents, int childAdd, double matrix[][]) {

        System.out.println("--------- Total Parents " + parents.size());

        List<Integer> children = new ArrayList<Integer>();

        if (childAdd != -1) {
            children.add(childAdd);
        }

        for (int parent : parents) {

            for (int i = 0; i < estimatedImage.length; i++) {

                progress = 50 + ((i * 50) / estimatedImage.length);
                if (!adapter.setProgress(progress)) {
                    return false;
                }

                // see if it is connected to the target and has not had its motion estimated
                if (matrix[parent][i] > connectThreshold && !processedImage[i]) {
                    estimateMotionPnP(parent, i);
                    children.add(i);
                }
            }
        }

        if (!children.isEmpty()) {
            performReconstruction(children, -1, matrix);
        }

        return true;
    }

    /**
     * Estimate the motion between two images.  Image A is assumed to have known features with 3D coordinates already
     * and image B is an unprocessed image with no 3D features yet.
     */
    private void estimateMotionPnP(int imageA, int imageB) {
        // Mark image B as processed so that it isn't processed a second time.
        processedImage[imageB] = true;

        System.out.println("Estimating PnP motion between " + imageA + " and " + imageB);

        // initially prune features using essential matrix
        Se3_F64 dummy = new Se3_F64();
        List<AssociatedIndex> inliers = new ArrayList<AssociatedIndex>();

        if (!estimateStereoPose(imageA, imageB, dummy, inliers))
            throw new RuntimeException("The first image pair is a bad keyframe!");

        FastQueue<Point2D_F64> pixelsA = imagePixels.get(imageA);
        FastQueue<Point2D_F64> pixelsB = imagePixels.get(imageB);
        List<Feature3D> featuresA = imageFeature3D.get(imageA);
        List<Feature3D> featuresB = imageFeature3D.get(imageB); // this should be empty

        // create the associated pair for motion estimation
        List<Point2D3D> features = new ArrayList<Point2D3D>();
        List<AssociatedIndex> inputRansac = new ArrayList<AssociatedIndex>();
        List<AssociatedIndex> unmatched = new ArrayList<AssociatedIndex>();
        for (int i = 0; i < inliers.size(); i++) {
            AssociatedIndex a = inliers.get(i);
            Feature3D t = lookupFeature(featuresA, imageA, pixelsA.get(a.src));
            if (t != null) {
                Point2D_F64 p = pixelsB.get(a.dst);
                features.add(new Point2D3D(p, t.worldPt));
                inputRansac.add(a);
            } else {
                unmatched.add(a);
            }
        }

        // make sure there are enough features to estimate motion
        if (features.size() < 15) {
            System.out.println("  Too few features for PnP!!  " + features.size());
            return;
        }

        // estimate the motion between the two images
        if (!estimatePnP.process(features))
            throw new RuntimeException("Motion estimation failed");

        // refine the motion estimate using non-linear optimization
        Se3_F64 motionWorldToB = new Se3_F64();
        if (!refinePnP.fitModel(estimatePnP.getMatchSet(), estimatePnP.getModelParameters(), motionWorldToB))
            throw new RuntimeException("Refine failed!?!?");

        motionWorldToCamera[imageB].set(motionWorldToB);
        estimatedImage[imageB] = true;

        // Add all tracks in the inlier list to the B's list of 3D features
        int N = estimatePnP.getMatchSet().size();
        boolean inlierPnP[] = new boolean[features.size()];
        for (int i = 0; i < N; i++) {
            int index = estimatePnP.getInputIndex(i);
            AssociatedIndex a = inputRansac.get(index);

            // find the track that this was associated with and add it to B
            Feature3D t = lookupFeature(featuresA, imageA, pixelsA.get(a.src));
            featuresB.add(t);
            t.frame.add(imageB);
            t.obs.grow().set(pixelsB.get(a.dst));
            inlierPnP[index] = true;
        }

        // Create new tracks for all features which were a member of essential matrix but not used to estimate
        // the motion using PnP.
        Se3_F64 motionBtoWorld = motionWorldToB.invert(null);
        Se3_F64 motionWorldToA = motionWorldToCamera[imageA];
        Se3_F64 motionBtoA = motionBtoWorld.concat(motionWorldToA, null);
        Point3D_F64 pt_in_b = new Point3D_F64();

        int totalAdded = 0;
        GrowQueue_I32 colorsA = imageColors.get(imageA);
        for (AssociatedIndex a : unmatched) {

            if (!triangulate.triangulate(pixelsB.get(a.dst), pixelsA.get(a.src), motionBtoA, pt_in_b))
                continue;

            // the feature has to be in front of the camera
            if (pt_in_b.z > 0) {
                Feature3D t = new Feature3D();

                // transform from B back to world frame
                SePointOps_F64.transform(motionBtoWorld, pt_in_b, t.worldPt);

                t.color = colorsA.get(a.src);
                t.obs.grow().set(pixelsA.get(a.src));
                t.obs.grow().set(pixelsB.get(a.dst));
                t.frame.add(imageA);
                t.frame.add(imageB);

                featuresAll.add(t);
                featuresA.add(t);
                featuresB.add(t);

                totalAdded++;
            }
        }

        // create new tracks for existing tracks which were not in the inlier set.  Maybe things will work
        // out better if the 3D coordinate is re-triangulated as a new feature
        for (int i = 0; i < features.size(); i++) {
            if (inlierPnP[i])
                continue;

            AssociatedIndex a = inputRansac.get(i);

            if (!triangulate.triangulate(pixelsB.get(a.dst), pixelsA.get(a.src), motionBtoA, pt_in_b))
                continue;

            // the feature has to be in front of the camera
            if (pt_in_b.z > 0) {
                Feature3D t = new Feature3D();

                // transform from B back to world frame
                SePointOps_F64.transform(motionBtoWorld, pt_in_b, t.worldPt);

                // only add this feature to image B since a similar one already exists in A.
                t.color = colorsA.get(a.src);
                t.obs.grow().set(pixelsB.get(a.dst));
                t.frame.add(imageB);

                featuresAll.add(t);
                featuresB.add(t);

                totalAdded++;
            }
        }

        System.out.println("  New added " + totalAdded + "  tracksA.size = " + featuresA.size() + "  tracksB.size = " + featuresB.size());
    }

    /**
     * Given a list of 3D features, find the feature which was observed at the specified frame at the
     * specified location.  If no feature is found return null.
     */
    private Feature3D lookupFeature(List<Feature3D> features, int frameIndex, Point2D_F64 pixel) {
        for (int i = 0; i < features.size(); i++) {
            Feature3D t = features.get(i);
            for (int j = 0; j < t.frame.size(); j++) {
                if (t.frame.get(j) == frameIndex) {
                    Point2D_F64 o = t.obs.get(j);
                    if (o.x == pixel.x && o.y == pixel.y) {
                        return t;
                    } else {
                        break;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Given two images compute the relative location of each image using the essential matrix.
     */
    protected boolean estimateStereoPose(int imageA, int imageB, Se3_F64 motionAtoB,
                                         List<AssociatedIndex> inliers) {
        // associate the features together
        associate.setSource(imageVisualFeatures.get(imageA));
        associate.setDestination(imageVisualFeatures.get(imageB));
        associate.associate();

        FastQueue<AssociatedIndex> matches = associate.getMatches();

        // create the associated pair for motion estimation
        FastQueue<Point2D_F64> pixelsA = imagePixels.get(imageA);
        FastQueue<Point2D_F64> pixelsB = imagePixels.get(imageB);
        List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
        for (int i = 0; i < matches.size(); i++) {
            AssociatedIndex a = matches.get(i);
            pairs.add(new AssociatedPair(pixelsA.get(a.src), pixelsB.get(a.dst)));
        }

        if (!estimateEssential.process(pairs))
            throw new RuntimeException("Motion estimation failed");

        List<AssociatedPair> inliersEssential = estimateEssential.getMatchSet();

        motionAtoB.set(estimateEssential.getModelParameters());

        for (int i = 0; i < inliersEssential.size(); i++) {
            int index = estimateEssential.getInputIndex(i);

            inliers.add(matches.get(index));
        }

        return true;
    }

    /**
     * Scale can only be estimated up to a scale factor.  Might as well set the distance to 1 since it is
     * less likely to have overflow/underflow issues.  This step is not strictly necessary.
     */
    public void normalizeScale(Se3_F64 transform, List<Feature3D> features) {

        double T = transform.T.norm();
        double scale = 1.0 / T;

        for (Se3_F64 m : motionWorldToCamera) {
            m.T.timesIP(scale);
        }

        for (Feature3D t : features) {
            t.worldPt.timesIP(scale);
        }
    }
}
