package com.napier.devops;


public class Country {
    private String code;
    private String name;
    private String continent;
    private String region;
    private long population;
    private String capital;

    public Country(String code, String name, String continent, String region,
                   long population, String capital) {
        this.code = code;
        this.name = name;
        this.continent = continent;
        this.region = region;
        this.population = population;
        this.capital = capital;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getContinent() {
        return continent;
    }

    public String getRegion() {
        return region;
    }

    public long getPopulation() {
        return population;
    }

    public String getCapital() {
        return capital;
    }

    @Override
    public String toString() {
        return "Country{" + "code='" + code + '\'' + ", name='" + name + '\'' + ", continent='" + continent + '\''
                + ", region='" + region + '\'' + ", population=" + population + ", capital=" + capital + '}';
    }
}