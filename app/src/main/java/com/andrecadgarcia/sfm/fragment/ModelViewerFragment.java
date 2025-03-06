package com.andrecadgarcia.sfm.fragment;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.andrecadgarcia.sfm.R;
import com.andrecadgarcia.sfm.model3D.Renderer;

import org.rajawali3d.surface.RajawaliSurfaceView;

import java.io.File;


public class ModelViewerFragment extends Fragment {

    private View rootview;

    private RajawaliSurfaceView surface;
    private Renderer renderer;

    private SeekBar x,y,z;
    private CheckBox rotation;
    private Spinner objs;

    private String file;
    private String folder;

    private String fullPath;

    public ModelViewerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(rootview == null) {

            rootview = inflater.inflate(R.layout.fragment_model_viewer, container, false);

            surface = (RajawaliSurfaceView) rootview.findViewById(R.id.rajwali_surface);
            renderer = new Renderer(getContext(), this.fullPath);
            surface.setSurfaceRenderer(renderer);

            x = (SeekBar) rootview.findViewById(R.id.sk_x);
            x.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    System.out.println(renderer.getCurrentCamera().getPosition());
                    renderer.getCurrentCamera().setX(i-25);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            y = (SeekBar) rootview.findViewById(R.id.sk_y);
            y.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    renderer.getCurrentCamera().setY(i-50);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            z = (SeekBar) rootview.findViewById(R.id.sk_z);
            z.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    renderer.getCurrentCamera().setZ(i-40);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            rotation = (CheckBox) rootview.findViewById(R.id.cb_rot);
            rotation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    renderer.setRotation(b);
                }
            });

            objs = (Spinner) rootview.findViewById(R.id.sp_objs);
            objs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    switch (i) {
                        case 0:
                            setModel("sequential.obj");
                            surface.invalidate();
                            break;
                        case 1:
                            setModel("points.obj");
                            surface.invalidate();
                            break;
                        case 2:
                            setModel("allToAll.obj");
                            surface.invalidate();
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

        }
        else {
            surface.invalidate();
        }

        x.setProgress(50);
        y.setProgress(50);
        z.setProgress(50);
        rotation.setChecked(true);

        return rootview;
    }

    public void setFolder(String name) {
        this.folder = name;
    }

    public void setModel(String file) {

        this.file = file;
        this.fullPath = Environment.getExternalStorageDirectory() + File.separator + "SFM" +
                File.separator + "Media" + File.separator + "Models" + File.separator + folder + File.separator + file;

        if(renderer != null) {
            renderer.clean();
            renderer.addObject(fullPath);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }


    @Override
    public void onResume() {
        super.onResume();
    }
}
