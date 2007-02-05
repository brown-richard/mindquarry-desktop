/**
 * Copyright (C) 2005 MindQuarry GmbH, All Rights Reserved
 */
package com.mindquarry.client.task;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXReader;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.mindquarry.client.MindClient;
import com.mindquarry.client.util.network.HttpUtil;
import com.mindquarry.client.util.widgets.MessageDialogUtil;
import com.mindquarry.client.util.widgets.UpdateComposite;
import com.mindquarry.client.xml.TaskListTransformer;
import com.mindquarry.client.xml.TaskTransformer;

/**
 * @author <a href="mailto:lars(dot)trieloff(at)mindquarry(dot)com">Lars
 *         Trieloff</a>
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class TaskManager {
    public static final String TITLE_COLUMN = "title"; //$NON-NLS-1$

    private List<Task> tasks = new ArrayList<Task>();

    private List<TaskListChangeListener> listeners = new Vector<TaskListChangeListener>();

    private final MindClient client;

    private final Composite taskContainer;

    private final Button doneButton;

    private final TaskManager myself = this;

    private Composite refreshWidget;

    private Table taskTable = null;

    private TableViewer taskTableViewer = null;

    private static final StreamSource taskDoneXSL = new StreamSource(
            TaskManager.class.getResourceAsStream("/xslt/taskDone.xsl")); //$NON-NLS-1$

    public TaskManager(final MindClient client, final Composite taskContainer,
            final Button doneButton) {
        this.client = client;
        this.taskContainer = taskContainer;
        this.doneButton = doneButton;
    }

    public void startTask(Task t) {
        for (Task task : tasks) {
            task.setActive(false);
        }
        t.setActive(true);
    }

    public void stopTask(Task t) {
        t.setActive(false);
    }

    public void setDone(Task task) {
        tasks.remove(task);
        taskTableViewer.refresh();

        // set task content to status "done"
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        Source xmlSource = new DocumentSource(task.getContent());

        TransformerFactory transFact = TransformerFactory.newInstance();
        try {
            Transformer trans = transFact.newTransformer(taskDoneXSL);
            trans.transform(xmlSource, new StreamResult(result));

            HttpUtil.putAsXML(client.getOptions().getProperty(
                    MindClient.LOGIN_KEY), client.getOptions().getProperty(
                    MindClient.PASSWORD_KEY), task.getId(), result
                    .toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // must disable doneButton explicitly, because removing tasks does
        // not fire a selection changed event
        doneButton.setEnabled(false);
    }

    public void removeChangeListener(TaskListChangeListener provider) {
        this.listeners.remove(provider);
    }

    public void addChangeListener(TaskListChangeListener provider) {
        this.listeners.remove(provider);
    }

    public Task[] getTasks() {
        return tasks.toArray(new Task[] {});
    }

    /**
     * Runs task update in a separate thread, so that GUI can continue
     * processing. Thus this method returns immediatly. While updating tasks the
     * Task Manager will show an update widget instead of the task table.
     */
    public void asyncRefresh() {
    	System.out.println("refereshing");
		new Thread(new Runnable() {
			public void run() {
				refresh();
			}
		}).start();
    }

    private void refresh() {
        //setRefreshing(true);
    	System.out.println("calling http server for task list");
        InputStream content = null;
        try {
            content = HttpUtil.getContentAsXML(client.getOptions().getProperty(
                    MindClient.LOGIN_KEY), client.getOptions().getProperty(
                    MindClient.PASSWORD_KEY), client.getOptions().getProperty(
                    MindClient.ENDPOINT_KEY)
                    + "/tasks"); //$NON-NLS-1$
        } catch (Exception e) {
            MessageDialogUtil
                    .displaySyncErrorMsg(Messages.getString("TaskManager.0")); //$NON-NLS-1$
        }
        System.out.println("got some http content");
        // check if some contant was received
        if (content == null) {
        	System.out.println("no content received...");
            //setRefreshing(false);
        } else {
	        SAXReader reader = new SAXReader();
	        Document doc;
	        try {
	            doc = reader.read(content);
	        } catch (DocumentException e) {
	            e.printStackTrace();
	            return;
	        }
	        // create and execute transformer for tasklist description
	        TaskListTransformer tlTransformer = new TaskListTransformer();
	        tlTransformer.execute(doc);
	        System.out.println("getting task descriptions");
	        // get task descriptions
	        for (String taskURI : tlTransformer.getTaskURIs()) {
	            InputStream taskcontent = null;
	            try {
	                taskcontent = HttpUtil.getContentAsXML(client.getOptions()
	                        .getProperty(MindClient.LOGIN_KEY), client.getOptions()
	                        .getProperty(MindClient.PASSWORD_KEY), client
	                        .getOptions().getProperty(MindClient.ENDPOINT_KEY)
	                        + "/tasks/" + taskURI); //$NON-NLS-1$
	            } catch (Exception e) {
	                MessageDialogUtil
	                        .displaySyncErrorMsg(Messages.getString("TaskManager.1")); //$NON-NLS-1$
	            }
	            try {
	                doc = reader.read(taskcontent);
	                
	                TaskTransformer tTransformer = new TaskTransformer();
	                tTransformer.execute(doc);
	
	                // get initialized teask and set content
	                Task newTask = tTransformer.getTask();
	                newTask.setContent(doc);
	
	                // add task to internal list of tasks, if not yet exist
	                if ((!tasks.contains(newTask))
	                        && (!newTask.getStatus().equals("done"))) { //$NON-NLS-1$
	                    tasks.add(newTask);
	                }
	            } catch (DocumentException e) {
	                e.printStackTrace();
	            }
	        }
        }
        // update task table
        System.out.println("Everything is parsed, I'm going to show you some content");
        setRefreshing(false);
        
        if ((taskContainer!=null)&&(!taskContainer.isDisposed())) {
	        taskContainer.getDisplay().syncExec(new Runnable() {
	            public void run() {
	                taskTableViewer.setInput(myself);
	            }
	        });
        }
		
    }

    private void setRefreshing(final boolean refreshing) {
    	if ((taskContainer!=null)&&(!taskContainer.isDisposed())) {
	        taskContainer.getDisplay().syncExec(new Runnable() {
	            /**
	             * @see java.lang.Runnable#run()
	             */
	            public void run() {
	                if (refreshing) {
	                    if (taskTable != null) {
	                        taskTable.dispose();
	                        taskTable = null;
	                    }
	                    refreshWidget = new UpdateComposite(taskContainer,
	                            Messages.getString("TaskManager.2")); //$NON-NLS-1$
	                } else {
	                    if (refreshWidget != null) {
	                        refreshWidget.dispose();
	                        refreshWidget = null;
	                    }
	                    taskTable = new Table(taskContainer, SWT.BORDER);
	                    taskTable.setHeaderVisible(false);
	                    taskTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
	                            true, true));
	                    ((GridData) taskTable.getLayoutData()).heightHint = ((GridData) taskTable
	                            .getParent().getLayoutData()).heightHint;
	                    taskTable.setLinesVisible(false);
	                    taskTableViewer = new TableViewer(taskTable);
	
	                    TableColumn activityColumn = new TableColumn(taskTable,
	                            SWT.NONE);
	                    activityColumn.setResizable(false);
	                    activityColumn.setWidth(100);
	                    activityColumn.setText(Messages.getString("TaskManager.3")); //$NON-NLS-1$
	
	                    // create task list
	                    CellEditor[] editors = new CellEditor[taskTable
	                            .getColumnCount()];
	                    editors[0] = new CheckboxCellEditor(taskTable.getParent());
	
	                    taskTableViewer.setCellEditors(editors);
	                    taskTableViewer
	                            .setColumnProperties(new String[] { TaskManager.TITLE_COLUMN });
	                    taskTableViewer.getTable().getColumn(0).setWidth(300);
	
	                    taskTableViewer
	                            .setLabelProvider(new TaskTableLabelProvider());
	                    taskTableViewer
	                            .setContentProvider(new TaskTableContentProvider());
	                    taskTableViewer
	                            .addSelectionChangedListener(new TaskSelectionChangedListener(
	                                    myself, taskTableViewer, doneButton));
	                }
	                taskContainer.layout(true);
	            }
	        });
    	}
    }

    public TableViewer getTaskTableViewer() {
        return taskTableViewer;
    }
}