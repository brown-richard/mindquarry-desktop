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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.NodeKind;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.javahl.StatusKind;

import com.mindquarry.desktop.client.MindClient;
import com.mindquarry.desktop.client.action.workspace.InteractiveConflictHandler;
import com.mindquarry.desktop.client.widget.util.container.ContainerWidget;
import com.mindquarry.desktop.model.team.Team;
import com.mindquarry.desktop.preferences.profile.Profile;
import com.mindquarry.desktop.workspace.SVNSynchronizer;

/**
 * Widget displaying incoming and outgoing file changes.
 * 
 * @author <a href="saar(at)mindquarry(dot)com">Alexander Saar</a>
 */
public class WorkspaceBrowserWidget extends ContainerWidget<TreeViewer> {
    private static Log log = LogFactory.getLog(WorkspaceBrowserWidget.class);

    protected File workspaceRoot;

    protected Map<File, Integer> localChanges = new HashMap<File, Integer>();
    protected Map<File, Integer> remoteChanges = new HashMap<File, Integer>();
    // ignore files like "<filename>.r200" that are created in case of conflicts:
    protected Map<File, Integer> toIgnore = new HashMap<File, Integer>();

    public WorkspaceBrowserWidget(Composite parent, MindClient client) {
        super(parent, SWT.NONE, client);
    }

    // #########################################################################
    // ### PUBLIC METHODS
    // #########################################################################

    /**
     * not implemented
     */
    public void asyncRefresh() {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Checks if a refresh of the changes list itself is needed.
     */
    public boolean refreshNeeded() {
        PreferenceStore store = client.getPreferenceStore();
        Profile selectedProfile = Profile.getSelectedProfile(store);
        if (selectedProfile == null) {
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

    /**
     * Refreshes the list of incoming and outgoing changes.
     */
    public void refresh() {
        PreferenceStore store = client.getPreferenceStore();
        Profile selectedProfile = Profile.getSelectedProfile(store);
        if (selectedProfile == null) {
            return;
        }
        log.info("Starting workspace changes refresh."); //$NON-NLS-1$
        if (refreshing) {
            log.info("Already refreshing, nothing to do."); //$NON-NLS-1$
            return;
        }
        refreshing = true;

        // update tree
        Map<File, Integer> newLocalChanges = new HashMap<File, Integer>();
        Map<File, Integer> newRemoteChanges = new HashMap<File, Integer>();
        getAllChanges(selectedProfile, newLocalChanges, newRemoteChanges);
        localChanges = newLocalChanges;
        remoteChanges = newRemoteChanges;
        workspaceRoot = new File(selectedProfile.getWorkspaceFolder());

        refreshing = false;
    }

    public boolean isRefreshListEmpty() {
        // FIXME: as ContentProvider does some filtering, this does
        // not always reflect the status in the GUI
        return localChanges.size() == 0 && remoteChanges.size() == 0;
    }
    
    /**
     * Return if at least one team workspace of the current profile has
     * been checked out. Return false on e.g. first start-up.
     */
    public boolean hasCheckout() {
        // TODO: check if there are actually team dirs and
        //  .svn/.svnref dirs/files
        PreferenceStore store = client.getPreferenceStore();
        Profile selected = Profile.getSelectedProfile(store);
        if (selected == null) {
            return false;
        }
        File wsFolder = new File(selected.getWorkspaceFolder());
        if (!wsFolder.exists() || wsFolder.list() == null || 
                wsFolder.list().length == 0) {
            log.debug("folder does not contain a checkout : " + 
                    wsFolder.getAbsolutePath());
            return false;
        }
        return true;
    }

    public void updateContainer(final boolean refreshing,
            final String refreshMessage, final String errMessage, boolean empty) {
        getDisplay().syncExec(
                new WorkspaceUpdateContainerRunnable(client, this, empty,
                        errMessage, refreshing, refreshMessage));
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
                sc.cleanup();
                boolean teamHasObstruction = false;
                List<Status> tmpLocalChanges = sc.getLocalChanges();
                for (Status status : tmpLocalChanges) {
                    if (status.getTextStatus() == StatusKind.obstructed) {
                        localChanges.put(new File(status.getPath()),
                                StatusKind.obstructed);
                        teamHasObstruction = true;
                    }
                }
                toIgnore = new HashMap<File, Integer>();
                // we need to stop here in case of obstruction,
                // as getRemoteAndLocalChanges() would throw a
                // ClientException:
                if (!teamHasObstruction) {
                    List<Status> allChanges = sc.getRemoteAndLocalChanges();
                    for (Status status : allChanges) {
                        File f = new File(status.getPath());
                        localChanges.put(new File(status.getPath()),
                                new Integer(status.getTextStatus()));
                        remoteChanges.put(new File(status.getPath()),
                                new Integer(status.getRepositoryTextStatus()));
                        // If we add a directory with contents, SVN will only
                        // report the directory as unversioned, not the files
                        // inside the directory, so we add the files (= all
                        // files
                        // below the directory) here:
                        if (status.getTextStatus() == StatusKind.unversioned
                                && f.isDirectory()) {
                            SetStatusFileFilter fileFilter = new SetStatusFileFilter();
                            SetStatusDirFilter dirFilter = new SetStatusDirFilter();
                            FileUtils.iterateFiles(f, fileFilter, dirFilter);
                            for (Status tmpStatus : fileFilter.getSubFiles()) {
                                localChanges.put(new File(tmpStatus.getPath()),
                                        StatusKind.unversioned);
                            }
                        }
                        // if there's a local conflict, ignore (i.e. don't display) files
                        // like <file>.r<rev> or <file>.mine, because these will not be
                        // uploaded to the server:
                        if (status.getTextStatus() == StatusKind.conflicted) {
                            // status.getConflictNew() etc return relative path names,
                            // but we need absolute path names in the map:
                            File dir = new File(status.getPath()).getParentFile();
                            toIgnore.put(new File(dir, status.getConflictNew()), status.getTextStatus());
                            toIgnore.put(new File(dir, status.getConflictOld()), status.getTextStatus());
                            toIgnore.put(new File(dir, status.getConflictWorking()), status.getTextStatus());
                        }
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
            refreshing = false;
        } catch (ClientException e) {
            // TODO: handle exception
            // may happen on very first checkout (before checkout, actually)
            // log.error(e.toString(), e);
            log.error(e.toString());
        }
    }

    // #########################################################################
    // ### INNER CLASSES
    // #########################################################################

    /**
     * Use the filter not as a filter, but to iterate over all files recursively.
     */
    class SetStatusFileFilter extends FileFileFilter {
        List<Status> subFiles = new ArrayList<Status>();

        public boolean accept(File file) {
            // make up a Status mostly with fake values, as only
            // path and StatusKind will be used:
            Status status = new Status(file.getAbsolutePath(), null,
                    NodeKind.file, 0, 0, 0, null, StatusKind.unversioned, 0, 0,
                    0, false, false, null, null, null, null, 0, false, null,
                    null, null, 0, null, 0, 0, 0, null);
            subFiles.add(status);
            return true;
        }

        List<Status> getSubFiles() {
            return subFiles;
        }
    }

    class SetStatusDirFilter extends DirectoryFileFilter {
        List<Status> subFiles = new ArrayList<Status>();

        public boolean accept(File file) {
            Status status = new Status(file.getAbsolutePath(), null,
                    NodeKind.dir, 0, 0, 0, null, StatusKind.unversioned, 0, 0,
                    0, false, false, null, null, null, null, 0, false, null,
                    null, null, 0, null, 0, 0, 0, null);
            subFiles.add(status);
            return true;
        }

        List<Status> getSubFiles() {
            return subFiles;
        }
    }
}
