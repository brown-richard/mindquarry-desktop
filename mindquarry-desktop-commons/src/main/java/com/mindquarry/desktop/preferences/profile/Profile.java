/*
 * Copyright (C) 2006-2007 Mindquarry GmbH, All Rights Reserved
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 */
package com.mindquarry.desktop.preferences.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.preference.PreferenceStore;

/**
 * Add summary documentation here.
 * 
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class Profile {
    private static Log log = LogFactory.getLog(Profile.class);

    public static final String PREF_WORKSPACES = "workspaces"; //$NON-NLS-1$

    public static final String PREF_SERVER_URL = "url"; //$NON-NLS-1$

    public static final String PREF_PASSWORD = "password"; //$NON-NLS-1$

    public static final String PREF_LOGIN = "login"; //$NON-NLS-1$

    public static final String PREF_NAME = "name"; //$NON-NLS-1$

    public static final String PREF_SELECTED_TEAMS = "teams"; //$NON-NLS-1$

    public static final String PROFILE_KEY_BASE = "com.mindquarry.server.profile."; //$NON-NLS-1$

    public static final String PROFILE_SELECTED = "com.mindquarry.server.selected"; //$NON-NLS-1$

    private static final String DELIM = "."; //$NON-NLS-1$

    private static final String EMPTY = ""; //$NON-NLS-1$

    private String name;

    private String login;

    private String password;

    private String serverURL;

    private String workspaceFolder;

    private List<String> selectedTeams = new ArrayList<String>();

    /**
     * Default constructor
     */
    public Profile() {
    }

    /**
     * Copy constructor.
     */
    public Profile(Profile old) {
        name = new String(old.getName());
        login = new String(old.getLogin());
        password = new String(old.getPassword());
        serverURL = new String(old.getServerURL());
        workspaceFolder = new String(old.getWorkspaceFolder());
        selectedTeams = new ArrayList<String>(old.getSelectedTeams());
    }

    /**
     * Constructor that initializes all fields.
     */
    public Profile(String name, String login, String password,
            String serverURL, String workspaceFolder) {
        this(name, login, password, serverURL, workspaceFolder,
                new ArrayList<String>());
    }

    /**
     * Constructor that initializes all fields.
     */
    public Profile(String name, String login, String password,
            String serverURL, String workspaceFolder, List<String> selectedTeams) {
        super();
        this.name = name;
        this.login = login;
        this.password = password;
        this.serverURL = serverURL;
        this.workspaceFolder = workspaceFolder;
        this.selectedTeams = selectedTeams;
    }

    /**
     * Getter for name.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for name.
     * 
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for serverURL.
     * 
     * @return the serverURL
     */
    public String getServerURL() {
        return serverURL;
    }

    /**
     * Setter for serverURL.
     * 
     * @param url
     *            the serverURL to set
     */
    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    /**
     * Getter for workspaceFolder.
     * 
     * @return the workspaceFolder
     */
    public String getWorkspaceFolder() {
        return workspaceFolder;
    }

    /**
     * Setter for location.
     * 
     * @param folder
     *            the location to set
     */
    public void setWorkspaceFolder(String workspaceFolder) {
        this.workspaceFolder = workspaceFolder;
    }

    /**
     * Getter for login.
     * 
     * @return the login
     */
    public String getLogin() {
        return login;
    }

    /**
     * Setter for login.
     * 
     * @param login
     *            the login to set
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Getter for password.
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Setter for password.
     * 
     * @param password
     *            the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getSelectedTeams() {
        return selectedTeams;
    }

    public void selectTeam(String teamID) {
        if (!selectedTeams.contains(teamID)) {
            selectedTeams.add(teamID);
        }
    }

    public void clearSelectedTeams() {
        selectedTeams.clear();
    }

    private void setSelectedTeams(List<String> selectedTeams) {
        this.selectedTeams = selectedTeams;
    }

    public static List<Profile> loadProfiles(PreferenceStore store) {
        HashMap<Integer, Profile> storedProfiles = new HashMap<Integer, Profile>();

        // load stored profiles
        String[] prefs = store.preferenceNames();
        for (String pref : prefs) {
            if (pref.startsWith(PROFILE_KEY_BASE)) {
                String val = store.getString(pref);
                if (val.trim().equals(EMPTY)) {
                    // ignore empty values as PreferenceStore cannot properly
                    // delete entries, so we just set deleted entries to the
                    // empty string
                    continue;
                }
                // analyze preference
                int number = Integer.valueOf(pref.substring(PROFILE_KEY_BASE
                        .length(), PROFILE_KEY_BASE.length() + 1));
                String prefName = pref.substring(PROFILE_KEY_BASE.length() + 2,
                        pref.length());

                // initialize profile
                Profile profile;
                if (storedProfiles.containsKey(number)) {
                    profile = storedProfiles.get(number);
                } else {
                    profile = new Profile(EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
                    storedProfiles.put(number, profile);
                }
                setProfileAttribute(store, profile, pref, prefName);
            }
        }
        // set profile list
        List<Profile> profiles = new ArrayList<Profile>();
        Iterator<Integer> keyIter = storedProfiles.keySet().iterator();
        while (keyIter.hasNext()) {
            Profile profile = storedProfiles.get(keyIter.next());
            profiles.add(profile);
        }
        return profiles;
    }

    public static void storeProfiles(PreferenceStore store,
            List<Profile> profiles) {
        if (store == null) {
            return;
        }
        // PreferenceStore cannot properly delete entries, so we first "delete"
        // all entries by setting them to the empty string and then set all
        // entries that are left in the following loop (on reading the config,
        // we ignore empty entries):
        int pos = 0;
        for (String storeKey : store.preferenceNames()) {
            if (storeKey.startsWith(Profile.PROFILE_KEY_BASE)) {
                store.putValue(Profile.PROFILE_KEY_BASE + pos + DELIM
                        + Profile.PREF_NAME, EMPTY);
                store.putValue(Profile.PROFILE_KEY_BASE + pos + DELIM
                        + Profile.PREF_LOGIN, EMPTY);
                store.putValue(Profile.PROFILE_KEY_BASE + pos + DELIM
                        + Profile.PREF_SERVER_URL, EMPTY);
                store.putValue(Profile.PROFILE_KEY_BASE + pos + DELIM
                        + Profile.PREF_WORKSPACES, EMPTY);
                store.putValue(Profile.PROFILE_KEY_BASE + pos + DELIM
                        + Profile.PREF_PASSWORD, EMPTY);
                pos++;
            }
        }
        // reset counter and store profiles
        pos = 0;
        for (Profile profile : profiles) {
            store.putValue(Profile.PROFILE_KEY_BASE + pos + DELIM
                    + Profile.PREF_NAME, profile.getName());
            store.putValue(Profile.PROFILE_KEY_BASE + pos + DELIM
                    + Profile.PREF_LOGIN, profile.getLogin());
            store.putValue(Profile.PROFILE_KEY_BASE + pos + DELIM
                    + Profile.PREF_SERVER_URL, profile.getServerURL());
            store.putValue(Profile.PROFILE_KEY_BASE + pos + DELIM
                    + Profile.PREF_WORKSPACES, profile.getWorkspaceFolder());

            // pseudo-encrypt and store password
            byte[] passwordData = Base64.encodeBase64(profile.getPassword()
                    .getBytes());
            store.putValue(Profile.PROFILE_KEY_BASE + pos + DELIM
                    + Profile.PREF_PASSWORD, new String(passwordData));

            // store selected teams
            String selectedTeamsValue = "";
            for (String teamID : profile.getSelectedTeams()) {
                if (selectedTeamsValue.equals("")) {
                    selectedTeamsValue += teamID;
                } else {
                    selectedTeamsValue += ("," + teamID);
                }
            }
            store.putValue(Profile.PROFILE_KEY_BASE + pos + DELIM
                    + Profile.PREF_SELECTED_TEAMS, selectedTeamsValue);
            pos++;
        }
    }

    public static boolean addProfile(PreferenceStore store, Profile toBeStored) {
        List<Profile> profiles = loadProfiles(store);
        for (Profile profile : profiles) {
            if (profile.getName().equals(toBeStored.getName())) {
                return false;
            }
        }
        profiles.add(toBeStored);
        storeProfiles(store, profiles);
        return true;
    }

    public static void selectProfile(PreferenceStore store, String name) {
        store.putValue(PROFILE_SELECTED, name);
    }

    public static Profile getSelectedProfile(PreferenceStore store) {
        Profile selected = getSelectedProfile(store, store
                .getString(PROFILE_SELECTED));
        if (selected == null) {
            log.debug("No profile selected."); //$NON-NLS-1$
            return null;
        }
        return selected;
    }

    private static Profile getSelectedProfile(PreferenceStore store,
            String namePrefId) {
        Profile profile = null;
        int profileID = -1;

        String[] prefs = store.preferenceNames();

        // try to find ID of the given
        for (String pref : prefs) {
            if (pref.startsWith(PROFILE_KEY_BASE)) {
                // analyze preference
                int id = Integer.valueOf(pref.substring(PROFILE_KEY_BASE
                        .length(), PROFILE_KEY_BASE.length() + 1));
                String prefName = pref.substring(PROFILE_KEY_BASE.length() + 2,
                        pref.length());

                if ((prefName.equals(PREF_NAME))
                        && (store.getString(pref).equals(namePrefId))) {
                    profileID = id;
                }
            }
        }
        if (profileID == -1) {
            return profile;
        }
        for (String pref : prefs) {
            if (pref.startsWith(PROFILE_KEY_BASE)) {
                // analyze preference
                int id = Integer.valueOf(pref.substring(PROFILE_KEY_BASE
                        .length(), PROFILE_KEY_BASE.length() + 1));
                String prefName = pref.substring(PROFILE_KEY_BASE.length() + 2,
                        pref.length());

                // initialize profile if not done already
                if (profile == null) {
                    profile = new Profile(EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
                }

                // set profile values
                if (id == profileID) {
                    setProfileAttribute(store, profile, pref, prefName);
                }
            }
        }
        return profile;
    }

    private static void setProfileAttribute(PreferenceStore store,
            Profile profile, String pref, String prefName) {
        if (prefName.equals(PREF_NAME)) {
            profile.setName(store.getString(pref));
        } else if (prefName.equals(PREF_LOGIN)) {
            profile.setLogin(store.getString(pref));
        } else if (prefName.equals(PREF_PASSWORD)) {
            byte[] passwordData = store.getString(pref).getBytes();
            String password = new String(Base64.decodeBase64(passwordData));
            profile.setPassword(password);
        } else if (prefName.equals(PREF_SERVER_URL)) {
            profile.setServerURL(store.getString(pref));
        } else if (prefName.equals(PREF_WORKSPACES)) {
            profile.setWorkspaceFolder(store.getString(pref));
        } else if (prefName.equals(PREF_SELECTED_TEAMS)) {
            profile.setSelectedTeams(new ArrayList<String>(Arrays.asList(store
                    .getString(pref).split(","))));
        }
    }
}
