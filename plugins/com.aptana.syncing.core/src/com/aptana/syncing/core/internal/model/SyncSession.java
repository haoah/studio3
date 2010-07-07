/**
 * This file Copyright (c) 2005-2009 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.syncing.core.internal.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import com.aptana.ide.core.io.IConnectionPoint;
import com.aptana.ide.core.io.vfs.IExtendedFileStore;
import com.aptana.syncing.core.events.ISyncSessionListener;
import com.aptana.syncing.core.events.SyncSessionEvent;
import com.aptana.syncing.core.model.ISyncItem;
import com.aptana.syncing.core.model.ISyncSession;
import com.aptana.syncing.core.model.ISyncItem.Type;

/**
 * @author Max Stepanov
 *
 */
/* package */ class SyncSession implements ISyncSession, ISchedulingRule {

	private IConnectionPoint leftConnectionPoint;
	private IConnectionPoint rightConnectionPoint;
	private ISyncItem[] rootItems = SyncItem.EMPTY_LIST;
	private ListenerList listeners = new ListenerList();
	private Stage stage = Stage.INITIAL;
	private SyncDispatcher dispatcher;

	private ISyncSessionListener syncItemListener = new ISyncSessionListener() {
		@Override
		public void handleEvent(SyncSessionEvent event) {
			notifyListeners(event);
		}
	};
	
	/**
	 * 
	 */
	protected SyncSession(IConnectionPoint sourceConnectionPoint, IConnectionPoint destinationConnectionPoint) {
		this.leftConnectionPoint = sourceConnectionPoint;
		this.rightConnectionPoint = destinationConnectionPoint;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncSession#getSourceConnectionPoint()
	 */
	@Override
	public IConnectionPoint getSourceConnectionPoint() {
		return leftConnectionPoint;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncSession#getDestinationConnectionPoint()
	 */
	@Override
	public IConnectionPoint getDestinationConnectionPoint() {
		return rightConnectionPoint;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncSession#addListener(com.aptana.syncing.core.model.ISyncSessionListener)
	 */
	@Override
	public void addListener(ISyncSessionListener listener) {
		listeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncSession#removeListener(com.aptana.syncing.core.model.ISyncSessionListener)
	 */
	@Override
	public void removeListener(ISyncSessionListener listener) {
		listeners.remove(listener);
	}
	
	private void notifyListeners(SyncSessionEvent event) {
		for (Object listener : listeners.getListeners()) {
			((ISyncSessionListener) listener).handleEvent(event);
		}
	}
	
	private void fetchTreeInternal(Object[] itemsOrPaths, boolean deep, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		Stack<IPath> paths = new Stack<IPath>();
		Map<IPath, SyncItem> foldersMap = new HashMap<IPath, SyncItem>();
		Set<IPath> rootPaths = new HashSet<IPath>();
		for (int i = itemsOrPaths.length-1; i >= 0; --i) {
			if (itemsOrPaths[i] instanceof ISyncItem) {
				SyncItem item = (SyncItem) itemsOrPaths[i];
				foldersMap.put(item.getPath(), item);
				paths.add(item.getPath());
			} else if (itemsOrPaths[i] instanceof IPath) {
				paths.add((IPath) itemsOrPaths[i]);
				rootPaths.add((IPath) itemsOrPaths[i]);
			}
		}

		List<ISyncItem> rootList = new ArrayList<ISyncItem>();
		// reuse maps, lists
		Map<String, IFileInfo> leftMap = new HashMap<String, IFileInfo>();
		Map<String, IFileInfo> rightMap = new HashMap<String, IFileInfo>();
		List<SyncItem> list = new ArrayList<SyncItem>();
		while (!paths.isEmpty()) {
			if (progress.isCanceled()) {
				return;
			}
			IPath path = paths.pop();
			IFileStore left = leftConnectionPoint.getRoot().getFileStore(path);
			IFileStore right = rightConnectionPoint.getRoot().getFileStore(path);
			Stack<IPath> childPaths = new Stack<IPath>();
			if (!path.isRoot() && rootPaths.contains(path)) {
				SyncPair syncPair = new SyncPair(left, left.fetchInfo(IExtendedFileStore.DETAILED, progress.newChild(5, SubMonitor.SUPPRESS_NONE)),
						right, right.fetchInfo(IExtendedFileStore.DETAILED, progress.newChild(5, SubMonitor.SUPPRESS_NONE)));
				syncPair.calculateDirection(progress);
				SyncItem item = new SyncItem(path, syncPair);
				list.add(item);
				if ((deep && item.getType() == Type.FOLDER) || (syncPair.getLeftFileInfo().isDirectory() && syncPair.getRightFileInfo().isDirectory())) {
					foldersMap.put(item.getPath(), item);
					childPaths.add(item.getPath());
				}
			} else {
				IFileInfo[] leftInfos = left.childInfos(IExtendedFileStore.DETAILED, progress.newChild(5, SubMonitor.SUPPRESS_NONE));
				if (progress.isCanceled()) {
					return;
				}
				IFileInfo[] rightInfos = right.childInfos(IExtendedFileStore.DETAILED, progress.newChild(5, SubMonitor.SUPPRESS_NONE));
				if (progress.isCanceled()) {
					return;
				}
				leftMap.clear();
				rightMap.clear();
				for (IFileInfo i : leftInfos) {
					leftMap.put(i.getName(), i);
				}
				for (IFileInfo i : rightInfos) {
					String name = i.getName();
					rightMap.put(name, i);
					if (!leftMap.containsKey(name)) {
						leftMap.put(name, new FileInfo(name));
					}
				}
				for (IFileInfo i : leftInfos) {
					String name = i.getName();
					if (!rightMap.containsKey(name)) {
						rightMap.put(name, new FileInfo(name));
					}
				}
				list.clear();
				for (String name : leftMap.keySet()) {
					SyncPair syncPair = new SyncPair(left.getChild(name), leftMap.get(name), right.getChild(name), rightMap.get(name));
					syncPair.calculateDirection(progress);
					SyncItem item = new SyncItem(path.append(name), syncPair);
					list.add(item);
					if ((deep && item.getType() == Type.FOLDER) || (syncPair.getLeftFileInfo().isDirectory() && syncPair.getRightFileInfo().isDirectory())) {
						foldersMap.put(item.getPath(), item);
						childPaths.add(item.getPath());
						
					}
					if (progress.isCanceled()) {
						return;
					}
				}
			}
			while (!childPaths.isEmpty()) {
				paths.add(childPaths.pop());
			}
			ISyncItem[] array = list.toArray(new ISyncItem[list.size()]);
			if (rootPaths.contains(path)) {
				rootList.addAll(list);
				rootPaths.remove(path);
				rootItems = rootList.toArray(new ISyncItem[rootList.size()]);
				notifyListeners(new SyncSessionEvent(this, SyncSessionEvent.ITEMS_ADDED, array));
			} else {
				SyncItem parentItem = foldersMap.remove(path);
				parentItem.setChildItems(array);
				notifyListeners(new SyncSessionEvent(parentItem, SyncSessionEvent.ITEMS_ADDED, array));
			}
			progress.setWorkRemaining(paths.size()*10);
		}
		progress.done();
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncSession#fetchTree(com.aptana.syncing.core.model.ISyncItem[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void fetchTree(ISyncItem[] items, boolean deep, IProgressMonitor monitor) throws CoreException {
		fetchTreeInternal(items, deep, monitor);
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncSession#fetchTree(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void fetchTree(IProgressMonitor monitor) throws CoreException {
		fetchTreeInternal(new IPath[] { Path.ROOT }, false, monitor);
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncSession#fetchTree(org.eclipse.core.runtime.IPath[], boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void fetchTree(IPath[] paths, boolean deep, IProgressMonitor monitor) throws CoreException {
		fetchTreeInternal(paths, deep, monitor);
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncSession#synchronize(com.aptana.syncing.core.model.ISyncItem[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void synchronize(ISyncItem[] items, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, items.length);
		for (ISyncItem item : items) {
			((SyncItem) item).synchronize(progress.newChild(1, SubMonitor.SUPPRESS_NONE), syncItemListener);
		}
		monitor.done();
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncSession#getItems()
	 */
	@Override
	public ISyncItem[] getItems() {
		return rootItems;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	@Override
	public boolean contains(ISchedulingRule rule) {
		return this == rule;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		if (rule instanceof ISyncSession) {
			return leftConnectionPoint.equals(((ISyncSession) rule).getSourceConnectionPoint())
					|| rightConnectionPoint.equals(((ISyncSession) rule).getDestinationConnectionPoint());
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncSession#getStage()
	 */
	@Override
	public Stage getStage() {
		return stage;
	}

	/**
	 * @param stage the stage to set
	 */
	@Override
	public void setStage(Stage stage) {
		this.stage = stage;
		notifyListeners(new SyncSessionEvent(this, SyncSessionEvent.SESSION_STAGE_CHANGED, null));
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncSession#getSyncItems()
	 */
	@Override
	public ISyncItem[] getSyncItems() {
		return dispatcher.getSyncItems();
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncSession#setSyncItems(java.util.List)
	 */
	@Override
	public void setSyncItems(List<ISyncItem> syncItems) {
		dispatcher = new SyncDispatcher(syncItems);
	}
	
	protected SyncDispatcher getSyncDispatcher() {
		return dispatcher;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[").append( //$NON-NLS-1$
				leftConnectionPoint).append(" <-> ") //$NON-NLS-1$
				.append(rightConnectionPoint).append("]"); //$NON-NLS-1$
		return builder.toString();
	}
}