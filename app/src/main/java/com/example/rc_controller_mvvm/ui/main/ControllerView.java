package com.example.rc_controller_mvvm.ui.main;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.example.rc_controller_mvvm.databinding.ControllerViewBinding;

public class ControllerView extends View {
    private final ControllerViewBinding binding;

    public ControllerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        binding = DataBindingUtil.getBinding(this);
    }
}
