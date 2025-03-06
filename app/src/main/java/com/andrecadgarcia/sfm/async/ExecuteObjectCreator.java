package com.andrecadgarcia.sfm.async;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.andrecadgarcia.sfm.activity.MainActivity;
import com.andrecadgarcia.sfm.adapter.GalleryRecyclerAdapter;
import com.andrecadgarcia.sfm.model3D.Feature3D;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;


public class ExecuteObjectCreator extends AsyncTask<String, Void, String> {

    FileOutputStream outStream = null;
    OutputStreamWriter myOutWriter = null;

    Context context;
    GalleryRecyclerAdapter adapter;

    List<Feature3D> cloud;
    String vertices;
    String basepath;

    public ExecuteObjectCreator(List<Feature3D> cloud, String vertices, GalleryRecyclerAdapter adapter, Context context, String basepath) {
        this.context = context;
        this.adapter = adapter;
        this.cloud = cloud;
        this.vertices = vertices;
        this.basepath = basepath;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        System.out.println("Getting sequential");

        this.adapter.addStep();

        switch(adapter.getSteps()) {
            case 2:
                adapter.setMessages("Sequential Creation (2/4)");
                adapter.setProgress(0);
                break;
            case 3:
                adapter.setMessages("Point Cloud Creation (3/4)");
                adapter.setProgress(0);
                break;
            case 4:
                adapter.setMessages("All to All (4/4)");
                adapter.setProgress(0);
                break;
            default:
                break;
        }

    }

    @Override
    protected String doInBackground(String... strings) {
        if (!isCancelled()) {
            switch(adapter.getSteps()) {
                case 2:
                    return getSequential(cloud, vertices);
                case 3:
                    return getPoints(cloud, vertices);
                case 4:
                    return getAllToAll(cloud, vertices);
                default:
                    return null;
            }
        }
        else {
            return null;
        }

    }

    @Override
    protected void onPostExecute(String result) {

        if (result != null) {

            try {
                String filePath;
                filePath = basepath + File.separator;
                switch (adapter.getSteps()) {
                    case 2:
                        filePath = filePath + "sequential.obj";
                        break;
                    case 3:
                        filePath = filePath + "points.obj";
                        break;
                    case 4:
                        filePath = filePath + "allToAll.obj";
                        break;
                }
                File dir = new File(filePath);
                outStream = new FileOutputStream(dir);
                myOutWriter = new OutputStreamWriter(outStream);
                myOutWriter.append(result);
                myOutWriter.close();

                outStream.flush();
                outStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (adapter.getSteps() == 4) {

                adapter.addStep();
                adapter.dismissAlert();

                ((MainActivity) context).setProcessingSFM(false);
                ((MainActivity) context).setResult(adapter.getElapsed() + vertices);
                ((MainActivity) context).fragmentTransaction(MainActivity.SFMRESULT_FRAGMENT);
            }
            else {
                ExecuteObjectCreator objectCreator = new ExecuteObjectCreator(cloud, vertices, adapter, context, basepath);
                objectCreator.execute();
            }
        }
    }

    public String getPoints(List<Feature3D> cloud, String vertices) {
        String result = "";

        int j = 0;

        for (Feature3D point : cloud) {

            if (!adapter.setProgress(((j*50)/cloud.size()))) {
                return null;
            }

            for (int i = 0; i < 8; i++) {
                result += "v ";
                result += (point.worldPt.x + (((i % 2) == 0) ? 0.05 : -0.05)) + " " + (point.worldPt.y + (((i > 1) && (i < 5)) ? 0.05 : -0.05)) + " " + (point.worldPt.z + ((i < 4) ? 0.05 : -0.05));
                result += '\n';
            }

            j++;
        }

        j = 0;

        int index = -1;
        for (Feature3D point : cloud) {
            index++;

            if (!adapter.setProgress(50+((j*50)/cloud.size()))) {
                return null;
            }

            int start = (index * 8) + 1;

            result += "f ";
            result += (start + 0) + " " + (start + 1) + " " + (start + 2);
            result += '\n';
            result += "f ";
            result += (start + 2) + " " + (start + 1) + " " + (start + 3);
            result += '\n';
            result += "f ";
            result += (start + 2) + " " + (start + 3) + " " + (start + 4);
            result += '\n';
            result += "f ";
            result += (start + 4) + " " + (start + 3) + " " + (start + 5);
            result += '\n';
            result += "f ";
            result += (start + 4) + " " + (start + 5) + " " + (start + 6);
            result += '\n';
            result += "f ";
            result += (start + 6) + " " + (start + 5) + " " + (start + 7);
            result += '\n';
            result += "f ";
            result += (start + 6) + " " + (start + 7) + " " + (start + 0);
            result += '\n';
            result += "f ";
            result += (start + 0) + " " + (start + 7) + " " + (start + 1);
            result += '\n';
            result += "f ";
            result += (start + 1) + " " + (start + 7) + " " + (start + 3);
            result += '\n';
            result += "f ";
            result += (start + 3) + " " + (start + 7) + " " + (start + 5);
            result += '\n';
            result += "f ";
            result += (start + 6) + " " + (start + 0) + " " + (start + 4);
            result += '\n';
            result += "f ";
            result += (start + 4) + " " + (start + 0) + " " + (start + 2);
            result += '\n';

            j++;
        }


        return result;
    }

    public String getSequential(List<Feature3D> cloud, String vertices) {
        String result = vertices;
        result += "\n";

        int j = 0;

        for(int i = 0 ; i <= cloud.size(); i++) {

            if (!adapter.setProgress(((j*100)/cloud.size()))) {
                return null;
            }

            result += "f ";
            result += ((i % (cloud.size())) + 1) + " " + (((i + 1) % (cloud.size())) + 1) + " " + (((i + 2) % (cloud.size())) + 1);
            result += '\n';
            result += "f ";
            result += (((i + 2)  % (cloud.size())) + 1) + " " + (((i + 1) % (cloud.size())) + 1) + " " + ((i % (cloud.size())) + 1);
            result += '\n';

            j++;
        }

        return result;
    }

    public String getAllToAll(List<Feature3D> cloud, String vertices) {
        String result = vertices;
        result += "\n";

        int k = 0;

        for (int i = 1; i <= cloud.size(); i++) {

            if (!adapter.setProgress(((k*100)/cloud.size()))) {
                return null;
            }

            for (int j = 1; j <= cloud.size(); j++) {
                result += "f ";
                result += 1 + " " + i + " " + j;
                result += '\n';
                result += "f ";
                result += j + " " + i + " " + 1;
                result += '\n';
            }
            k++;
        }


        return result;
    }
}
