package com.blaze.mutatum.dashboard;

/*
 * PURPOSE:
 * - Represents a single app/card in the dashboard
 *
 * DATA:
 * - title: display name
 * - description: short info text
 * - iconResId: drawable resource for icon
 * - appId: identifier to decide which fragment to open
 *
 * USAGE:
 * - Used by adapters to populate dashboard UI
 * - appId drives navigation logic
 *
 */

public class AppModel {
    private String title;
    private String description;
    private int iconResId;
    private int appId;

    public AppModel(String title, String description, int iconResId, int appId) {
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
        this.appId = appId;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIconResId() { return iconResId; }
    public int getAppId() { return appId; }
}