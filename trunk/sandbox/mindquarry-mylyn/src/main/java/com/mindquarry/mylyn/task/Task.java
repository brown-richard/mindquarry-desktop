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
package com.mindquarry.mylyn.task;

import org.eclipse.mylyn.tasks.core.AbstractTask;

import com.mindquarry.mylyn.repository.RepositoryConnector;

/**
 * Add summary documentation here.
 *
 * @author <a hef="mailto:saar@mindquarry.com">Alexander Saar</a>
 */
public class Task extends AbstractTask {
	public Task(String repositoryUrl, String taskId, String summary) {
		super(repositoryUrl, taskId, summary);
	}

	/**
	 * @see org.eclipse.mylyn.tasks.core.AbstractTask#getConnectorKind()
	 */
	@Override
	public String getConnectorKind() {
		return RepositoryConnector.KIND;
	}
	
	/**
	 * @see org.eclipse.mylyn.tasks.core.AbstractTask#isLocal()
	 */
	@Override
	public boolean isLocal() {
		return false;
	}
}