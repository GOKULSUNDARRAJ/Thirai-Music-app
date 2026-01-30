package com.Saalai.SalaiMusicApp.Models;

import java.util.List;

public class PlaylistSection {
    private String sectionName;
    private List<ArtistCategory> artists;
    private int layoutType; // 1: horizontal, 2: vertical, 3: grid

    public PlaylistSection(String sectionName, List<ArtistCategory> artists, int layoutType) {
        this.sectionName = sectionName;
        this.artists = artists;
        this.layoutType = layoutType;
    }

    // Default constructor for backward compatibility
    public PlaylistSection(String sectionName, List<ArtistCategory> artists) {
        this(sectionName, artists, 1); // Default to horizontal
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public List<ArtistCategory> getArtists() {
        return artists;
    }

    public void setArtists(List<ArtistCategory> artists) {
        this.artists = artists;
    }

    public int getLayoutType() {
        return layoutType;
    }

    public void setLayoutType(int layoutType) {
        this.layoutType = layoutType;
    }
}