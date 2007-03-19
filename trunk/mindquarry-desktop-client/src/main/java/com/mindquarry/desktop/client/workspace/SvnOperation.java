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
package com.mindquarry.desktop.client.workspace;

import java.util.List;

import org.tigris.subversion.javahl.Status;

import com.mindquarry.desktop.client.MindClient;
import com.mindquarry.desktop.client.workspace.widgets.SynchronizeWidget;

/**
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public abstract class SvnOperation implements Runnable {
    protected final MindClient client;

    protected final List<SynchronizeWidget> synAreas;

    public SvnOperation(final MindClient client,
            List<SynchronizeWidget> synAreas) {
        this.client = client;
        this.synAreas = synAreas;
    }

    protected String getStatiDescription(Status[] stati, String pathPrefix) {
        StringBuffer msg = new StringBuffer();
        for (Status status : stati) {
            // show the relative path
            String path = status.getPath().substring(pathPrefix.length() + 1);

            if (!status.isManaged() || status.isAdded()) {
                msg.append(Messages.getString("PublishOperation.5") //$NON-NLS-1$
                        + path + "\n"); //$NON-NLS-1$
            } else if (status.isModified()) {
                msg.append(Messages.getString("PublishOperation.7") //$NON-NLS-1$
                        + path + "\n"); //$NON-NLS-1$ 
            } else {
                msg.append(status.getTextStatusDescription()
                        + ": " + path + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return msg.toString();
    }

    public void setProgressSteps(final int value) {
        MindClient.getShell().getDisplay().syncExec(new Runnable() {
            public void run() {
                for (SynchronizeWidget synArea : synAreas) {
                    synArea.setProgressSteps(value);
                }
            }
        });
    }

    public void updateProgress() {
        MindClient.getShell().getDisplay().syncExec(new Runnable() {
            public void run() {
                for (SynchronizeWidget synArea : synAreas) {
                    synArea.updateProgress();
                }
            }
        });
    }

    public void resetProgress() {
        MindClient.getShell().getDisplay().syncExec(new Runnable() {
            public void run() {
                for (SynchronizeWidget synArea : synAreas) {
                    synArea.resetProgress();
                    synArea.setMessage(""); //$NON-NLS-1$
                }
            }
        });
    }

    public void setMessage(final String message) {
        MindClient.getShell().getDisplay().syncExec(new Runnable() {
            public void run() {
                for (SynchronizeWidget synArea : synAreas) {
                    synArea.setMessage(message);
                }
            }
        });
    }
}