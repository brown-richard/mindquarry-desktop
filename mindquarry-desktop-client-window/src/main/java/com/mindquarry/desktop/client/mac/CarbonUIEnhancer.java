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
package com.mindquarry.desktop.client.mac;

/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *  * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.internal.carbon.HICommand;
import org.eclipse.swt.internal.carbon.OS;
import org.eclipse.swt.widgets.Display;

import com.mindquarry.desktop.client.Messages;
import com.mindquarry.desktop.client.MindClient;


public class CarbonUIEnhancer {
   private static final int kHICommandPreferences= ('p'<<24) + ('r'<<16) + ('e'<<8) + 'f';
   private static final int kHICommandAbout= ('a'<<24) + ('b'<<16) + ('o'<<8) + 'u';
   private static final int kHICommandServices= ('s'<<24) + ('e'<<16) + ('r'<<8) + 'v';

   private static String fgAboutActionName;
   
   private MindClient client;

   public CarbonUIEnhancer(MindClient client) {
      this.client = client;
      if (fgAboutActionName == null) {
          fgAboutActionName = Messages.getString("About Mindquarry Desktop Client...");
      }
      earlyStartup();
   }

   /* (non-Javadoc)
    * @see org.eclipse.ui.IStartup#earlyStartup()
    */
   public void earlyStartup() {
      final Display display= Display.getDefault();
      display.syncExec(
         new Runnable() {
            public void run() {
               hookApplicationMenu(display);
            }
         }
      );
   }
   
   /**
    * See Apple Technical Q&A 1079 (http://developer.apple.com/qa/qa2001/qa1079.html)
    */
   public void hookApplicationMenu(Display display) {
            // Callback target
      Object target= new Object() {
         int commandProc(int nextHandler, int theEvent, int userData) {
            if (OS.GetEventKind(theEvent) == OS.kEventProcessCommand) {
               HICommand command= new HICommand();
               OS.GetEventParameter(theEvent, OS.kEventParamDirectObject, OS.typeHICommand, null, HICommand.sizeof, null, command);
               switch (command.commandID) {
               case kHICommandPreferences:
                  client.showPreferenceDialog(true);
                  return OS.noErr;
               case kHICommandAbout:
                  // FIXME: implement about dialog
                  //return runAction("about"); //$NON-NLS-1$
               default:
                  break;
               }
            }
            return OS.eventNotHandledErr;
         }
      };
      
      final Callback commandCallback= new Callback(target, "commandProc", 3); //$NON-NLS-1$
      int commandProc= commandCallback.getAddress();
      if (commandProc == 0) {
         commandCallback.dispose();
         return;  // give up
      }

      // Install event handler for commands
      int[] mask= new int[] {
         OS.kEventClassCommand, OS.kEventProcessCommand
      };
      OS.InstallEventHandler(OS.GetApplicationEventTarget(), commandProc, mask.length / 2, mask, 0, null);

      // create About Eclipse menu command
      int[] outMenu= new int[1];
      short[] outIndex= new short[1];
      if (OS.GetIndMenuItemWithCommandID(0, kHICommandPreferences, 1, outMenu, outIndex) == OS.noErr && outMenu[0] != 0) {
         int menu= outMenu[0];

         int l= fgAboutActionName.length();
         char buffer[]= new char[l];
         fgAboutActionName.getChars(0, l, buffer, 0);
         int str= OS.CFStringCreateWithCharacters(OS.kCFAllocatorDefault, buffer, l);
         // FIXME: implement about dialog
         //OS.InsertMenuItemTextWithCFString(menu, str, (short) 0, 0, kHICommandAbout);
         OS.CFRelease(str);
                  // add separator between About & Preferences
         OS.InsertMenuItemTextWithCFString(menu, 0, (short) 1, OS.kMenuItemAttrSeparator, 0);

         // enable pref menu
         OS.EnableMenuCommand(menu, kHICommandPreferences);
               // disable services menu
         OS.DisableMenuCommand(menu, kHICommandServices);
      }

      // schedule disposal of callback object
      display.disposeExec(
         new Runnable() {
            public void run() {
               commandCallback.dispose();
            }
         }
      );
   }
}