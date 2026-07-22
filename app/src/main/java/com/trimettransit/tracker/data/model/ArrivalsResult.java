package com.trimettransit.tracker.data.model;

import java.util.List;

public class ArrivalsResult {

    private List<Arrival> arrivals = null;
    private boolean queryError = false;
    private List<BlockPosition> blockPositions = null;
    private List<Detour> detours = null;

    public List<Arrival> getArrivals() {
        return this.arrivals;
    }

    public boolean isQueryError() {
        return this.queryError;
    }

    public List<BlockPosition> getBlockPositions() {
        return this.blockPositions;
    }

    public List<Detour> getDetours() {
        return this.detours;
    }

    public void setArrivals(List<Arrival> arrivals) {
        this.arrivals = arrivals;
    }

    public void setQueryError(boolean queryError) {
        this.queryError = queryError;
    }

    public void setBlockPositions(List<BlockPosition> blockPositions) {
        this.blockPositions = blockPositions;
    }

    public void setDetours(List<Detour> detours) {
        this.detours = detours;
    }
}
