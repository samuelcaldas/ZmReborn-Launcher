package org.zmreborn;

/**
 * Defines application permission constants.
 */
public final class Manifest {
    private Manifest() {
    }

    /**
     * Permission name constants.
     */
    public static final class permission {
        private static final String PREFIX = BuildConfig.APPLICATION_ID + ".permission.";
        public static final String INSTALL_SHORTCUT = PREFIX + "INSTALL_SHORTCUT";
        public static final String READ_SETTINGS = PREFIX + "READ_SETTINGS";
        public static final String UNINSTALL_SHORTCUT = PREFIX + "UNINSTALL_SHORTCUT";
        public static final String WRITE_SETTINGS = PREFIX + "WRITE_SETTINGS";

        private permission() {
        }
    }
}
