/**
 * This file Copyright (c) 2005-2010 Aptana, Inc. This program is
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
package com.aptana.core.util;

import junit.framework.TestCase;

public class VersionUtilTest extends TestCase
{

	public void testCompareVersions()
	{
		assertTrue(VersionUtil.compareVersions("2.0", "1.0") > 0);
		assertTrue(VersionUtil.compareVersions("2.10", "2.2") > 0);
		assertTrue(VersionUtil.compareVersions("2.1a", "2.1b") < 0);

		// Firebug-specific version #s
		assertTrue(VersionUtil.compareVersions("1.7X.0a1", "1.7X.0a1") == 0);
		assertTrue(VersionUtil.compareVersions("1.7X.0a1", "1.7X.0a2") < 0);
		assertTrue(VersionUtil.compareVersions("1.7X.0a2", "1.7X.0a1") > 0);
		assertTrue(VersionUtil.compareVersions("1.2.1b1", "1.2.1") > 0);
		assertTrue(VersionUtil.compareVersions("1.2.1", "1.2.1b1") < 0);

		// Eclipse-style version #s
		assertTrue(VersionUtil.compareVersions("1.3.0.v20100106-170", "1.3.0.v20100106-170") == 0);
		assertTrue(VersionUtil.compareVersions("1.3.0.v20100106-170", "1.3.0.v20100518-1140") < 0);
		assertTrue(VersionUtil.compareVersions("1.3.0.v20100518-1140", "1.3.0.v20100106-170") > 0);
		assertTrue(VersionUtil.compareVersions("v20100101-900", "v20100101-1200") > 0);
		
		assertTrue(VersionUtil.compareVersions("1.12.127", "1.12.82") > 0);
		assertTrue(VersionUtil.compareVersions("1.2.3.1000a", "1.2.3.1000b") < 0);
		assertTrue(VersionUtil.compareVersions("1.12", "1.12") == 0);
		assertTrue(VersionUtil.compareVersions("1.12", "1.12.0") < 0);
		assertTrue(VersionUtil.compareVersions("1.12.0", "1.12") > 0);
	}
}
