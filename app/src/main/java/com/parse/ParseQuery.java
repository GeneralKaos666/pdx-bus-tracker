// Minimal ParseQuery stub.
package com.parse;

import java.util.Collections;
import java.util.List;

public class ParseQuery<T extends ParseObject> {
    public static final int ORDER_ASCENDING = 1;
    public static final int ORDER_DESCENDING = -1;

    private final String className;

    public ParseQuery(String className) {
        this.className = className;
    }

    public ParseQuery<T> whereEqualTo(String key, Object value) {
        return this;
    }

    public ParseQuery<T> whereContainedIn(String key, List<?> values) {
        return this;
    }

    public ParseQuery<T> whereContains(String key, String substring) {
        return this;
    }

    public ParseQuery<T> whereExists(String key) {
        return this;
    }

    public static <T extends ParseObject> ParseQuery<T> or(List<ParseQuery<T>> queries) {
        if (queries.isEmpty()) return new ParseQuery<>("");
        return queries.get(0);
    }

    public ParseQuery<T> orderByAscending(String key) {
        return this;
    }

    public ParseQuery<T> orderByDescending(String key) {
        return this;
    }

    public ParseQuery<T> setLimit(int limit) {
        return this;
    }

    public ParseQuery<T> setSkip(int skip) {
        return this;
    }

    public ParseQuery<T> fromLocalDatastore() {
        return this;
    }

    public T getFirst() throws ParseException {
        return null;
    }

    public void getFirstInBackground(GetCallback<T> callback) {
        if (callback != null) {
            callback.done(null, null);
        }
    }

    public List<T> find() throws ParseException {
        return Collections.emptyList();
    }

    public void findInBackground(FindCallback<T> callback) {
        if (callback != null) {
            callback.done(Collections.emptyList(), null);
        }
    }

    public void countInBackground(CountCallback callback) {
        if (callback != null) {
            callback.done(0, null);
        }
    }

    public interface GetCallback<T extends ParseObject> {
        void done(T object, ParseException e);
    }

    public interface FindCallback<T extends ParseObject> {
        void done(List<T> objects, ParseException e);
    }

    public interface CountCallback {
        void done(int count, ParseException e);
    }
}
