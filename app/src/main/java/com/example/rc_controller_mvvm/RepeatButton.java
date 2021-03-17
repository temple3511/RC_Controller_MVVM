package com.example.rc_controller_mvvm;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;


import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RepeatButton extends AppCompatButton {
    private static final TimeUnit LONGCLICK_SCALE = TimeUnit.MILLISECONDS;
    private static final int LONGCLICK_INTERVAL = 100;
    private final Handler handler;
    private ScheduledExecutorService scheduledExecutorService;
    private boolean isTouched;
    private boolean isLongTouched;

    public RepeatButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        handler = new Handler(Looper.myLooper());

        isTouched = false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(scheduledExecutorService != null){
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(RepeatButton.this::performLongClick,LONGCLICK_INTERVAL,LONGCLICK_INTERVAL,LONGCLICK_SCALE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(scheduledExecutorService != null){
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }
    }

    @Override
    public boolean performLongClick() {
        if(isLongTouched){
            super.performLongClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if(this.isEnabled()){
            switch (event.getAction()){

                case MotionEvent.ACTION_DOWN:
                    isTouched = true;
                    handler.postDelayed(() -> {
                        if(isTouched){
                            isLongTouched = true;
                        }
                    },500);

                    return true;
                case MotionEvent.ACTION_UP:
                    isTouched = false;
                    isLongTouched = false;
                    performClick();
                    return true;
            }
        }else {
            isTouched = false;
        }

        return isEnabled();
    }
}
