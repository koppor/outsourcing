# Source Outsourcing with Process Views

This repository contains the implementation of the paper
"[Source Outsourcing with Process Views](http://doi.ieeecomputersociety.org/10.1109/TSC.2013.51)"
by [Rik Eshuis](http://orcid.org/0000-0003-2314-7155), [Alex Norta](https://www.researchgate.net/profile/Alex_Norta), [Oliver Kopp](http://orcid.org/0000-0001-6962-4290) and [Esa Pitkänen](http://orcid.org/0000-0002-9818-6370).

## Repository organization

* `matching`: main service outsourcing code
* `matching.application`: configuration to launch the application
* `processes`: example processes

## Requirements

Eclipse Juno.

Download [Eclipse IDE for Java EE Developers](http://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/junosr2).

## Installation steps

### Overview

1. Fetch libraries
1. Import projects into Eclipse workspace
1. Create new empty workspace
1. Fetch Eclipse BPEL Designer Model
1. Fetch BPEL model utilities

### Fetch libraries

This is currently not required. We checked in the libraries in the version control as [git-annex](http://git-annex.branchable.com/) currently [has problems with web sources](http://git-annex.branchable.com/bugs/__34__fatal:_bad_config_file__34__/).

### Import project into Eclipse workspace

1. Import projects `matching`, `matching application`, and `processes` via Import / Existing Projects into Workspace

### Fetch Eclipse BPEL Designer Model

1. Right click on empty space in Package Explorer
1. Import...
1. Git/Projects from Git
2. Next
3. Select "URI"
3. Next
4. URI: git://git.eclipse.org/gitroot/bpel/org.eclipse.bpel.git
5. Next
6. Deselect all except `master`
7. Next
8. Leave "Directory" as is
9. Next
10. "Import existing projects"
11. Next
11. Deselect all except `org.eclipse.bpel`, `org.eclipse.bpel.common.model`, `org.eclipse.bpel.model`
12. Finish

### Fetch BPEL model utilities

1. Right click on empty space in Package Explorer
1. Import...
1. Git/Projects from Git
2. Next
3. Select "URI"
3. Next
4. URI: git://github.com/IAAS/BPEL-model-utilities.git
5. Next
7. Next
8. Leave "Directory" as is
9. Next
10. "Import existing projects"
11. Next
12. Finish
13. In case the "de.uni_stuttgart.iaas.bpel.model.utilities" project refers to a wrong jdk:
 * Right click on it
 * Properties
 * "Java Build Path"
 * "Libraries"
 * Click on the faulty entry
 * Edit
 * Select "Workspace default JRE (jre7)"


## Running the application

1. Run / Run Configurations
2. Select Eclipse Application / "Matching Application - EG2007 - Example - Req and Offer 1" 
3. Click "Run"
4. Now, the selected application is also shown in the Run menu.

### In case it does not launch

1. Run / Run Configurations
2. Select Eclipse Application / "Matching Application - WaterTank - match OEM and Cluster A"
3. Tab "Plug-ins"
4. Click on "Add Required Plug-ins"
5. Click on "Validate Plug-ins"
6. "No problems were detected" should be shown
7. Click "Apply"
8. Click "Run"

**javax.wsdl is required in version 1.5.x**. Ensore that javax.wsdl is **not** loaded in version 1.6.x.

## License

SPDX: `(Apache-2.0 AND GPL-3.0+)`

This work is licensed under the Apache License 2.0.
Currently, this work is *additionally* covered by the GNU Public License Version 3.0.
As soon as the class [`com.jopdesign.wcet.graphutils.Dominators<V, E>`](https://github.com/koppor/outsourcing/blob/master/outsourcing/src/com/jopdesign/wcet/graphutils/Dominators.java) is replaced by a non-GPL version, the license of is Apache 2.0 only.

This work includes `org.eclipse.ui.internal.ide.misc.DisjointSet<Element>`, which is licensed under the [Eclipse Public License v1.0](http://www.eclipse.org/legal/epl-v10.html).
