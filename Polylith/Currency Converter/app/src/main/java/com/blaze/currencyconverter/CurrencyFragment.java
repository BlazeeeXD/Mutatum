package com.blaze.currencyconverter;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.blaze.currencyconverter.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/*
 * PURPOSE:
 * - Convert user-entered amount between selected currencies
 * - Use live API rates, fallback to local rates if unavailable
 *
 * UI:
 * - amountInput: user input
 * - spinnerFrom / spinnerTo: currency selectors
 * - resultText: output display
 * - themeSwitch: dark/light toggle
 * - btnSwap: swaps currencies
 * - btnConvert: triggers conversion
 *
 * DATA:
 * - currencies[]: supported currencies (INR, USD, JPY, EUR)
 * - fallbackRates: hardcoded rates (base = USD)
 *
 * STATE:
 * - lastFromPos / lastToPos: prevents same-currency selection via smart swap logic
 *
 * FLOW:
 * - setupSpinners(): init + enforce unique selections
 * - setupThemeToggle(): sync + apply theme
 * - executeConversion():
 * - validate → fetch API → update UI → fallback if needed
 *
 * THREADING:
 * - ExecutorService: background API call
 * - Handler: UI updates on main thread
 *
 */

public class CurrencyFragment extends Fragment {

    private TextInputEditText amountInput;
    private Spinner spinnerFrom, spinnerTo;
    private TextView resultText;
    private SwitchMaterial themeSwitch;

    private final String[] currencies = {"INR", "USD", "JPY", "EUR"};

    private int lastFromPos = 1;
    private int lastToPos = 0;

    private final Map<String, Double> fallbackRates = new HashMap<>();

    public CurrencyFragment() {
        fallbackRates.put("USD", 1.0);
        fallbackRates.put("INR", 83.50);
        fallbackRates.put("EUR", 0.92);
        fallbackRates.put("JPY", 151.20);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_currency, container, false);

        amountInput = view.findViewById(R.id.amountEditText);
        spinnerFrom = view.findViewById(R.id.spinnerFrom);
        spinnerTo = view.findViewById(R.id.spinnerTo);
        resultText = view.findViewById(R.id.resultText);
        themeSwitch = view.findViewById(R.id.themeSwitch);
        ImageButton btnSwap = view.findViewById(R.id.btnSwap);
        MaterialButton btnConvert = view.findViewById(R.id.btnConvert);

        setupSpinners();
        setupThemeToggle();

        btnSwap.setOnClickListener(v -> {
            int fromPos = spinnerFrom.getSelectedItemPosition();
            spinnerFrom.setSelection(spinnerTo.getSelectedItemPosition());
            spinnerTo.setSelection(fromPos);
        });

        btnConvert.setOnClickListener(v -> executeConversion());

        return view;
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner, currencies);
        adapter.setDropDownViewResource(R.layout.item_spinner);

        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

        spinnerFrom.setSelection(lastFromPos);
        spinnerTo.setSelection(lastToPos);

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent == spinnerFrom) {
                    if (position == spinnerTo.getSelectedItemPosition()) {
                        spinnerTo.setSelection(lastFromPos);
                    }
                } else if (parent == spinnerTo) {
                    if (position == spinnerFrom.getSelectedItemPosition()) {
                        spinnerFrom.setSelection(lastToPos);
                    }
                }

                lastFromPos = spinnerFrom.getSelectedItemPosition();
                lastToPos = spinnerTo.getSelectedItemPosition();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerFrom.setOnItemSelectedListener(spinnerListener);
        spinnerTo.setOnItemSelectedListener(spinnerListener);
    }

    private void setupThemeToggle() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Unhook the listener temporarily so it doesn't fire while we set up
        themeSwitch.setOnCheckedChangeListener(null);
        themeSwitch.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_NO);

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // THE CRASH FIX: Only execute if the user physically tapped it
            if (!buttonView.isPressed()) return;

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });
    }

    private void executeConversion() {
        String amountStr = amountInput.getText().toString();
        if (amountStr.isEmpty()) {
            amountInput.setError("Enter an amount");
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String fromCurr = spinnerFrom.getSelectedItem().toString();
        String toCurr = spinnerTo.getSelectedItem().toString();

        if (fromCurr.equals(toCurr)) {
            resultText.setText(String.format("%.2f %s", amount, toCurr));
            return;
        }

        resultText.setText("Fetching...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                String urlStr = "https://api.frankfurter.app/latest?amount=" + amount + "&from=" + fromCurr + "&to=" + toCurr;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject jsonObject = new JSONObject(response.toString());
                double result = jsonObject.getJSONObject("rates").getDouble(toCurr);

                handler.post(() -> resultText.setText(String.format("%.2f %s", result, toCurr)));

            } catch (Exception e) {
                handler.post(() -> {
                    Toast.makeText(getContext(), "Offline mode: Using fallback rates", Toast.LENGTH_SHORT).show();
                    double baseAmount = amount / fallbackRates.get(fromCurr);
                    double finalAmount = baseAmount * fallbackRates.get(toCurr);
                    resultText.setText(String.format("%.2f %s", finalAmount, toCurr));
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }
}


