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
        // Ba�lat�l�yor
        markerPoints = new ArrayList<LatLng>();
        // activity_maps in SupportMapFragment a referans al�n�yor
        SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        // SupportMapFragment i�in harita al�n�yor
        map = fm.getMap();
        if (map != null) {
            // Konumumu g�ster butonu aktifle�tirildi
            map.setMyLocationEnabled(true);
            // Haritada bir noktaya t�kland���nda yap�lacaklar
            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng point) {
                    // Daha �nce iki nokta se�ilmi�se
                    if (markerPoints.size() > 1) {
                        markerPoints.clear();
                        map.clear();
                    }
                    // markerpoints arraylistine yeni nokta ekliyor
                    markerPoints.add(point);
                    // ��aretleyici ayarlar� olu�turuluyor
                    MarkerOptions options = new MarkerOptions();
                    // ��aretleyicinin pozisyon ayar�
                    options.position(point);
                    /**
                     * Ba�lang�� lokasyonu i�in, ye�il i�aretleyici.
                     * Biti� lokasyonu i�in, k�rm�z� i�aretleyici.
                     */
                    if (markerPoints.size() == 1) {
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    } else if (markerPoints.size() == 2) {
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }
                    //Haritaya i�aretleyiciler ekleniyor
                    map.addMarker(options);
                    // Ba�lang�� ve biti� noktalar�n�n al�n�p al�nmad��� kontrol ediliyor
                    if (markerPoints.size() >= 2) {
                        LatLng origin = markerPoints.get(0);
                        LatLng dest = markerPoints.get(1);
                        // Google Directions API ye url al�n�yor
                        String url = getDirectionsUrl(origin, dest);
                        DownloadTask downloadTask = new DownloadTask();
                        // Google Directions API dan json data ya indirme i�lemi yap�l�yor
                        downloadTask.execute(url);
                    }
                }
            });
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        // Ba�lang�� noktas�n�n enlem ve boylam bilgileri
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Hedef noktan�n enlem ve boylam bilgileri
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // sens�r aktifle�tiriliyor
        String sensor = "sensor=false";
        // webservice paratmetreleri yap�land�r�l�yor
        String parameters = str_origin + "&" + str_dest + "&" + sensor;
        // ��k�� format�
        String output = "json";
        // webservice url si yap�land�r�l�yor
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
            // Url ile ileti�im kurmak i�in http ba�lant�s� olu�turuluyor
            urlConnection = (HttpURLConnection) url.openConnection();
            // url ye ba�lan�l�yor
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

    // Ge�en url lerden datalar al�n�yor
    private class DownloadTask extends AsyncTask<String, Void, String> {
        //thread de olmayan data indiriliyor
        @Override
        protected String doInBackground(String... url) {
            // webservice den data depolamak i�in
            String data = "";
            try {
                // webserviceden data al�n�yor
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Thread �al��t�r�l�yor, after the execution of
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
     * JSON da ki yerleri parse eden s�n�f
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
                // Data y� parse etme ba�l�yor
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Thread �al���r, parse etme i�leminden sonra.
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
                // i. enlem boylam i�in b�t�n noktalar getiriliyor
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                // �izgi ayarlar� i�in ba�lang�� biti� noktalar� vs ayarlar� yap�l�yor
                lineOptions.addAll(points);
                lineOptions.width(2);
                lineOptions.color(Color.RED);
            }

            // �izgi �iziliyor
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