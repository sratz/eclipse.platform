/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ssh;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;
public class SSHPlugin extends Plugin {
	
	public static String ID = "org.eclipse.team.cvs.ssh"; //$NON-NLS-1$

	private static SSHPlugin instance;
	
	/**
	 * Log the given exception along with the provided message and severity indicator
	 */
	public static void log(int severity, String message, Throwable e) {
		getPlugin().getLog().log(new Status(severity, ID, 0, message, e));
	}
	
	/**
	 * Constructor for SSHPlugin
	 */
	public SSHPlugin() {
		super();	
		instance = this;
	}
	
	/**
	 * @see Plugin#start(BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		Policy.localize("org.eclipse.team.internal.ccvs.ssh.messages"); //$NON-NLS-1$
	}
	
	/**
	 * Method getPlugin.
	 */
	public static SSHPlugin getPlugin() {
		return instance;
	}

}
