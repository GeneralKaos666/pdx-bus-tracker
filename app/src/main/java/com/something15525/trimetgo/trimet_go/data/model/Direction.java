package com.something15525.trimetgo.trimet_go.data.model;

public class Direction {

    private int dir = 0;
    private String desc = null;
    private Route route = null;

    public void setDir(int dir) {
        this.dir = dir;
    }

    public int getDir() {
        return this.dir;
    }

    public Route getRoute() {
        return this.route;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setRoute(Route route) {
        this.route = route;
    }
}
