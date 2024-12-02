package com.napier.devops;

public class CapitalCity {
    private String countryName;
    private String capitalCityName;
    private long population;

    public CapitalCity(String countryName, String capitalCityName, long population) {
        this.countryName = countryName;
        this.capitalCityName = capitalCityName;
        this.population = population;
    }

    // Getters and Setters
    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCapitalCityName() {
        return capitalCityName;
    }

    public void setCapitalCityName(String capitalCityName) {
        this.capitalCityName = capitalCityName;
    }

    public long getPopulation() {
        return population;
    }

    public void setPopulation(long population) {
        this.population = population;
    }
}
