package com.mikepenz.ionicons_typeface_library;

import com.mikepenz.iconics.IIcon;

/** Minimal stub — Ionicons icon constants used by TriMet Go. */
public class Ionicons {
    public enum Icon implements IIcon {
        ion_android_sad,
        ion_outlet;

        @Override
        public String getName() {
            return name();
        }
    }
}
