/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.templates;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;


/**
 * Helper methods for handling project templates.
 * 
 * @author Kimmo Karlsson
 */
public class ProjectTemplateManager {
    
    // templates directory in plugin's config dir in workspace
    private static final String USER_TEMPLATES_DIR = "templates";
    
    // templates directory in plugin's read-only system area
    private static final String SYSTEM_TEMPLATES_DIR = "templates";
    
    /**
     * @return a sorted array of user template names
     */
    public static String[] loadUserTemplateNames() {
        
        ArrayList list = new ArrayList();
        
        // list user-defined templates
        appendUserTemplates(list);
        
        // return sorted list
        String[] array = (String[]) list.toArray(new String[0]);
        Arrays.sort(array);
        return array;
    }
    
    /**
     * @return a sorted array of system template names
     */
    public static String[] loadSystemTemplateNames() {
        
        ArrayList list = new ArrayList();
        
        appendSystemTemplates(list);
        
        // return sorted list
        String[] array = (String[]) list.toArray(new String[0]);
        Arrays.sort(array);
        return array;
    }
    
    
    /**
     * Loads a sorted list of template names.
     * @return a sorted list of template names
     */
    public static String[] loadTemplateNames() {
        
        ArrayList list = new ArrayList();
        
        appendSystemTemplates(list);
        
        appendUserTemplates(list);
        
        // return sorted list
        String[] array = (String[]) list.toArray(new String[0]);
        Arrays.sort(array);
        return array;
    }
    
    /**
     * List system templates.
     * @param list a non-null list to append the names of system templates to
     */
    private static void appendSystemTemplates(ArrayList list) {
        
        Enumeration templateEnum = TexlipsePlugin.getDefault().getBundle().getEntryPaths(SYSTEM_TEMPLATES_DIR + "/");
        while (templateEnum.hasMoreElements()) {
            String path = (String) templateEnum.nextElement();
            if (path.endsWith(".tex")) {
                list.add(path.substring(path.lastIndexOf('/')+1, path.lastIndexOf('.')));
            }
        }
    }
    
    /**
     * List user-defined templates.
     * @param list a non-null list to append the names of user-defined templates to
     */
    private static void appendUserTemplates(ArrayList list) {
        
        File userTemplateFolder = getUserTemplateFolder();
        if (userTemplateFolder.exists()) {
            File[] templates = userTemplateFolder.listFiles();
            for (int i = 0; i < templates.length; i++) {
                String name = templates[i].getName();
                int index = name.lastIndexOf('.');
                String ext = (index > 0) ? name.substring(index+1) : "";
                if (ext != null && ext.length() > 0) {
                    name = name.substring(0, name.length() - ext.length() -1);
                }
                list.add(name);
            }
        }
    }
    
    /**
     * Find an existing template with the given name.
     * @param templateName Template name
     * @return True, if the given template already exists
     */
    public static boolean templateExists(String templateName) {
        // check extension
        int index = templateName.lastIndexOf('.');
        if (index > 0) {
            String ext = templateName.substring(index+1);
            if (!ext.equals("tex")) {
                templateName += ".tex";
            }
        } else {
            templateName += ".tex";
        }
        
        URL templ = TexlipsePlugin.getDefault().getBundle().getEntry(SYSTEM_TEMPLATES_DIR + "/" + templateName);
        if (templ != null) {
            return true;
        }
        File userTemplateFolder = getUserTemplateFolder();
        return new File(userTemplateFolder, templateName).exists();
    }
    
    /**
     * Returns the contents of the named user template.
     * 
     * @param name template name
     * @return contents of the template file
     * @throws IOException if an error occurs
     */
    public static byte[] readUserTemplate(String name) throws IOException {
        if (!name.endsWith(".tex")) {
            name += ".tex";
        }
        return readStream(new FileInputStream(new File(getUserTemplateFolder(), name)));
    }
    
    /**
     * Returns the contents of the named system template.
     * @param name template name
     * @return contents of the template
     * @throws IOException if an error occurs
     */
    public static byte[] readSystemTemplate(String name) throws IOException {
        if (!name.endsWith(".tex")) {
            name += ".tex";
        }
        
        URL templ = TexlipsePlugin.getDefault().getBundle().getEntry(SYSTEM_TEMPLATES_DIR + "/" + name);
        if (templ != null) {
            return readStream(templ.openStream());
        }
        return null;
    }
    
    /**
     * Reads the contents of the given stream and closes it.
     * 
     * @param in a stream to read in the data
     * @return the data
     * @throws IOException if an error occurs
     */
    protected static byte[] readStream(InputStream in) throws IOException {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buff = new byte[4096];
        
        int len = 0;
        while ((len = in.read(buff)) == buff.length) {
            out.write(buff);
        }
        
        if (len > 0) {
            out.write(buff, 0, len);
        }
        
        in.close();
        
        return out.toByteArray();
    }
    
    /**
     * @return the path to user templates
     */
    public static File getUserTemplateFolder() {
        IPath pluginStatePath = TexlipsePlugin.getDefault().getStateLocation().append(USER_TEMPLATES_DIR).addTrailingSeparator();
        return pluginStatePath.toFile();
    }
    
    /**
     * Saves the given file as a user template.
     * @param file the file to save as template
     * @param templateName name for the user template
     */
    public static void saveProjectTemplate(IFile file, String templateName) {
        
        IPath templateDir = TexlipsePlugin.getDefault().getStateLocation().append(USER_TEMPLATES_DIR);
        File dir = new File(templateDir.toOSString());
        if (!dir.exists()) {
            dir.mkdir();
        }
        
        // check extension
        int index = templateName.lastIndexOf('.');
        if (index > 0) {
            String ext = templateName.substring(index+1);
            if (!ext.equals("tex")) {
                templateName += ".tex";
            }
        } else {
            templateName += ".tex";
        }
        
        IPath dest = templateDir.append(templateName);
        
        try {
            copyFile(file.getLocation().toFile(), dest.toFile());
        } catch (IOException e) {
            TexlipsePlugin.log("Saving template", e);
        }
    }
    
    /**
     * Copies contents of a file to another. If destination exists, the file
     * is overwritten.
     * @param src source file
     * @param dest destination file
     */
    private static void copyFile(File src, File dest) throws IOException {
        
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dest);
        byte[] buff = new byte[4096];
        
        int len = 0;
        while ((len = in.read(buff)) == buff.length) {
            out.write(buff);
        }
        
        if (len > 0) {
            out.write(buff, 0, len);
        }
        
        out.close();
        in.close();
    }
    
    /**
     * Deletes a user template file.
     * @param name Name of the user template file
     */
    public static void deleteUserTemplate(String name) {
        
        if (name == null || name.length() == 0) {
            return;
        }
        
        IPath pluginStatePath = TexlipsePlugin.getDefault().getStateLocation().append(USER_TEMPLATES_DIR).addTrailingSeparator();
        File userTemplateFolder = pluginStatePath.toFile();
        if (userTemplateFolder.exists()) {
            
            File[] templates = userTemplateFolder.listFiles();
            for (int i = 0; i < templates.length; i++) {
                
                String fileName = templates[i].getName();
                if (fileName.startsWith(name)) {
                    
                    if (fileName.length() > name.length()) {
                        if (fileName.charAt(name.length()) == '.') {
                            templates[i].delete();
                            return;
                        }
                    } else if (fileName.equals(name)) {
                        templates[i].delete();
                        return;
                    }
                }
            }
        }
    }
}
