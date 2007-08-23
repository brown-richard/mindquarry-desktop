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
package com.mindquarry.desktop.client.widget.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.javahl.StatusKind;

import com.mindquarry.desktop.client.Messages;
import com.mindquarry.desktop.client.MindClient;
import com.mindquarry.desktop.client.action.ActionBase;
import com.mindquarry.desktop.client.action.workspace.InteractiveConflictHandler;
import com.mindquarry.desktop.client.widget.util.container.ContainerWidget;
import com.mindquarry.desktop.model.team.Team;
import com.mindquarry.desktop.preferences.profile.Profile;
import com.mindquarry.desktop.workspace.SVNSynchronizer;

/**
 * @author <a href="saar(at)mindquarry(dot)com">Alexander Saar</a>
 */
public class WorkspaceBrowserWidget extends ContainerWidget<TreeViewer> {
    private static Log log = LogFactory.getLog(WorkspaceBrowserWidget.class);

    protected File workspaceRoot;

    protected Map<File, Integer> localChanges = new HashMap<File, Integer>();

    protected Map<File, Integer> remoteChanges = new HashMap<File, Integer>();

    private static final String NO_CHANGES_MESSAGE = Messages
            .getString("No changes to synchronize.");

    public WorkspaceBrowserWidget(Composite parent, MindClient client) {
        super(parent, SWT.NONE, client);
    }

    // #########################################################################
    // ### PUBLIC METHODS
    // #########################################################################

    /**
     * Runs task update in a separate thread, so that GUI can continue
     * processing. Thus this method returns immediately. While updating tasks
     * the Task Manager will show an update widget instead of the task table.
     */
    public void asyncRefresh() {
        log.info("Starting async workspace changes refresh."); //$NON-NLS-1$
        if (refreshing) {
            log.info("Already refreshing, nothing to do."); //$NON-NLS-1$
            return;
        }
        refreshing = true;
        Thread updateThread = new Thread(new Runnable() {
            public void run() {
                client.startAction(Messages
                        .getString("Refreshing workspace changes"));
                refresh();
                client.stopAction(Messages
                        .getString("Refreshing workspace changes"));
            }
        }, "workspace-changes-update");
        updateThread.setDaemon(true);
        updateThread.start();
    }

    /**
     * Checks if a refresh of the changes list itself is needed.
     */
    public boolean refreshNeeded() {
        PreferenceStore store = client.getPreferenceStore();
        Profile selectedProfile = Profile.getSelectedProfile(store);
        if (selectedProfile == null) {
            log.debug("No profile selected."); //$NON-NLS-1$
            return false;
        }
        Map<File, Integer> newLocalChanges = new HashMap<File, Integer>();
        Map<File, Integer> newRemoteChanges = new HashMap<File, Integer>();
        getAllChanges(selectedProfile, newLocalChanges, newRemoteChanges);
        if (newLocalChanges.equals(localChanges)
                && newRemoteChanges.equals(remoteChanges)) {
            log.debug("Changes list does not need update");
            return false;
        }
        log.debug("Changes list needs update");
        return true;
    }

    public void refresh() {
        PreferenceStore store = client.getPreferenceStore();
        Profile selectedProfile = Profile.getSelectedProfile(store);
        if (selectedProfile == null) {
            log.debug("No profile selected."); //$NON-NLS-1$
            return;
        }
        
        client.enableActions(false, ActionBase.WORKSPACE_ACTION_GROUP);
        updateContainer(true, null, false);
        Map<File, Integer> newLocalChanges = new HashMap<File, Integer>();
        Map<File, Integer> newRemoteChanges = new HashMap<File, Integer>();
        getAllChanges(selectedProfile, newLocalChanges, newRemoteChanges);
        localChanges = newLocalChanges;
        remoteChanges = newRemoteChanges;
        workspaceRoot = new File(selectedProfile.getWorkspaceFolder());

        boolean empty = localChanges.size() == 0 && remoteChanges.size() == 0;
        if (empty) {
            updateContainer(false, null, true);
        } else {
            updateContainer(false, null, false);
        }
        refreshing = false;
        client.enableActions(true, ActionBase.WORKSPACE_ACTION_GROUP);
    }

    // #########################################################################
    // ### PRIVATE METHODS
    // #########################################################################

    private void getAllChanges(Profile selected,
            Map<File, Integer> localChanges, Map<File, Integer> remoteChanges) {
        try {
            final List<Team> selectedTeams = new ArrayList<Team>();
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    selectedTeams.addAll(client.getSelectedTeams());
                }
            });
            for (Team team : selectedTeams) {
                String url = team.getWorkspaceURL();
                String folder = selected.getWorkspaceFolder() + "/"
                        + team.getName();
                String login = selected.getLogin();
                String password = selected.getPassword();

                // iterate over local changes first to avoid exceptions
                // later (in rare cases of "obstructed" state only):
                File teamDir = new File(folder);
                // TODO: also consider the case where the team folder exists
                // but is not a SVN checkout
                if (!teamDir.exists()) {
                    continue;
                }
                SVNSynchronizer sc = new SVNSynchronizer(url, folder, login,
                        password, new InteractiveConflictHandler(client
                                .getShell()));
                List<Status> tmpLocalChanges = sc.getLocalChanges();
                for (Status status : tmpLocalChanges) {
                    if (status.getTextStatus() == StatusKind.obstructed) {
                        localChanges.put(new File(status.getPath()),
                                StatusKind.obstructed);
                    }
                }
                // we need to stop here in case of obstruction,
                // as getRemoteAndLocalChanges() would throw a
                // ClientException:
                if (localChanges.size() == 0) {
                    List<Status> allChanges = sc.getRemoteAndLocalChanges();
                    for (Status status : allChanges) {
                        localChanges.put(new File(status.getPath()),
                                new Integer(status.getTextStatus()));
                        remoteChanges.put(new File(status.getPath()),
                                new Integer(status.getRepositoryTextStatus()));
                        // TODO: get the two files (remote/local) from the
                        // conflict,
                        // e.g. conflict.txt.r172 etc -> hide them!
                    }
                } else {
                    log
                            .info("obstructed status, not calling getRemoteAndLocalChanges()");
                }
                // FIXME:
                System.err.println("local " + localChanges);
                System.err.println("remote " + remoteChanges);
            }
            workspaceRoot = new File(selected.getWorkspaceFolder());
            updateContainer(false, null, false);
            refreshing = false;
        } catch (ClientException e) {
            // TODO: handle exception
            // may happen on very first checkout (before checkout, actually)
            // log.error(e.toString(), e);
            log.error(e.toString());
        }
    }

    private void updateContainer(final boolean refreshing,
            final String errMessage, boolean empty) {
        getDisplay().syncExec(
                new WorkspaceUpdateContainerRunnable(client, this, empty,
                        errMessage, refreshing));
    }
}
