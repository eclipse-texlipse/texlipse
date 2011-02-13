/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.wizards;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

/**
 * Simple attribute container class.
 * This class contains all the values that can be changed in
 * the new project wizard.
 * 
 * @author Kimmo Karlsson
 */
public class TexlipseProjectAttributes {

    private String projectName;
    private String outputDir;
    private String sourceDir;
    private String tempDir;
    private String sourceFile;
    private String outputFile;
    private String template;
    private String outputFormat;
    private int builder;
    private String projectLocation;
    private String languageCode;

    /**
     * 
     */
    public TexlipseProjectAttributes() {
        projectName = "";
        outputDir = "";
        sourceDir = "";
        tempDir = "tmp";
        sourceFile = "document.tex";
        outputFormat = TexlipsePlugin.getPreference(TexlipseProperties.OUTPUT_FORMAT);
        outputFile = sourceFile.substring(0, sourceFile.lastIndexOf('.')+1) + outputFormat;
        template = "";
        languageCode = "en";
        builder = TexlipsePlugin.getDefault().getPreferenceStore().getInt(TexlipseProperties.BUILDER_NUMBER);
    }

    public String getOutputDir() {
        return outputDir;
    }
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }
    public String getOutputFile() {
        return outputFile;
    }
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }
    public String getProjectName() {
        return projectName;
    }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    public String getSourceDir() {
        return sourceDir;
    }
    public void setSourceDir(String sourceDir) {
        this.sourceDir = sourceDir;
    }
    public String getSourceFile() {
        return sourceFile;
    }
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }
    public String getTempDir() {
        return tempDir;
    }
    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }
    public String getTempFile() {
        return sourceFile.substring(0, sourceFile.lastIndexOf('.')+1) + TexlipseProperties.OUTPUT_FORMAT_AUX;
    }
    public String getTemplate() {
        return template;
    }
    public void setTemplate(String template) {
        this.template = template;
    }
    public String getOutputFormat() {
        return outputFormat;
    }
    public void setOutputFormat(String selectedFormat) {
        this.outputFormat = selectedFormat;
    }
    public int getBuilder() {
        return builder;
    }
    public void setBuilder(int selectedBuilder) {
        this.builder = selectedBuilder;
    }
    public String getProjectLocation() {
        return projectLocation;
    }
    public void setProjectLocation(String text) {
        this.projectLocation = text;
    }
    public void setLanguageCode(String text) {
        this.languageCode = text;
    }
    public String getLanguageCode() {
        return this.languageCode;
    }
}
