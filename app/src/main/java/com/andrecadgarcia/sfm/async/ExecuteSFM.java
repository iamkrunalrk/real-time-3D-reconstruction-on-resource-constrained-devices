package com.andrecadgarcia.sfm.async;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.andrecadgarcia.sfm.activity.MainActivity;
import com.andrecadgarcia.sfm.adapter.GalleryRecyclerAdapter;
import com.andrecadgarcia.sfm.model3D.Feature3D;
import com.andrecadgarcia.sfm.model3D.StructureFromMotion;

import java.io.File;
import java.util.List;

import boofcv.struct.calib.IntrinsicParameters;


public class ExecuteSFM extends AsyncTask<String, Void, List<Feature3D>> {

    GalleryRecyclerAdapter adapter;
    IntrinsicParameters intrinsic;
    List<String> pictures;
    Context context;

    public ExecuteSFM(IntrinsicParameters intrinsic, List<String> pictures, Context context, GalleryRecyclerAdapter adapter) {
        this.context = context;
        this.adapter = adapter;
        this.intrinsic = intrinsic;
        this.pictures = pictures;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        adapter.addStep();
        adapter.setMessages("SFM Reconstruction (1/4)");
        adapter.setProgress(0);
    }

    @Override
    protected List<Feature3D> doInBackground(String... strings) {
        StructureFromMotion example = new StructureFromMotion();
        if (!isCancelled()) {
            return example.process(intrinsic, pictures, context, adapter);
        }
        else {
            return null;
        }

    }

    @Override
    protected void onPostExecute(List<Feature3D> result) {

        if (result != null) {

            Log.d("Log", "onPictureTaken - wrote bytes: " + result.size());

            String points = "";

            for (Feature3D p : result) {
                points += "v " + p.worldPt.x + " " + p.worldPt.y + " " + p.worldPt.z + "\n";
            }

            String basePath = Environment.getExternalStorageDirectory() +
                    File.separator + "SFM";
            File dir = new File(basePath);
            if(!dir.exists()){
                dir.mkdir();
                Log.d("Log", "onDirCreated");
            }
            basePath += File.separator + "Media";
            dir = new File(basePath);
            if(!dir.exists()){
                dir.mkdir();
                Log.d("Log", "onDirCreated");
            }
            basePath += File.separator + "Models";
            dir = new File(basePath);
            if(!dir.exists()){
                dir.mkdir();
                Log.d("Log", "onDirCreated");
            }
            basePath += File.separator + String.valueOf(System.currentTimeMillis());
            dir = new File(basePath);
            if(!dir.exists()){
                dir.mkdir();
                Log.d("Log", "onDirCreated");
            }

            adapter.setVertices(points);

            ExecuteObjectCreator objectCreator = new ExecuteObjectCreator(result, points, adapter, context, basePath);
            objectCreator.execute();
        }
    }

}
