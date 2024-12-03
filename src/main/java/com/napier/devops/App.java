package com.napier.devops;

import hello.Application;
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
            outputReport00();
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

    @RequestMapping("report01")
    public String getReport01() {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get all countries in the world, ordered by population
            String sql = "SELECT country.Name AS CountryName, country.Continent, country.Region, country.Population " +
                    "FROM country " +
                    "ORDER BY country.Population DESC";

            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>01. All Countries in the World (Largest to Smallest Population)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Region</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String countryName = rset.getString("CountryName");
                String continent = rset.getString("Continent");
                String region = rset.getString("Region");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(region).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get country details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report02")
    public String getReport02(@RequestParam(value = "continentName") String continentName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get all countries in the specified continent, ordered by population
            String sql = "SELECT country.Name AS CountryName, country.Continent, country.Region, country.Population " +
                    "FROM country " +
                    "WHERE country.Continent = ? " +
                    "ORDER BY country.Population DESC";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, continentName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>02. Countries in ").append(continentName).append(" (Largest to Smallest Population)</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report02?continentName=(enter your continent without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Region</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String countryName = rset.getString("CountryName");
                String continent = rset.getString("Continent");
                String region = rset.getString("Region");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(region).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get country details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report03")
    public String getCountriesInRegion(@RequestParam(value = "regionName") String regionName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get all countries in the specified region, ordered by population
            String sql = "SELECT country.Name AS CountryName, country.Region, country.Continent, country.Population " +
                    "FROM country " +
                    "WHERE country.Region = ? " +
                    "ORDER BY country.Population DESC";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, regionName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>03. Countries in ").append(regionName).append(" (Largest to Smallest Population)</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report03?regionName=(enter your region without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Region</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String countryName = rset.getString("CountryName");
                String region = rset.getString("Region");
                String continent = rset.getString("Continent");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(region).append("</td>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get country details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report04")
    public String getReport04(@RequestParam(value = "n") int n) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the top N populated countries in the world
            String sql = "SELECT country.Name AS CountryName, country.Continent, country.Population " +
                    "FROM country " +
                    "ORDER BY country.Population DESC " +
                    "LIMIT ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, n);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>04. Top ").append(n).append(" Populated Countries in the World</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report04?n=(enter your n without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String countryName = rset.getString("CountryName");
                String continent = rset.getString("Continent");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get country details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report05")
    public String getReport05(
            @RequestParam(value = "continentName") String continentName,
            @RequestParam(value = "n") int n) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the top N populated countries in the specified continent
            String sql = "SELECT country.Name AS CountryName, country.Continent, country.Population " +
                    "FROM country " +
                    "WHERE country.Continent = ? " +
                    "ORDER BY country.Population DESC " +
                    "LIMIT ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, continentName);
            pstmt.setInt(2, n);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>05. Top ").append(n).append(" Populated Countries in ").append(continentName).append("</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report05?continentName=(enter your continent without the bracket)" +
                    "&n=(enter your n without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String countryName = rset.getString("CountryName");
                String continent = rset.getString("Continent");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get country details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report06")
    public String getReport06(
            @RequestParam(value = "regionName") String regionName,
            @RequestParam(value = "n") int n) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the top N populated countries in the specified region
            String sql = "SELECT country.Name AS CountryName, country.Region, country.Continent, country.Population " +
                    "FROM country " +
                    "WHERE country.Region = ? " +
                    "ORDER BY country.Population DESC " +
                    "LIMIT ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, regionName);
            pstmt.setInt(2, n);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>06. Top ").append(n).append(" Populated Countries in ").append(regionName).append("</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report06?regionName=(enter your region without the bracket)" +
                    "&n=(enter your n without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Region</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String countryName = rset.getString("CountryName");
                String region = rset.getString("Region");
                String continent = rset.getString("Continent");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(region).append("</td>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get country details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report07")
    public String getReport07() {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get all cities in the world, ordered by population
            String sql = "SELECT city.Name AS CityName, city.Population, city.District, country.Name AS CountryName, country.Continent " +
                    "FROM city " +
                    "JOIN country ON city.CountryCode = country.Code " +
                    "ORDER BY city.Population DESC";

            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>07. All Cities in the World (Largest to Smallest Population)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>City Name</th>");
            htmlTable.append("<th>District</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String cityName = rset.getString("CityName");
                String district = rset.getString("District");
                String countryName = rset.getString("CountryName");
                String continent = rset.getString("Continent");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(cityName).append("</td>");
                htmlTable.append("<td>").append(district).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report08")
    public String getReport08(@RequestParam(value = "continentName") String continentName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get all cities in the specified continent, ordered by population
            String sql = "SELECT city.Name AS CityName, city.Population, city.District, country.Name AS CountryName, country.Continent " +
                    "FROM city " +
                    "JOIN country ON city.CountryCode = country.Code " +
                    "WHERE country.Continent = ? " +
                    "ORDER BY city.Population DESC";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, continentName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>08. Cities in ").append(continentName).append(" (Largest to Smallest Population)</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report08?continentName=(enter your continent without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>City Name</th>");
            htmlTable.append("<th>District</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String cityName = rset.getString("CityName");
                String district = rset.getString("District");
                String countryName = rset.getString("CountryName");
                String continent = rset.getString("Continent");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(cityName).append("</td>");
                htmlTable.append("<td>").append(district).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report09")
    public String getReport09(@RequestParam(value = "regionName") String regionName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get all cities in the specified region, ordered by population
            String sql = "SELECT city.Name AS CityName, city.Population, city.District, country.Name AS CountryName, country.Region " +
                    "FROM city " +
                    "JOIN country ON city.CountryCode = country.Code " +
                    "WHERE country.Region = ? " +
                    "ORDER BY city.Population DESC";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, regionName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>09. Cities in ").append(regionName).append(" (Largest to Smallest Population)</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report09?regionName=(enter your region without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>City Name</th>");
            htmlTable.append("<th>District</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Region</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String cityName = rset.getString("CityName");
                String district = rset.getString("District");
                String countryName = rset.getString("CountryName");
                String region = rset.getString("Region");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(cityName).append("</td>");
                htmlTable.append("<td>").append(district).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(region).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report10")
    public String getReport10(@RequestParam(value = "countryName") String countryName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get all cities in the specified country, ordered by population
            String sql = "SELECT city.Name AS CityName, city.Population, city.District, country.Name AS CountryName " +
                    "FROM city " +
                    "JOIN country ON city.CountryCode = country.Code " +
                    "WHERE country.Name = ? " +
                    "ORDER BY city.Population DESC";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, countryName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>10. Cities in ").append(countryName).append(" (Largest to Smallest Population)</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report10?countryName=(enter your country without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>City Name</th>");
            htmlTable.append("<th>District</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String cityName = rset.getString("CityName");
                String district = rset.getString("District");
                String country = rset.getString("CountryName");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(cityName).append("</td>");
                htmlTable.append("<td>").append(district).append("</td>");
                htmlTable.append("<td>").append(country).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report11")
    public String getReport11(@RequestParam(value = "districtName") String districtName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get all cities in the specified district, ordered by population
            String sql = "SELECT city.Name AS CityName, city.Population, city.District, country.Name AS CountryName " +
                    "FROM city " +
                    "JOIN country ON city.CountryCode = country.Code " +
                    "WHERE city.District = ? " +
                    "ORDER BY city.Population DESC";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, districtName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>11. Cities in ").append(districtName).append(" (Largest to Smallest Population)</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report11?districtName=(enter your district without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>City Name</th>");
            htmlTable.append("<th>District</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String cityName = rset.getString("CityName");
                String district = rset.getString("District");
                String countryName = rset.getString("CountryName");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(cityName).append("</td>");
                htmlTable.append("<td>").append(district).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report12")
    public String getReport12(@RequestParam(value = "n") int n) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the top N populated cities in the world
            String sql = "SELECT city.Name AS CityName, city.Population, city.District, country.Name AS CountryName, country.Continent " +
                    "FROM city " +
                    "JOIN country ON city.CountryCode = country.Code " +
                    "ORDER BY city.Population DESC " +
                    "LIMIT ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, n);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>12. Top ").append(n).append(" Populated Cities in the World</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report12?n=(enter your n without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>City Name</th>");
            htmlTable.append("<th>District</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String cityName = rset.getString("CityName");
                String district = rset.getString("District");
                String countryName = rset.getString("CountryName");
                String continent = rset.getString("Continent");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(cityName).append("</td>");
                htmlTable.append("<td>").append(district).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report13")
    public String getReport13(
            @RequestParam(value = "continentName") String continentName,
            @RequestParam(value = "n") int n) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the top N populated cities in the specified continent
            String sql = "SELECT city.Name AS CityName, city.Population, city.District, country.Name AS CountryName, country.Continent " +
                    "FROM city " +
                    "JOIN country ON city.CountryCode = country.Code " +
                    "WHERE country.Continent = ? " +
                    "ORDER BY city.Population DESC " +
                    "LIMIT ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, continentName);
            pstmt.setInt(2, n);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>13. Top ").append(n).append(" Populated Cities in ").append(continentName).append("</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report13?continentName=(enter your continent without the bracket)" +
                    "&n=(enter your n without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>City Name</th>");
            htmlTable.append("<th>District</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String cityName = rset.getString("CityName");
                String district = rset.getString("District");
                String countryName = rset.getString("CountryName");
                String continent = rset.getString("Continent");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(cityName).append("</td>");
                htmlTable.append("<td>").append(district).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report14")
    public String getReport14(
            @RequestParam(value = "regionName") String regionName,
            @RequestParam(value = "n") int n) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the top N populated cities in the specified region
            String sql = "SELECT city.Name AS CityName, city.Population, city.District, country.Name AS CountryName, country.Region " +
                    "FROM city " +
                    "JOIN country ON city.CountryCode = country.Code " +
                    "WHERE country.Region = ? " +
                    "ORDER BY city.Population DESC " +
                    "LIMIT ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, regionName);
            pstmt.setInt(2, n);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>14. Top ").append(n).append(" Populated Cities in ").append(regionName).append("</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report14?regionName=(enter your region without the bracket)" +
                    "&n=(enter your n without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>City Name</th>");
            htmlTable.append("<th>District</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Region</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String cityName = rset.getString("CityName");
                String district = rset.getString("District");
                String countryName = rset.getString("CountryName");
                String region = rset.getString("Region");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(cityName).append("</td>");
                htmlTable.append("<td>").append(district).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(region).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report15")
    public String getReport15(
            @RequestParam(value = "countryName") String countryName,
            @RequestParam(value = "n") int n) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the top N populated cities in the specified country
            String sql = "SELECT city.Name AS CityName, city.Population, city.District, country.Name AS CountryName " +
                    "FROM city " +
                    "JOIN country ON city.CountryCode = country.Code " +
                    "WHERE country.Name = ? " +
                    "ORDER BY city.Population DESC " +
                    "LIMIT ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, countryName);
            pstmt.setInt(2, n);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>15. Top ").append(n).append(" Populated Cities in ").append(countryName).append("</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report15?countryName=(enter your country without the bracket)" +
                    "&n=(enter your n without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>City Name</th>");
            htmlTable.append("<th>District</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String cityName = rset.getString("CityName");
                String district = rset.getString("District");
                String country = rset.getString("CountryName");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(cityName).append("</td>");
                htmlTable.append("<td>").append(district).append("</td>");
                htmlTable.append("<td>").append(country).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report16")
    public String getReport16(
            @RequestParam(value = "districtName") String districtName,
            @RequestParam(value = "n") int n) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the top N populated cities in the specified district
            String sql = "SELECT city.Name AS CityName, city.Population, city.District " +
                    "FROM city " +
                    "WHERE city.District = ? " +
                    "ORDER BY city.Population DESC " +
                    "LIMIT ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, districtName);
            pstmt.setInt(2, n);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>16. Top ").append(n).append(" Populated Cities in ").append(districtName).append("</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report16?districtName=(enter your district without the bracket)" +
                    "&n=(enter your n without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>City Name</th>");
            htmlTable.append("<th>District</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String cityName = rset.getString("CityName");
                String district = rset.getString("District");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(cityName).append("</td>");
                htmlTable.append("<td>").append(district).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report17")
    public String getReport17() {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get all capital cities in the world, ordered by population
            String sql = "SELECT city.Name AS CapitalCityName, city.Population, country.Name AS CountryName, country.Continent " +
                    "FROM country " +
                    "JOIN city ON country.Capital = city.ID " +
                    "ORDER BY city.Population DESC";

            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>17. Capital Cities in the World (Largest to Smallest)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Capital City Name</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String capitalCityName = rset.getString("CapitalCityName");
                String countryName = rset.getString("CountryName");
                String continent = rset.getString("Continent");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(capitalCityName).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get capital city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report18")
    public String getReport18(@RequestParam(value = "continentName") String continentName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get all capital cities in the specified continent, ordered by population
            String sql = "SELECT city.Name AS CapitalCityName, city.Population, country.Name AS CountryName, country.Continent " +
                    "FROM country " +
                    "JOIN city ON country.Capital = city.ID " +
                    "WHERE country.Continent = ? " +
                    "ORDER BY city.Population DESC";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, continentName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>18. Capital Cities in ").append(continentName).append(" (Largest to Smallest)</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report18?continentName=(enter your continent without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Capital City Name</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String capitalCityName = rset.getString("CapitalCityName");
                String countryName = rset.getString("CountryName");
                String continent = rset.getString("Continent");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(capitalCityName).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get capital city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report19")
    public String getReport19(@RequestParam(value = "regionName") String regionName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get all capital cities in the specified region, ordered by population
            String sql = "SELECT city.Name AS CapitalCityName, city.Population, country.Name AS CountryName, country.Region " +
                    "FROM country " +
                    "JOIN city ON country.Capital = city.ID " +
                    "WHERE country.Region = ? " +
                    "ORDER BY city.Population DESC";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, regionName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>19. Capital Cities in ").append(regionName).append(" (Largest to Smallest)</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report19?regionName=(enter your region without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Capital City Name</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Region</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String capitalCityName = rset.getString("CapitalCityName");
                String countryName = rset.getString("CountryName");
                String region = rset.getString("Region");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(capitalCityName).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(region).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get capital city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report20")
    public String getReport20(@RequestParam(value = "n") int n) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the top N populated capital cities in the world
            String sql = "SELECT city.Name AS CapitalCityName, city.Population, country.Name AS CountryName " +
                    "FROM country " +
                    "JOIN city ON country.Capital = city.ID " +
                    "ORDER BY city.Population DESC " +
                    "LIMIT ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, n);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Population Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>20. Top ").append(n).append(" Populated Capital Cities in the World</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report20?n=(enter your n without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Capital City Name</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String capitalCityName = rset.getString("CapitalCityName");
                String countryName = rset.getString("CountryName");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(capitalCityName).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get capital city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report21")
    public String getReport21(
            @RequestParam(value = "continentName") String continentName,
            @RequestParam(value = "n") int n) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the top N populated capital cities in the specified continent
            String sql = "SELECT city.Name AS CapitalCityName, city.Population, country.Name AS CountryName " +
                    "FROM country " +
                    "JOIN city ON country.Capital = city.ID " +
                    "WHERE country.Continent = ? " +
                    "ORDER BY city.Population DESC " +
                    "LIMIT ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, continentName);
            pstmt.setInt(2, n);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Population Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>21. Top ").append(n).append(" Populated Capital Cities in ").append(continentName).append("</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report21?continentName=(enter your continent without the bracket)" +
                    "&n=(enter your n without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Capital City Name</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String capitalCityName = rset.getString("CapitalCityName");
                String countryName = rset.getString("CountryName");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(capitalCityName).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get capital city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report22")
    public String getReport22(
            @RequestParam(value = "regionName") String regionName,
            @RequestParam(value = "n") int n) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the top N populated capital cities in the specified region
            String sql = "SELECT city.Name AS CapitalCityName, city.Population, country.Name AS CountryName " +
                    "FROM country " +
                    "JOIN city ON country.Capital = city.ID " +
                    "WHERE country.Region = ? " +
                    "ORDER BY city.Population DESC " +
                    "LIMIT ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, regionName);
            pstmt.setInt(2, n);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Population Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>22. Top ").append(n).append(" Populated Capital Cities in ").append(regionName).append("</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report22?regionName=(enter your region without the bracket)" +
                    "&n=(enter your n without the bracket)</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Capital City Name</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String capitalCityName = rset.getString("CapitalCityName");
                String countryName = rset.getString("CountryName");
                long population = rset.getLong("Population");

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(capitalCityName).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(population).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get capital city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report23")
    public String getReport23() {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get population details for each continent
            String sql = "SELECT country.Continent AS ContinentName, SUM(country.Population) AS TotalPopulation, " +
                    "IFNULL(SUM(city.Population), 0) AS CityPopulation " +
                    "FROM country " +
                    "LEFT JOIN city ON country.Code = city.CountryCode " +
                    "GROUP BY country.Continent";

            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Population Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>23. The population report of people, people living in cities,</h2>");
            htmlTable.append("<h2>and people not living in cities in each continent.</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Continent Name</th>");
            htmlTable.append("<th>Total Population</th>");
            htmlTable.append("<th>Population in Cities</th>");
            htmlTable.append("<th>Population not in Cities</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String continentName = rset.getString("ContinentName");
                long totalPopulation = rset.getLong("TotalPopulation");
                long cityPopulation = rset.getLong("CityPopulation");
                long nonCityPopulation = totalPopulation - cityPopulation;

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(continentName).append("</td>");
                htmlTable.append("<td>").append(totalPopulation).append("</td>");
                htmlTable.append("<td>").append(cityPopulation).append("</td>");
                htmlTable.append("<td>").append(nonCityPopulation).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get continent population details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report24")
    public String getReport24() {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get population details for each region
            String sql = "SELECT country.Region AS RegionName, SUM(country.Population) AS TotalPopulation, " +
                    "IFNULL(SUM(city.Population), 0) AS CityPopulation " +
                    "FROM country " +
                    "LEFT JOIN city ON country.Code = city.CountryCode " +
                    "GROUP BY country.Region";

            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Population Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>24. The population report of people, people living in cities,</h2>");
            htmlTable.append("<h2>and people not living in cities in each region.</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Region Name</th>");
            htmlTable.append("<th>Total Population</th>");
            htmlTable.append("<th>Population in Cities</th>");
            htmlTable.append("<th>Population not in Cities</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String regionName = rset.getString("RegionName");
                long totalPopulation = rset.getLong("TotalPopulation");
                long cityPopulation = rset.getLong("CityPopulation");
                long nonCityPopulation = totalPopulation - cityPopulation;

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(regionName).append("</td>");
                htmlTable.append("<td>").append(totalPopulation).append("</td>");
                htmlTable.append("<td>").append(cityPopulation).append("</td>");
                htmlTable.append("<td>").append(nonCityPopulation).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get region population details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report25")
    public String getReport25() {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get population details for each country
            String sql = "SELECT country.Name AS CountryName, country.Population AS TotalPopulation, " +
                    "IFNULL(SUM(city.Population), 0) AS CityPopulation " +
                    "FROM country " +
                    "LEFT JOIN city ON country.Code = city.CountryCode " +
                    "GROUP BY country.Code";

            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Population Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>25. The population report of people, people living in cities,</h2>");
            htmlTable.append("<h2>and people not living in cities in each country.</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Total Population</th>");
            htmlTable.append("<th>Population in Cities</th>");
            htmlTable.append("<th>Population not in Cities</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String countryName = rset.getString("CountryName");
                long totalPopulation = rset.getLong("TotalPopulation");
                long cityPopulation = rset.getLong("CityPopulation");
                long nonCityPopulation = totalPopulation - cityPopulation;

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(totalPopulation).append("</td>");
                htmlTable.append("<td>").append(cityPopulation).append("</td>");
                htmlTable.append("<td>").append(nonCityPopulation).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get country population details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report26")
    public String getReport26() {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the total population of the world
            String sql = "SELECT SUM(Population) AS WorldPopulation FROM country";

            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>World Population</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>26. World Population Report</h2>");

            if (rset.next()) {
                htmlTable.append("<table border='1'>");
                htmlTable.append("<tr>");
                htmlTable.append("<th>World Population</th>");
                htmlTable.append("</tr>");
                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(rset.getLong("WorldPopulation")).append("</td>");
                htmlTable.append("</tr>");
                htmlTable.append("</table>");
            } else {
                htmlTable.append("<p>No data found for world population.</p>");
            }

            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get world population details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report27")
    public String getReport27(@RequestParam(value = "continentName") String continentName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the total population of the specified continent
            String sql = "SELECT Continent, SUM(Population) AS TotalPopulation FROM country WHERE Continent = ? GROUP BY Continent";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, continentName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Population of A Continent Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>27. Population of A Continent Report</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report27?continentName=(Enter your continent name without the bracket)</h2>");
            if (rset.next()) {
                htmlTable.append("<table border='1'>");
                htmlTable.append("<tr>");
                htmlTable.append("<th>Continent Name</th>");
                htmlTable.append("<th>Total Population</th>");
                htmlTable.append("</tr>");
                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(rset.getString("Continent")).append("</td>");
                htmlTable.append("<td>").append(rset.getLong("TotalPopulation")).append("</td>");
                htmlTable.append("</tr>");
                htmlTable.append("</table>");
            } else {
                htmlTable.append("<p>No continent found with the name: ").append(continentName).append("</p>");
            }

            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get continent population details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report28")
    public String getReport28(@RequestParam(value = "regionName") String regionName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the total population of the specified region
            String sql = "SELECT Region, SUM(Population) AS TotalPopulation FROM country WHERE Region = ? GROUP BY Region";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, regionName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Population of A Region Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>28. Population of A Region Report</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report28?regionName=(Enter your region name without the bracket)</h2>");
            if (rset.next()) {
                htmlTable.append("<table border='1'>");
                htmlTable.append("<tr>");
                htmlTable.append("<th>Region Name</th>");
                htmlTable.append("<th>Total Population</th>");
                htmlTable.append("</tr>");
                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(rset.getString("Region")).append("</td>");
                htmlTable.append("<td>").append(rset.getLong("TotalPopulation")).append("</td>");
                htmlTable.append("</tr>");
                htmlTable.append("</table>");
            } else {
                htmlTable.append("<p>No region found with the name: ").append(regionName).append("</p>");
            }

            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get region population details</h2><p>" + e.getMessage() + "</p></body></html>";
        }

        return htmlTable.toString();
    }

    @RequestMapping("report29")
    public String getReport29(@RequestParam(value = "countryName") String countryName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the total population of the specified country
            String sql = "SELECT Name, Population FROM country WHERE Name = ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, countryName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Population of A Country Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>29. Population of A Country Report</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report29?countryName=(Enter your country name without the bracket)</h2>");
            if (rset.next()) {
                htmlTable.append("<table border='1'>");
                htmlTable.append("<tr>");
                htmlTable.append("<th>Country Name</th>");
                htmlTable.append("<th>Total Population</th>");
                htmlTable.append("</tr>");
                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(rset.getString("Name")).append("</td>");
                htmlTable.append("<td>").append(rset.getLong("Population")).append("</td>");
                htmlTable.append("</tr>");
                htmlTable.append("</table>");
            } else {
                htmlTable.append("<p>No country found with the name: ").append(countryName).append("</p>");
            }

            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get country population details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report30")
    public String getReport30(@RequestParam(value = "districtName") String districtName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the total population of the specified district
            String sql = "SELECT District, SUM(Population) AS TotalPopulation FROM city WHERE District = ? GROUP BY District";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, districtName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Population of A District Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>30. Population of A District Report</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report30?districtName=(Enter your district name without the bracket)</h2>");
            if (rset.next()) {
                htmlTable.append("<table border='1'>");
                htmlTable.append("<tr>");
                htmlTable.append("<th>District Name</th>");
                htmlTable.append("<th>Total Population</th>");
                htmlTable.append("</tr>");
                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(rset.getString("District")).append("</td>");
                htmlTable.append("<td>").append(rset.getLong("TotalPopulation")).append("</td>");
                htmlTable.append("</tr>");
                htmlTable.append("</table>");
            } else {
                htmlTable.append("<p>No district found with the name: ").append(districtName).append("</p>");
            }

            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get district population details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report31")
    public String getReport31(@RequestParam(value = "cityName") String cityName) {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get the population of the specified city
            String sql = "SELECT Name, Population FROM city WHERE Name = ?";

            // Use PreparedStatement to prevent SQL injection
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, cityName);

            // Execute SQL statement
            ResultSet rset = pstmt.executeQuery();

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Population of A City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>31. Population of A City Report</h2>");
            htmlTable.append("<h2>Search by: http://localhost:8080/report31?cityName=(Enter your city name without the bracket)</h2>");
            if (rset.next()) {
                htmlTable.append("<table border='1'>");
                htmlTable.append("<tr>");
                htmlTable.append("<th>City Name</th>");
                htmlTable.append("<th>Population</th>");
                htmlTable.append("</tr>");
                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(rset.getString("Name")).append("</td>");
                htmlTable.append("<td>").append(rset.getInt("Population")).append("</td>");
                htmlTable.append("</tr>");
                htmlTable.append("</table>");
            } else {
                htmlTable.append("<p>No city found with the name: ").append(cityName).append("</p>");
            }

            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get city population details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report32")
    public String getReport32() {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Step 1: Calculate the total world population
            String worldPopulationQuery = "SELECT SUM(Population) AS WorldPopulation FROM country";
            ResultSet worldPopResult = stmt.executeQuery(worldPopulationQuery);
            long worldPopulation = 0;
            if (worldPopResult.next()) {
                worldPopulation = worldPopResult.getLong("WorldPopulation");
            }

            // Step 2: Calculate the number of speakers for each specified language
            String sql = "SELECT cl.Language, SUM(c.Population * cl.Percentage / 100) AS TotalSpeakers " +
                    "FROM countrylanguage cl " +
                    "JOIN country c ON cl.CountryCode = c.Code " +
                    "WHERE cl.Language IN ('Chinese', 'English', 'Hindi', 'Spanish', 'Arabic') " +
                    "GROUP BY cl.Language " +
                    "ORDER BY TotalSpeakers DESC";

            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Language Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>32. A language report for number of people who speak the following the following" +
                    " languages</h2>");
            htmlTable.append("<h2>from greatest number to smallest, including the percentage of the world" +
                    " population:</h2>");
            htmlTable.append("<h2>Chinese, English, Hindi, Spanish, Arabic.</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Language</th>");
            htmlTable.append("<th>Total Speakers</th>");
            htmlTable.append("<th>% of World Population</th>");
            htmlTable.append("</tr>");

            // Step 3: Process the result set and create table rows
            while (rset.next()) {
                String language = rset.getString("Language");
                long totalSpeakers = rset.getLong("TotalSpeakers");
                double percentOfWorldPopulation = (worldPopulation > 0) ? ((double) totalSpeakers / worldPopulation) * 100 : 0;

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(language).append("</td>");
                htmlTable.append("<td>").append(totalSpeakers).append("</td>");
                htmlTable.append(String.format("<td>%.2f%%</td>", percentOfWorldPopulation));
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get language report details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report33")
    public String getReport33() {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to join the country and city tables to get capital city name
            String sql = "SELECT country.Code, country.Name AS CountryName, country.Continent, country.Region, " +
                    "country.Population, city.Name AS CapitalCityName " +
                    "FROM country " +
                    "LEFT JOIN city ON country.Capital = city.ID";

            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Country Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>33. A country report with Country Code, Country Name," +
                    "Continent, Region, Population, Capital.</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Country Code</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Region</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("<th>Capital City</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(rset.getString("Code")).append("</td>");
                htmlTable.append("<td>").append(rset.getString("CountryName")).append("</td>");
                htmlTable.append("<td>").append(rset.getString("Continent")).append("</td>");
                htmlTable.append("<td>").append(rset.getString("Region")).append("</td>");
                htmlTable.append("<td>").append(rset.getLong("Population")).append("</td>");
                htmlTable.append("<td>").append(rset.getString("CapitalCityName") != null ? rset.getString("CapitalCityName") : "N/A").append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get country details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report34")
    public String getReport34() {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();
            // Create string for SQL statement
            String sql = "SELECT city.ID, city.Name AS CityName, city.CountryCode, city.District, city.Population, country.Name AS CountryName " +
                    "FROM city " +
                    "JOIN country ON city.CountryCode = country.Code";
            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>34. A city report with City Name, Country Name, District, Population.</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>City Name</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>District</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(rset.getString("CityName")).append("</td>");
                htmlTable.append("<td>").append(rset.getString("CountryName")).append("</td>");
                htmlTable.append("<td>").append(rset.getString("District")).append("</td>");
                htmlTable.append("<td>").append(rset.getInt("Population")).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report35")
    public String getReport35() {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to join the country and city tables to get capital city information
            String sql = "SELECT city.Name AS CapitalCityName, country.Name AS CountryName, city.Population " +
                    "FROM city " +
                    "JOIN country ON city.ID = country.Capital";

            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Capital City Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>35. A capital city report with City Name, Country Name, Population.</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Capital City Name</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Population</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(rset.getString("CapitalCityName")).append("</td>");
                htmlTable.append("<td>").append(rset.getString("CountryName")).append("</td>");
                htmlTable.append("<td>").append(rset.getLong("Population")).append("</td>");
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get capital city details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
    }

    @RequestMapping("report36")
    public String getReport36() {
        StringBuilder htmlTable = new StringBuilder();
        try {
            // Create an SQL statement
            Statement stmt = con.createStatement();

            // Create SQL query to get population data for continents, regions, and countries
            String sql = "SELECT country.Continent, country.Region, country.Name AS CountryName, " +
                    "country.Population AS CountryPopulation, " +
                    "IFNULL(SUM(city.Population), 0) AS CityPopulation " +
                    "FROM country " +
                    "LEFT JOIN city ON country.Code = city.CountryCode " +
                    "GROUP BY country.Code";

            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(sql);

            // Start building the HTML table
            htmlTable.append("<!DOCTYPE html>");
            htmlTable.append("<html>");
            htmlTable.append("<head><title>Population Report</title></head>");
            htmlTable.append("<body>");
            htmlTable.append("<h2>36. A population report with the following:</h2>");
            htmlTable.append("<h2>The name of the continent/region/country.</h2>");
            htmlTable.append("<h2>The total population of the continent/region/country.</h2>");
            htmlTable.append("<h2>The total population of the continent/region/country living in cities (including a %).</h2>");
            htmlTable.append("<h2>The total population of the continent/region/country not living in cities (including a %).</h2>");
            htmlTable.append("<table border='1'>");
            htmlTable.append("<tr>");
            htmlTable.append("<th>Continent</th>");
            htmlTable.append("<th>Region</th>");
            htmlTable.append("<th>Country Name</th>");
            htmlTable.append("<th>Total Population</th>");
            htmlTable.append("<th>Population in Cities</th>");
            htmlTable.append("<th>% in Cities</th>");
            htmlTable.append("<th>Population not in Cities</th>");
            htmlTable.append("<th>% not in Cities</th>");
            htmlTable.append("</tr>");

            // Process the result set and create table rows
            while (rset.next()) {
                String continent = rset.getString("Continent");
                String region = rset.getString("Region");
                String countryName = rset.getString("CountryName");
                long totalPopulation = rset.getLong("CountryPopulation");
                long cityPopulation = rset.getLong("CityPopulation");
                long nonCityPopulation = totalPopulation - cityPopulation;

                double percentInCities = (totalPopulation > 0) ? ((double) cityPopulation / totalPopulation) * 100 : 0;
                double percentNotInCities = (totalPopulation > 0) ? ((double) nonCityPopulation / totalPopulation) * 100 : 0;

                htmlTable.append("<tr>");
                htmlTable.append("<td>").append(continent).append("</td>");
                htmlTable.append("<td>").append(region).append("</td>");
                htmlTable.append("<td>").append(countryName).append("</td>");
                htmlTable.append("<td>").append(totalPopulation).append("</td>");
                htmlTable.append("<td>").append(cityPopulation).append("</td>");
                htmlTable.append(String.format("<td>%.2f%%</td>", percentInCities));
                htmlTable.append("<td>").append(nonCityPopulation).append("</td>");
                htmlTable.append(String.format("<td>%.2f%%</td>", percentNotInCities));
                htmlTable.append("</tr>");
            }

            // Close the table and HTML tags
            htmlTable.append("</table>");
            htmlTable.append("</body>");
            htmlTable.append("</html>");

        } catch (Exception e) {
            return "<html><body><h2>Error: Failed to get population report details</h2><p>" + e.getMessage() + "</p></body></html>";
        }
        return htmlTable.toString();
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

    public static void outputReport00() {
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
                // Print header
                sb.append("|" + "\t" + name + "\t" + "|" + "\t" + population + "\t" + "|" + "\r\n");
            }
            new File("./output/").mkdir();
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File("./output/report00.txt")));
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