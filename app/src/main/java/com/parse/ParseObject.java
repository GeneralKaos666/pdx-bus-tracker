package com.parse;

import org.json.JSONArray;

// Minimal ParseObject stub.
public class ParseObject {
    private final String className;

    public ParseObject(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public String getObjectId() {
        return "";
    }

    public String getString(String key) {
        return null;
    }

    public int getInt(String key) {
        return 0;
    }

    public double getDouble(String key) {
        return 0.0;
    }

    public boolean has(String key) {
        return false;
    }

    public JSONArray getJSONArray(String key) {
        return new JSONArray();
    }

    public <T> T getParseObject(String key) {
        return null;
    }
}
