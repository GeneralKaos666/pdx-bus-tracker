package com.trimettransit.tracker.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Detour implements Parcelable {
    public static final Parcelable.Creator CREATOR = new Creator();

    private int id;
    private String desc;
    private int[] routes;

    static class Creator implements Parcelable.Creator {
        Creator() {
        }

        @Override // android.os.Parcelable.Creator
        public Object createFromParcel(Parcel parcel) {
            return new Detour(parcel, null);
        }

        @Override // android.os.Parcelable.Creator
        public Object[] newArray(int i) {
            return new Detour[i];
        }
    }

    /* synthetic */ Detour(Parcel parcel, Creator creator) {
        this(parcel);
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDesc() {
        return this.desc;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.id);
        parcel.writeString(this.desc);
        parcel.writeIntArray(this.routes);
    }

    public Detour() {
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    private Detour(Parcel parcel) {
        this.id = parcel.readInt();
        this.desc = parcel.readString();
        this.routes = parcel.createIntArray();
    }

    public int[] getRoutes() {
        return this.routes;
    }

    public void setRoutes(int[] routes) {
        this.routes = routes;
    }
}
