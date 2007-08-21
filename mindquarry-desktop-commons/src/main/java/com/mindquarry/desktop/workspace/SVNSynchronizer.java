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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.NodeKind;
import org.tigris.subversion.javahl.Notify2;
import org.tigris.subversion.javahl.NotifyAction;
import org.tigris.subversion.javahl.NotifyInformation;
import org.tigris.subversion.javahl.PropertyData;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.javahl.StatusKind;
import org.tigris.subversion.javahl.Status.Kind;
import org.tmatesoft.svn.core.javahl.SVNClientImpl;

import com.mindquarry.desktop.util.RelativePath;
import com.mindquarry.desktop.workspace.conflict.AddConflict;
import com.mindquarry.desktop.workspace.conflict.Conflict;
import com.mindquarry.desktop.workspace.conflict.ConflictHandler;
import com.mindquarry.desktop.workspace.conflict.DeleteWithModificationConflict;
import com.mindquarry.desktop.workspace.exception.CancelException;

/**
 * Helper class that implements desktop synchronization using the SVN kit. It
 * provides callback hooks for handling conflicts that need user interaction.
 * 
 * Callback handler for conflict resolving must implement the
 * {@link ConflictHandler} interface.
 * 
 * @author <a href="mailto:saar@mindquarry.com">Alexander Saar</a>
 * @author <a href="mailto:victor.saar@mindquarry.com">Victor Saar</a>
 * @author <a href="mailto:alexander.klimetschek@mindquarry.com">Alexander Klimetschek</a>
 */
public class SVNSynchronizer {
	private static final Log log = LogFactory.getLog(SVNSynchronizer.class);

	protected String repositoryURL;

	protected String localPath;

    protected File localPathFile;
    
	protected String username;

	protected String password;

	protected SVNClientImpl client;
	
	protected ConflictHandler handler;

	/**
	 * Constructor for SVNSynchronizer that has all mandatory fields as
	 * parameter.
	 * 
	 * @param repositoryURL URL of the central SVN repository
	 * @param localPath local working copy path (typically the root of the wc)
	 * @param username subversion username
	 * @param password subversion password
	 * @param handler callback handler to resolve conflicts in the GUI
	 */
	public SVNSynchronizer(String repositoryURL, String localPath,
			String username, String password,
			ConflictHandler handler) {
		this.repositoryURL = repositoryURL;
		this.localPath = localPath;
		this.username = username;
		this.password = password;
		this.handler = handler;
		
		this.localPathFile = new File(localPath);
		
		if (handler == null) {
		    throw new NullPointerException("Constructor parameter ConflictHandler handler cannot be null");
		}

		// create SVN client, set authentication info
		client = SVNClientImpl.newInstance();
		if (username != null) {
			client.username(username);
			if (password != null) {
				client.password(password);
			}
		}
	}
	
	/**
	 * Sets an optional notify listener to get notifications directly from the
	 * svn client upon update and commit.
	 */
	public void setNotifyListener(Notify2 notifyListener) {
        // register for svn notifications on update and commit
        client.notification2(notifyListener);
	}

	/**
	 * Like synchronize(), but does a checkout if <tt>localPath</tt>
	 * isn't a checkout. Also, creates the path if it doesn't exist.
	 */
	public void synchronizeOrCheckout() {
        File localDir = new File(localPath);
        
        // is directory doesn't exist, create it:
        if (!localDir.exists()) {
            boolean createdDir = localDir.mkdirs();
            if (!createdDir) {
                throw new RuntimeException("Could not create directory: " +
                        localDir.getAbsolutePath());
            }
        }
        if (localDir.isFile()) {
            throw new IllegalArgumentException("File where directory " +
                    "was expected: " + localDir.getAbsolutePath());
        }

        boolean isCheckout = isCheckout(localPath);
        if (isCheckout) {
            synchronize();
        } else {
            // TODO: check if the directories are empty,
            // othwise we'd try to check out into a directory
            // that contains local files already whoch causes
            // confusion.
            try {
                client.checkout(repositoryURL, localPath, Revision.HEAD, true);
            } catch (ClientException e) {
                throw new RuntimeException("Checkout of " +repositoryURL +
                        " to " +localPath+ " failed", e);
            }
        }
	}

	private boolean isCheckout(String path) {
        try {
            // throws exception if no .svnref or .svn exists
            client.info(path);
        } catch (ClientException e) {
            // probably not a checkout directory:
            log.info("Got exception on " + localPath + ": " + e);
            return false;
        }	    
        return true;
	}
	
	/**
	 * Central method: will do a full synchronization, including update and
	 * commit. During that the ConflictHandler will be asked.
	 * Will fail if there's no checkout yet, see synchronizeOrCheckout().
	 */
	public void synchronize() {
		try {
		    // a cleanup is good for removing any old working copy locks
			client.cleanup(localPath);
			
			// TODO: enable repo locking (also unlock in finally below)
			// client.lock(new String[] {localPath}, "locking for
			// synchronization", false);

            deleteMissingAndAddUnversioned(localPath);
            
			List<Conflict> conflicts = analyzeChangesAndAskUser();
			
			handleConflictsBeforeUpdate(conflicts);
			client.update(localPath, Revision.HEAD, true);
			handleConflictsAfterUpdate(conflicts);

//			handleContentConflicts();
//			String message = askForCommitMessage();
			
//			// here something goes over the wire
//			client.commit(new String[] { localPath }, message, true);

		} catch (ClientException e) {
			// TODO think about exception handling
			e.printStackTrace();
			if (e.getCause() != null) {
			    e.getCause().printStackTrace();
			}
			throw new RuntimeException("synchronize() failed", e);
		} catch (CancelException e) {
			log.info("Canceled");
		} finally {
			// try {
			// client.unlock(new String[] {localPath}, false);
			// } catch (ClientException e) {
			// e.printStackTrace();
			// }
		}
	}
	
	/**
	 * Standard Operating System hidden / helper files that should get
	 * ignored by SVN.
	 */
	public static final List<String> filesToIgnore = new ArrayList<String>();
	static {
	    filesToIgnore.add("Thumbs.db"); // Windows thumbnails
	    filesToIgnore.add(".DS_Store"); // Mac finder folder info
	}

	/**
	 * This removes the need for calling svn del and svn add manually. It also
	 * automatically adds standard to-be-ignored files such as Thumbs.db to the
	 * ignore list.
     */
    private void deleteMissingAndAddUnversioned(String path) throws ClientException {
        for (Status s : getLocalChanges(path)) {
            log.debug("deleting/adding/ignoring " + Kind.getDescription(s.getTextStatus()) +
                    " " + nodeKindDesc(s.getNodeKind()) +
                    " <->" +
                    " " + Kind.getDescription(s.getRepositoryTextStatus()) +
                    " " + nodeKindDesc(s.getReposKind()) +
                    " '" + wcPath(s) + "'");
            
            if (s.getTextStatus() == StatusKind.missing) {
                // if the first parameter would be an URL, it would do a commit
                // (and use the second parameter as commit message) - but we
                // use a local filesystem path here and thus we only schedule
                // for a deletion
                client.remove(new String[] { s.getPath() }, null, true);
                
            } else if (s.getTextStatus() == StatusKind.unversioned) {
                // set standard to-be-ignored files
                File file = new File(s.getPath());
                if (filesToIgnore.contains(file.getName())) {
                    // update the svn:ignore property by appending a new line
                    // with the filename to be ignored (on the parent folder!)
                    PropertyData ignoreProp = client.propertyGet(file.getParent(), PropertyData.IGNORE);
                    ignoreProp.setValue(ignoreProp.getValue() + "\n" + file.getName(), false);
                } else {
                    // TODO: check for new files that have the same name when
                    // looking at it case-insensitive (on unix systems) to avoid
                    // problems when checking out on Windows (eg. 'report' is
                    // the same as 'REPORT' under windows, but not on unix).
                    // for this to work we simply check if there is a file with
                    // the same case-insensitive name in this folder, exclude it
                    // from the add and give a warning message to the user
                    
                    // otherwise we turn all unversioned into added
                    // Do not recurse, we do that ourselve below; we need to
                    // look at each file individually because we want to ignore
                    // some - setting the recurse flag here would add all files
                    // and folders inside the directory
                    client.add(s.getPath(), false);
                    
                    // For directories, we recurse into it: the reason is that
                    // we need to re-retrieve the stati for that directory after
                    // it has been added, because all the unversioned children
                    // are not part of the initial stati list (when the dir is
                    // unversioned). Note: we cannot use isNodeKind() == 'dir'
                    // because svn sees it as 'none' at this point
                    if (new File(s.getPath()).isDirectory()) {
                        deleteMissingAndAddUnversioned(s.getPath());
                    }
                }
            }
        }
    }

    /**
     * Retrieves local changes for the wc root as a list that is sorted with the
     * top-most folder or file first.
     */
    public List<Status> getLocalChanges() throws ClientException {
        return getLocalChanges(localPath);
    }
    
    /**
     * Retrieves local changes for a wc path as a list that is sorted with the
     * top-most folder or file first.
     */
    public List<Status> getLocalChanges(String path) throws ClientException {
		log.info("## local changes for '" + path + "':");

		// we need a modifiable list - Arrays.asList is fixed
		List<Status> statusList = new ArrayList<Status>(); 
		statusList.addAll(
		        Arrays.asList(client.status(path, true, false, false)));
		
		// sort the list from top-level folder to bottom which is important
		// for handling multiple conflicts on the parent folder first
		Collections.sort(statusList, new Comparator<Status>() {
			public int compare(Status left, Status right) {
				return left.getPath().compareTo(right.getPath());
			}
		});

		for (Status s : statusList) {
			log.info(s.getTextStatusDescription() + " " + s.getPath());
		}
		
		return statusList;
	}

	/**
	 * Returns a list with all local and remote changes combined. It's not
	 * easily possible to get only the remote changes, that's why we use this
	 * combined list throughout the code. The status inside this list will be
	 * different from the one returned by getLocalChanges() since it might
	 * contain the remote change of the same path. The list will be sorted
	 * from top to down.
	 */
	public List<Status> getRemoteAndLocalChanges() throws ClientException {
		log.info("## remote changes:");

        // we need a modifiable list - Arrays.asList is fixed
        List<Status> statusList = new ArrayList<Status>(); 
        statusList.addAll(
		    Arrays.asList(client.status(localPath, true, true, false)));

        // sort the list from top-level folder to bottom which is important
        // for handling multiple conflicts on the parent folder first
        Collections.sort(statusList, new Comparator<Status>() {
            public int compare(Status left, Status right) {
                return left.getPath().compareTo(right.getPath());
            }
        });
        
		for (Status s : statusList) {
			log.info(Kind.getDescription(s.getRepositoryTextStatus()) + " "
					+ s.getPath());
		}

		return statusList;
	}

	/**
	 * Helper method that creates a map that maps file paths to status objects
	 * for faster lookup.
	 */
    private Map<String, Status> createStatusMap(List<Status> stati) {
        Map<String, Status> map = new HashMap<String, Status>();
        for (Status s : stati) {
            map.put(s.getPath(), s);
        }
        return map;
    }
    
    private void presentNewConflict(Conflict conflict, List<Conflict> conflicts) throws CancelException {
        conflict.setSVNClient(client);
        
        log.info("-----------------------------------------------------------");
        log.info("## Found conflict: " + conflict.toString());
        
        // resolve it, ask the user
        conflict.accept(handler);
        
        // collect all conflicts
        conflicts.add(conflict);
    }
    
    /**
     * Important method that looks out for any structure conflicts before an
     * update and creates {@link Conflict} objects for those. Upon each conflict
     * found, the user is asked to resolve it.
     * 
     * If the user cancels during the conflict resolving, a CancelException is
     * thrown.
     */
	private List<Conflict> analyzeChangesAndAskUser() throws CancelException, ClientException {
        List<Status> remoteAndLocalChanges = getRemoteAndLocalChanges();

		List<Conflict> conflicts = new ArrayList<Conflict>();

        for (Status s : remoteAndLocalChanges) {
            log.debug("analyzing " + Kind.getDescription(s.getTextStatus()) +
                    " " + nodeKindDesc(s.getNodeKind()) +
                    " <->" +
                    " " + Kind.getDescription(s.getRepositoryTextStatus()) +
                    " " + nodeKindDesc(s.getReposKind()) +
                    " '" + wcPath(s) + "'");
        }
        
        // LOCAL status can be everything except:
        //   none/normal          won't be displayed in local changes
        //   unversioned/missing  set to added/deleted (handled anyway)
        //   merged               only happens on update
        //   ignored              can be ignored ;-)
        //   incomplete (on dir)  missing files are set to deleted
        
        // LOCAL status can be any one of those:
        // simple ones:
        //   modified
        //   added
        //   deleted
        //   replaced (only possible with svn client)
        // hard ones:
        //   conflicted
        //   obstructed (eg. deleted file, created dir with same name)
        //   external (only possible with svn client)
        
        // REMOTE status can be only the following:
        //   normal
        //   modified
        //   added
        //   deleted
        //   unversioned
        //   replaced (delete and re-add in one step)
		
		conflicts.addAll(findAddConflicts(remoteAndLocalChanges));
		
		conflicts.addAll(findLocalContainerDeleteConflicts(remoteAndLocalChanges));
		
        conflicts.addAll(findRemoteContainerDeleteConflicts(remoteAndLocalChanges));
        
        conflicts.addAll(findDeleteModifiedConflicts(remoteAndLocalChanges));
        
        conflicts.addAll(findModifiedDeleteConflicts(remoteAndLocalChanges));
        
		return conflicts;
	}

	/**
	 * Finds all Add/Add conflicts, including file/file, file/dir, dir/file and
	 * dir/dir conflicts.
	 */
    private List<Conflict> findAddConflicts(List<Status> remoteAndLocalChanges) throws CancelException {
        List<Conflict> conflicts = new ArrayList<Conflict>();
        
        Iterator<Status> iter = remoteAndLocalChanges.iterator();
        
        while (iter.hasNext()) {
            Status status = iter.next();
            
            // local ADD (as we added everything, unversioned shouldn't happen)
            if (status.getTextStatus() == StatusKind.added
                    || status.getTextStatus() == StatusKind.unversioned) {
                
                // if remote ADD
                if (status.getRepositoryTextStatus() == StatusKind.added) {
                    Status conflictParent = status;
                    // we remove all files/stati connected to this conflict
                    iter.remove();
                    
                    List<Status> localAdded = new ArrayList<Status>();
                    List<Status> remoteAdded = new ArrayList<Status>();
                    
                    // find all children (locally and remotely)
                    while (iter.hasNext()) {
                        status = iter.next();
                        if (isParent(conflictParent.getPath(), status.getPath())) {
                            if (status.getTextStatus() == StatusKind.added) {
                                localAdded.add(status);
                            } else if (status.getRepositoryTextStatus() == StatusKind.added) {
                                remoteAdded.add(status);
                            }
                            iter.remove();
                        } else {
                            // no more children found, this conflict is done
                            // reset global iterator for next conflict search
                            iter = remoteAndLocalChanges.iterator();
                            break;
                        }
                    }
                    
                    presentNewConflict(new AddConflict(conflictParent, localAdded, remoteAdded), conflicts);
                }
            }
        }
        
        return conflicts;
    }

    /**
     * Finds all conflicts where a local folder delete conflicts with remotely
     * added or modified files in that directory.
     */
    private List<Conflict> findLocalContainerDeleteConflicts(List<Status> remoteAndLocalChanges) throws CancelException {
        List<Conflict> conflicts = new ArrayList<Conflict>();
        
        Iterator<Status> iter = remoteAndLocalChanges.iterator();
        
        while (iter.hasNext()) {
            Status status = iter.next();
            
            // DELETED DIRECTORIES (locally)
            if (status.getNodeKind() == NodeKind.dir &&
                    status.getTextStatus() == StatusKind.deleted) {
                
                // conflict if there is a child that is added or removed remotely
                
                Status conflictParent = status;
                List<Status> remoteModList = new ArrayList<Status>();
                
                // find all children
                while (iter.hasNext()) {
                    status = iter.next();
                    if (isParent(conflictParent.getPath(), status.getPath())) {
                        if (status.getRepositoryTextStatus() == StatusKind.added ||
                                status.getRepositoryTextStatus() == StatusKind.replaced ||
                                status.getRepositoryTextStatus() == StatusKind.modified) {
                            remoteModList.add(status);
                        }
                        iter.remove();
                    } else {
                        // no more children found, this conflict is done
                        break;
                    }
                }
                
                if (remoteModList.size() > 0) {
                    // also remove the deleted folder status object
                    remoteAndLocalChanges.remove(conflictParent);
                    
                    presentNewConflict(new DeleteWithModificationConflict(true, conflictParent, remoteModList), conflicts);
                }
                
                // reset global iterator for next conflict search
                iter = remoteAndLocalChanges.iterator();
            }
        }
        
        return conflicts;
    }

    /**
     * Finds all conflicts where a remote folder delete conflicts with locally
     * added or modified files in that directory.
     */
    private List<Conflict> findRemoteContainerDeleteConflicts(List<Status> remoteAndLocalChanges) throws CancelException {
        List<Conflict> conflicts = new ArrayList<Conflict>();
        
        Iterator<Status> iter = remoteAndLocalChanges.iterator();
        
        while (iter.hasNext()) {
            Status status = iter.next();
            
            // DELETED DIRECTORIES (remotely)
            if (status.getNodeKind() == NodeKind.dir &&
                    status.getRepositoryTextStatus() == StatusKind.deleted) {
                
                // conflict if there is a child that is added or removed locally
                
                Status conflictParent = status;
                List<Status> localModList = new ArrayList<Status>();
                
                // find all children
                while (iter.hasNext()) {
                    status = iter.next();
                    if (isParent(conflictParent.getPath(), status.getPath())) {
                        if (status.getTextStatus() == StatusKind.added ||
                                status.getTextStatus() == StatusKind.replaced ||
                                status.getTextStatus() == StatusKind.modified) {
                            localModList.add(status);
                        }
                        iter.remove();
                    } else {
                        // no more children found, this conflict is done
                        break;
                    }
                }
                
                if (localModList.size() > 0) {
                    // also remove the deleted folder status object
                    remoteAndLocalChanges.remove(conflictParent);
                    
                    presentNewConflict(new DeleteWithModificationConflict(false, conflictParent, localModList), conflicts);
                }
                
                // reset global iterator for next conflict search
                iter = remoteAndLocalChanges.iterator();
            }
        }
        
        return conflicts;
    }

    /**
     * Finds all conflicts where a locally deleted file conflicts with a remote
     * file modification.
     */
    private List<Conflict> findDeleteModifiedConflicts(List<Status> remoteAndLocalChanges) throws CancelException {
        List<Conflict> conflicts = new ArrayList<Conflict>();
        
        Iterator<Status> iter = remoteAndLocalChanges.iterator();
        
        while (iter.hasNext()) {
            Status status = iter.next();
            
            // local DELETE (as we deleted everything, missing shouldn't happen)
            if (status.getNodeKind() == NodeKind.file &&
                    (status.getTextStatus() == StatusKind.deleted ||
                            status.getTextStatus() == StatusKind.missing)) {
                
                // if remote MOD
                if (status.getRepositoryTextStatus() == StatusKind.modified) {
                    iter.remove();
                    
                    presentNewConflict(new DeleteWithModificationConflict(true, status, null), conflicts);
                }
            }
        }
        
        return conflicts;
    }

    /**
     * Finds all conflicts where a locally modified file conflicts with a remote
     * file deletion.
     */
    private List<Conflict> findModifiedDeleteConflicts(List<Status> remoteAndLocalChanges) throws CancelException {
        List<Conflict> conflicts = new ArrayList<Conflict>();
        
        Iterator<Status> iter = remoteAndLocalChanges.iterator();
        
        while (iter.hasNext()) {
            Status status = iter.next();
            
            // remote DELETE
            if (status.getNodeKind() == NodeKind.file &&
                    status.getRepositoryTextStatus() == StatusKind.deleted) {
                
                // if local MOD
                if (status.getTextStatus() == StatusKind.modified) {
                    iter.remove();
                    
                    presentNewConflict(new DeleteWithModificationConflict(false, status, null), conflicts);
                }
            }
        }
        
        return conflicts;
    }

    /**
	 * Calls {@link Conflict.handleBeforeUpdate} on all conflicts in the list.
	 */
	private void handleConflictsBeforeUpdate(List<Conflict> conflicts) throws ClientException {
		for (Conflict conflict : conflicts) {
			log.info(">> Before Update: " + conflict.toString());
			conflict.beforeUpdate();
		}
	}

    /**
     * Calls {@link Conflict.handleAfterUpdate} on all conflicts in the list.
     */
	private void handleConflictsAfterUpdate(List<Conflict> conflicts) throws ClientException {
		for (Conflict conflict : conflicts) {
			log.info(">> After Update: " + conflict.toString());
			conflict.afterUpdate();
		}
	}

	/**
	 * Returns the relative path in the working copy of the status object (for
	 * shorter strings in log output).
	 */
    private String wcPath(Status status) {
        return RelativePath.getRelativePath(localPathFile, new File(status.getPath()));
    }

    /**
     * Gets all children and grand-children and so on for the path.
     */
    public static List<Status> getChildren(String path, List<Status> remoteAndLocalChanges) {
        List<Status> result = new ArrayList<Status>();
        // FIXME: not the fastest way (iterate over all + isParent for each)
        for (Status s : remoteAndLocalChanges) {
            if (isParent(path, s.getPath())) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * Checks if the file path 'parentPath' is a filesystem parent of the path
     * 'childPath'. Uses the java File api to check that correctly.
     */
    public static boolean isParent(String parentPath, String childPath) {
        File parent = new File(parentPath);
        File child = new File(childPath);

        while ((child = child.getParentFile()) != null) {
            if (child.equals(parent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes all status objects that are added and lie below the parent path.
     */
//	public static void removeNestedAdds(Status parent, List<Status> localChanges) {
//		Iterator<Status> iter = localChanges.iterator();
//		while(iter.hasNext()) {
//			if(isParent(parent.getPath(), iter.next().getPath())) {
//				iter.remove();
//			}
//		}
//	}

	/**
	 * Helper method that stringifies a notify object from the notify callback
	 * of svnkit.
	 */
	public static String notifyToString(NotifyInformation info) {
		return NotifyAction.actionNames[info.getAction()] + " "
				+ info.getPath();
	}

	/**
	 * Returns the given nodekind int value as a human-readable string (eg.
	 * "file" or "dir").
	 */
    public static String nodeKindDesc(int nodeKind) {
        if (nodeKind == NodeKind.file) {
            return "file";
        } else if (nodeKind == NodeKind.dir) {
            return "dir";
        } else if (nodeKind == NodeKind.none) {
            return "none";
        } else {
            return "unknown";
        }
    }

	public String getLocalPath() {
		return localPath;
	}

	public String getRepositoryURL() {
		return repositoryURL;
	}

}
