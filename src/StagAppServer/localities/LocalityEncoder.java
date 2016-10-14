package StagAppServer.localities;

import StagAppServer.DBHandler;
import StagAppServer.WsHandler;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

import java.sql.Connection;

public class LocalityEncoder {
    private final static String SERVER_KEY = "AIzaSyD245oW2pdZRsPZGf3CyQ7Tqp2ZqIK2NSQ";
    public static LocalityEncoder instance = new LocalityEncoder();
    private GeoApiContext geoContext;

    public static LocalityEncoder getInstance(){
        return instance;
    }

    private LocalityEncoder(){
        geoContext = new GeoApiContext().setApiKey(SERVER_KEY);
    }

    private Locality encodeLatLng(LatLng coordinate){
        GeocodingApiRequest req = GeocodingApi.reverseGeocode(geoContext, coordinate);
        String city = "";
        String area = "";
        String country = "";
        try {
            GeocodingResult[] results = req.await();
            for (GeocodingResult res : results){
                for (AddressComponent component : res.addressComponents){
                    if (component.types[0] == AddressComponentType.COUNTRY)
                        country = component.longName;
                    if (component.types[0] == AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1)
                        area = component.longName;
                    if (component.types[0] == AddressComponentType.LOCALITY)
                        city = component.longName;
                }
//                if (!country.equals("") && !area.equals("") && !city.equals(""))
//                    break;
            }
            return new Locality(country, area, city);
        } catch (Exception e){
            e.printStackTrace();
            return new Locality(country, area, city);
        }
    }

    public void startEncodeLatLngTask(final String profileId, LatLng coordinate, final Connection dbConnection){
        Thread thread = new Thread(){
            @Override
            public void run() {
                Locality locality = encodeLatLng(coordinate);
                System.out.println("userId: " + profileId + " country: " + locality.getCountry() + " area: " + locality.getArea() + " city: " + locality.getCity());
                if (locality.isValidAddress()){
                    if (DBHandler.checkDiscoverer(locality, dbConnection)) {
                        DBHandler.addDiscoverer(locality, profileId, dbConnection);
                        WsHandler.getInstance().sendDiscovererNotification(profileId, locality);
                    }
                }
            }
        };
        thread.start();
    }
}
