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
package org.eclipse.team.internal.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.mapping.IResourceMappingScope;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.TeamUIMessages;

public class AdditionalMappingsDialog extends DetailsDialog {

    private ResourceMappingHierarchyArea selectedMappingsArea;
    private ResourceMappingHierarchyArea allMappingsArea;
	private final IResourceMappingScope scope;
	private final ISynchronizationContext context;
	private String previewMessage;
	protected boolean forcePreview = true;

    public AdditionalMappingsDialog(Shell parentShell, String dialogTitle, IResourceMappingScope scope, ISynchronizationContext context) {
        super(parentShell, dialogTitle);
		this.scope = scope;
		this.context = context;
    }

	protected void createMainDialogArea(Composite parent) {
        createWrappingLabel(parent, TeamUIMessages.AdditionalMappingsDialog_0);
        createSelectedMappingsArea(parent);
        createAllMappingsArea(parent);
        createPreviewOptionArea(parent);
    }

	/*
     * Create a list that allows the selection of mappings via checkbox
     */
    private void createSelectedMappingsArea(Composite parent) {
        Composite composite = createComposite(parent);
        selectedMappingsArea = ResourceMappingHierarchyArea.create(scope.asInputScope(), null /* no context */);
        selectedMappingsArea.setDescription(TeamUIMessages.AdditionalMappingsDialog_1);
        selectedMappingsArea.createArea(composite);
        // Create a separator between the two sets of buttons
        Label seperator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        seperator.setLayoutData(new GridData (GridData.FILL_HORIZONTAL));
    }
    
    /*
     * Create a list that allows the selection of mappings via checkbox
     */
    private void createAllMappingsArea(Composite parent) {
        Composite composite = createComposite(parent);
        allMappingsArea = ResourceMappingHierarchyArea.create(scope, context);
        allMappingsArea.setDescription(TeamUIMessages.AdditionalMappingsDialog_2);
        //allMappingsArea.addPropertyChangeListener(this);
        allMappingsArea.createArea(composite);
    }

    private void createPreviewOptionArea(Composite parent) {
    	if (previewMessage != null) {
    		final Button forcePreviewButton = SWTUtils.createCheckBox(parent, previewMessage);
    		forcePreviewButton.setSelection(forcePreview);
    		forcePreviewButton.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					// Ignore
				}
				public void widgetSelected(SelectionEvent e) {
					forcePreview  = forcePreviewButton.getSelection();
				}
			});
    	}
	}
    
    protected Composite createDropDownDialogArea(Composite parent) {
        // TODO Auto-generated method stub
        return null;
    }

    protected void updateEnablements() {
        // TODO Auto-generated method stub

    }
    
    protected boolean includeDetailsButton() {
        return false;
    }

	public String getPreviewMessage() {
		return previewMessage;
	}

	public void setPreviewMessage(String previewMessage) {
		this.previewMessage = previewMessage;
	}

	public boolean isForcePreview() {
		return forcePreview;
	}

}
