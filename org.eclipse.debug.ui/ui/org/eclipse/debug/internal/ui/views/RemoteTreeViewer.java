/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.progress.UIJob;

/**
 * A tree viewer that displays remote content. Content is retrieved in a background
 * job, and the viewer is updated incrementally on a refresh.
 * 
 * @since 3.1
 */
public class RemoteTreeViewer extends TreeViewer {

    private ExpansionJob fExpansionJob = null;
    private SelectionJob fSelectionJob = null;
    

    class ExpansionJob extends UIJob {
        
        private Object element;
        private List parents = new ArrayList(); // top down
        
        /**
         * Constucts a job to expand the given element.
         * 
         * @param target the element to expand
         */
        public ExpansionJob() {
            super(DebugUIViewsMessages.LaunchViewer_1); //$NON-NLS-1$
            setPriority(Job.INTERACTIVE);
            setSystem(true);
        }

        /* (non-Javadoc)
         * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
         */
        public IStatus runInUIThread(IProgressMonitor monitor) {
            if (getControl().isDisposed() || element == null) {
                return Status.OK_STATUS;
            }            
            synchronized (RemoteTreeViewer.this) {
                boolean allParentsExpanded = true;
                Iterator iterator = parents.iterator();
                while (iterator.hasNext() && !monitor.isCanceled()) {
                    Object parent = iterator.next();
                    Widget item = findItem(parent);
                    if (item != null) {
                        expandToLevel(parent, 1);
                    } else {
                        allParentsExpanded = false;
                        break;
                    }
                }
                if (allParentsExpanded) {
                    Widget item = findItem(element); 
                    if (item != null) {
                        if (isExpandable(element)) {
    	                    expandToLevel(element, 1);
                        }
                        element = null;
                        parents.clear();
                        return Status.OK_STATUS;
                    }
                }
                return Status.OK_STATUS;
            }
        }
        
        public void validate(Object object) {
            if (element != null) {   
                if (element.equals(object) || parents.contains(object)) {
                    cancel();
                    element = null;
                }
            }
        }

        public void setDeferredExpansion(Object toExpand) {
            element = toExpand;
            parents.clear();
            addAllParents(parents, element);
        }
        
    }

    class SelectionJob extends UIJob {
        
        private IStructuredSelection selection;
        private Object first;
        private List parents = new ArrayList(); // top down
        
        /**
         * Constucts a job to select the given element.
         * 
         * @param target the element to select
         */
        public SelectionJob() {
            super(DebugUIViewsMessages.LaunchViewer_0); //$NON-NLS-1$
            setPriority(Job.INTERACTIVE);
            setSystem(true);
        }

        /* (non-Javadoc)
         * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
         */
        public IStatus runInUIThread(IProgressMonitor monitor) {
            if (getControl().isDisposed() || selection == null) {
                return Status.OK_STATUS;
            }
            synchronized (RemoteTreeViewer.this) {
                boolean allParentsExpanded = true;
                Iterator iterator = parents.iterator();
                while (iterator.hasNext() && !monitor.isCanceled()) {
                    Object parent = iterator.next();
                    Widget item = findItem(parent);
                    if (item != null) {
                        expandToLevel(parent, 1);
                    } else {
                        allParentsExpanded = false;
                        break;
                    }
                }
                if (allParentsExpanded) {
                    if (findItem(first) != null) {
                        setSelection(selection, true);
                        selection = null;
                        first = null;
                        parents.clear();
                        return Status.OK_STATUS;
                    }
                }

                return Status.OK_STATUS;
            }
        }
        
        public void setDeferredSelection(IStructuredSelection sel) {
            selection = sel;
            first = selection.getFirstElement();
            parents.clear();
            addAllParents(parents, first);
        }
        
        public void validate(Object object) {
            if (first != null) {
                if (first.equals(object) || parents.contains(object)) {
                    cancel();
                    selection = null;
                }
            }
        }
    }
    

    /**
     * Constructs a remote tree viewer parented by the given composite.
     *   
     * @param parent parent composite
     */
    public RemoteTreeViewer(Composite parent) {
        super(parent);
        addDisposeListener();
        fExpansionJob = new ExpansionJob();
        fSelectionJob = new SelectionJob();
    }

    /**
     * Constructs a remote tree viewer parented by the given composite
     * with the given style.
     * 
     * @param parent parent composite
     * @param style style bits
     */
    public RemoteTreeViewer(Composite parent, int style) {
        super(parent, style);
        addDisposeListener();
        fExpansionJob = new ExpansionJob();
        fSelectionJob = new SelectionJob();
    }

    /**
     * Constructs a remote tree viewer with the given tree.
     * 
     * @param tree tree widget
     */
    public RemoteTreeViewer(Tree tree) {
        super(tree);
        addDisposeListener();
        fExpansionJob = new ExpansionJob();
        fSelectionJob = new SelectionJob();
    }
    
    private void addDisposeListener() {
        getControl().addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                cancelJobs();
            }
        });
    }
    
    protected void runDeferredUpdates() {
        if (fExpansionJob != null) {
            fExpansionJob.schedule();
        }
        if (fSelectionJob != null) {
            fSelectionJob.schedule();
        }        
    }
    
    /**
     * The given element is being removed from the tree. Cancel
     * any deferred updates for the element.
     * 
     * @param element
     */
    protected void validateDeferredUpdates(Object element) {
        if (element != null) {
	        if (fExpansionJob != null) {
	            fExpansionJob.validate(element);
	        }
	        if (fSelectionJob != null) {
	            fSelectionJob.validate(element);
	        }
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.AbstractTreeViewer#add(java.lang.Object, java.lang.Object)
     */
    public synchronized void add(Object parentElement, Object childElement) {
        super.add(parentElement, childElement);
        runDeferredUpdates();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.AbstractTreeViewer#add(java.lang.Object, java.lang.Object[])
     */
    public synchronized void add(Object parentElement, Object[] childElements) {
        super.add(parentElement, childElements);
        runDeferredUpdates();
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#remove(java.lang.Object)
	 */
	public synchronized void remove(Object element) {
	    validateDeferredUpdates(element);
		super.remove(element);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#remove(java.lang.Object[])
	 */
	public synchronized void remove(Object[] elements) {
	    for (int i = 0; i < elements.length; i++) {
            validateDeferredUpdates(elements[i]);
        }
		super.remove(elements);
	}    

    /**
     * Cancels any deferred updates currently scheduled/running.
     */
    public void cancelJobs() {
        cancel(fSelectionJob);
        cancel(fExpansionJob);
    }

    public synchronized void deferExpansion(Object element) {
        TreeItem treeItem = (TreeItem) findItem(element);
        if (treeItem == null) {
            fExpansionJob.setDeferredExpansion(element);
            fExpansionJob.schedule();
        } else {
            if (!getExpanded(treeItem)) {
                fExpansionJob.setDeferredExpansion(element);
                fExpansionJob.schedule();
            }
        }
    }
    
    public synchronized void deferSelection(IStructuredSelection selection) {
        if (fSelectionJob == null) {
            fSelectionJob = new SelectionJob();
        }
        
        fSelectionJob.setDeferredSelection(selection);
        fSelectionJob.schedule();        
    }    
    
    public IStructuredSelection getDeferredSelection() {
        if (fSelectionJob != null) {
            return fSelectionJob.selection;
        }
        return null;
    }

    private void cancel(Job job) {
        if (job != null) {
            job.cancel();
        }	    
    }

    private void addAllParents(List list, Object element) {
        if (element instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) element;
            IWorkbenchAdapter adapter = (IWorkbenchAdapter) adaptable.getAdapter(IWorkbenchAdapter.class);
            if (adapter != null) {
                Object parent = adapter.getParent(element);
                if (parent != null) {
                    list.add(0, parent);
                    if (!(parent instanceof ILaunch))
                    addAllParents(list, parent);
                }
            }
        }
    }

    public Object[] filter(Object[] elements) {
        return super.filter(elements);
    }
    
    public Object[] getCurrentChildren(Object parent) {
        Widget widget = findItem(parent);
        if (widget != null) {
            Item[] items = getChildren(widget);
            Object[] children = new Object[items.length];
            for (int i = 0; i < children.length; i++) {
                Object data = items[i].getData();
                if (data == null) {
                    return null;
                }
                children[i] = data;
    		}
            return children;
        }
        return null;
    }

    public synchronized void prune(final Object parent, final int offset) {
        Widget widget = findItem(parent);
        if (widget != null) {
            final Item[] currentChildren = getChildren(widget);
            if (offset < currentChildren.length) {
                preservingSelection(new Runnable() {
                    public void run() {
                        for (int i = offset; i < currentChildren.length; i++) {
                            if (currentChildren[i].getData() != null) {
                                disassociate(currentChildren[i]);
                            } 
                            currentChildren[i].dispose();
                        }
                    }
                });
            }
        }
    }

    public synchronized void replace(final Object parent, final Object[] children, final int offset) {
        preservingSelection(new Runnable() {
            public void run() {
                Widget widget = findItem(parent);
                if (widget == null) {
                    add(parent, children);
                    return;
                }
                Item[] currentChildren = getChildren(widget);
                int pos = offset;
                if (pos >= currentChildren.length) {
                    // append
                    add(parent, children);
                } else {
                    // replace
                    for (int i = 0; i < children.length; i++) {
                        Object child = children[i];
                        if (pos < currentChildren.length) {
                            // replace
                            Item item = currentChildren[pos];
                            Object data = item.getData();
                            if (!child.equals(data)) {
                                // no need to cancel pending updates here, the child may have shifted up/down
                                internalRefresh(item, child, true, true);
                            } else {
                            	// If it's the same child, the label/content may still have changed
                                doUpdateItem(item, child);
                            	updatePlus(item, child);
                            }
                        } else {
                            // add
                        	int numLeft = children.length - i;
                        	if (numLeft > 1) {
                        		Object[] others = new Object[numLeft];
                        		System.arraycopy(children, i, others, 0, numLeft);
                        		add(parent, others);
                        	} else {
                        		add(parent, child);
                        	}
                        	return;
                        }
                        pos++;
                    }
                }
                runDeferredUpdates();
            }
        });
    }

	protected void doUpdateItem(Item item, Object element) {
		// update icon and label
		ILabelProvider provider= (ILabelProvider) getLabelProvider();
		String text= provider.getText(element);
		if ("".equals(item.getText()) || !DebugViewInterimLabelProvider.PENDING_LABEL.equals(text)) { //$NON-NLS-1$
			// If an element already has a label, don't set the label to
			// the pending label. This avoids labels flashing when they're
			// updated.
			item.setText(text);
		}
		Image image = provider.getImage(element);
		if (item.getImage() != image) {
			item.setImage(image);
		}
		if (provider instanceof IColorProvider) {
			IColorProvider cp = (IColorProvider) provider;
			TreeItem treeItem = (TreeItem) item;
			treeItem.setForeground(cp.getForeground(element));
			treeItem.setBackground(cp.getBackground(element));
		}
		if (provider instanceof IFontProvider) {
			IFontProvider fontProvider= (IFontProvider) provider;
			TreeItem treeItem = (TreeItem) item;
			treeItem.setFont(fontProvider.getFont(element));
		}
	    
	}
    
}


