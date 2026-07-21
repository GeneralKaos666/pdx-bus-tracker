package com.something15525.trimetgo.trimet_go.data.model;

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
}
