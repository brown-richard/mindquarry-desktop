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
package com.mindquarry.desktop.client.widget.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;

import com.mindquarry.desktop.client.Messages;
import com.mindquarry.desktop.client.MindClient;
import com.mindquarry.desktop.client.action.ActionBase;
import com.mindquarry.desktop.client.widget.util.container.ContainerWidget;
import com.mindquarry.desktop.model.task.Task;
import com.mindquarry.desktop.model.task.TaskList;
import com.mindquarry.desktop.model.team.Team;
import com.mindquarry.desktop.preferences.profile.Profile;
import com.mindquarry.desktop.util.NotAuthorizedException;

/**
 * @author <a href="mailto:saar(at)mindquarry(dot)com">Alexander Saar</a>
 */
public class TaskContainerWidget extends ContainerWidget<TableViewer> {
    private static Log log = LogFactory.getLog(TaskContainerWidget.class);

    private TaskList tasks;

    private String statusFacet = "all";
    private String priorityFacet = "all";
    private String searchFacet = "";

    public TaskContainerWidget(Composite parent, MindClient client) {
        super(parent, SWT.BORDER, client);
    }

    // #########################################################################
    // ### WIDGET METHODS
    // #########################################################################
    protected void createContents(Composite parent) {
        super.createContents(parent);
        setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 7,
                1));
    }

    // #########################################################################
    // ### PUBLIC METHODS
    // #########################################################################
    public void addTask(Task task) {
        if (viewer != null) {
            TaskList content = (TaskList) viewer.getInput();
            content.getTasks().add(task);
            viewer.setInput(content);
            viewer.refresh();
        }
    }

    /**
     * Runs task update in a separate thread, so that GUI can continue
     * processing. Thus this method returns immediately. While updating tasks
     * the Task Manager will show an update widget instead of the task table.
     */
    public void asyncRefresh() {
        log.info("Starting async task list refresh."); //$NON-NLS-1$
        if (refreshing) {
            log.info("Already refreshing, nothing to do."); //$NON-NLS-1$
            return;
        }
        refreshing = true;
        Thread updateThread = new Thread(new Runnable() {
            public void run() {
                client.startAction(Messages.getString("Synchronizing tasks"));
                refresh();
                client.stopAction(Messages.getString("Synchronizing tasks"));
            }
        }, "task-update");
        updateThread.setDaemon(true);
        updateThread.start();
    }

    public void applyFacets(String status, String priority, String search) {
        statusFacet = status;
        priorityFacet = priority;
        searchFacet = search;
        applyFacets();
    }

    private void applyFacets() {
        if ((viewer != null) && (tasks != null)) {
            TaskList content = new TaskList();
            content.getTasks().addAll(tasks.getTasks());

            if ((statusFacet.equals("all")) && (priorityFacet.equals("all"))
                    && searchFacet.equals("")) {
                viewer.setInput(tasks);
                viewer.refresh();
            } else {
                Iterator<Task> it = content.getTasks().iterator();
                while (it.hasNext()) {
                    Task task = it.next();
                    boolean hide = false;

                    // check status facet
                    if ((!statusFacet.equals("all"))
                            && (!task.getStatus().equals(statusFacet))) {
                        hide = true;
                    }
                    // check priority facet
                    if ((task.getPriority() == null)
                            && (!priorityFacet.equals("all"))) {
                        hide = true;
                    } else if ((!priorityFacet.equals("all"))
                            && (!task.getPriority().equals(priorityFacet))) {
                        hide = true;
                    }
                    if (hide) {
                        it.remove();
                    }
                }
            }
            // check live search constraint
            Iterator<Task> it = content.getTasks().iterator();
            while (it.hasNext()) {
                Task task = it.next();
                if ((!searchFacet.equals(""))
                        && (!task.getTitle().toLowerCase()
                                .contains(searchFacet))) {
                    it.remove();
                }
            }
            // update table
            viewer.setInput(content);
            viewer.refresh();
            markColumns();
        }
    }

    // #########################################################################
    // ### PRIVATE METHODS
    // #########################################################################
    protected void refresh() {
        log.info("Starting task list refresh."); //$NON-NLS-1$
        client.enableActions(false, ActionBase.TASK_ACTION_GROUP);
        refreshing = true;

        PreferenceStore store = client.getPreferenceStore();
        Profile profile = Profile.getSelectedProfile(store);

        // check profile
        if (profile == null) {
            log.debug("No profile selected."); //$NON-NLS-1$
            client.enableActions(true, ActionBase.TASK_ACTION_GROUP);
            refreshing = false;
            return;
        }
        updateContainer(true, null, false);
        log.info("Retrieving list of tasks."); //$NON-NLS-1$

        // cleanup current task list
        if (tasks == null) {
            tasks = new TaskList();
        }
        tasks.getTasks().clear();

        // retrieve tasks for all selected teams
        final List<Team> items = new ArrayList<Team>();
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                items.addAll(client.getSelectedTeams());
            }
        });
        try {
            for (Object item : items) {
                String url = profile.getServerURL();
                String login = profile.getLogin();
                String password = profile.getPassword();

                Team team = (Team) item;
                tasks.getTasks().addAll(new TaskList(url + "/tasks/" //$NON-NLS-1$
                        + team.getId() + "/", login, password).getTasks());

            }
        } catch (final NotAuthorizedException e) {
            log.error("Could not update list of tasks for "
                    + profile.getServerURL(), e); //$NON-NLS-1$
            
            getDisplay().syncExec(new Runnable() {
                public void run() {
                    Boolean retry = client.handleNotAuthorizedException(e);
                    refreshing = retry;
                }
            });

            if (refreshing) {
                refresh();
            } else {
                updateContainer(false, e.getLocalizedMessage(), false);
                client.enableActions(true, ActionBase.TASK_ACTION_GROUP);
            }
            
            return;
            
//            getDisplay().syncExec(new Runnable() {
//                public void run() {
//                    MessageDialog.openError(getShell(), Messages
//                            .getString("Error"), //$NON-NLS-1$
//                            e.getLocalizedMessage());
//                }
//            });
//            updateContainer(false, e.getLocalizedMessage(), false);
//            refreshing = false;
//            return;
        } catch (Exception e) {
            log.error("Could not update list of tasks for "
                    + profile.getServerURL(), e); //$NON-NLS-1$

            String errMessage = Messages
                    .getString("List of tasks could not be updated"); //$NON-NLS-1$
            errMessage += " " + e.getLocalizedMessage(); //$NON-NLS-1$

            MessageDialog.openError(getShell(),
                    Messages.getString("Error"), errMessage);

            updateContainer(false, errMessage, false);
            client.enableActions(true, ActionBase.TASK_ACTION_GROUP);
            refreshing = false;
            return;
        }

        if (tasks.getTasks().isEmpty()) {
            updateContainer(false, null, true);
        } else {
            updateContainer(false, null, false);

            // update task table
            log.info("Updating list of tasks."); //$NON-NLS-1$
            getDisplay().syncExec(new Runnable() {
                public void run() {
                    // viewer.setInput(tasks);
                    applyFacets();
                }
            });
        }
        refreshing = false;
        client.enableActions(true, ActionBase.TASK_ACTION_GROUP);
    }

    private void markColumns() {
        // set background color for every second table item
        TableItem[] items = viewer.getTable().getItems();
        for (int i = 0; i < items.length; i++) {
            if (i % 2 == 1) {
                items[i].setBackground(new Color(Display.getCurrent(), 233,
                        233, 251));
            }
        }
    }

    public void updateContainer(final boolean refreshing,
            final String errMessage, final boolean empty) {
        getDisplay().syncExec(
                new TaskUpdateContainerRunnable(client, this, empty,
                        errMessage, refreshing));
    }
}
