# Eclipse TeXlipse™
The TeXlipse plugin for Eclipse provides support for LaTeX projects. It is primarily aimed at users who already know the basics about LaTeX, newbies will surely find it useful but also a steeper learning curve. The following main features are offered:

* Syntax highlighting
* Document outline
* Code folding
* Templates
* Build support, also partial building
* Annotations for errors (while editing)
* Content assist (completion of commands and references)
* Easy navigation with F3
* Outline of the current file and the full project
* Spell checking
* Menu with common LaTeX math symbols
* BibTeX editor and BibTeX-support
* BibLaTeX support
* Line wrapping
* Table editor
* Support for several platforms (Windows, Linux, OS X)
* Optional: Live PDF preview with [Pdf4Eclipse](http://borisvl.github.com/Pdf4Eclipse "Pdf4Eclipse")  
* Optional: [Bibsonomy](http://www.bibsonomy.org/ "Bibsonomy")  integration with  [TeXlipseBibSonomyExtension](http://www.bibsonomy.org/help_en/TeXlipseBibSonomyExtension "TeXlipseBibSonomyExtension") 
* ...and many more

In other words, TeXlipse includes quite a complete set of features for day-to-day editing tasks. This manual explains the use of these features in detail, but please go ahead and explore TeXlipse!

# Getting Started

## Setting up a TeXlipse development environment

We are using Eclipse as development IDE and using the latest 2020-20 release as TeXlipse target platform.

### Using the TeXlipse development Oomph setup

This approach allows you to start almost from scratch (with JDK 11 (or newer) and a Git client installed already), and to obtain a ready-for-use TeXlipse development environment including :
* an OSGi / Eclipse RCP development environment
* plugins to work with [target platform definitions](https://github.com/mbarbero/fr.obeo.releng.targetplatform/).
* Java code formatting preferences as defined in our [Coding Style](https://wiki.eclipse.org/Triquetrum/Coding_Style)

This involves following steps :

1. Download and start the [Eclipse Installer](http://www.eclipse.org/downloads/eclipse-packages/).
2. Within the wizard switch to _advanced mode_ by selecting it in the burger menu of the upper right corner, then select your desired Eclipse installation package e.g. _Eclipse IDE for Eclipse Committers_ from the product list, set your current Java installation folder as well as 32 Bit or 64 Bit dependent on your Java installation and click next.
3. Select _Eclipse Texlipse_ from the list of projects and click next.
4. Within the _Variables_ window activate _Show all variables_ in the bottom left corner. In the appearing fields, set your desired _Installation folder name_ as well as your _Root install folder_, click next and confirm the listed execution tasks by pressing the finish button. Let the installer do its thing to download the Eclipse IDE package and extra plugins and to start your new Eclipse instance.
6. When Eclipse is starting, you will see a progress indication at the bottom right for the startup tasks of the Oomph setup for TeXlipse. This will clone the TeXlipse repository.
7. In case your _project presentation_ inside your _Project Explorer_ is set to _flat_, there should be four projects within your IDE. Open the _default.target_ inside your _texlipse_ project and _update_ the plugins listed there. This will take a while. After it has been updated successfully press _Set as Active Target Platform_ and save the file.
8. To start an Texlipse instance e.g. for testing changes, open the _build.properties_ inside the _org.eclipse.texlipse_ project and press the play button (_Launch an Eclipse application_) in the upper right corner. A new Eclipse instance will be started, where you can switch to the _LaTeX_ perspective.

### Manual setup

Use Eclipse to check out the repository.

1. Install [Eclipse IDE for Committers](http://www.eclipse.org/downloads/). Other versions may work.
2. Start Eclipse. Create a new workspace.  You may need to close the Welcome tab by clicking on the X.
3. Open **Window > Show View > Other > Git > Git Repositories**
4. Click on **Clone a Git repository**
5. Click on **Clone URI**, then **Next**
6. Enter the URI ``https://github.com/eclipse/texlipse/``
7. In the Branch Selection window, keep the default of the Master branch and click Next.
8. In the Local Destination window, select **Finish**.

## Build your own Eclipse TeXlipse with the TeXlipse development environment

1. Run the _texlipse/pom.xml_ as Maven build by setting the goal to _clean install_. Within the other projects of the workspace a target folder will be created. In case of an error concerning the _org.eclipse.license.feature.group_ please update the version number provided in the _source_ tab of the _default.target_.
2. The Maven build creates an update site, which can be used within a fresh Eclipse installation e.g. _Eclipse IDE for Eclipse Committers_. Open the _Install new software_ site and add the file _.../texlipse/org.eclipse.texlipse-site/target/org.eclipse.texlipse-2.0.3.201911021750.zip_ as new repository.
3. Install the new software _LaTeX support for Eclipse_. After Eclipse restart you can switch to the _LaTeX_ perspective.

# Resources

* The [official TeXlipse project page](https://projects.eclipse.org/projects/science.texlipse "TeXlipse Project Page")
* The [Eclipse Science](https://science.eclipse.org "Eclipse Science Working Group") working group
* [Contribution guide](CONTRIBUTING.md)

# License

EPL v1.0 see [LICENSE](LICENSE) file.

---
_Eclipse TeXlipse, Eclipse, the Eclipse logo, and the Eclipse TeXlipse project logo are either registered trademarks or trademarks of The Eclipse Foundation in the United States and/or other countries._

