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

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * @author <a href="mailto:lars(dot)trieloff(at)mindquarry(dot)com">Lars
 *         Trieloff</a>
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class TaskTableTooltipProvider extends CellLabelProvider implements
        ITableLabelProvider {
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
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object,
     *      int)
     */
    public Image getColumnImage(Object element, int columnIndex) {
        Task task = (Task) element;
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
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object,
     *      int)
     */
    public String getColumnText(Object element, int columnIndex) {
        Task task = (Task) element;

        String text = null;
        if (columnIndex == 0) {
            text = task.getTitle();
        }
        return text;
    }

    /**
     * @see org.eclipse.jface.viewers.CellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
     */
    @Override
    public void update(ViewerCell cell) {
        // nothing to do here
    }

    /**
     * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.Object)
     */
    @Override
    public String getToolTipText(Object element) {
        Task task = (Task) element;

        String text = ""; //$NON-NLS-1$
        text += "Title" + ": " + task.getTitle(); //$NON-NLS-1$ //$NON-NLS-2$ 
        text += "\n" + "Status" + ": " + task.getStatus(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        text += "\n" + "Summary" + ": " + task.getSummary(); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
        return text;
    }
    
    /**
     * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipTimeDisplayed(java.lang.Object)
     */
    @Override
    public int getToolTipTimeDisplayed(Object object) {
        return 1000;
    }
}
