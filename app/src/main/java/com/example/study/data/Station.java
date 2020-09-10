package com.example.study.data;

public class Station {
    private String station_no;
    private String station_id;
    private String station_name;
    private String zipcode;
    private String gu;
    private String dong;
    private String addr;
    private String x_pos;
    private String y_pos;
    private String area;
    private String geofence_distance;
    private String bike_parking;

    public Station() {
    }

    public Station(String station_no, String station_id, String station_name, String zipcode, String gu, String dong, String addr, String x_pos, String y_pos, String area, String geofence_distance, String bike_parking) {
        this.station_no = station_no;
        this.station_id = station_id;
        this.station_name = station_name;
        this.zipcode = zipcode;
        this.gu = gu;
        this.dong = dong;
        this.addr = addr;
        this.x_pos = x_pos;
        this.y_pos = y_pos;
        this.area = area;
        this.geofence_distance = geofence_distance;
        this.bike_parking = bike_parking;
    }

    public String getStation_no() {
        return station_no;
    }

    public void setStation_no(String station_no) {
        this.station_no = station_no;
    }

    public String getStation_id() {
        return station_id;
    }

    public void setStation_id(String station_id) {
        this.station_id = station_id;
    }

    public String getStation_name() {
        return station_name;
    }

    public void setStation_name(String station_name) {
        this.station_name = station_name;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String getGu() {
        return gu;
    }

    public void setGu(String gu) {
        this.gu = gu;
    }

    public String getDong() {
        return dong;
    }

    public void setDong(String dong) {
        this.dong = dong;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getX_pos() {
        return x_pos;
    }

    public void setX_pos(String x_pos) {
        this.x_pos = x_pos;
    }

    public String getY_pos() {
        return y_pos;
    }

    public void setY_pos(String y_pos) {
        this.y_pos = y_pos;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getGeofence_distance() {
        return geofence_distance;
    }

    public void setGeofence_distance(String geofence_distance) {
        this.geofence_distance = geofence_distance;
    }

    public String getBike_parking() {
        return bike_parking;
    }

    public void setBike_parking(String bike_parking) {
        this.bike_parking = bike_parking;
    }
}
