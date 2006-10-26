/**
 * Copyright (C) 2005 MindQuarry GmbH, All Rights Reserved
 */
package com.mindquarry.client.workspace;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.springframework.beans.factory.BeanFactory;
import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.SVNClientInterface;
import org.xml.sax.InputSource;

import com.mindquarry.client.MindClient;
import com.mindquarry.client.util.RegUtil;
import com.mindquarry.client.xml.TeamlistContentHandler;

/**
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class SynchronizeOperation implements IRunnableWithProgress {
    private final MindClient client;

    private final SVNClientInterface svnClient;

    public SynchronizeOperation(MindClient client) {
        this.client = client;

        // get SVN client interface component
        BeanFactory factory = client.getFactory();
        svnClient = (SVNClientInterface) factory
                .getBean(SVNClientInterface.class.getName());
    }

    /**
     * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        monitor.beginTask("Synchronizing workspaces ...",
                IProgressMonitor.UNKNOWN);

        HashMap<String, String> workspaces = new HashMap<String, String>();
        getTeamspaceList(workspaces, monitor);

        if (monitor.isCanceled()) {
            return;
        }
        updateWorkspaces(workspaces, monitor);
        monitor.done();
    }

    private void getTeamspaceList(HashMap<String, String> workspaces,
            IProgressMonitor monitor) {
        HttpClient httpClient = new HttpClient();
        httpClient.getState().setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT,
                        AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(client.getOptions()
                        .getProperty(MindClient.LOGIN_KEY), client.getOptions()
                        .getProperty(MindClient.PASSWORD_KEY)));

        GetMethod get = new GetMethod(client.getOptions().getProperty(
                MindClient.ENDPOINT_KEY)
                + "/teamspace/teamlist.xml"); //$NON-NLS-1$
        get.setDoAuthentication(true);

        try {
            monitor.setTaskName("Retrieving teamspace list ...");

            httpClient.executeMethod(get);
            String teamlistXML = get.getResponseBodyAsString();
            InputStream is = new ByteArrayInputStream(teamlistXML.getBytes());
            get.releaseConnection();

            if (monitor.isCanceled()) {
                return;
            }

            // parse teamspace list
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(is), new TeamlistContentHandler(
                    workspaces));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateWorkspaces(HashMap<String, String> workspaces,
            IProgressMonitor monitor) {
        monitor.setTaskName("Synchronizing workspaces ...");

        // init SVN types
        svnClient.username(client.getOptions()
                .getProperty(MindClient.LOGIN_KEY));
        svnClient.password(client.getOptions().getProperty(
                MindClient.PASSWORD_KEY));

        // get directory for workspaces
        File workspacesDir = new File(RegUtil.getMyDocumentsFolder());
        if (!workspacesDir.exists()) {
            workspacesDir.mkdir();
        }
        // loop existing workspace directories
        for (String wsID : workspacesDir.list()) {
            if (monitor.isCanceled()) {
                return;
            }
            // loop received workspace items
            if (workspaces.containsKey(wsID)) {
                // remove entry from workspace list
                workspaces.remove(wsID);

                // TODO check if folder is under version control

                // update workspace
                monitor.setTaskName("Synchronizing workspace " + wsID + " ..."); //$NON-NLS-2$
                updateWorkspace(new File(workspacesDir.getAbsolutePath()
                        + "/" + wsID)); //$NON-NLS-1$
            }
        }
        // add additional workspace directories
        for (String wsID : workspaces.keySet()) {
            if (monitor.isCanceled()) {
                return;
            }
            // create directory for the new workspace
            File newWorkspaceDir = new File(workspacesDir.getAbsolutePath()
                    + "/" + wsID); //$NON-NLS-1$
            newWorkspaceDir.mkdir();

            monitor.setTaskName("Synchronizing workspace " + wsID + " ..."); //$NON-NLS-2$
            checkoutWorkspace(workspaces.get(wsID), newWorkspaceDir);
        }
    }

    private void checkoutWorkspace(String url, File dir) {
        try {
            svnClient.checkout(url, dir.getAbsolutePath(), Revision.HEAD, true);
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }

    private void updateWorkspace(File dir) {
        try {
            svnClient.update(dir.getAbsolutePath(), Revision.HEAD, true);
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }
}
