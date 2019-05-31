package ru.alexis.audioguide.model;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

public class Place {

    private Location location;
    private double latitude;
    private double longitude;
    private String street;
    private String region;
    private String audioFileName;
    private String placeName;
    private String description;

    private List<Place> allPlaces = new ArrayList<>();

    public Place(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        location = new Location(street);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        addPlace(this);
    }

    public Place(double latitude, double longitude, String region, String street) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.region = region;
        this.street = street;
        location = new Location(street);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        addPlace(this);
    }

    public Place(double latitude, double longitude, String region, String street, String audioFileName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.region = region;
        this.street = street;
        this.audioFileName = audioFileName;
        location = new Location(street);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        addPlace(this);
    }

    public void addPlace(Place place) {
        allPlaces.add(place);
    }

    public List<Place> getAllPlaces() {
        return allPlaces;
    }

    public String getAudioFileName() {
        return audioFileName;
    }

    public void setAudioFileName(String audioFileName) {
        this.audioFileName = audioFileName;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
