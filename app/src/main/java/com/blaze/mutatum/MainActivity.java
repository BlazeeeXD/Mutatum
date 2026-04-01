package com.blaze.mutatum;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.blaze.mad.dashboard.HomeFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inject the Dashboard on startup, but only if it's a fresh launch
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
        }
    }

    /**
     * The master method for navigating between our apps.
     * @param fragment The fragment (app) to load.
     * @param addToBackStack True if pressing 'back' should return to the previous screen.
     */
    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Custom animations for that premium feel
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