package com.blaze.sensors;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blaze.sensors.R;
import com.google.android.material.card.MaterialCardView;

/*
 * PURPOSE:
 * - Displays live sensor data with visual + semantic feedback
 *
 * SENSORS:
 * - Accelerometer: tilt-based dot movement (X/Y)
 * - Light: lux value → semantic states (DARK → BRIGHT)
 * - Proximity: near/far detection → alert UI
 *
 * UI:
 * - tvAccelData + tiltDotCard: motion visualization
 * - tvLightData / tvLightSemantic + progress bar: light intensity + meaning
 * - cardProximity + tvProximityStatus: proximity state + animations
 * - tvSystemStatus: global system state
 *
 * FLOW:
 * - setupSensors(): initialize + validate availability
 * - onResume()/onPause(): register/unregister listeners
 * - onSensorChanged(): route data to specific handlers
 *
 * PROCESSING:
 * - handleAccelerometer():
 *     applies low-pass filter → smooth motion + boundary clamp
 * - handleLight():
 *     maps lux → semantic labels + dynamic color UI
 * - handleProximity():
 *     triggers alert/idle states + animations
 *
 */

public class SensorFragment extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelSensor, lightSensor, proxSensor;

    private TextView tvSystemStatus;
    private TextView tvAccelData, tvLightData, tvLightSemantic, tvProximityStatus, tvProxLabel;
    private MaterialCardView tiltDotCard, cardProximity;
    private ProgressBar lightProgressBar;

    private float smoothX = 0f;
    private float smoothY = 0f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensors, container, false);

        initViews(view);
        setupSensors();

        return view;
    }

    private void initViews(View view) {
        tvSystemStatus = view.findViewById(R.id.tvSystemStatus);

        tvAccelData = view.findViewById(R.id.tvAccelData);
        tiltDotCard = view.findViewById(R.id.tiltDotCard);

        tvLightData = view.findViewById(R.id.tvLightData);
        tvLightSemantic = view.findViewById(R.id.tvLightSemantic);
        lightProgressBar = view.findViewById(R.id.lightProgressBar);

        cardProximity = view.findViewById(R.id.cardProximity);
        tvProximityStatus = view.findViewById(R.id.tvProximityStatus);
        tvProxLabel = view.findViewById(R.id.tvProxLabel);
    }

    private void setupSensors() {
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        if (accelSensor == null) Toast.makeText(getContext(), "No Accelerometer", Toast.LENGTH_SHORT).show();
        if (lightSensor == null) Toast.makeText(getContext(), "No Light Sensor", Toast.LENGTH_SHORT).show();
        if (proxSensor == null) Toast.makeText(getContext(), "No Proximity Sensor", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (accelSensor != null) sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void handleAccelerometer(float[] values) {
        float rawX = values[0];
        float rawY = values[1];

        tvAccelData.setText(String.format("X: %.1f | Y: %.1f", rawX, rawY));

        float dotBoundary = 140f;
        float targetX = -rawX * (dotBoundary / 9.8f);
        float targetY = rawY * (dotBoundary / 9.8f);

        smoothX = smoothX + (targetX - smoothX) * 0.15f;
        smoothY = smoothY + (targetY - smoothY) * 0.15f;

        smoothX = Math.max(-dotBoundary, Math.min(smoothX, dotBoundary));
        smoothY = Math.max(-dotBoundary, Math.min(smoothY, dotBoundary));

        tiltDotCard.setTranslationX(smoothX);
        tiltDotCard.setTranslationY(smoothY);
    }

    private void handleLight(float lux) {
        tvLightData.setText((int) lux + " LUX");
        lightProgressBar.setProgress(Math.min((int) lux, 1000));

        String semanticText;
        int color;

        if (lux < 50) {
            semanticText = "DARK";
            color = Color.parseColor("#888888");
        } else if (lux < 300) {
            semanticText = "DIM";
            color = getResources().getColor(R.color.text_light, null);
        } else if (lux < 1000) {
            semanticText = "NORMAL";
            color = Color.parseColor("#4CAF50");
        } else if (lux < 5000) {
            semanticText = "BRIGHT";
            color = getResources().getColor(R.color.primary_accent, null);
        } else {
            semanticText = "Yes That Is Sun";
            color = Color.parseColor("#FFFFFF");
        }

        tvLightSemantic.setText(semanticText);
        tvLightSemantic.setTextColor(color);
        lightProgressBar.setProgressTintList(ColorStateList.valueOf(color));
    }

    // --- 3. PROXIMITY (Aggressive UX) ---
    private void handleProximity(float distance) {
        boolean isNear = distance < proxSensor.getMaximumRange();

        if (isNear) {
            // ALARM STATE
            tvProximityStatus.setText("OBJECT DETECTED");
            tvProximityStatus.setTextColor(Color.WHITE);
            tvProxLabel.setTextColor(Color.parseColor("#FFAAAAAA"));

            cardProximity.setCardBackgroundColor(Color.parseColor("#8F0B13")); // Deep Red

            // Pulse & Scale Animation
            cardProximity.animate().scaleX(1.03f).scaleY(1.03f).setDuration(150).start();

            tvSystemStatus.setText("ALERT ACTIVE ●");
            tvSystemStatus.setTextColor(getResources().getColor(R.color.primary_accent, null));
        } else {
            // IDLE STATE
            tvProximityStatus.setText("NO OBJECT");
            tvProximityStatus.setTextColor(Color.parseColor("#888888"));
            tvProxLabel.setTextColor(getResources().getColor(R.color.secondary_bg_dark, null));

            cardProximity.setCardBackgroundColor(Color.parseColor("#1A1A1A")); // Back to dark gray

            // Relax Animation
            cardProximity.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();

            tvSystemStatus.setText("Sensors Active ●");
            tvSystemStatus.setTextColor(Color.parseColor("#4CAF50"));
        }
    }
}