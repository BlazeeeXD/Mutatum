package com.blaze.mad.dashboard;

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
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.dashboardRecyclerView);

        // Setup Grid: 2 Columns
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Load App Data
        List<AppModel> apps = new ArrayList<>();
        // Note: Replace android.R.drawable icons with your own vector assets later
        apps.add(new AppModel("Currency", "Live exchange rates", android.R.drawable.ic_menu_sort_by_size, 1));
        apps.add(new AppModel("Media", "Audio & Video stream", android.R.drawable.ic_media_play, 2));
        apps.add(new AppModel("Sensors", "Hardware visualizer", android.R.drawable.ic_menu_compass, 3));
        apps.add(new AppModel("Gallery", "Camera & Storage", android.R.drawable.ic_menu_camera, 4));

        // Initialize Adapter with Click Listener
        DashboardAdapter adapter = new DashboardAdapter(apps, appId -> {
            // This is where we will route to the other fragments later
            Toast.makeText(getContext(), "Launching App ID: " + appId, Toast.LENGTH_SHORT).show();
            // TODO: Add FragmentTransaction logic here
        });

        recyclerView.setAdapter(adapter);
        return view;
    }
}