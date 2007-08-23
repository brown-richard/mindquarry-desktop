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
package com.mindquarry.desktop.client.widget.util.container;

import org.eclipse.jface.viewers.Viewer;

import com.mindquarry.desktop.client.Messages;

/**
 * Add summary documentation here.
 * 
 * @author <a href="mailto:saar@mindquarry.com">Alexander Saar</a>
 */
public abstract class UpdateContainerRunnable<V extends Viewer> implements
        Runnable {
    protected ContainerWidget<V> containerWidget;

    private final boolean empty;
    private final String errMessage;
    private final boolean refreshing;

    public UpdateContainerRunnable(ContainerWidget<V> containerWidget,
            boolean empty, String errMessage, boolean refreshing) {
        this.containerWidget = containerWidget;

        this.empty = empty;
        this.errMessage = errMessage;
        this.refreshing = refreshing;
    }

    public void run() {
        if (refreshing) {
            destroyContent();
            containerWidget.refreshWidget = new UpdateWidget(containerWidget,
                    Messages.getString("Updating task list") //$NON-NLS-1$
                            + " ..."); //$NON-NLS-1$
        } else if (errMessage == null && !empty) {
            destroyContent();
            createContainerContent();
        } else if (errMessage == null && empty) {
            destroyContent();
            containerWidget.noContentWidget = new NoContentWidget(
                    containerWidget, Messages
                            .getString("Currently no tasks are active.")); //$NON-NLS-1$
        } else {
            destroyContent();
            containerWidget.errorWidget = new ErrorWidget(containerWidget,
                    errMessage);
        }
        containerWidget.layout(true);
    }

    protected abstract void createContainerContent();

    private void destroyContent() {
        destroyContainerContent();
        if (containerWidget.refreshWidget != null) {
            containerWidget.refreshWidget.dispose();
            containerWidget.refreshWidget = null;
        }
        if (containerWidget.errorWidget != null) {
            containerWidget.errorWidget.dispose();
            containerWidget.errorWidget = null;
        }
        if (containerWidget.noContentWidget != null) {
            containerWidget.noContentWidget.dispose();
            containerWidget.noContentWidget = null;
        }
    }

    protected abstract void destroyContainerContent();
}