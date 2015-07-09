package com.onurgurbuz.googlemapsapi;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity {
    GoogleMap map;
    ArrayList<LatLng> markerPoints;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Baþlatýlýyor
        markerPoints = new ArrayList<LatLng>();
        // activity_maps in SupportMapFragment a referans alýnýyor
        SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        // SupportMapFragment için harita alýnýyor
        map = fm.getMap();
        if (map != null) {
            // Konumumu göster butonu aktifleþtirildi
            map.setMyLocationEnabled(true);
            // Haritada bir noktaya týklandýðýnda yapýlacaklar
            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng point) {
                    // Daha önce iki nokta seçilmiþse
                    if (markerPoints.size() > 1) {
                        markerPoints.clear();
                        map.clear();
                    }
                    // markerpoints arraylistine yeni nokta ekliyor
                    markerPoints.add(point);
                    // Ýþaretleyici ayarlarý oluþturuluyor
                    MarkerOptions options = new MarkerOptions();
                    // Ýþaretleyicinin pozisyon ayarý
                    options.position(point);
                    /**
                     * Baþlangýç lokasyonu için, yeþil iþaretleyici.
                     * Bitiþ lokasyonu için, kýrmýzý iþaretleyici.
                     */
                    if (markerPoints.size() == 1) {
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    } else if (markerPoints.size() == 2) {
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }
                    //Haritaya iþaretleyiciler ekleniyor
                    map.addMarker(options);
                    // Baþlangýç ve bitiþ noktalarýnýn alýnýp alýnmadýðý kontrol ediliyor
                    if (markerPoints.size() >= 2) {
                        LatLng origin = markerPoints.get(0);
                        LatLng dest = markerPoints.get(1);
                        // Google Directions API ye url alýnýyor
                        String url = getDirectionsUrl(origin, dest);
                        DownloadTask downloadTask = new DownloadTask();
                        // Google Directions API dan json data ya indirme iþlemi yapýlýyor
                        downloadTask.execute(url);
                    }
                }
            });
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        // Baþlangýç noktasýnýn enlem ve boylam bilgileri
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Hedef noktanýn enlem ve boylam bilgileri
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // sensör aktifleþtiriliyor
        String sensor = "sensor=false";
        // webservice paratmetreleri yapýlandýrýlýyor
        String parameters = str_origin + "&" + str_dest + "&" + sensor;
        // Çýkýþ formatý
        String output = "json";
        // webservice url si yapýlandýrýlýyor
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        return url;
    }

    /**
     * url den json data ya indirmeyi yapan method
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            // Url ile iletiþim kurmak için http baðlantýsý oluþturuluyor
            urlConnection = (HttpURLConnection) url.openConnection();
            // url ye baðlanýlýyor
            urlConnection.connect();
            // url den data okunuyor
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Log.d("URL indirilirken hata", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Geçen url lerden datalar alýnýyor
    private class DownloadTask extends AsyncTask<String, Void, String> {
        //thread de olmayan data indiriliyor
        @Override
        protected String doInBackground(String... url) {
            // webservice den data depolamak için
            String data = "";
            try {
                // webserviceden data alýnýyor
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Thread çalýþtýrýlýyor, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            // JSON data parse edilir
            parserTask.execute(result);
        }
    }

    /**
     * JSON da ki yerleri parse eden sýnýf
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                // Data yý parse etme baþlýyor
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Thread çalýþýr, parse etme iþleminden sonra.
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            // enlem boylamlar travers ediliyor
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();
                // i. enlem boylam getiriliyor
                List<HashMap<String, String>> path = result.get(i);
                // i. enlem boylam için bütün noktalar getiriliyor
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                // Çizgi ayarlarý için baþlangýç bitiþ noktalarý vs ayarlarý yapýlýyor
                lineOptions.addAll(points);
                lineOptions.width(2);
                lineOptions.color(Color.RED);
            }

            // Çizgi çiziliyor
            map.addPolyline(lineOptions);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}