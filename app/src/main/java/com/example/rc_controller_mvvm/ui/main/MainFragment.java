package com.example.rc_controller_mvvm.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.rc_controller_mvvm.R;
import com.example.rc_controller_mvvm.databinding.ControllerViewBinding;


public class MainFragment extends Fragment {

    private ControllerViewModel mViewModel;

    public static MainFragment newInstance() {

        return new MainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
//        ControllerViewBinding binding = ControllerViewBinding.inflate(inflater,container,false);
        if(mViewModel == null){
            mViewModel = new ViewModelProvider(requireActivity()).get(ControllerViewModel.class);
        }
        View view = inflater.inflate(R.layout.controller_view,container,false);
        ControllerViewBinding binding =ControllerViewBinding.bind(view);
        binding.setVm(mViewModel);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // TODO: Use the ViewModel

    }

}