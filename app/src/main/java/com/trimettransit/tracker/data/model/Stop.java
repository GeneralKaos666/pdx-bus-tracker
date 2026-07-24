package com.trimettransit.tracker.data.model;

import java.util.ArrayList;
import java.util.List;

public class Stop {

    private String desc;
    private String dirDesc;
    private double latitude;
    private double longitude;
    private String transitType;
    private int routeNum;
    private int locId;
    private List<Route> routes;
    public Stop() {
    }

    public Stop(String desc, String dirDesc, double latitude, double longitude, String transitType, int locId, List<Route> routes) {
        this.desc = desc;
        this.dirDesc = dirDesc;
        this.latitude = latitude;
        this.longitude = longitude;
        this.transitType = transitType;
        this.locId = locId;
        this.routes = routes;
    }

    public String getDesc() {
        return this.desc;
    }

    public String getDirDesc() {
        return this.dirDesc;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public int getLocId() {
        return this.locId;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public List<Route> getRoutes() {
        return this.routes;
    }

    public String getTransitType() {
        return this.transitType;
    }

    public void computeTransitType() {
        String str;
        for (int i = 0; i < getRoutes().size(); i++) {
            if (getRoutes().get(i).isStreetcar()) {
                this.transitType = "S";
            } else if (getRoutes().get(i).isBus()) {
                if (!getRoutes().get(i).getDesc().contains("Shuttle") && ((str = this.transitType) == null || (!str.equals("M") && !this.transitType.equals("W") && !this.transitType.equals("S")))) {
                    this.transitType = "B";
                }
            } else if (getRoutes().get(i).isMax() || getRoutes().get(i).getDesc().contains("Vintage Trolley")) {
                this.transitType = "M";
            } else if (getRoutes().get(i).isWes()) {
                this.transitType = "W";
            } else {
                this.transitType = "Z";
            }
        }
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setDirDesc(String dirDesc) {
        this.dirDesc = dirDesc;
    }

    public void setTransitType(String transitType) {
        this.transitType = transitType;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setLocId(int locId) {
        this.locId = locId;
    }

    public int getRouteNum() {
        return this.routeNum;
    }

    public void setRouteNum(int routeNum) {
        this.routeNum = routeNum;
    }

    public void addRoute(Route route) {
        List<Route> list = this.routes;
        if (list != null) {
            list.add(route);
        } else {
            this.routes = new ArrayList();
            this.routes.add(route);
        }
    }
}
