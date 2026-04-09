package com.example.moviesapp_part2_yassinejaabouk;

public class MyMovieData {
    private int movieId;
    private String movieName;
    private String movieDate;
    private String movieImage;
    private String movieDescription;

    public MyMovieData(int movieId, String movieName, String movieDate, String movieImage) {
        this.movieId = movieId;
        this.movieName = movieName;
        this.movieDate = movieDate;
        this.movieImage = movieImage;
    }

    public int getMovieId() {
        return movieId;
    }

    public String getMovieName() {
        return movieName;
    }

    public String getMovieDate() {
        return movieDate;
    }

    public String getMovieImage() {
        return movieImage;
    }

    public String getMovieDescription() {
        return movieDescription;
    }
}