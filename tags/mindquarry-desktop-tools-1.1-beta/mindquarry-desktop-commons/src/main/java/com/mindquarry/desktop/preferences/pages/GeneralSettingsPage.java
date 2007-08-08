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

import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.mindquarry.desktop.Messages;

/**
 * This class creates a preference page for shortcuts.
 * 
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class GeneralSettingsPage extends FieldEditorPreferencePage {
    public static final String NAME = "general"; //$NON-NLS-1$

    public static final String AUTOSTART = "com.mindquarry.desktop.autostart"; //$NON-NLS-1$

    public static final String NOTIFY_DELAY = "com.mindquarry.desktop.notify.delay"; //$NON-NLS-1$

    /**
     * ShortcutsPage default constructor
     */
    public GeneralSettingsPage() {
        super(Messages.getString(GeneralSettingsPage.class, "0"), //$NON-NLS-1$
                FieldEditorPreferencePage.GRID);
        setDescription(Messages.getString(GeneralSettingsPage.class, "1")); //$NON-NLS-1$
        setImageDescriptor(ImageDescriptor
                .createFromImage(new Image(
                        null,
                        getClass()
                                .getResourceAsStream(
                                        "/org/tango-project/tango-icon-theme/16x16/categories/preferences-system.png")))); //$NON-NLS-1$
    }

    protected void createFieldEditors() {
        IPreferenceStore store = getPreferenceStore();
        store.setDefault(AUTOSTART, false);
        store.setDefault(NOTIFY_DELAY, 2);

        BooleanFieldEditor autostartFlag = new BooleanFieldEditor(AUTOSTART,
                Messages.getString(GeneralSettingsPage.class, "3"), //$NON-NLS-1$
                getFieldEditorParent());
        addField(autostartFlag);

        IntegerFieldEditor notifyDisplayDelay = new IntegerFieldEditor(
                NOTIFY_DELAY, Messages
                        .getString(GeneralSettingsPage.class, "2") //$NON-NLS-1$
                        + ":", //$NON-NLS-1$
                getFieldEditorParent());
        addField(notifyDisplayDelay);
    }
}