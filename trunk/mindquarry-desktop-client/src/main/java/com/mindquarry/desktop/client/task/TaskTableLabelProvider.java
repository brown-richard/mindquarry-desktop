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
package com.mindquarry.desktop.client.task;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.mindquarry.desktop.client.Messages;
import com.mindquarry.desktop.model.task.Task;

/**
 * Label provider for the Task table.
 * 
 * @author <a href="mailto:lars(dot)trieloff(at)mindquarry(dot)com">Lars
 *         Trieloff</a>
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class TaskTableLabelProvider extends ColumnLabelProvider {
    private Image newTask = new Image(Display.getCurrent(), getClass()
            .getResourceAsStream(
                    "/com/mindquarry/icons/16x16/status/task-new.png")); //$NON-NLS-1$

    private Image runningTask = new Image(Display.getCurrent(), getClass()
            .getResourceAsStream(
                    "/com/mindquarry/icons/16x16/status/task-running.png")); //$NON-NLS-1$

    private Image pausedTask = new Image(Display.getCurrent(), getClass()
            .getResourceAsStream(
                    "/com/mindquarry/icons/16x16/status/task-paused.png")); //$NON-NLS-1$

    private Image doneTask = new Image(Display.getCurrent(), getClass()
            .getResourceAsStream(
                    "/com/mindquarry/icons/16x16/status/task-done.png")); //$NON-NLS-1$

    /**
     * @see org.eclipse.jface.viewers.ColumnLabelProvider#getImage(java.lang.Object)
     */
    public Image getImage(Object element) {
        Task task = (Task) element;
        if(task.getStatus() == null) {
            return null;
        }
        if (task.getStatus().equals(Task.STATUS_NEW)) {
            return newTask;
        } else if (task.getStatus().equals(Task.STATUS_RUNNING)) {
            return runningTask;
        } else if (task.getStatus().equals(Task.STATUS_PAUSED)) {
            return pausedTask;
        } else if (task.getStatus().equals(Task.STATUS_DONE)) {
            return doneTask;
        }
        return null;
    }

    /**
     * @see org.eclipse.jface.viewers.ColumnLabelProvider#getText(java.lang.Object)
     */
    public String getText(Object element) {
        Task task = (Task) element;
        String text = task.getTitle();
        return text;
    }

    /**
     * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.Object)
     */
    public String getToolTipText(Object element) {
        Task task = (Task) element;

        final int maxLength = 100;
        String text = ""; //$NON-NLS-1$
        String title = task.getTitle();
        if (title != null) {
            title = title.length() > maxLength ? title.substring(0, maxLength) + "..." : title; //$NON-NLS-1$
        } else {
            title = "-"; //$NON-NLS-1$
        }
        text += Messages.getString("com.mindquarry.desktop.client.53") + ": " + title;  //$NON-NLS-1$//$NON-NLS-2$ 
        text += "\n" + Messages.getString("com.mindquarry.desktop.client.54") + ": " + task.getStatus(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        String summary = task.getSummary();
        if (summary != null) {
            summary = summary.length() > maxLength ? summary.substring(0, maxLength) + "..." : summary; //$NON-NLS-1$
        } else {
            summary = "-"; //$NON-NLS-1$
        }
        text += "\n" + Messages.getString("com.mindquarry.desktop.client.55") + ": " + summary; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return text;
    }

    /**
     * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipTimeDisplayed(java.lang.Object)
     */
    public int getToolTipTimeDisplayed(Object object) {
        return 2000;
    }

    /**
     * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipDisplayDelayTime(java.lang.Object)
     */
    public int getToolTipDisplayDelayTime(Object object) {
        return 10;
    }
}
