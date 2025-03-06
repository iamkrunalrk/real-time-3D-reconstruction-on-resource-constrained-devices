package com.andrecadgarcia.sfm.model3D;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.MotionEvent;

import com.andrecadgarcia.sfm.activity.MainActivity;

import org.rajawali3d.Camera;
import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.renderer.RajawaliRenderer;

import java.io.File;


public class Renderer extends RajawaliRenderer {

    Context context;

    private DirectionalLight directionalLight;
    private Object3D object;
    private String path;

    private boolean rotate;

    public Renderer(Context context, String path) {
        super(context);
        this.context = context;
        setFrameRate(60);
        this.path = path;
        this.rotate = false;
        //object = getObject(path);
    }

    @Override
    protected void initScene() {
        directionalLight = new DirectionalLight(1f, .2f, -1.0f);
        directionalLight.setColor(1.0f, 1.0f, 1.0f);
        directionalLight.setPower(2);
        getCurrentScene().addLight(directionalLight);
        getCurrentScene().setBackgroundColor(Color.rgb(255,255,255));

        addObject(this.path);
    }

    @Override
    public void onRender(final long elapsedTime, final double deltaTime) {
        super.onRender(elapsedTime, deltaTime);
        if (rotate) {
            object.setRotation(Vector3.Axis.Y, 0.5);
        }
        //getCurrentCamera().rotateAround(Vector3.getAxisVector(Vector3.Axis.Y), 0.5);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    /*
    public void rotCamera(float touchOffset) {
        xOffset = touchOffset;
        if(xOffset > 360) xOffset -= 360;

        float x = (float) (5*Math.sin(Math.toRadians(xOffset)));
        float z = (float) (5*Math.cos(Math.toRadians(xOffset)));

        mCamera.setPosition(x, 0, z);
    }
*/
    public void clean() {
        getCurrentScene().clearChildren();
    }

    public Object3D getObject(String path) {
        LoaderOBJ parser = null;
        try {
            File object = new File(path);

            parser = new LoaderOBJ(this, object);

            parser.parse();
        } catch (Exception e) {
            final AlertDialog alert = new AlertDialog.Builder(context)
                    .setTitle("Error")
                    .setMessage(e.getMessage())
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ((MainActivity) context).fragmentTransaction(MainActivity.GALLERY_FRAGMENT);
                        }
                    })
                    .create();
             alert.show();
        }

        return parser.getParsedObject();
    }

    public void addObject(String path) {
        LoaderOBJ parser = null;
        try {
            File object = new File(path);

            parser = new LoaderOBJ(this, object);

            parser.parse();
        } catch (Exception e) {
            final AlertDialog alert = new AlertDialog.Builder(context)
                    .setTitle("Error")
                    .setMessage(e.getMessage())
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ((MainActivity) context).fragmentTransaction(MainActivity.GALLERY_FRAGMENT);
                        }
                    })
                    .create();
            alert.show();
        }

        Material material = new Material();
        material.enableLighting(true);
        material.setDiffuseMethod(new DiffuseMethod.Lambert());

        object = parser.getParsedObject();

        object.setMaterial(material);
        object.setColor(Color.BLUE);


        getCurrentScene().addChild(object);

        //object.setPosition(object.getScaleX()/2, object.getScaleY()/2, object.getScaleZ()/2);

        //getCurrentCamera().setZ(object.getZ() );

        getCurrentCamera().setLookAt(-500,100,-100);
        getCurrentCamera().setX(25);
        getCurrentCamera().setZ(10);
    }

    public void setRotation(boolean rotate) {
        this.rotate = rotate;
    }
}
