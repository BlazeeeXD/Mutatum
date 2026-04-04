package com.blaze.mutatum;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.blaze.mutatum.dashboard.HomeFragment;

/*
 * PURPOSE:
 * - Acts as single activity hosting all fragments (apps)
 * - Handles fragment transactions and navigation flow
 *
 * FLOW:
 * - onCreate():
 *     loads HomeFragment on fresh launch
 *
 * NAVIGATION:
 * - loadFragment(fragment, addToBackStack):
 *     replaces current fragment
 *     applies animations
 *     optionally adds to back stack
 *
 * ROLE:
 * - Central entry point for all app modules (dashboard, currency, media, etc.)
 * - Fragments delegate navigation here
 *
 */

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        }

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
        }
    }
    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
        );

        transaction.replace(R.id.fragment_container, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }
}