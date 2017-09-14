# Guide to contributing

Please read this if you intend to contribute to the project.

## Legal stuff

Apologies in advance for the extra work required here - this is necessary to comply with the Eclipse Foundation's strict IP policy.

Please also read [Contributing via Git](http://wiki.eclipse.org/Development_Resources/Contributing_via_Git) (Eclipse Wiki).

In order for any contributions to be accepted you MUST do the following things.

* Sign the [Eclipse Foundation Contributor License Agreement](http://www.eclipse.org/legal/CLA.php).
To sign the Eclipse CLA you need to:

  * Obtain an Eclipse Foundation userid. Anyone who currently uses Eclipse Bugzilla or Gerrit systems already has one of those. If you don’t, you need to [register](https://dev.eclipse.org/site_login/createaccount.php).

  * Login into the [projects portal](https://projects.eclipse.org/), select “My Account”, and then the “Contributor License Agreement” tab.

* Add your github username in your Eclipse Foundation account settings.

* "Sign-off" your commits


Every commit you make in your patch or pull request MUST be "signed off".

You do this by adding the `-s` flag when you make the commit(s), e.g.

    git commit -s -m "Shave the yak some more"

## Making your changes

### Setting up a TeXlipse development environment

We are using Eclipse Oxygen as development IDE and using the latest Oxygen release as TeXlipse target platform.

#### Using the TeXlipse development Oomph setup

This approach allows you to start almost from scratch (with a JDK 1.8 and a Git client installed already), and to obtain a ready-for-use TeXlipse development environment including :
* an OSGi / Eclipse RCP development environment
* plugins to work with [target platform definitions](https://github.com/mbarbero/fr.obeo.releng.targetplatform/).
* Java code formatting preferences as defined in our [Coding Style](https://wiki.eclipse.org/Triquetrum/Coding_Style)

This involves following steps :

1. Download and start the [Eclipse Installer](http://www.eclipse.org/downloads/eclipse-packages/)
2. Select your desired **Oxygen** Eclipse installation package (typically *Eclipse IDE for Eclipse Committers*) and click next
3. Paste the [URL to the setup file](https://raw.githubusercontent.com/eclipse/texlipse/master/EclipseTexlipse.setup) as a user project setup in your installer, under the Github projects section. (TODO : we need to add our setup to the standard list at Eclipse)
4. Select the "TeXlipse development" setup and click next
5. Check and adapt the setup variables for e.g. root installation folder, git clone location, github account info. Click next and let the installer do its thing to download the Eclipse IDE package and extra plugins and to start your new Eclipse instance.
6. When Eclipse is starting, you will see a progress indication at the bottom right for the startup tasks of the Oomph setup for TeXlipse. This will clone the TeXlipse repository, apply default Java code formatting preferences, and set the target platform.
7. Wait some minutes (resolving and setting the target can take some time) and you're all set to start contributing to TeXlipse!

#### Manual setup

Use Eclipse to check out the repo.

1. Install [Eclipse IDE for Committers](http://www.eclipse.org/downloads/). Other versions may work. These instructions were tested using Neon under Mac OS X and Windows 10.
2. Start Eclipse. Create a new workspace.  You may need to close the Welcome tab by clicking on the X.
3. Open **Window > Show View > Other > Git > Git Repositories**
4. Click on **Clone a Git repository**
5. Click on **Clone URI**, then **Next**
6. Enter the URI ``https://github.com/eclipse/texlipse/``
7. In the Branch Selection window, keep the default of the Master branch and click Next.
8. In the Local Destination window, select **Finish**.

### Create an Issue
Create a [GitHub Issue](https://github.com/eclipse/texlipse/issues) for every significant piece of work ( > 2 hrs).

### Create a new branch for your changes

1. In the Git Repositories tab, expand the triquetrum repository.
2. Right click on the "Branches" node and select "Switch To" -> "New Branch".  
3. Enter the new branch name.  
Branch name should be {GitHubUserName}/{summary or issue id} e.g. ``erwin/integrate-display-actor``.  
Alternative idea is a bit more elaborated : {GitHubUserName}/{ChangeType}/{issue id}/{summary} e.g. ``jake/ft/5/integrate-display-actor``. In this approach change type acronyms can be e.g. ft (feature i.e. with functional value) ; eh (enhancement without functional value) ; bg (bug) ; doc ; ...

### Committing
* Make your changes.
* Make sure you include tests.
* Make sure the test suite passes after your changes.
* Commit your changes into that branch. 
* For files that are in Eclipse packages, right click on the file in the Package Explorer and commit it.  
* For files that are not in Eclipse packages, invoke the Git Staging via Window -> Show View -> Other -> Git -> Git
* Use descriptive and meaningful commit messages. See [git commit records in the Eclipse Project Handbook](https://www.eclipse.org/projects/handbook/#resources-source).  Mention issue_id in each commit comment using syntax like "Adapt this interface for #15" to link to issue 15.
* Make sure you use the sign off your commit.
  * If you are commiting using Eclipse, then click on the signature button  
  * If you are invoking git from the command line, then use the `-s` flag.  
  * If you are using some other tool, add ``Signed-off-by: YourFirstName YourLastName <YourEclipseAccountEmailAddress>`` For example: ``Signed-off-by: Christopher Brooks <cxh@eecs.berkeley.edu>``
* Push your changes to your branch in your forked repository.

## Submitting the changes

1. Submit a pull request via the normal [GitHub UI](https://github.com/eclipse/texlipse) to trigger to request feedback / code review / ... 
2. Mention issue_id in each comment using syntax like "Adapt this interface for #15" to link to issue 15 in the initial comment for a Pull Request.
3. Pay attention to the "Jenkins Build" check, if it fails you will find more information following the "details" link leading to the [Eclipse Texlipse Jenkins instance](https://ci.eclipse.org/texlipse/).
4. The pull request will be reviewed by one of the committers, and then merged into master.
 
## After submitting

* Do not use your branch for any other development, otherwise further changes that you make will be visible in the PR.

# Credit

This file is based on a file written by the Vert.x team at https://raw.githubusercontent.com/eclipse/vert.x/master/CONTRIBUTING.md

We have shamelessly copied, modified and co-opted it for our own repo and we graciously acknowledge the work of the original authors.
