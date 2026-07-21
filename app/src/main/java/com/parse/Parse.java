// Minimal Parse SDK stub — backend (trimet-go-search.somex.app) is defunct.
package com.parse;

import android.content.Context;

public class Parse {
    public static void enableLocalDatastore(Context context) {
        // No-op
    }

    public static void initialize(Configuration configuration) {
        // No-op
    }

    public static class Configuration {
        public static class Builder {
            private final Context context;
            private String applicationId;
            private String clientKey;
            private String server;

            public Builder(Context context) {
                this.context = context;
            }

            public Builder applicationId(String id) {
                this.applicationId = id;
                return this;
            }

            public Builder clientKey(String key) {
                this.clientKey = key;
                return this;
            }

            public Builder server(String url) {
                this.server = url;
                return this;
            }

            public Configuration build() {
                return new Configuration();
            }
        }
    }
}
