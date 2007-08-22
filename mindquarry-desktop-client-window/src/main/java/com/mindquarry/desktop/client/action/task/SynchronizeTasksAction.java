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
package com.mindquarry.desktop.client.action.task;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.mindquarry.desktop.client.Messages;
import com.mindquarry.desktop.client.MindClient;
import com.mindquarry.desktop.client.action.ActionBase;
import com.mindquarry.desktop.client.widget.task.TaskContainerWidget;

/**
 * Add summary documentation here.
 * 
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class SynchronizeTasksAction extends ActionBase {
    public static final String ID = SynchronizeTasksAction.class.getSimpleName();

	private TaskContainerWidget taskContainer;

	private static final Image IMAGE = new Image(
			Display.getCurrent(),
			SynchronizeTasksAction.class
					.getResourceAsStream("/org/tango-project/tango-icon-theme/" + ICON_SIZE + "/actions/view-refresh.png")); //$NON-NLS-1$

	public SynchronizeTasksAction(MindClient client) {
		super(client);

		setId(ID);
		setActionDefinitionId(ID);

		setText(Messages.getString("Refresh task list"));
		setToolTipText(Messages.getString("Refresh the list of tasks."));
		setAccelerator(SWT.CTRL + +SWT.SHIFT + 'S');
		setImageDescriptor(ImageDescriptor.createFromImage(IMAGE));
	}

	public void run() {
		taskContainer.asyncRefresh();
	}

	public void setTaskContainer(TaskContainerWidget taskContainer) {
		this.taskContainer = taskContainer;
	}

    public String getGroup() {
        return ActionBase.TASK_ACTION_GROUP;
    }
    
    public boolean isToolbarAction() {
        return true;
    }
}