#!/bin/sh

# Jazzy's license is LGPL and does not have a binary distribution, so we need
# to download it from somewhere. See https://github.com/reckart/jazzy for the
# source code.
curl -O https://resheim.net/texlipse/jazzy-core.jar

# These binaries needs to be built by a Windows machine. The source code can
# be found in the "cppsource" folder.
curl -O https://resheim.net/texlipse/ddeclient-x86_64.dll
curl -O https://resheim.net/texlipse/ddeclient-x86.dll