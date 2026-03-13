package net.map591.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;


public class LocationData {
    private String terminalPhone;
    private String platePhone;
    private int msgSn;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime gpsTime;
    private double longitude;
    private double latitude;
    private double speed;
    private double direction;
    private String geom;

    public LocationData() {
    }

    public LocationData(String terminalPhone, String platePhone, int msgSn, LocalDateTime gpsTime, double longitude, double latitude, double speed, double direction, String geom) {
        this.terminalPhone = terminalPhone;
        this.platePhone = platePhone;
        this.msgSn = msgSn;
        this.gpsTime = gpsTime;
        this.longitude = longitude;
        this.latitude = latitude;
        this.speed = speed;
        this.direction = direction;
        this.geom = geom;
    }

    public String getGeom() {
        return geom;
    }

    public void setGeom(double longitude, double latitude) {
        this.geom = String.format("POINT(%f %f)", longitude, latitude);
    }

    public String getTerminalPhone() {
        return terminalPhone;
    }

    public void setTerminalPhone(String terminalPhone) {
        this.terminalPhone = terminalPhone;
    }

    public String getPlatePhone() {
        return platePhone;
    }

    public void setPlatePhone(String platePhone) {
        this.platePhone = platePhone;
    }

    public int getMsgSn() {
        return msgSn;
    }

    public void setMsgSn(int msgSn) {
        this.msgSn = msgSn;
    }

    public LocalDateTime getGpsTime() {
        return gpsTime;
    }

    public void setGpsTime(LocalDateTime gpsTime) {
        this.gpsTime = gpsTime;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getDirection() {
        return direction;
    }

    public void setDirection(double direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return "{" +
                "terminalPhone:'" + terminalPhone + '\'' +
                ", platePhone:'" + platePhone + '\'' +
                ", msgSn:" + msgSn +
                ", gpsTime:" + gpsTime +
                ", longitude:" + longitude +
                ", latitude:" + latitude +
                ", speed:" + speed +
                ", direction:" + direction +
                '}';
    }
}
