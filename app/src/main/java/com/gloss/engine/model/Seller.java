package com.glass.engine.model;

public class Seller {
    public String id;
    public String photo;
    public String name;
    public String country;
    public String telegram;
    public String website;

    public Seller(String id, String photo, String name,
                  String country, String telegram, String website) {
        this.id = id;
        this.photo = photo;
        this.name = name;
        this.country = country;
        this.telegram = telegram;
        this.website = website;
    }
}
