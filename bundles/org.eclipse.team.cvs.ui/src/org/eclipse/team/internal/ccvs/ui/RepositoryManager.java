package org.eclipse.team.internal.ccvs.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.ccvs.core.CVSTag;
import org.eclipse.team.ccvs.core.CVSTeamProvider;
import org.eclipse.team.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.core.ITeamManager;
import org.eclipse.team.core.ITeamProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamPlugin;
import org.eclipse.team.core.sync.IRemoteSyncElement;
import org.eclipse.team.internal.ccvs.ui.model.BranchTag;
import org.eclipse.team.ui.sync.TeamFile;

/**
 * This class is repsible for maintaining the UI's list of known repositories,
 * and a list of known tags within each of those repositories.
 * 
 * It also provides a number of useful methods for assisting in repository operations.
 */
public class RepositoryManager {
	private static final String STATE_FILE = ".repositoryManagerState";
	
	Hashtable repositories = new Hashtable();
	// Map ICVSRepositoryLocation -> List of Tags
	Hashtable branchTags = new Hashtable();
	// Map ICVSRepositoryLocation -> Hashtable of (Project name -> Set of CVSTags)
	Hashtable versionTags = new Hashtable();
	
	List listeners = new ArrayList();

	// The previously remembered comment
	private static String previousComment = "";
	
	/**
	 * Answer an array of all known remote roots.
	 */
	public ICVSRepositoryLocation[] getKnownRoots() {
		return (ICVSRepositoryLocation[])repositories.values().toArray(new ICVSRepositoryLocation[0]);
	}
	/**
	 * Answer the root corresponding with the given properties.
	 * If the root is in the list of known roots, it is returned.
	 * If it is not in the list of known roots, it is created and
	 * added.
	 */
	public ICVSRepositoryLocation getRoot(Properties properties) {
		StringBuffer keyBuffer = new StringBuffer();
		keyBuffer.append(":");
		keyBuffer.append(properties.getProperty("connection"));
		keyBuffer.append(":");
		keyBuffer.append(properties.getProperty("user"));
		keyBuffer.append("@");
		keyBuffer.append(properties.getProperty("host"));
		String port = properties.getProperty("port");
		if (port != null) {
			keyBuffer.append("#");
			keyBuffer.append(port);
		}
		keyBuffer.append(":");
		keyBuffer.append(properties.getProperty("root"));
		String key = keyBuffer.toString();
		
		ICVSRepositoryLocation result = (ICVSRepositoryLocation)repositories.get(key);
		if (result != null) {
			return result;
		}
		try {
			result = CVSProviderPlugin.getProvider().createRepository(properties);
			addRoot(result);
		} catch (TeamException e) {
			CVSUIPlugin.log(e.getStatus());
			return null;
		}
		return result;
	}
	/**
	 * Get the list of known branch tags for a given remote root.
	 */
	public BranchTag[] getKnownBranchTags(ICVSRepositoryLocation root) {
		Set set = (Set)branchTags.get(root);
		if (set == null) return new BranchTag[0];
		return (BranchTag[])set.toArray(new BranchTag[0]);
	}
	/**
	 * Get the list of known version tags for a given project.
	 */
	public CVSTag[] getKnownVersionTags(ICVSRemoteResource resource) {
		Hashtable table = (Hashtable)versionTags.get(resource.getRepository());
		if (table == null) return new CVSTag[0];
		Set set = (Set)table.get(resource.getName());
		if (set == null) return new CVSTag[0];
		return (CVSTag[])set.toArray(new CVSTag[0]);
	}
	/**
	 * Add the given branch tags to the list of known tags for the given
	 * remote root.
	 */
	public void addBranchTags(ICVSRepositoryLocation root, BranchTag[] tags) {
		Set set = (Set)branchTags.get(root);
		if (set == null) {
			set = new HashSet();
			branchTags.put(root, set);
		}
		for (int i = 0; i < tags.length; i++) {
			set.add(tags[i]);
		}
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IRepositoryListener listener = (IRepositoryListener)it.next();
			listener.branchTagsAdded(tags, root);
		}
	}
	/**
	 * Add the given repository location to the list of known repository
	 * locations. Listeners are notified.
	 */
	public void addRoot(ICVSRepositoryLocation root) {
		if (repositories.get(root.getLocation()) != null) return;
		repositories.put(root.getLocation(), root);
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IRepositoryListener listener = (IRepositoryListener)it.next();
			listener.repositoryAdded(root);
		}
	}
	/**
	 * Add the given version tags to the list of known tags for the given
	 * remote project.
	 */
	public void addVersionTags(ICVSRemoteResource resource, CVSTag[] tags) {
		String name = resource.getName();
		Hashtable table = (Hashtable)versionTags.get(resource.getRepository());
		if (table == null) {
			table = new Hashtable();
			versionTags.put(resource.getRepository(), table);
		}
		Set set = (Set)table.get(name);
		if (set == null) {
			set = new HashSet();
			table.put(name, set);
		}
		for (int i = 0; i < tags.length; i++) {
			set.add(tags[i]);
		}
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IRepositoryListener listener = (IRepositoryListener)it.next();
			listener.versionTagsAdded(tags, resource.getRepository());
		}
	}
	/**
	 * Remove the given branch tag from the list of known tags for the
	 * given remote root.
	 */
	public void removeBranchTag(ICVSRepositoryLocation root, BranchTag[] tags) {
		Set set = (Set)branchTags.get(root);
		if (set == null) return;
		for (int i = 0; i < tags.length; i++) {
			set.remove(tags[i]);
		}
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IRepositoryListener listener = (IRepositoryListener)it.next();
			listener.branchTagsRemoved(tags, root);
		}
	}
	/**
	 * Remove the given tags from the list of known tags for the
	 * given remote root.
	 */
	public void removeVersionTags(ICVSRemoteResource resource, CVSTag[] tags) {
		Hashtable table = (Hashtable)versionTags.get(resource.getRepository());
		if (table == null) return;
		Set set = (Set)table.get(resource.getName());
		if (set == null) return;
		for (int i = 0; i < tags.length; i++) {
			set.remove(tags[i]);
		}
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IRepositoryListener listener = (IRepositoryListener)it.next();
			listener.versionTagsRemoved(tags, resource.getRepository());
		}
	}
	/**
	 * Remove the given root from the list of known remote roots.
	 * Also removed the tags defined for this root.
	 */
	public void removeRoot(ICVSRepositoryLocation root) {
		BranchTag[] branchTags = getKnownBranchTags(root);
		Hashtable vTags = (Hashtable)this.versionTags.get(root);
		Object o = repositories.remove(root.getLocation());
		if (o == null) return;
		this.branchTags.remove(root);
		this.versionTags.remove(root);
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IRepositoryListener listener = (IRepositoryListener)it.next();
			listener.branchTagsRemoved(branchTags, root);
			Iterator keyIt = vTags.keySet().iterator();
			while (keyIt.hasNext()) {
				String projectName = (String)keyIt.next();
				Set tagSet = (Set)vTags.get(projectName);
				CVSTag[] versionTags = (CVSTag[])tagSet.toArray(new CVSTag[0]);
				listener.versionTagsRemoved(versionTags, root);
			}
			listener.repositoryRemoved(root);
		}
	}
	
	public void startup() throws TeamException {
		loadState();
	}
	
	public void shutdown() throws TeamException {
		saveState();
	}
	
	private void loadState() throws TeamException {
		IPath pluginStateLocation = CVSUIPlugin.getPlugin().getStateLocation().append(STATE_FILE);
		File file = pluginStateLocation.toFile();
		if (file.exists()) {
			try {
				DataInputStream dis = new DataInputStream(new FileInputStream(file));
				readState(dis);
				dis.close();
			} catch (IOException e) {
				throw new TeamException(new Status(Status.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.ioException"), e));
			}
		} else {
			// If the file did not exist, then prime the list of repositories with
			// the providers with which the projects in the workspace are shared.
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			ITeamManager manager = TeamPlugin.getManager();
			for (int i = 0; i < projects.length; i++) {
				ITeamProvider provider = manager.getProvider(projects[i]);
				if (provider instanceof CVSTeamProvider) {
					CVSTeamProvider cvsProvider = (CVSTeamProvider)provider;
					ICVSRepositoryLocation result = cvsProvider.getRemoteResource(projects[i]).getRepository();
					repositories.put(result.getLocation(), result);
					Iterator it = listeners.iterator();
					while (it.hasNext()) {
						IRepositoryListener listener = (IRepositoryListener)it.next();
						listener.repositoryAdded(result);
					}
				}
			}
		}
	}
	
	private void saveState() throws TeamException {
		IPath pluginStateLocation = CVSUIPlugin.getPlugin().getStateLocation();
		File tempFile = pluginStateLocation.append(STATE_FILE + ".tmp").toFile();
		File stateFile = pluginStateLocation.append(STATE_FILE).toFile();
		try {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(tempFile));
			writeState(dos);
			dos.close();
			if (stateFile.exists()) {
				stateFile.delete();
			}
			boolean renamed = tempFile.renameTo(stateFile);
			if (!renamed) {
				throw new TeamException(new Status(Status.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.rename", tempFile.getAbsolutePath()), null));
			}
		} catch (IOException e) {
			throw new TeamException(new Status(Status.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.save",stateFile.getAbsolutePath()), e));
		}
	}
	private void writeState(DataOutputStream dos) throws IOException {
		// Write the repositories
		Collection repos = repositories.values();
		dos.writeInt(repos.size());
		Iterator it = repos.iterator();
		while (it.hasNext()) {
			ICVSRepositoryLocation root = (ICVSRepositoryLocation)it.next();
			dos.writeUTF(root.getMethod().getName());
			dos.writeUTF(root.getUsername());
			dos.writeUTF(root.getHost());
			dos.writeUTF("" + root.getPort());
			dos.writeUTF(root.getRootDirectory());
			BranchTag[] branchTags = getKnownBranchTags(root);
			dos.writeInt(branchTags.length);
			for (int i = 0; i < branchTags.length; i++) {
				dos.writeUTF(branchTags[i].getTag().getName());
				dos.writeInt(branchTags[i].getTag().getType());
			}
			// write number of projects for which there are tags in this root
			Hashtable table = (Hashtable)versionTags.get(root);
			if (table == null) {
				dos.writeInt(0);
			} else {
				dos.writeInt(table.size());
				// for each project, write the name of the project, number of tags, and each tag.
				Iterator projIt = table.keySet().iterator();
				while (projIt.hasNext()) {
					String name = (String)projIt.next();
					dos.writeUTF(name);
					Set tagSet = (Set)table.get(name);
					dos.writeInt(tagSet.size());
					Iterator tagIt = tagSet.iterator();
					while (tagIt.hasNext()) {
						CVSTag tag = (CVSTag)tagIt.next();
						dos.writeUTF(tag.getName());
					}
				}
			}
		}
	}
	private void readState(DataInputStream dis) throws IOException {
		int repoSize = dis.readInt();
		for (int i = 0; i < repoSize; i++) {
			Properties properties = new Properties();
			properties.setProperty("connection", dis.readUTF());
			properties.setProperty("user", dis.readUTF());
			properties.setProperty("host", dis.readUTF());
			String port = dis.readUTF();
			if (!port.equals("" + ICVSRepositoryLocation.USE_DEFAULT_PORT)) {
				properties.setProperty("port", port);
			}
			properties.setProperty("root", dis.readUTF());
			ICVSRepositoryLocation root = getRoot(properties);
			int tagsSize = dis.readInt();
			BranchTag[] branchTags = new BranchTag[tagsSize];
			for (int j = 0; j < tagsSize; j++) {
				String tagName = dis.readUTF();
				int tagType = dis.readInt();
				branchTags[j] = new BranchTag(new CVSTag(tagName, tagType), root);
			}
			addBranchTags(root, branchTags);
			// read the number of projects for this root that have version tags
			int projSize = dis.readInt();
			if (projSize > 0) {
				Hashtable projTable = new Hashtable();
				versionTags.put(root, projTable);
				for (int j = 0; j < projSize; j++) {
					String name = dis.readUTF();
					Set tagSet = new HashSet();
					projTable.put(name, tagSet);
					int numTags = dis.readInt();
					for (int k = 0; k < numTags; k++) {
						tagSet.add(new CVSTag(dis.readUTF(), CVSTag.VERSION));
					}
					Iterator it = listeners.iterator();
					while (it.hasNext()) {
						IRepositoryListener listener = (IRepositoryListener)it.next();
						listener.versionTagsAdded((CVSTag[])tagSet.toArray(new CVSTag[0]), root);
					}
				}
			}
		}
	}
	
	public void addRepositoryListener(IRepositoryListener listener) {
		listeners.add(listener);
	}
	
	public void remoteRepositoryListener(IRepositoryListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Add the given resources to their associated providers.
	 * This schedules the resources for additin; they still need to be committed.
	 */
	public void add(IResource[] resources, IProgressMonitor monitor) throws TeamException {
		Hashtable table = getProviderMapping(resources);
		Set keySet = table.keySet();
		monitor.beginTask("", keySet.size() * 1000);
		monitor.setTaskName(Policy.bind("RepositoryManager.adding"));
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
			provider.setComment(previousComment);
			List list = (List)table.get(provider);
			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
			provider.add(providerResources, IResource.DEPTH_ZERO, subMonitor);
		}		
	}
	
	/**
	 * Delete the given resources from their associated providers.
	 * This schedules the resources for deletion; they still need to be committed.
	 */
	public void delete(IResource[] resources, IProgressMonitor monitor) throws TeamException {
		Hashtable table = getProviderMapping(resources);
		Set keySet = table.keySet();
		monitor.beginTask("", keySet.size() * 1000);
		monitor.setTaskName(Policy.bind("RepositoryManager.deleting"));
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
			provider.setComment(previousComment);
			List list = (List)table.get(provider);
			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
			provider.delete(providerResources, subMonitor);
		}		
	}
	/**
	 * Mark the files as merged.
	 */
	public void merged(IRemoteSyncElement[] elements) throws TeamException {
		Hashtable table = getProviderMapping(elements);
		Set keySet = table.keySet();
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
			provider.setComment(previousComment);
			List list = (List)table.get(provider);
			IRemoteSyncElement[] providerElements = (IRemoteSyncElement[])list.toArray(new IRemoteSyncElement[list.size()]);
			provider.merged(providerElements);
		}		
	}
	/**
	 * Commit the given resources to their associated providers.
	 * Prompt for a release comment, which will be applied to all committed
	 * resources. Persist the release comment for the next caller.
	 * 
	 * What should happen with errors?
	 * Should this do a workspace operation?
	 * 
	 * @param resources  the resources to commit
	 * @param shell  the shell that will be the parent of the release comment dialog
	 * @param monitor  the progress monitor
	 */
	public void commit(IResource[] resources, final Shell shell, IProgressMonitor monitor) throws TeamException {
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				ReleaseCommentDialog dialog = new ReleaseCommentDialog(shell);
				dialog.setComment(previousComment);
				int result = dialog.open();
				if (result != ReleaseCommentDialog.OK) return;
				previousComment = dialog.getComment();
			}
		});
		
		Hashtable table = getProviderMapping(resources);
		Set keySet = table.keySet();
		monitor.beginTask("", keySet.size() * 1000);
		monitor.setTaskName(Policy.bind("CommitAction.committing"));
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
			provider.setComment(previousComment);
			List list = (List)table.get(provider);
			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
			provider.checkin(providerResources, IResource.DEPTH_INFINITE, subMonitor);
		}
	}
	
	/**
	 * Get the given resources from their associated providers.
	 *
	 * @param resources  the resources to commit
	 * @param monitor  the progress monitor
	 */
	public void get(IResource[] resources, IProgressMonitor monitor) throws TeamException {
		Hashtable table = getProviderMapping(resources);
		Set keySet = table.keySet();
		monitor.beginTask("", keySet.size() * 1000);
		monitor.setTaskName(Policy.bind("GetAction.getting"));
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
			provider.setComment(previousComment);
			List list = (List)table.get(provider);
			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
			provider.get(providerResources, IResource.DEPTH_INFINITE, subMonitor);
		}
	}

	/**
	 * Helper method. Return a hashtable mapping provider to a list of resources
	 * shared with that provider.
	 */
	private Hashtable getProviderMapping(IResource[] resources) {
		Hashtable result = new Hashtable();
		for (int i = 0; i < resources.length; i++) {
			ITeamProvider provider = TeamPlugin.getManager().getProvider(resources[i].getProject());
			List list = (List)result.get(provider);
			if (list == null) {
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(resources[i]);
		}
		return result;
	}
	/**
	 * Helper method. Return a hashtable mapping provider to a list of IRemoteSyncElements
	 * shared with that provider.
	 */
	private Hashtable getProviderMapping(IRemoteSyncElement[] elements) {
		Hashtable result = new Hashtable();
		for (int i = 0; i < elements.length; i++) {
			ITeamProvider provider = TeamPlugin.getManager().getProvider(elements[i].getLocal().getProject());
			List list = (List)result.get(provider);
			if (list == null) {
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(elements[i]);
		}
		return result;
	}
}
