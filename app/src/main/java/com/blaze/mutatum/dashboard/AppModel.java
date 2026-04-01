package com.blaze.mad.dashboard;

public class AppModel {
    private String title;
    private String description;
    private int iconResId;
    private int appId; // To identify which fragment to open

    public AppModel(String title, String description, int iconResId, int appId) {
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
        this.appId = appId;
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIconResId() { return iconResId; }
    public int getAppId() { return appId; }
}