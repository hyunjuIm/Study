package com.example.study;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.study.data.Station;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Vector;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private String TAG = "HYUNJU";

    private static FragmentActivity instance;

    private ArrayList<Station> stationList = new ArrayList<>();

    // 마커 정보 저장시킬 변수들 선언
    private Vector<LatLng> markersPosition;
    private Vector<Marker> activeMarkers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);
    }


    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        // 카메라 초기 위치 설정
        LatLng initialPosition = new LatLng(36.502812, 127.256329);
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(initialPosition);
        naverMap.moveCamera(cameraUpdate);

        sendRequest(naverMap);
    }

    public void sendRequest(@NonNull NaverMap naverMap){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://app.sejongbike.kr/v1/station/list";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray bikeStation = response.getJSONArray("sbike_station");
                            int length = bikeStation.length();
                            for(int i=0;i<length;i++){
                                JSONObject stationInfo = bikeStation.getJSONObject(i);
                                stationList.add(new Station(stationInfo.getString("station_no"),
                                        stationInfo.getString("station_id"),
                                        stationInfo.getString("station_name"),
                                        stationInfo.getString("zipcode"),
                                        stationInfo.getString("gu"),
                                        stationInfo.getString("dong"),
                                        stationInfo.getString("addr"),
                                        stationInfo.getString("x_pos"),
                                        stationInfo.getString("y_pos"),
                                        stationInfo.getString("area"),
                                        stationInfo.getString("geofence_distance"),
                                        stationInfo.getString("bike_parking")));
                                Log.e(TAG, stationList.get(i).getGu());
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        markersPosition = new Vector<LatLng>();
                        for(int i=0;i<stationList.size();i++){
                            float x = Float.parseFloat(stationList.get(i).getX_pos());
                            float y = Float.parseFloat(stationList.get(i).getY_pos());
                            markersPosition.add(new LatLng(y, x));
                            Log.e(TAG, "x : "+ x + "/ y : " + y);
                        }

                        // 마커 표시
                        for (LatLng markerPosition: markersPosition) {
                            Marker marker = new Marker();
                            marker.setPosition(markerPosition);
                            marker.setMap(naverMap);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "대여소 데이터 가져오기 실패");
            }
        });
        queue.add(jsonObjectRequest);
    }
}