/*******************************************************************************
 * Copyright (c) 2017 the TeXlipse team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse;

import org.eclipse.core.runtime.Platform;

/** 
 * Small wrapper for Win32 DDE execute commands
 * 
 * @author Tor Arne Vestbø
 *
 */
public class DDEClient {

	public static native int execute(String server, String topic,
			String command);

	static {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			if (Platform.getOSArch().equals(Platform.ARCH_X86_64)) { 
				System.loadLibrary("ddeclient-x86_64");
			} else {
				System.loadLibrary("ddeclient-x86");
			}
		}
	}

	public static void main(String[] args) {
		int error = DDEClient.execute("acroview", "control",
				"[DocOpen(\"C:\\test.pdf\")][FileOpen(\"C:\\test.pdf\")]");
		// Try [DocClose("test.pdf")], but must be opened by DDE (not user)
		// Also, [MenuitemExecute("GoBack")] works in Acrobat (full)
		System.out.println("Error: " + error);
	}
}
