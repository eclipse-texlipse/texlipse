/*
 * $Id$
 *
 * Copyright (c) 2004-2006 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @author Tor Arne Vestbø
 *
 * Compile with cl -I$jdk\include -I$jdk\include\win32
 * -LD DDEClient.cpp -Fddeclient.dll User32.lib
 */

#define UNICODE
#define _UNICODE

#include <jni.h>
#include <windows.h>
#include <ddeml.h>
#include "ddeclient.h"

// Not used for anything but must be defined
HDDEDATA CALLBACK DdeCallback(UINT uType, UINT uFmt, HCONV hconv,
    HSZ hsz1, HSZ hsz2, HDDEDATA hdata, DWORD dwData1, DWORD dwData2)
{
    return 0;
}


JNIEXPORT jint JNICALL Java_net_sourceforge_texlipse_DDEClient_execute
  (JNIEnv* env, jclass obj, jstring server, jstring topic, jstring command)
{
    // Pull unicode strings from the java arguments
	const jchar *szServer = env->GetStringChars(server, 0);
	const jchar *szTopic =  env->GetStringChars(topic, 0);
	const jchar *szCommand = env->GetStringChars(command, 0);

	// Init the DDEM Library
	DWORD idInst = 0;
    UINT iReturn = DdeInitialize(&idInst, (PFNCALLBACK)DdeCallback,
                            APPCLASS_STANDARD | APPCMD_CLIENTONLY, 0 );
    if (iReturn != DMLERR_NO_ERROR)
    {
        return 1;
    }

	// Connect to the DDE server
	HSZ hszServer = DdeCreateStringHandle(idInst, (WCHAR*)szServer, 0);
	HSZ hszTopic = DdeCreateStringHandle(idInst, (WCHAR*)szTopic, 0);
    HCONV hConv = DdeConnect(idInst, hszServer, hszTopic, NULL);

	// Free up some resources
	DdeFreeStringHandle(idInst, hszServer);
	env->ReleaseStringChars(server, szServer);
    DdeFreeStringHandle(idInst, hszTopic);
	env->ReleaseStringChars(topic, szTopic);

	// Make sure we're connected
	if (hConv == NULL)
    {
        DdeUninitialize(idInst);
		env->ReleaseStringChars(command, szCommand);
        return 2;
    }

	// Prepare data for transaction

	HDDEDATA hData = DdeCreateDataHandle(idInst, (LPBYTE)szCommand,                                      
									  (lstrlen((WCHAR*)szCommand) + 1) * sizeof(WCHAR),
									   0L, 0L, CF_UNICODETEXT, 0);
	env->ReleaseStringChars(command, szCommand);

    // Data is OK?
	if (hData == NULL)
	{
        DdeDisconnect(hConv);
	    DdeUninitialize(idInst);
		return 3;
    }
    else
	{
		DdeClientTransaction((LPBYTE)hData, 0xFFFFFFFF, hConv, 0L, 0,
                             XTYP_EXECUTE, TIMEOUT_ASYNC, NULL);
    }

	// Clean up
	DdeDisconnect(hConv);
    DdeUninitialize(idInst);

    return 0;
}
