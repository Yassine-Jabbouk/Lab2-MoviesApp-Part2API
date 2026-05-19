package com.example.moviesapp_part2_yassinejaabouk;

public class MyMovieData {
    private int movieId;
    private String movieName;
    private String movieDate;
    private String movieImage;
    private double rating;

    public MyMovieData(int movieId, String movieName, String movieDate, String movieImage, double rating) {
        this.movieId = movieId;
        this.movieName = movieName;
        this.movieDate = movieDate;
        this.movieImage = movieImage;
        this.rating = rating;
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

    public double getRating() {
        return rating;
    }
}