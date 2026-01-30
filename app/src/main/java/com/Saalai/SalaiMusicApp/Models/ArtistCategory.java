package com.Saalai.SalaiMusicApp.Models;

import java.util.List;

public class ArtistCategory {
    private String artistName;
    private List<AudioModel> songs;
    private String artistImageUrl;
    private int adapterType;

    public ArtistCategory(String artistName, List<AudioModel> songs, String artistImageUrl, int adapterType) {
        this.artistName = artistName;
        this.songs = songs;
        this.artistImageUrl = artistImageUrl;
        this.adapterType = adapterType;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public List<AudioModel> getSongs() {
        return songs;
    }

    public void setSongs(List<AudioModel> songs) {
        this.songs = songs;
    }

    public String getArtistImageUrl() {
        return artistImageUrl;
    }

    public void setArtistImageUrl(String artistImageUrl) {
        this.artistImageUrl = artistImageUrl;
    }

    public int getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(int adapterType) {
        this.adapterType = adapterType;
    }
}