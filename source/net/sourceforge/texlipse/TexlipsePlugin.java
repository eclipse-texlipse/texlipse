/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse;

import java.io.IOException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sourceforge.texlipse.bibeditor.BibCodeScanner;
import net.sourceforge.texlipse.bibeditor.BibColorProvider;
import net.sourceforge.texlipse.bibeditor.BibEntryScanner;
import net.sourceforge.texlipse.properties.StringListFieldEditor;
import net.sourceforge.texlipse.templates.BibTexContextType;
import net.sourceforge.texlipse.templates.TexContextType;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The main plugin class to be used in the desktop.
 * 
 * @author Kimmo Karlsson
 * @author Esa Seuranen
 * @author Taavi Hupponen
 * @author Oskar Ojala
 * @author Tor Arne Vestbø
 */
public class TexlipsePlugin extends AbstractUIPlugin {
    
    //Plugin_ID
    private static final String PLUGIN_ID = "net.sourceforge.texlipse";
    private static final String ICONS_PATH = "icons/";
    
    // Key to store custom templates. 
    private static final String CUSTOM_TEMPLATES_TEX_KEY = "TeXTemplates";
    private static final String CUSTOM_TEMPLATES_BIBTEX_KEY = "BiBTeXTemplates";
    
    // the shared instance.
    private static TexlipsePlugin plugin;
    
    // resource bundle.
    private ResourceBundle resourceBundle;
    
    /// The template stores. 
    private TemplateStore texTemplateStore;
    private TemplateStore bibtexTemplateStore;
    
    // The context type registrys. 
    private ContributionContextTypeRegistry texTypeRegistry = null;
    private ContributionContextTypeRegistry bibtexTypeRegistry = null;
    
    // BibEditor presentation reconciler resources that are shared
    private BibColorProvider bibColor;
    private BibCodeScanner bibCodeScanner;
    private BibEntryScanner bibEntryScanner;
    
    // used by the current project solving mechanism
    protected static IWorkbenchWindow currentWindow;
    
    /**
     * Constructs a new TeXlipse plugin.
     */
    public TexlipsePlugin() {
        super();
        plugin = this;
        
        // Force construction, so that editors from the last
        // eclipse session are caught by the change listener
        SelectedResourceManager.getDefault();
        
        try {
            resourceBundle = ResourceBundle.getBundle(getClass().getPackage().getName() + ".TexlipsePluginResources");
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }        
    }
    
    /**
     * This method is called upon plug-in activation
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
    }
    
    /**
     * This method is called when the plug-in is stopped
     */
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
    }
    
    /**
     * Returns the shared instance.
     */
    public static TexlipsePlugin getDefault() {
        return plugin;
    }
    
    /**
     * Returns the string from the plugin's resource bundle,
     * or <code>key</code> if not found.
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = getDefault().getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }
    
    /**
     * Return a value from the plugin's preferences.
     * @param key preference name
     * @return value of the named preference
     */
    public static String getPreference(String key) {
        return getDefault().getPreferenceStore().getString(key);
    }
    
    /**
     * Return a value from the plugin's preferences as an array.
     * This method always returns a non-null value.
     * @param key preference name
     * @return an array of strings or an empty array
     */
    public static String[] getPreferenceArray(String key) {
        String g = getDefault().getPreferenceStore().getString(key);
        if (g == null) {
            return new String[0];
        }
        return g.split(StringListFieldEditor.SEPARATOR);
    }
    
    /**
     * Return an image from the plugin's icons-directory.
     * @param name name of the icon
     * @return the icon as an image object
     */
    public static Image getImage(String name) {
        return getDefault().getCachedImage(name);
    }
    
    /**
     * Cache the image if it is found.
     * @param key name of the image
     * @return image from the cache or from disk, null if image is not found in either
     */
    protected Image getCachedImage(String key) {
        if (key == null) {
            return null;
        }
        Image g = getImageRegistry().get(key);
        if (g != null) {
            return g;
        }

        ImageDescriptor d = getImageDescriptor(key);
        if (d == null ) {
            return null;
        }

        // we want null instead of default missing image
        if (d.equals(ImageDescriptor.getMissingImageDescriptor())) {
            return null;
        }

        g = d.createImage();
        getImageRegistry().put(key, g);
        return g;
    }
    
    /**
     * Get the workbench image with the given path relative to
     * ICON_PATH.
     * @param relativePath
     * @return ImageDescriptor
     */
    public static ImageDescriptor getImageDescriptor(String relativePath){
        return imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH + relativePath + ".gif");
    }
    
    /**
     * Returns the plugin's resource bundle,
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }
    
    /**
     * Returns the reference to the project that owns the
     * file currently open in editor.
     * @return reference to the currently active project 
     */
    public static IProject getCurrentProject() {
        IWorkbenchPage page = TexlipsePlugin.getCurrentWorkbenchPage();
        IEditorPart actEditor = null;
        if (page.isEditorAreaVisible()
             && page.getActiveEditor() != null) {
            actEditor = page.getActiveEditor();
        }
        else {
            return null;
        }
        IEditorInput editorInput = actEditor.getEditorInput();
        
        IFile aFile = (IFile)editorInput.getAdapter(IFile.class);
        if (aFile != null) return aFile.getProject();
        // If the first way does not gonna work...
        // actually this returns the file of the editor that was last selected
        IResource res = SelectedResourceManager.getDefault().getSelectedResource();
        return res == null ? null : res.getProject();
    }
    
    /**
     * Returns the current workbench page.
     * 
     * Used by getCurrentProject() and by gotoMarker().
     * @return the currently open WorkbenchPage or <code>null</code> if none
     */
    public static IWorkbenchPage getCurrentWorkbenchPage() {
        IWorkbench workbench = getDefault().getWorkbench();
        
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window == null) {
            Display display = workbench.getDisplay();
            display.syncExec(new Runnable() {
                public void run() {
                    currentWindow = TexlipsePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
                }});
            window = currentWindow;
        }
        
        return window.getActivePage();
    }
    
    /**
     * Returns the name of the plugin.
     * 
     * Used by project creation wizard.
     * @return unique id of this plugin
     */
    public static String getPluginId() {
        return getDefault().getBundle().getSymbolicName();
    }       
    
    /**
     * Create ae error-level status object out of textual message.
     * These status-objects are needed when creating new CoreExceptions.  
     * Used by e.g. the project creation wizard.
     * @param msg error message to display in error log
     * @param t exception
     * @return new error-level status message 
     */
    public static IStatus stat(String msg, Throwable t) {
        return new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, msg, t);
    }
    
    /**
     * This is equivalent to calling <code>stat(msg, null)</code>.
     * @param msg error message to display in error log
     * @return new error-level status message 
     */
    public static IStatus stat(String msg) {
        return stat(msg, null);
    }
    
    /**
     * Display a message in the Eclipse's Error Log.
     * This is equivalent to calling <code>log(msg, t, IStatus.ERROR)</code>.
     * @param msg error message to display in error log
     * @param t exception
     */
    public static void log(String msg, Throwable t) {
        log(msg, t, IStatus.ERROR);
    }
    
    /**
     * Display a message in the Eclipse's Error Log.
     * Used by e.g. the project creation wizard.
     * @param msg error message
     * @param t exception
     * @param level one of the error levels defined in the <code>IStatus</code> -interface
     */
    public static void log(String msg, Throwable t, int level) {
        IStatus stat = new Status(level, getPluginId(), level, msg, t);
        getDefault().getLog().log(stat);
    }
    
    /**
     * Returns this plugin's TeX template store.
     * 
     * @return the template store of this plug-in instance
     */
    public TemplateStore getTexTemplateStore() {
        if (texTemplateStore == null) {
            texTemplateStore = new ContributionTemplateStore(getTexContextTypeRegistry(),
                    getPreferenceStore(),
                    CUSTOM_TEMPLATES_TEX_KEY);
            try {
                texTemplateStore.load();
            } catch (IOException e) {
                //e.printStackTrace();
                TexlipsePlugin.log("Loading TeX template store", e);
                throw new RuntimeException(e);
            }
        }
        return texTemplateStore;
    }
    
    /**
     * Returns this plug-in's BiBTeX template store.
     * 
     * @return the template store of this plug-in instance
     */
    public TemplateStore getBibTemplateStore() {
        if (bibtexTemplateStore == null) {
            bibtexTemplateStore = new ContributionTemplateStore(getBibContextTypeRegistry(),
                    getPreferenceStore(),
                    CUSTOM_TEMPLATES_BIBTEX_KEY);
            try {
                bibtexTemplateStore.load();
            } catch (IOException e) {
                //e.printStackTrace();
                TexlipsePlugin.log("Loading BibTeX template store", e);
                throw new RuntimeException(e);
            }
        }
        return bibtexTemplateStore;
    }
    
    /**
     * Returns this plugin's LaTeX context type registry.
     * 
     * @return the context type registry for this plug-in instance
     */
    public ContextTypeRegistry getTexContextTypeRegistry() {
        if (texTypeRegistry == null) {
            // create an configure the contexts available in the template editor
            texTypeRegistry = new ContributionContextTypeRegistry();
            texTypeRegistry.addContextType(TexContextType.TEX_CONTEXT_TYPE);
            texTypeRegistry.addContextType(TexContextType.MATH_CONTEXT_TYPE);
        }
        return texTypeRegistry;
    }
    
    /**
     * Returns this plugin's BibTeX context type registry.
     * 
     * @return the context type registry for this plug-in instance
     */
    public ContextTypeRegistry getBibContextTypeRegistry() {
        if (bibtexTypeRegistry == null) {
            // create an configure the contexts available in the template editor
            bibtexTypeRegistry = new ContributionContextTypeRegistry();
            bibtexTypeRegistry.addContextType(BibTexContextType.BIBTEX_CONTEXT_TYPE);
        }
        return bibtexTypeRegistry;
    }
    
    /**
     * @return The BibTeX editor color provider.
     */
    public BibColorProvider getBibColorProvider() {
        if (bibColor == null) {
            bibColor = new BibColorProvider();
        }
        return bibColor;
    }
    /**
     * @return Returns the bibCodeScanner.
     */
    public BibCodeScanner getBibCodeScanner() {
        if (bibCodeScanner == null) {
            bibCodeScanner = new BibCodeScanner(getBibColorProvider());
        }
        return bibCodeScanner;
    }
    /**
     * @return Returns the bibEntryScanner.
     */
    public BibEntryScanner getBibEntryScanner() {
        if (bibEntryScanner == null) {
            bibEntryScanner = new BibEntryScanner(getBibColorProvider());
        }
        return bibEntryScanner;
    }
}
