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
package com.mindquarry.desktop.preferences.pages;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * This class creates a preference page for Mindquarry task management.
 * 
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class TaskPage extends FieldEditorPreferencePage {
    public static final String NAME = "task"; //$NON-NLS-1$
    
    public static final String LIST_FINISHED_TASKS = "com.mindquarry.desktop.task.finished"; //$NON-NLS-1$

    /**
     * ProfilesPage default constructor
     */
    public TaskPage() {
        super("Task Settings", SWT.FLAT);

        // inital preference page
        setDescription("Manage your task settings.");
        Image img = new Image(Display.getCurrent(), getClass()
                .getResourceAsStream(
                "/com/mindquarry/icons/16x16/apps/mindquarry-tasks.png")); //$NON-NLS-1$
        ImageDescriptor imgDesc = ImageDescriptor.createFromImage(img);
        setImageDescriptor(imgDesc);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    @Override
    protected void createFieldEditors() {
        IPreferenceStore store = getPreferenceStore();
        store.setDefault(LIST_FINISHED_TASKS, false);
        
        BooleanFieldEditor showFinishedTasks = new BooleanFieldEditor(
                LIST_FINISHED_TASKS, "&List finished tasks",
                getFieldEditorParent());
        addField(showFinishedTasks);
    }
}
