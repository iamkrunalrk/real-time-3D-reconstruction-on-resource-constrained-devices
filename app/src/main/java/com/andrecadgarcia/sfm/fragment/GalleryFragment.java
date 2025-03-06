package com.andrecadgarcia.sfm.fragment;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.andrecadgarcia.sfm.R;
import com.andrecadgarcia.sfm.adapter.GalleryRecyclerAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class GalleryFragment extends Fragment {

    private View rootview;

    private RecyclerView rv_cardList;
    private ImageView iv_extension;

    private GalleryRecyclerAdapter galleryAdapter;

    private boolean showingPNG = true;

    List<String> folders;

    public GalleryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(rootview == null) {

            rootview = inflater.inflate(R.layout.fragment_gallery, container, false);

            folders = new ArrayList<>();

            rv_cardList = (RecyclerView) rootview.findViewById(R.id.cardList);
            iv_extension = (ImageView) rootview.findViewById(R.id.iv_file_extension);

            iv_extension.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (showingPNG) {
                        iv_extension.setImageDrawable(getResources().getDrawable(R.drawable.obj, null));
                        showingPNG = false;
                        openFolder("Models");
                        rv_cardList.getAdapter().notifyDataSetChanged();
                        galleryAdapter.setPNGViwer(false);

                    } else {
                        iv_extension.setImageDrawable(getResources().getDrawable(R.drawable.png, null));
                        showingPNG = true;
                        openFolder("Pictures");
                        rv_cardList.getAdapter().notifyDataSetChanged();
                        galleryAdapter.setPNGViwer(true);
                    }

                }
            });

            openFolder("Pictures");

            galleryAdapter = new GalleryRecyclerAdapter(folders, getContext());
            galleryAdapter.notifyDataSetChanged();

            GridLayoutManager glm = new GridLayoutManager(getContext(), 2);
            rv_cardList.setLayoutManager(glm);
            rv_cardList.setAdapter(galleryAdapter);
        }

        return rootview;
    }

    public void openFolder(String extension) {
        folders.clear();
        try {
            File dir = new File(Environment.getExternalStorageDirectory() + File.separator + "SFM" +
                    File.separator + "Media" + File.separator + extension);
            File[] dirFiles = dir.listFiles();
            for (File folder : dirFiles) {
                System.out.println(folder.getName());
                folders.add(folder.getName());
            }
        } catch(Exception e) {
            Log.d("GALLERY","NO FILES");
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
