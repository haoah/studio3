/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.core.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.aptana.core.CorePlugin;
import com.aptana.core.ICorePreferenceConstants;

@SuppressWarnings("restriction")
public class EclipseUtil
{

	protected static final class LauncherFilter implements FilenameFilter
	{

		public boolean accept(File dir, String name)
		{
			IPath path = Path.fromOSString(dir.getAbsolutePath()).append(name);
			name = path.removeFileExtension().lastSegment();
			String ext = path.getFileExtension();
			if (Platform.OS_MACOSX.equals(Platform.getOS()))
			{
				if (!"app".equals(ext)) //$NON-NLS-1$
				{
					return false;
				}
			}
			for (String launcherName : LAUNCHER_NAMES)
			{
				if (launcherName.equalsIgnoreCase(name))
				{
					return true;
				}
			}
			return false;
		}
	}

	public static final String STANDALONE_PLUGIN_ID = "com.aptana.rcp"; //$NON-NLS-1$

	@SuppressWarnings("nls")
	private static final String[] UNIT_TEST_IDS = { "org.eclipse.pde.junit.runtime.uitestapplication",
			"org.eclipse.test.coretestapplication", "org.eclipse.test.uitestapplication",
			"org.eclipse.pde.junit.runtime.legacytestapplication", "org.eclipse.pde.junit.runtime.coretestapplication",
			"org.eclipse.pde.junit.runtime.coretestapplicationnonmain",
			"org.eclipse.pde.junit.runtime.nonuithreadtestapplication" };
	@SuppressWarnings("nls")
	static final String[] LAUNCHER_NAMES = { "Eclipse", "AptanaStudio3", "Aptana Studio 3", "TitaniumStudio",
			"Titanium Studio" };

	private static Boolean isTesting;

	private EclipseUtil()
	{
	}

	/**
	 * Determines if the specified debug option is on and set to true
	 * 
	 * @param option
	 * @return
	 */
	public static boolean isDebugOptionEnabled(String option)
	{
		return Boolean.valueOf(Platform.getDebugOption(option));
	}

	/**
	 * Determines if the specified application/platform option has been enabled
	 * 
	 * @param option
	 * @return
	 */
	public static boolean isSystemPropertyEnabled(String option)
	{
		return getSystemProperty(option) != null;
	}

	/**
	 * Returns specified application/platform option. If not specified, returns null.
	 * 
	 * @param option
	 * @return
	 */
	public static String getSystemProperty(String option)
	{
		if (option == null)
		{
			return null;
		}
		return System.getProperty(option);
	}

	/**
	 * Is the current plugin actually loaded (needed for unit testing)
	 * 
	 * @param plugin
	 * @return boolean
	 */
	public static boolean isPluginLoaded(Plugin plugin)
	{
		return plugin != null && plugin.getBundle() != null;
	}

	/**
	 * Retrieves the bundle version of a plugin.
	 * 
	 * @param plugin
	 *            the plugin to retrieve from
	 * @return the bundle version, or null if not found.
	 */
	public static String getPluginVersion(Plugin plugin)
	{
		if (!isPluginLoaded(plugin))
		{
			return null;
		}
		return plugin.getBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION).toString(); // $codepro.audit.disable
																											// com.instantiations.assist.eclipse.analysis.unnecessaryToString
	}

	/**
	 * Retrieves the bundle version of a plugin based on its id.
	 * 
	 * @param pluginId
	 *            the id of the plugin
	 * @return the bundle version, or null if not found.
	 */
	public static String getPluginVersion(String pluginId)
	{
		if (pluginId == null)
		{
			return null;
		}

		Bundle bundle = Platform.getBundle(pluginId);
		if (bundle == null)
		{
			return null;
		}
		return bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION).toString(); // $codepro.audit.disable
																								// com.instantiations.assist.eclipse.analysis.unnecessaryToString
	}

	/**
	 * Retrieves the product version from the Platform aboutText property
	 * 
	 * @return
	 */
	public static String getProductVersion()
	{
		String version = null;
		try
		{
			IProduct product = Platform.getProduct();
			String aboutText = product.getProperty("aboutText"); //$NON-NLS-1$

			String pattern = "Version: (.*)\n"; //$NON-NLS-1$
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(aboutText);
			boolean found = m.find();
			if (!found)
			{
				p = Pattern.compile("build: (.*)\n"); //$NON-NLS-1$
				m = p.matcher(aboutText);
				found = m.find();
			}

			if (found)
			{
				version = m.group(1);
			}
		}
		catch (Exception e)
		{
			// ignores
		}
		return version;
	}

	/**
	 * Determines if the IDE is running as a standalone app versus as a plugin
	 * 
	 * @return
	 */
	public static boolean isStandalone()
	{
		return getPluginVersion(STANDALONE_PLUGIN_ID) != null;
	}

	/**
	 * Determines if the IDE is running in a unit test
	 * 
	 * @return
	 */
	public static boolean isTesting()
	{
		if (isTesting != null)
		{
			return isTesting;
		}
		String application = System.getProperty("eclipse.application"); //$NON-NLS-1$
		if (application != null)
		{
			for (String id : UNIT_TEST_IDS)
			{
				if (id.equals(application))
				{
					isTesting = Boolean.TRUE;
					return isTesting;
				}
			}
		}
		Object commands = System.getProperties().get("eclipse.commands"); //$NON-NLS-1$
		isTesting = Boolean.valueOf((commands != null) ? commands.toString().contains("-testLoaderClass") : false); //$NON-NLS-1$
		return isTesting;
	}

	/**
	 * Returns path to application launcher executable
	 * 
	 * @return
	 */
	public static IPath getApplicationLauncher()
	{
		return getApplicationLauncher(false);
	}

	/**
	 * Returns path to application launcher executable
	 * 
	 * @param asSplashLauncher
	 * @return
	 */
	public static IPath getApplicationLauncher(boolean asSplashLauncher)
	{
		IPath launcher = null;
		String cmdline = System.getProperty("eclipse.commands"); //$NON-NLS-1$
		if (cmdline != null && cmdline.length() > 0)
		{
			String[] args = cmdline.split("\n"); //$NON-NLS-1$
			for (int i = 0; i < args.length; ++i)
			{
				if ("-launcher".equals(args[i]) && (i + 1) < args.length) { //$NON-NLS-1$
					launcher = Path.fromOSString(args[i + 1]);
					break;
				}
			}
		}
		if (launcher == null)
		{
			Location location = Platform.getInstallLocation();
			if (location != null)
			{
				launcher = new Path(location.getURL().getFile());
				if (launcher.toFile().isDirectory())
				{
					String[] executableFiles = launcher.toFile().list(new LauncherFilter());
					if (executableFiles.length > 0)
					{
						launcher = launcher.append(executableFiles[0]);
					}
				}
			}
		}
		if (launcher == null || !launcher.toFile().exists())
		{
			return null;
		}
		if (Platform.OS_MACOSX.equals(Platform.getOS()) && asSplashLauncher)
		{
			launcher = new Path(PlatformUtil.getApplicationExecutable(launcher.toOSString()).getAbsolutePath());
		}
		return launcher;
	}

	/**
	 * Checks to see if user has turned on showing system jobs to user, etc. If -debug flag from Eclipse is set, we also
	 * return true.
	 * 
	 * @return
	 */
	public static boolean showSystemJobs()
	{
		if (Platform.inDebugMode())
		{
			return true;
		}
		return Platform.getPreferencesService().getBoolean(CorePlugin.PLUGIN_ID,
				ICorePreferenceConstants.PREF_SHOW_SYSTEM_JOBS, false, null);
	}

	/**
	 * Set the debugging state of the platform
	 */
	public static void setPlatformDebugging(boolean debugEnabled)
	{
		if (debugEnabled)
		{
			FrameworkProperties.setProperty("osgi.debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			FrameworkProperties.clearProperty("osgi.debug"); //$NON-NLS-1$
		}
	}

	/**
	 * Returns a list of all possible trace items across all plugins
	 */
	public static Map<String, String> getTraceableItems()
	{
		Map<String, String> stringModels = new HashMap<String, String>();
		BundleContext context = CorePlugin.getDefault().getContext();
		Bundle[] bundles = context.getBundles();
		for (Bundle bundle : bundles)
		{
			Properties props = getTraceOptions(bundle);
			for (Object obj : props.keySet())
			{
				String key = obj.toString();
				stringModels.put(key, props.getProperty(key));
			}
		}
		return stringModels;
	}

	/**
	 * Returns all the trace options for a particular bundle
	 * 
	 * @param bundle
	 * @return
	 */
	public static Properties getTraceOptions(Bundle bundle)
	{
		Path path = new Path(".options"); //$NON-NLS-1$
		URL fileURL = FileLocator.find(bundle, path, null);
		if (fileURL != null)
		{
			InputStream in;
			try
			{
				in = fileURL.openStream();
				Properties options = new Properties();
				options.load(in);
				return options;
			}
			catch (IOException e1)
			{
			}
		}

		return new Properties();

	}

	/**
	 * Returns a map of all loaded bundle symbolic names mapped to bundles
	 * 
	 * @return
	 */
	public static Map<String, BundleContext> getCurrentBundleContexts()
	{
		Map<String, BundleContext> contexts = new HashMap<String, BundleContext>();

		BundleContext context = CorePlugin.getDefault().getContext();
		contexts.put(context.getBundle().getSymbolicName(), context);

		Bundle[] bundles = context.getBundles();
		for (Bundle bundle : bundles)
		{
			BundleContext bContext = bundle.getBundleContext();
			if (bContext == null)
			{
				continue;
			}
			contexts.put(bundle.getSymbolicName(), bContext);
		}

		return contexts;
	}

	/**
	 * Set debugging for the specified bundle
	 * 
	 * @param currentOptions
	 * @param debugEnabled
	 */
	public static void setBundleDebugOptions(String[] currentOptions, boolean debugEnabled)
	{
		Map<String, BundleContext> bundles = getCurrentBundleContexts();
		for (String key : currentOptions)
		{
			String symbolicName = key.substring(0, key.indexOf('/'));
			BundleContext bundleContext = bundles.get(symbolicName);
			if (bundleContext == null)
			{
				continue;
			}
			// don't add <?> as it's for Eclipse 3.7's getServiceReference() only
			ServiceReference sRef = bundleContext.getServiceReference(DebugOptions.class.getName());
			DebugOptions options = (DebugOptions) bundleContext.getService(sRef);

			// have to set debug enabled first if re-enabling, or else the internal property list will be null
			// and the set won't happen
			if (debugEnabled)
			{
				options.setDebugEnabled(debugEnabled);
				options.setOption(key, Boolean.toString(debugEnabled));
			}
			else
			{
				options.setOption(key, Boolean.toString(debugEnabled));
				options.setDebugEnabled(debugEnabled);
			}
		}
	}

	/**
	 * Gets the list of components currently in debug mode
	 * 
	 * @return
	 */
	public static String[] getCurrentDebuggableComponents()
	{
		String checked = Platform.getPreferencesService().getString(CorePlugin.PLUGIN_ID,
				ICorePreferenceConstants.PREF_DEBUG_COMPONENT_LIST, null, null);
		if (checked != null)
		{
			return checked.split(","); //$NON-NLS-1$
		}
		return ArrayUtil.NO_STRINGS;
	}

	/**
	 * Find all elements of a given name for an extension point and delegate processing to an
	 * IConfigurationElementProcessor.
	 * 
	 * @param pluginId
	 * @param extensionPointId
	 * @param processor
	 * @param elementNames
	 */
	public static void processConfigurationElements(String pluginId, String extensionPointId,
			IConfigurationElementProcessor processor, String... elementNames)
	{
		if (!StringUtil.isEmpty(pluginId) && !StringUtil.isEmpty(extensionPointId) && processor != null)
		{
			IExtensionRegistry registry = Platform.getExtensionRegistry();

			if (registry != null)
			{
				IExtensionPoint extensionPoint = registry.getExtensionPoint(pluginId, extensionPointId);

				if (extensionPoint != null)
				{
					for (IExtension extension : extensionPoint.getExtensions())
					{
						IConfigurationElement[] elements = extension.getConfigurationElements();
						for (String elementName : elementNames)
						{
							for (IConfigurationElement element : elements)
							{
								if (element.getName().equals(elementName))
								{
									processor.processElement(element);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Wrapper for Eclipse 3.6- to collect all deprecated usages into a single location. Once Eclipse 3.7 is the default
	 * base platform, we can remove this call.
	 * 
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static InstanceScope instanceScope()
	{
		return new InstanceScope();
	}

	/**
	 * Wrapper for Eclipse 3.6- to collect all deprecated usages into a single location. Once Eclipse 3.7 is the default
	 * base platform, we can remove this call.
	 * 
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static DefaultScope defaultScope()
	{
		return new DefaultScope();
	}

	/**
	 * Wrapper for Eclipse 3.6- to collect all deprecated usages into a single location. Once Eclipse 3.7 is the default
	 * base platform, we can remove this call.
	 * 
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static ConfigurationScope configurationScope()
	{
		return new ConfigurationScope();
	}

}
