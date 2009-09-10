/*******************************************************************************
 * Copyright (c) 2009 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipe.debug.tests.viewer.model;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipe.debug.tests.viewer.model.TestModel.TestElement;
import org.eclipse.debug.internal.ui.viewers.model.ITreeModelContentProviderTarget;
import org.eclipse.debug.internal.ui.viewers.model.ITreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Tests to verify that the viewer property retrieves and processes the 
 * model deltas generated by the test model. 
 */
abstract public class DeltaTests extends TestCase {
    Display fDisplay;
    Shell fShell;
    ITreeModelViewer fViewer;
    TestModelUpdatesListener fListener;
    
    public DeltaTests(String name) {
        super(name);
    }

    /**
     * @throws java.lang.Exception
     */
    protected void setUp() throws Exception {
        fDisplay = PlatformUI.getWorkbench().getDisplay();
        fShell = new Shell(fDisplay/*, SWT.ON_TOP | SWT.SHELL_TRIM*/);
        fShell.setMaximized(true);
        fShell.setLayout(new FillLayout());

        fViewer = createViewer(fDisplay, fShell);
        
        fListener = new TestModelUpdatesListener(false, false);
        fViewer.addViewerUpdateListener(fListener);
        fViewer.addLabelUpdateListener(fListener);
        fViewer.addModelChangedListener(fListener);

        fShell.open ();
    }

    abstract protected ITreeModelContentProviderTarget createViewer(Display display, Shell shell);
    
    /**
     * @throws java.lang.Exception
     */
    protected void tearDown() throws Exception {
        fViewer.removeLabelUpdateListener(fListener);
        fViewer.removeViewerUpdateListener(fListener);
        fViewer.removeModelChangedListener(fListener);
        
        // Close the shell and exit.
        fShell.close();
        while (!fShell.isDisposed()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
    }

    public void testUpdateLabel() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleSingleLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
        
        // Update the model
        TestElement element = model.getRootElement().getChildren()[0];
        TreePath elementPath = new TreePath(new Object[] { element });
        ModelDelta delta = model.appendElementLabel(elementPath, "-modified");
        
        fListener.reset(elementPath, element, -1, true, false); 
        model.postDelta(delta);
        while (!fListener.isFinished(TestModelUpdatesListener.LABEL_COMPLETE | TestModelUpdatesListener.MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
    }

    public void testRefreshStruct() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleSingleLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
        
        // Update the model
        TestElement element = model.getRootElement().getChildren()[0];
        TreePath elementPath = new TreePath(new Object[] { element });
        TestElement[] newChildren = new TestElement[] {
            model.new TestElement("1.1", new TestElement[0]),
            model.new TestElement("1.2", new TestElement[0]),
            model.new TestElement("1.3", new TestElement[0]),
        };
        ModelDelta delta = model.setElementChildren(elementPath, newChildren);
        
        fListener.reset(elementPath, element, -1, true, false); 
        model.postDelta(delta);
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
    }

    public void testInsertElement() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleSingleLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
        
        // Update the model
        TestElement element = model.new TestElement("7", new TestElement[0]);
        TreePath elementPath = new TreePath(new Object[] { element });
        ModelDelta delta = model.insertElementChild(TreePath.EMPTY, 6, element);
        
        // Insert causes the update of element's data, label and children.
        // TODO: update of element's data after insert seems redundant
        // but it's probably not a big inefficiency
        fListener.reset();
        fListener.addChildreUpdate(TreePath.EMPTY, 6);
        fListener.addHasChildrenUpdate(elementPath);
        fListener.addLabelUpdate(elementPath);
        // TODO: redundant updates on insert!
        fListener.setFailOnRedundantUpdates(false);
        model.postDelta(delta);
        while (!fListener.isFinished(TestModelUpdatesListener.ALL_UPDATES_COMPLETE | TestModelUpdatesListener.MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
    }

    public void testAddElement() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleSingleLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
        
        // Update the model
        TestElement element = model.new TestElement("7", new TestElement[0]);
        TreePath elementPath = new TreePath(new Object[] { element });
        ModelDelta delta = model.addElementChild(TreePath.EMPTY, 6, element);
        
        // Add causes the update of parent child count and element's children.
        fListener.reset(elementPath, element, -1, true, false); 
        fListener.addChildreUpdate(TreePath.EMPTY, 6);
        // TODO: redundant updates on add!
        fListener.setFailOnRedundantUpdates(false);
        model.postDelta(delta);
        while (!fListener.isFinished(TestModelUpdatesListener.ALL_UPDATES_COMPLETE | TestModelUpdatesListener.MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
    }

    
    public void testRemoveElement() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleSingleLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
        
        // Update the model
        ModelDelta delta = model.removeElementChild(TreePath.EMPTY, 5);
        
        // Remove delta should generate no new updates, but we still need to wait for the event to
        // be processed.
        fListener.reset(); 
        model.postDelta(delta);
        while (!fListener.isFinished(TestModelUpdatesListener.MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
    }
    
    public void testExpandAndSelect() {
        TestModel model = TestModel.simpleMultiLevel();
        
        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY, true);

        // Create the delta
        fListener.reset();
        // TODO Investigate: there seem to be unnecessary updates being issued 
        // by the viewer.  These include the updates that are commented out:  
        // For now disable checking for extra updates.
        fListener.setFailOnRedundantUpdates(false);
        TestElement element = model.getRootElement();
        TreePath path_root = TreePath.EMPTY;
        ModelDelta delta= new ModelDelta(model.getRootElement(), -1, IModelDelta.EXPAND, element.getChildren().length);
        ModelDelta deltaRoot = delta;
        element = element.getChildren()[2];
        TreePath path_root_3 = path_root.createChildPath(element);
        delta = delta.addNode(element, 2, IModelDelta.EXPAND, element.fChildren.length);
        fListener.addChildreUpdate(path_root_3, 0);
        TreePath path_root_3_1 = path_root_3.createChildPath(element.getChildren()[0]);
        fListener.addHasChildrenUpdate(path_root_3_1);
        fListener.addLabelUpdate(path_root_3_1);
        TreePath path_root_3_3 = path_root_3.createChildPath(element.getChildren()[2]);
        fListener.addHasChildrenUpdate(path_root_3_3);
        fListener.addLabelUpdate(path_root_3_3);
        //TODO unnecessary update: fListener.addChildreUpdate(path1, 1); 
        fListener.addChildreUpdate(path_root_3, 2);
        element = element.getChildren()[1];
        TreePath path_root_3_2 = path_root_3.createChildPath(element);
        delta = delta.addNode(element, 1, IModelDelta.EXPAND, element.fChildren.length);
        fListener.addLabelUpdate(path_root_3_2);
        TreePath path_root_3_2_1 = path_root_3_2.createChildPath(element.getChildren()[0]);
        fListener.addHasChildrenUpdate(path_root_3_2_1);
        fListener.addLabelUpdate(path_root_3_2_1);
        TreePath path_root_3_2_3 = path_root_3_2.createChildPath(element.getChildren()[2]);
        fListener.addHasChildrenUpdate(path_root_3_2_3);
        fListener.addLabelUpdate(path_root_3_2_3);
        // TODO unnecessary update: fListener.addChildreCountUpdate(path2);
        fListener.addChildreUpdate(path_root_3_2, 0);
        // TODO unnecessary update: fListener.addChildreUpdate(path2, 1); 
        fListener.addChildreUpdate(path_root_3_2, 2);
        element = element.getChildren()[1];
        TreePath path_root_3_2_2 = path_root_3_2.createChildPath(element);
        delta = delta.addNode(element, 1, IModelDelta.SELECT, element.fChildren.length);
        fListener.addLabelUpdate(path_root_3_2_2);
        fListener.addHasChildrenUpdate(path_root_3_2_2);

        // Validate the expansion state BEFORE posting the delta.
        
        ITreeModelContentProviderTarget contentProviderViewer = (ITreeModelContentProviderTarget)fViewer; 
        Assert.assertFalse(contentProviderViewer.getExpandedState(path_root_3));
        Assert.assertFalse(contentProviderViewer.getExpandedState(path_root_3_2));
        Assert.assertFalse(contentProviderViewer.getExpandedState(path_root_3_2_2));
        
        model.postDelta(deltaRoot);
        while (!fListener.isFinished(TestModelUpdatesListener.ALL_UPDATES_COMPLETE | TestModelUpdatesListener.MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY, true);

        // Validate the expansion state AFTER posting the delta.
        Assert.assertTrue(contentProviderViewer.getExpandedState(path_root_3));
        Assert.assertTrue(contentProviderViewer.getExpandedState(path_root_3_2));
        Assert.assertFalse(contentProviderViewer.getExpandedState(path_root_3_2_2));
        
        // Verify selection
        ISelection selection = fViewer.getSelection();
        if (selection instanceof ITreeSelection) {
            List selectionPathsList = Arrays.asList( ((ITreeSelection)selection).getPaths() );
            Assert.assertTrue(selectionPathsList.contains(path_root_3_2_2));
        } else {
            Assert.fail("Not a tree selection");
        }
    }


}
