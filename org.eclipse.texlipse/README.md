# Building the Windows binaries

The Windows DDE client is used to synchronize the editor within Eclipse and
the preview opened in an external viewer.

The Windows binaries _ddeclient-x86_64.dll_ and _ddeclient-x86.dll_ can be
built on a Windows 10 host using the Linux subsystem and MinGW. Note that 
Java 8 must be installed on Windows.

See https://msdn.microsoft.com/en-us/commandline/wsl/install-win10 for details
on how to install the Linux subsystem for Windows. 

Then execute the following sequence to install MinGW

    sudo apt-get install mingw-w64
    
Follow up by cloning this repository
    
    git clone https://github.com/eclipse/texlipse.git

Now the 32-bit and 64-bit clients can be built. Make sure that each of
these lines are combined into one as they cannot be copy-pasted into the
Windows Bash shell. The path to the JDK may also have to be adjusted.

    i686-w64-mingw32-gcc -shared -o./texlipse/org.eclipse.texlipse/ddeclient-x86.dll \
      -I"/mnt/c/Program Files/Java/jdk1.8.0_151/include/" \
      -I"/mnt/c/Program Files/Java/jdk1.8.0_151/include/win32/" \ 
      ./texlipse/org.eclipse.texlipse/cppsource/ddeclient.cpp

    x86_64-w64-mingw32-gcc -shared -o./texlipse/org.eclipse.texlipse/ddeclient-x86_64.dll \
      -I"/mnt/c/Program Files/Java/jdk1.8.0_151/include/" \ 
      -I"/mnt/c/Program Files/Java/jdk1.8.0_151/include/win32/" \ 
      ./texlipse/org.eclipse.texlipse/cppsource/ddeclient.cpp
