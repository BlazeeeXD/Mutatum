package com.blaze.mutatum.sensors;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blaze.mutatum.R;

public class SensorFragment extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelSensor, lightSensor, proxSensor;

    // UI Elements
    private TextView tvAccelData, tvLightData, tvProximityStatus;
    private ImageView tiltDot;
    private FrameLayout tiltContainer;
    private ProgressBar lightProgressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensors, container, false);

        initViews(view);
        setupSensors();

        return view;
    }

    private void initViews(View view) {
        tvAccelData = view.findViewById(R.id.tvAccelData);
        tvLightData = view.findViewById(R.id.tvLightData);
        tvProximityStatus = view.findViewById(R.id.tvProximityStatus);
        tiltDot = view.findViewById(R.id.tiltDot);
        tiltContainer = view.findViewById(R.id.tiltContainer);
        lightProgressBar = view.findViewById(R.id.lightProgressBar);
    }

    private void setupSensors() {
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        if (accelSensor == null) Toast.makeText(getContext(), "No Accelerometer found", Toast.LENGTH_SHORT).show();
        if (lightSensor == null) Toast.makeText(getContext(), "No Light Sensor found", Toast.LENGTH_SHORT).show();
        if (proxSensor == null) Toast.makeText(getContext(), "No Proximity Sensor found", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (accelSensor != null) sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI);
            if (lightSensor != null) sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
            if (proxSensor != null) sensorManager.registerListener(this, proxSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleAccelerometer(event.values);
        } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            handleLight(event.values[0]);
        } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            handleProximity(event.values[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void handleAccelerometer(float[] values) {
        float x = values[0];
        float y = values[1];

        tvAccelData.setText(String.format("X: %.1f | Y: %.1f", x, y));

        float dotBoundary = 150f;
        float translationX = -x * (dotBoundary / 9.8f);
        float translationY = y * (dotBoundary / 9.8f);

        translationX = Math.max(-dotBoundary, Math.min(translationX, dotBoundary));
        translationY = Math.max(-dotBoundary, Math.min(translationY, dotBoundary));

        tiltDot.setTranslationX(translationX);
        tiltDot.setTranslationY(translationY);
    }

    private void handleLight(float lux) {
        tvLightData.setText((int) lux + " LUX");
        lightProgressBar.setProgress(Math.min((int) lux, 1000));
    }

    private void handleProximity(float distance) {
        if (distance < proxSensor.getMaximumRange()) {
            tvProximityStatus.setText("OBJECT DETECTED");
            tvProximityStatus.setTextColor(getResources().getColor(R.color.primary_accent, null));
        } else {
            tvProximityStatus.setText("CLEAR");
            tvProximityStatus.setTextColor(Color.parseColor("#4CAF50"));
        }
    }
}