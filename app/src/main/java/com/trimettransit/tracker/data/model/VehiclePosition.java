package com.trimettransit.tracker.data.model;

public class VehiclePosition {

    private int vehicleID;
    private String type;
    private int blockID;
    private double latitude;
    private double longitude;
    private float bearing;
    private int routeNumber;
    private int direction;
    private String tripID;
    private boolean newTrip;
    private int delay;
    private String signMessage;
    private String signMessageLong;
    private int nextLocID;
    private int nextStopSeq;
    private int lastLocID;
    private int lastStopSeq;
    private long serviceDate;
    private int locationInScheduleDay;
    private long time;
    private long expires;
    private boolean inCongestion;
    private int loadPercentage;
    private String garage;
    private String extrablockID;
    private boolean offRoute;

    public VehiclePosition() {
    }

    public int getVehicleID() {
        return vehicleID;
    }

    public void setVehicleID(int vehicleID) {
        this.vehicleID = vehicleID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getBlockID() {
        return blockID;
    }

    public void setBlockID(int blockID) {
        this.blockID = blockID;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public int getRouteNumber() {
        return routeNumber;
    }

    public void setRouteNumber(int routeNumber) {
        this.routeNumber = routeNumber;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public String getTripID() {
        return tripID;
    }

    public void setTripID(String tripID) {
        this.tripID = tripID;
    }

    public boolean isNewTrip() {
        return newTrip;
    }

    public void setNewTrip(boolean newTrip) {
        this.newTrip = newTrip;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public String getSignMessage() {
        return signMessage;
    }

    public void setSignMessage(String signMessage) {
        this.signMessage = signMessage;
    }

    public String getSignMessageLong() {
        return signMessageLong;
    }

    public void setSignMessageLong(String signMessageLong) {
        this.signMessageLong = signMessageLong;
    }

    public int getNextLocID() {
        return nextLocID;
    }

    public void setNextLocID(int nextLocID) {
        this.nextLocID = nextLocID;
    }

    public int getNextStopSeq() {
        return nextStopSeq;
    }

    public void setNextStopSeq(int nextStopSeq) {
        this.nextStopSeq = nextStopSeq;
    }

    public int getLastLocID() {
        return lastLocID;
    }

    public void setLastLocID(int lastLocID) {
        this.lastLocID = lastLocID;
    }

    public int getLastStopSeq() {
        return lastStopSeq;
    }

    public void setLastStopSeq(int lastStopSeq) {
        this.lastStopSeq = lastStopSeq;
    }

    public long getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(long serviceDate) {
        this.serviceDate = serviceDate;
    }

    public int getLocationInScheduleDay() {
        return locationInScheduleDay;
    }

    public void setLocationInScheduleDay(int locationInScheduleDay) {
        this.locationInScheduleDay = locationInScheduleDay;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    public boolean isInCongestion() {
        return inCongestion;
    }

    public void setInCongestion(boolean inCongestion) {
        this.inCongestion = inCongestion;
    }

    public int getLoadPercentage() {
        return loadPercentage;
    }

    public void setLoadPercentage(int loadPercentage) {
        this.loadPercentage = loadPercentage;
    }

    public String getGarage() {
        return garage;
    }

    public void setGarage(String garage) {
        this.garage = garage;
    }

    public String getExtrablockID() {
        return extrablockID;
    }

    public void setExtrablockID(String extrablockID) {
        this.extrablockID = extrablockID;
    }

    public boolean isOffRoute() {
        return offRoute;
    }

    public void setOffRoute(boolean offRoute) {
        this.offRoute = offRoute;
    }
}
