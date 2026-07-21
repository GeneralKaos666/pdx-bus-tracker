package com.something15525.trimetgo.trimet_go.data.model;

public class Route {

    private String desc;
    private int routeId;
    private boolean isBus = false;
    private boolean isMax = false;
    private boolean isStreetcar = false;
    private boolean isWes = false;

    public String getDesc() {
        return this.desc;
    }

    public boolean isBus() {
        return this.isBus;
    }

    public boolean isMax() {
        return this.isMax;
    }

    public boolean isStreetcar() {
        return this.isStreetcar;
    }

    public boolean isWes() {
        return this.isWes;
    }

    public int getRouteId() {
        return this.routeId;
    }

    public String getTypeLetter() {
        if (this.isWes) {
            return "W";
        }
        if (this.isMax) {
            return "M";
        }
        if (this.isBus) {
            return "B";
        }
        return this.isStreetcar ? "S" : "Z";
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public void setType(String type, String desc) {
        if (type.equals("R") && desc.contains("WES")) {
            this.isWes = true;
            return;
        }
        if (type.equals("R") && desc.contains("MAX")) {
            this.isMax = true;
        } else if (type.equals("B")) {
            this.isBus = true;
        }
    }

    public void setStreetcarType(String desc, String type) {
        if (desc.contains("Portland Streetcar") && type.equals("R")) {
            this.isStreetcar = true;
        }
    }
}
