package org.eclipse.update.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import java.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.update.ui.internal.model.*;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.viewers.*;
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;
import java.net.URL;

public class TargetPage extends WizardPage {
	private TableViewer tableViewer;
	
	
class TableContentProvider extends DefaultContentProvider 
							implements IStructuredContentProvider {
	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object parent) {
		try {
		   ILocalSite localSite = SiteManager.getLocalSite();
		   return localSite.getCurrentConfiguration().getInstallSites();
		}
		catch (CoreException e) {
			UpdateUIPlugin.logException(e);
		}
		return new Object[0];
	}
}	

class TableLabelProvider extends LabelProvider implements
								ITableLabelProvider {
									/**
	 * @see ITableLabelProvider#getColumnImage(Object, int)
	 */
	public Image getColumnImage(Object obj, int col) {
		return null;
	}

	/**
	 * @see ITableLabelProvider#getColumnText(Object, int)
	 */
	public String getColumnText(Object obj, int col) {
		if (obj instanceof ISite && col==0) {
			ISite site = (ISite)obj;
			URL url = site.getURL();
			return url.toString();
		}
		return null;
	}

}
	
	
	/**
	 * Constructor for ReviewPage
	 */
	public TargetPage() {
		super("Target");
		setTitle("Install Location");
		setDescription("Choose the location where the feature will be installed.");
	}

	/**
	 * @see DialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite client = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		client.setLayout(layout);
		createTableViewer(client);
		Composite buttonContainer = new Composite(client, SWT.NULL);
		GridLayout blayout = new GridLayout();
		blayout.marginWidth = blayout.marginHeight = 0;
		buttonContainer.setLayout(blayout);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		final Button button = new Button(buttonContainer, SWT.PUSH);
		button.setText("&New...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addTargetLocation();
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		button.setLayoutData(gd);
		setControl(client);
	}
	private void createTableViewer(Composite parent) {
		tableViewer = new TableViewer(parent);
		GridData gd = new GridData(GridData.FILL_BOTH);
		Table table = tableViewer.getTable();
		table.setLayoutData(gd);
		table.setHeaderVisible(true);
		
		TableColumn tc = new TableColumn(table, SWT.NULL);
		tc.setText("Target Location");
		
		TableLayout layout= new TableLayout();
		ColumnLayoutData ld = new ColumnWeightData(100);
		layout.addColumnData(ld);
		table.setLayout(layout);
		tableViewer.setContentProvider(new TableContentProvider());
		tableViewer.setLabelProvider(new TableLabelProvider());
		tableViewer.setInput(tableViewer);
		selectFirstTarget();
		table.setFocus();
	}
	private void selectFirstTarget() {
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			ISite [] sites = localSite.getCurrentConfiguration().getInstallSites();
			if (sites.length>0) {
				tableViewer.setSelection(new StructuredSelection(sites[0]));
			}
		}
		catch (CoreException e) {
		}
	}
	private void addTargetLocation() {
	}
	
	public ISite getTargetSite() {
		IStructuredSelection sel = (IStructuredSelection)tableViewer.getSelection();
		if (sel.isEmpty()) return null;
		return (ISite)sel.getFirstElement();
	}
}