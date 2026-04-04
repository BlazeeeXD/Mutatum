package com.blaze.mutatum.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blaze.mutatum.R;
import com.blaze.mutatum.MainActivity;
import com.blaze.mutatum.currency.CurrencyFragment;

import java.util.ArrayList;
import java.util.List;

/*
 * PURPOSE:
 * - Shows a grid of app cards
 * - Handles navigation based on selected app
 *
 * UI:
 * - RecyclerView (dashboardRecyclerView): displays apps in a 2x2 grid
 *
 * DATA:
 * - apps (List<AppModel>): static list of dashboard items
 *   (title, description, icon, appId)
 *
 * FLOW:
 * - onCreateView():
 *     inflate layout → setup RecyclerView → load app data → attach adapter
 *
 * NAVIGATION:
 * - DashboardAdapter click → returns appId
 * - switch(appId):
 *     1 → loads CurrencyFragment (added to back stack)
 *     others → placeholder (Toast messages)
 *
 * NOTE:
 * - Navigation delegated to MainActivity (loadFragment)
 * - appId drives routing logic
 *
 */

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.dashboardRecyclerView);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));


        List<AppModel> apps = new ArrayList<>();
        apps.add(new AppModel("Currency", "Live exchange rates", android.R.drawable.ic_menu_sort_by_size, 1));
        apps.add(new AppModel("Media", "Audio & Video stream", android.R.drawable.ic_media_play, 2));
        apps.add(new AppModel("Sensors", "Hardware visualizer", android.R.drawable.ic_menu_compass, 3));
        apps.add(new AppModel("Gallery", "Camera & Storage", android.R.drawable.ic_menu_camera, 4));

        DashboardAdapter adapter = new DashboardAdapter(apps, appId -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                switch (appId) {
                    case 1:
                        mainActivity.loadFragment(new CurrencyFragment(), true);
                        break;
                    case 2:
                        mainActivity.loadFragment(new com.blaze.mutatum.media.MediaFragment(), true);
                        break;
                    case 3:
                        mainActivity.loadFragment(new com.blaze.mutatum.sensors.SensorFragment(), true);
                        break;
                    case 4:
                        Toast.makeText(getContext(), "Gallery App coming soon...", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        recyclerView.setAdapter(adapter);
        return view;
    }
}