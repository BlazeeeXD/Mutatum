package com.blaze.mad.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.blaze.mutatum.R;
import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.ViewHolder> {

    private List<AppModel> appList;
    private OnAppClickListener listener;

    // Interface for click events
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