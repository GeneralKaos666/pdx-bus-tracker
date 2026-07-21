package com.something15525.trimetgo.trimet_go.data.model;

import java.util.List;

public class ArrivalsResult {

    private List<Arrival> arrivals = null;
    private boolean queryError = false;

    public List<Arrival> getArrivals() {
        return this.arrivals;
    }

    public boolean isQueryError() {
        return this.queryError;
    }

    public void setArrivals(List<Arrival> arrivals) {
        this.arrivals = arrivals;
    }

    public void setQueryError(boolean queryError) {
        this.queryError = queryError;
    }
}
