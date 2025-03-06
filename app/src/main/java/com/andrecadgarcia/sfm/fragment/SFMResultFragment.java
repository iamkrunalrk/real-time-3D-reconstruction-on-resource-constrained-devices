package com.andrecadgarcia.sfm.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.andrecadgarcia.sfm.R;
import com.andrecadgarcia.sfm.activity.MainActivity;


public class SFMResultFragment extends Fragment{

    private View rootview;

    private TextView result;

    public SFMResultFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(rootview == null) {

            rootview = inflater.inflate(R.layout.fragment_sfm_result, container, false);
            result = (TextView) rootview.findViewById(R.id.tv_sfm_result);

        }

        result.setText(((MainActivity) getContext()).getResult());


        return rootview;
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
