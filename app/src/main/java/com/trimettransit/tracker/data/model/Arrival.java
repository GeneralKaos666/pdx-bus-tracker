package com.trimettransit.tracker.data.model;

import java.util.ArrayList;
import org.joda.time.DateTime;

public class Arrival {

    private String fullSign;
    private String shortSign;
    private DateTime estimated;
    private DateTime scheduled;
    private ArrayList<Detour> detours;
    private int route;
    private String status;
    private String tripID;
    private int blockID;
    private int vehicleID;
    private int feet;
    private int dir;
    private long estimatedMillis;
    private long scheduledMillis;

    public ArrayList<Detour> getDetours() {
        return this.detours;
    }

    public DateTime getEstimated() {
        return this.estimated;
    }

    public String getFullSign() {
        return this.fullSign;
    }

    public int getRouteId() {
        return this.route;
    }

    public DateTime getScheduled() {
        return this.scheduled;
    }

    public String getShortSign() {
        return this.shortSign;
    }

    public String getStatus() {
        return this.status;
    }

    public String getTripID() {
        return this.tripID;
    }

    public int getBlockID() {
        return this.blockID;
    }

    public int getVehicleID() {
        return this.vehicleID;
    }

    public int getFeet() {
        return this.feet;
    }

    public int getDir() {
        return this.dir;
    }

    public long getEstimatedMillis() {
        return this.estimatedMillis;
    }

    public long getScheduledMillis() {
        return this.scheduledMillis;
    }

    public void setFullSign(String fullSign) {
        this.fullSign = fullSign;
    }

    public void setShortSign(String shortSign) {
        this.shortSign = shortSign;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setEstimated(DateTime estimated) {
        this.estimated = estimated;
    }

    public void setScheduled(DateTime scheduled) {
        this.scheduled = scheduled;
    }

    public void setRouteId(int route) {
        this.route = route;
    }

    public void setDetours(ArrayList<Detour> detours) {
        this.detours = detours;
    }

    public void setTripID(String tripID) {
        this.tripID = tripID;
    }

    public void setBlockID(int blockID) {
        this.blockID = blockID;
    }

    public void setVehicleID(int vehicleID) {
        this.vehicleID = vehicleID;
    }

    public void setFeet(int feet) {
        this.feet = feet;
    }

    public void setDir(int dir) {
        this.dir = dir;
    }

    public void setEstimatedMillis(long estimatedMillis) {
        this.estimatedMillis = estimatedMillis;
    }

    public void setScheduledMillis(long scheduledMillis) {
        this.scheduledMillis = scheduledMillis;
    }
}
