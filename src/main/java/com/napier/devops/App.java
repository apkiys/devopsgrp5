package com.napier.devops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

@SpringBootApplication
@RestController
public class App {

    /**
     * Connection to MySQL database.
     */
    private static Connection con = null;

    public static void main(String[] args) throws IOException {


        // Connect to database
        if (args.length < 1) {
            connect("localhost:33060", 0);
        } else {
            //connect via docker or actions
            connect(args[0], 10000);
            report2();
            //needed on actions else build runs forever
            // changed added dummy container that sleeps for a min then exits so actions works
            //System.exit(0);
        }

        ConfigurableApplicationContext context = SpringApplication.run(App.class, args);
        // Simulate application tasks
        System.out.println("Application tasks running...");
        // Shutdown the application after tasks are completed
        new Thread(() -> {
            try {
                Thread.sleep(10000); // Simulate 10 seconds of runtime
                System.out.println("Shutting down the application...");
                context.close(); // Gracefully close Spring Boot context
            } catch (InterruptedException e) {
                System.err.println("Error during shutdown: " + e.getMessage());
            }
        }).start();
    }

    @RequestMapping("countries")
    public ArrayList<Country> getCountries() {
        ArrayList<Country> countries = new ArrayList<>();
        try {

            // Create an SQL statement
            Statement stmt = con.createStatement();
            // SQL query to join country and city tables
            String sql = "SELECT c.Code, c.Name, c.Continent, c.Region, c.Population, c.Capital, " +
                    "ct.Name AS CapitalCityName " +
                    "FROM country c " +
                    "LEFT JOIN city ct ON c.Capital = ct.ID";
            // Execute SQL query
            ResultSet rset = stmt.executeQuery(sql);
            //cycle
            while (rset.next()) {
                String code = rset.getString("Code");
                String name = rset.getString("Name");
                String continent = rset.getString("Continent");
                String region = rset.getString("Region");
                long population = rset.getLong("Population");
                String capitalCityName = rset.getString("CapitalCityName"); // Name from the city table
                Country country = new Country(code, name, continent, region,
                        population, capitalCityName);
                countries.add(country);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Failed to get details");
            return null;
        }
        return countries;
    }

    @RequestMapping("cities")
    public ArrayList<City> getCities() {
        ArrayList<City> cities = new ArrayList<>();
        try {

            // Create an SQL statement
            Statement stmt = con.createStatement();
            // Create string for SQL statement
            String sql = "select * from city";
            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);
            //cycle
            while (rset.next()) {
                Integer id = rset.getInt("ID");
                String name = rset.getString("Name");
                String countryCode = rset.getString("CountryCode");
                String district = rset.getString("District");
                Integer population = rset.getInt("Population");
                City city = new City(id, name, countryCode, district, population);
                cities.add(city);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Failed to get details");
            return null;
        }
        return cities;
    }

    @RequestMapping("city")
    public ArrayList<City> getCity(@RequestParam(value = "id") String id) {
        City city = null;
        ArrayList<City> cities = new ArrayList<>();
        try {

            // Create an SQL statement
            Statement stmt = con.createStatement();
            // Create string for SQL statement
            String sql = "select * from city where ID = " + id;
            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);
            //cycle
            if (rset.next()) {
                String name = rset.getString("Name");
                String countryCode = rset.getString("CountryCode");
                String district = rset.getString("District");
                Integer population = rset.getInt("Population");
                city = new City(Long.parseLong(id), name, countryCode, district, population);
                cities.add(city);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Failed to get details");
            return null;
        }
        System.out.println(city);
        return cities;
    }

    @RequestMapping("capitals")
    public ArrayList<CapitalCity> getCapitalCities() {
        ArrayList<CapitalCity> capitalCities = new ArrayList<>();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();
            // SQL query to get country name, capital city name, and population
            String sql = "SELECT c.Name AS countryName, ct.Name AS capitalCityName, c.Population " +
                    "FROM country c " +
                    "JOIN city ct ON c.Capital = ct.ID";
            // Execute SQL query
            ResultSet rset = stmt.executeQuery(sql);

            // Process the result set
            while (rset.next()) {
                String countryName = rset.getString("countryName");
                String capitalCityName = rset.getString("capitalCityName");
                long population = rset.getLong("Population");

                CapitalCity capitalCity = new CapitalCity(countryName, capitalCityName, population);
                capitalCities.add(capitalCity);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("Failed to fetch capital cities");
            return null;
        }
        return capitalCities;
    }

    public static void report2() {
        StringBuilder sb = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();
            // Create string for SQL statement
            String sql = "select * from country";
            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);
            //cycle
            while (rset.next()) {
                String name = rset.getString("name");
                Integer population = rset.getInt("population");
                sb.append(name + "\t" + population + "\r\n");
            }
            new File("./output/").mkdir();
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File("./output/report1.txt")));
            writer.write(sb.toString());
            writer.close();
            System.out.println(sb.toString());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Failed to get details");
            return;
        }
    }

    /**
     * Connect to the MySQL database.
     *
     * @param conString Use db:3306 for docker and localhost:33060 for local or Integration Tests
     * @param
     */
    public static void connect(String conString, int delay) {
        try {
            // Load Database driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Could not load SQL driver");
            System.exit(-1);
        }

        int retries = 10;
        for (int i = 0; i < retries; ++i) {
            System.out.println("Connecting to database...");
            try {
                // Wait a bit for db to start
                Thread.sleep(delay);
                // Connect to database
                //Added allowPublicKeyRetrieval=true to get Integration Tests
                // to work. Possibly due to accessing from another class?
                con = DriverManager.getConnection(
                        "jdbc:mysql://" + conString + "/world?allowPublicKeyRetrieval=true&useSSL" + "=false", "root",
                        "example");
                System.out.println("Successfully connected");
                break;
            } catch (SQLException sqle) {
                System.out.println("Failed to connect to database attempt " + Integer.toString(i));
                System.out.println(sqle.getMessage());
            } catch (InterruptedException ie) {
                System.out.println("Thread interrupted? Should not happen.");
            }
        }
    }

    /**
     * Disconnect from the MySQL database.
     */
    public static void disconnect() {
        if (con != null) {
            try {
                // Close connection
                con.close();
            } catch (Exception e) {
                System.out.println("Error closing connection to database");
            }
        }
    }

    public void printCountryReport(ArrayList<Country> countries) {
        if (countries == null) {
            System.out.println("No countries found");
            return;
        }
        for (Country country : countries) {
            System.out.println(country);
        }
    }

    public void printCityReport(ArrayList<City> cities) {
        if (cities == null) {
            System.out.println("No cities found");
            return;
        }
        for (City city : cities) {
            System.out.println(city);
        }
    }
}