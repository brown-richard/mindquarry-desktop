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
package com.mindquarry.desktop.workspace;

import org.tigris.subversion.javahl.NotifyInformation;
import org.tigris.subversion.javahl.Status;

/**
 * Add summary documentation here.
 *
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class MacSVNHelper extends SVNHelper {
    
    public MacSVNHelper(String repositoryURL, String localPath, String username, String password) {
        super(repositoryURL, localPath, username, password);
    }
    
    public void onNotify(NotifyInformation info) {
        System.out.println("mac notify: " + info.getPath());
    }
    
    protected int resolveConflict(Status status) {
        return CONFLICT_OVERRIDE_FROM_WC;
    }
    
    protected String getCommitMessage() {
        return "generic commit message";
    }
    
}
