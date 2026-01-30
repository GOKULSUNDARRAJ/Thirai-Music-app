package com.Saalai.SalaiMusicApp.Models;


public class AudioModel {
    private String audioName;
    private String audioUrl;
    private String audioArtist;
    private String imageUrl;

    public AudioModel(String audioName, String audioUrl, String imageUrl) {
        this.audioName = audioName;
        this.audioUrl = audioUrl;
        this.audioArtist = "Unknown Artist";
        this.imageUrl = imageUrl;
    }

    public String getAudioName() {
        return audioName;
    }

    public void setAudioName(String audioName) {
        this.audioName = audioName;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getAudioArtist() {
        return audioArtist;
    }

    public void setAudioArtist(String audioArtist) {
        this.audioArtist = audioArtist;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
