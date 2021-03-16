package com.example.rc_controller_mvvm.ui.main;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.databinding.ObservableByte;

import java.util.concurrent.ScheduledExecutorService;

public class Controller {


    private static final int ROLL_MEMORY_SIZE = 5;
    private final double[] rollMemory;
    private int index = 0;
    private byte roll;

    private byte speed;

    public Controller(){

        rollMemory = new double[ROLL_MEMORY_SIZE];
        for(int i=0; i<ROLL_MEMORY_SIZE;i++){
            rollMemory[i] = 90.0;
        }
        roll = 90;
        index = 0;
        speed = 0;
    }

    public void setRoll(double x,double y) {
        rollMemory[index] = Math.toDegrees(Math.atan2(x,y));
        if(index < ROLL_MEMORY_SIZE){
            index++;
        }else {
            index = 0;
        }
        double result = 0.0;
        for(double memory : rollMemory){
            result += memory;
        }
        result /= ROLL_MEMORY_SIZE;
        if(result < 0){
            result = 0;
        }
        if (result > 180){
            result = 180;
        }
        roll=(byte)result;
    }

    public void setSpeed(int speed) {
        if(speed < 0){
            this.speed = 0;
        }else if(speed > 100) {
            this.speed = 100;
        }else {
            this.speed = (byte) speed;
        }
    }

    public byte getRoll(){
        return roll;
    }

    public byte getSpeed() {
        return speed;
    }


}
