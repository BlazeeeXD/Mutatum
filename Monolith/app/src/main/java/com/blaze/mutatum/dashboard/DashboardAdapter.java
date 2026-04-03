package com.blaze.mutatum.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.blaze.mutatum.R;
import java.util.List;


/*
 * PURPOSE:
 * - Binds AppModel class data to dashboard card UI (the recyclerview)
 *
 * DATA:
 * - appList: list of apps to display
 * - listener: handles item click events (navigation via appId)
 *
 * FLOW:
 * - onCreateViewHolder(): inflates item layout
 * - onBindViewHolder():
 *     binds title, description, icon
 *     attaches click → triggers listener with appId
 * - getItemCount(): returns list size
 *
 * INNER:
 * - ViewHolder: caches views (title, description, icon) for performance
 *
 * USAGE:
 * - Used in dashboard screen to render app grid/list
 * - Clicks delegated via OnAppClickListener (keeps adapter decoupled)
 *
 */

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.ViewHolder> {

    private List<AppModel> appList;
    private OnAppClickListener listener;

    public interface OnAppClickListener {
        void onAppClick(int appId);
    }

    public DashboardAdapter(List<AppModel> appList, OnAppClickListener listener) {
        this.appList = appList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppModel app = appList.get(position);
        holder.titleText.setText(app.getTitle());
        holder.descText.setText(app.getDescription());
        holder.iconImage.setImageResource(app.getIconResId());

        holder.itemView.setOnClickListener(v -> listener.onAppClick(app.getAppId()));
    }

    @Override
    public int getItemCount() { return appList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, descText;
        ImageView iconImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.appNameText);
            descText = itemView.findViewById(R.id.appDescText);
            iconImage = itemView.findViewById(R.id.iconImage);
        }
    }
}