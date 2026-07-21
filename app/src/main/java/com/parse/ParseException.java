// Minimal stub matching decompiled ParseException usage.
package com.parse;

public class ParseException extends Exception {
    // Error codes referenced by app
    public static final int USERNAME_MISSING = 200;
    public static final int EMAIL_TAKEN = 203;
    public static final int CONNECTION_FAILED = 100;
    public static final int OBJECT_NOT_FOUND = 101;
    public static final int INVALID_QUERY = 102;
    public static final int INVALID_CLASS_NAME = 103;
    public static final int MISSING_OBJECT_ID = 104;
    public static final int INVALID_KEY_NAME = 105;
    public static final int INVALID_POINTER = 106;
    public static final int INVALID_JSON = 107;
    public static final int COMMAND_UNAVAILABLE = 108;
    public static final int NOT_INITIALIZED = 109;
    public static final int INCORRECT_TYPE = 111;
    public static final int INVALID_CHANNEL_NAME = 112;
    public static final int PUSH_MISCONFIGURED = 115;
    public static final int OBJECT_TOO_LARGE = 116;
    public static final int OPERATION_FORBIDDEN = 119;
    public static final int CACHE_MISS = 120;
    public static final int INVALID_NESTED_KEY = 121;
    public static final int INVALID_FILE_NAME = 122;
    public static final int INVALID_ACL = 123;
    public static final int TIMEOUT = 124;
    public static final int INVALID_EMAIL_ADDRESS = 125;
    public static final int MISSING_REQUIRED_FIELD_ERROR = 1000;
    public static final int LINKED_ID_MISSING = 250;
    public static final int ACCOUNT_ALREADY_LINKED = 208;

    private final int code;

    public ParseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
