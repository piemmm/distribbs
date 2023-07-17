package org.ka2ddo.util;
/*
 * Copyright (C) 2011-2023 Andrew Pavlin, KA2DDO
 * This file is part of YAAC.
 *
 *  YAAC is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  YAAC is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  and GNU Lesser General Public License along with YAAC.  If not,
 *  see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class manages the debug controls for the application, independent of how
 * the controls are set by any user interface. As such, code wanting to test the
 * debug controls to decide whether to output debug data can do so without being
 * locked to a particular application.
 * @author Andrew Pavlin, KA2DDO
 */
public class DebugCtl {
    /**
     * Implementors of this interface, if registered with the {@link #addDbgListener(DbgListener, String, String)}
     * method, can be informed of dynamic changes in the debug flags.
     * @author Andrew Pavlin, KA2DDO
     */
    @FunctionalInterface
    public interface DbgListener {
        /**
         * Specify if a specific category of debug messages should be printed out.
         * @param categoryName String name of category to enable debug logging for
         * @param setting boolean true or false to enable or disable debugging this category
         */
        void setDebug(String categoryName, boolean setting);
    }
    private static final HashMap<String,Boolean> debugCategories = new LinkedHashMap<>();
    private static final HashMap<String, ArrayList<DbgListener>> debugListeners = new HashMap<>();
    private static final HashMap<String,String> debugTags = new HashMap<>();

    private DebugCtl() {} // singleton class with only static methods

    /**
     * Specify if a specific category of debug messages should be printed out.
     * @param categoryName String name of category to enable debug logging for
     * @param setting boolean true or false to enable or disable debugging this category
     */
    public static void setDebug(String categoryName, boolean setting) {
        debugCategories.put(categoryName, Boolean.valueOf(setting));
        ArrayList<DbgListener> listeners = debugListeners.get(categoryName);
        if (listeners != null) {
            for (int i = listeners.size() - 1; i >= 0; i--) {
                listeners.get(i).setDebug(categoryName, setting);
            }
        }
        if (!"all".equals(categoryName)) {
            listeners = debugListeners.get("all");
            if (listeners != null) {
                for (int i = listeners.size() - 1; i >= 0; i--) {
                    listeners.get(i).setDebug(categoryName, setting);
                }
            }
        }
    }

    /**
     * Indicate if debug messages should be printed out.
     * @return boolean true if debug messages should be printed
     */
    public static boolean isDebug() {
        return Boolean.TRUE.equals(debugCategories.get("all"));
    }

    /**
     * Indicate if a specific category of debug messages should be printed out.
     * @param categoryName String name of category to enable debug logging for
     * @return boolean true if the named category of debug messages should be printed
     */
    public static boolean isDebug(String categoryName) {
        return true;
    }

    /**
     * Indicate if a specific category of debug messages should be printed out.
     * @param categoryName String name of category to enable debug logging for
     * @return boolean true if the named category of debug messages should be printed
     */
    public static boolean isDebugOnly(String categoryName) {
        Boolean isDebug = debugCategories.get(categoryName);
        if (isDebug == null) {
            setDebug(categoryName, Boolean.FALSE);
        }
        return Boolean.TRUE.equals(isDebug);
    }

    /**
     * Register a listener for the specified categories of debug events. If registered for
     * the "all" category, such listeners will be informed of all debug changes and given
     * the name of the category actually changed.
     * @param l DbgListener to register (or null to just register a type and description)
     * @param categoryName String names of debug categories; null implies "all"
     * @param tagsToDisplayName String tag name for displaying the categories for dynamic debug level switching from the UI
     */
    public static void addDbgListener(DbgListener l, String categoryName, String tagsToDisplayName) {
        if (categoryName == null) {
            categoryName = "all";
        }
        if (l != null) {
            ArrayList<DbgListener> listeners = debugListeners.get(categoryName);
            if (listeners == null) {
                debugListeners.put(categoryName, listeners = new ArrayList<>());
            }
            if (!listeners.contains(l)) {
                listeners.add(l);
            }
        }
        if (tagsToDisplayName != null) {
            debugTags.put(categoryName, tagsToDisplayName);
        }
    }

    /**
     * Unregister a listener for the specified categories of debug events.
     * @param l DbgListener to unregister
     * @param categoryNames array of String names of debug categories; zero-length implies an array of "all"
     */
    public static void removeDbgListener(DbgListener l, String... categoryNames) {
        if (categoryNames.length == 0) {
            categoryNames = new String[]{"all"};
        }
        for (String cName : categoryNames) {
            ArrayList<DbgListener> listeners = debugListeners.get(cName);
            if (listeners != null) {
                listeners.remove(l);
                if (listeners.size() == 0) {
                    debugListeners.remove(cName);
                }
            }
        }
    }

    /**
     * Get the list of known dynamically modifiable debug categories and their tags for locale-specific descriptions.
     * @return read-only Set of Map.Entry of category ID to resource tag names
     */
    public static Set<Map.Entry<String,String>> getCategoryCodesAndTagNames() {
        return Collections.unmodifiableSet(debugTags.entrySet());
    }

    /**
     * Get the list of known dynamically modifiable debug categories and their tags for locale-specific descriptions.
     * @return read-only Set of Map.Entry of category ID to resource tag names
     */
    public static List<Map.Entry<String,Boolean>> getCategorySettings() {
        return new ArrayList<>(debugCategories.entrySet());
    }

    /**
     * Get the localization tag name for a description of the category.
     * @param category String category name
     * @return String tag for looking up localized description in ResourceBundles, or
     *              null if no known tag
     */
    public static String getCategoryTag(String category) {
        return debugTags.get(category);
    }
}
