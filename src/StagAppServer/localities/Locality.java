package StagAppServer.localities;

public class Locality {
    private final String country;
    private final String area;
    private final String city;

    public Locality(String country, String area, String city){
        this.country = country;
        this.area = area;
        this.city = city;
    }

    public String getCountry(){
        return country;
    }

    public String getArea() {
        return area;
    }

    public String getCity() {
        return city;
    }

    public int getCountryHash(){
        return country.hashCode();
    }

    public int getAreaHash(){
        return area.hashCode();
    }

    public int getCityHash(){
        return city.hashCode();
    }

    public boolean isValidAddress(){
        return !country.equals("") && !area.equals("") && !city.equals("");
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Locality && (((Locality) obj).getCity()).equals(city) && (((Locality) obj).getArea()).equals(area) && ((((Locality) obj).getCountry()).equals(country));
    }

    @Override
    public int hashCode() {
        return area.hashCode() + country.hashCode() + city.hashCode();
    }
}
