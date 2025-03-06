package com.andrecadgarcia.sfm.activity;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.andrecadgarcia.sfm.util.CameraSpecs;
import com.andrecadgarcia.sfm.util.DemoPreference;
import com.andrecadgarcia.sfm.R;
import com.andrecadgarcia.sfm.util.UtilVarious;
import com.andrecadgarcia.sfm.fragment.CameraFragment;
import com.andrecadgarcia.sfm.fragment.GalleryFragment;
import com.andrecadgarcia.sfm.fragment.HomeFragment;
import com.andrecadgarcia.sfm.fragment.ModelViewerFragment;
import com.andrecadgarcia.sfm.fragment.SFMResultFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import boofcv.android.BoofAndroidFiles;


public class MainActivity extends AppCompatActivity {

    public static final Integer HOME_FRAGMENT = 0;
    public static final Integer CAMERA_FRAGMENT = 1;
    public static final Integer GALLERY_FRAGMENT = 2;
    public static final Integer SFMRESULT_FRAGMENT = 3;
    public static final Integer MODELVIEWER_FRAGMENT = 4;

    private static final String CURRENT_FRAGMENT = "current_fragment_index_flag";

    private HashMap<Integer, Fragment> hashFragment;
    private int currentFragmentIndex;

    private boolean doubleBackToExitPressedOnce, processingSFM;

    private Toolbar toolbar;

    // contains information on all the cameras.  less error prone and easier to deal with
    public static List<CameraSpecs> specs = new ArrayList<CameraSpecs>();

    // specifies which camera to use an image size
    public static DemoPreference preference;
    // If another activity modifies the demo preferences this needs to be set to true so that it knows to reload
    // camera parameters.
    public static boolean changedPreferences = false;

    private String result = "";

    public MainActivity() {
        loadCameraSpecs();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        hashFragment = new HashMap<>();

        hashFragment.put(HOME_FRAGMENT, new HomeFragment());
        hashFragment.put(CAMERA_FRAGMENT, new CameraFragment());
        hashFragment.put(GALLERY_FRAGMENT, new GalleryFragment());
        hashFragment.put(SFMRESULT_FRAGMENT, new SFMResultFragment());
        hashFragment.put(MODELVIEWER_FRAGMENT, new ModelViewerFragment());

        if(savedInstanceState != null){
            currentFragmentIndex = savedInstanceState.getInt(CURRENT_FRAGMENT);
            fragmentTransaction(currentFragmentIndex);
        } else {
            fragmentTransaction(HOME_FRAGMENT);
        }

        Locale locale = new Locale("pt","BR");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;

        doubleBackToExitPressedOnce = false;
        processingSFM = false;

    }

    @Override
    protected void onResume() {
        super.onResume();
        if( preference == null ) {
            preference = new DemoPreference();
            setDefaultPreferences();
        } else if( changedPreferences ) {
            loadIntrinsic();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(CURRENT_FRAGMENT, currentFragmentIndex);

    }


    public Fragment getClass(Integer name) {
        return hashFragment.get(name);
    }

    public void fragmentTransaction(int id) {

        try {
            FragmentManager fragmentManager = getSupportFragmentManager();

            fragmentManager.beginTransaction()
                    .replace(R.id.container, hashFragment.get(id))
                    .commit();
            currentFragmentIndex = id;


            setToolBarTitle(id);


        } catch (Exception e) {
            System.out.println(e.getMessage());
            //Toast.makeText(this, "Em Desenvolvimento... :D", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {

        if (currentFragmentIndex == HOME_FRAGMENT) {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Precione voltar mais uma vez para sair", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
        else if (currentFragmentIndex == SFMRESULT_FRAGMENT || currentFragmentIndex == MODELVIEWER_FRAGMENT ) {
            if (!processingSFM) {
                fragmentTransaction(GALLERY_FRAGMENT);
            }
        }
        else{
            fragmentTransaction(HOME_FRAGMENT);
        }
    }

    private void setToolBarTitle(int id){
        id++;
        switch (id){
            case 1:
                toolbar.setTitle(getString(R.string.menu_item_title1));
                break;
            case 2:
                toolbar.setTitle(getString(R.string.menu_item_title2));
                break;
            case 3:
                toolbar.setTitle(getString(R.string.menu_item_title3));
                break;
            case 4:
                toolbar.setTitle(getString(R.string.menu_item_title4));
                break;
            case 5:
                toolbar.setTitle(getString(R.string.menu_item_title4));
                break;
            case 6:
                toolbar.setTitle(getString(R.string.menu_item_title4));
                break;
            default:
                toolbar.setTitle(getString(R.string.app_name));
        }
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return this.result;
    }

    public void setProcessingSFM(boolean processingSFM) {
        this.processingSFM = processingSFM;
    }

    private void loadCameraSpecs() {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraSpecs c = new CameraSpecs();
            specs.add(c);

            Camera.getCameraInfo(i, c.info);
            Camera camera = Camera.open(i);
            Camera.Parameters params = camera.getParameters();
            c.horizontalViewAngle = params.getHorizontalViewAngle();
            c.verticalViewAngle = params.getVerticalViewAngle();
            c.sizePreview.addAll(params.getSupportedPreviewSizes());
            c.sizePicture.addAll(params.getSupportedPictureSizes());
            camera.release();
        }
    }

    private void setDefaultPreferences() {
        preference.showFps = false;

        // There are no cameras.  This is possible due to the hardware camera setting being set to false
        // which was a work around a bad design decision where front facing cameras wouldn't be accepted as hardware
        // which is an issue on tablets with only front facing cameras
        if( specs.size() == 0 ) {
            dialogNoCamera();
        }
        // select a front facing camera as the default
        for (int i = 0; i < specs.size(); i++) {
            CameraSpecs c = specs.get(i);

            if( c.info.facing == Camera.CameraInfo.CAMERA_FACING_BACK ) {
                preference.cameraId = i;
                break;
            } else {
                // default to a front facing camera if a back facing one can't be found
                preference.cameraId = i;
            }
        }

        CameraSpecs camera = specs.get(preference.cameraId);
        preference.preview = UtilVarious.closest(camera.sizePreview,320,240);
        preference.picture = UtilVarious.closest(camera.sizePicture,640,480);

        // see if there are any intrinsic parameters to load
        loadIntrinsic();
    }

    private void dialogNoCamera() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your device has no cameras!")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        System.exit(0);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void loadIntrinsic() {
        preference.intrinsic = null;
        try {

            String path = Environment.getExternalStorageDirectory() +
                    File.separator + "SFM" +
                    File.separator + "camParam.txt";

            FileInputStream fos = new FileInputStream(new File(path));
            Reader reader = new InputStreamReader(fos);
            preference.intrinsic = BoofAndroidFiles.readIntrinsic(reader);
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            Toast.makeText(this, "Failed to load intrinsic parameters", Toast.LENGTH_SHORT).show();
        }
    }
}
