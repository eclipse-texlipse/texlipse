/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse;

import org.eclipse.core.runtime.Platform;

/** 
 * Small wrapper for Win32 DDE execute commands
 * 
 * @author Tor Arne Vestb�
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
