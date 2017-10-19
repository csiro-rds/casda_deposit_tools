CASDA Data Deposit
================

This project contains several Java command line tools that are be used to progress the deposit of ASKAP files produced by the RTC into the archive. They include the following:

* **observation_import** - parses an `observation.xml` metadata file and creates appropriate database records to represent the observation and its various artefacts (eg: image cubes).
* **fits_import** - extracts image metadata from an image cube datafile and updates the associated database record
* **catalogue_import** - extracts catalogue entries from a catalogue file and creates the appropriate records in the database. Types currently accepted:
	+ Continuum island
	+ Continuum component
	+ Spectral line absorption
	+ Spectral line emission
	+ Polarisation component
	+ Level 7 collections
	
* **stage_artefact** - copies artefacts from the RTC onto a 'staging' area on NGAS ready for the artefact to be 'registered' with NGAS
* **register_artefact** - takes an artefact in the NGAS 'staging' area and asks NGAS to put it under its management
* **rtc_notify** - 'notifies' the RTC that the deposit has completed (by writing a DONE file)


Setting up
----------

This project assumes that Eclipse is being used.  Having checked out the project from Stash into the standard project location (ie: 'C:\Projects\Casda'), you can import it into Eclipse as a Gradle project.  You will then need to right-click on the project and do Gradle->Disable Dependency Management, followed by a Gradle -> Refresh All (for some reason with Dependency Management enabled, the src/main/generated directory doesn't get included in the classpath).  

You will also need to add the src/generated folder to the build path by right clicking on the project, selecting Build Path -> Configure Build Path, then on the Source tab click Add Folder and select the src/generated folder.

Please then follow the instructions at [https://wiki.csiro.au/display/CASDA/CASDA+Application+setup+-+common] (https://wiki.csiro.au/display/CASDA/CASDA+Application+setup+-+common) to ensure that the code templates and code formatting rules are properly configured.

Make sure you have the Casda Postgres database installed locally, see the [casda_deposit_manager](https://stash.csiro.au/projects/CASDA/repos/casda_deposit_manager/browse) project for instructions.

### Cygwin

You may need to [Cygwin](https://www.cygwin.com/) as well as any required plugins (e.g curl, git, etc), as some of the local settings expect it to be present.

For more information about Cygwin and it's required plugins please visit [this page](https://wiki.csiro.au/display/CASDA/Development+Environment+setup#DevelopmentEnvironmentsetup-Cygwin)

Eclipse
-------

Once checked out, the code will have to be imported into eclipse as a gradle project

JUnit tests should also run in eclipse without anything needing to be done in Eclipse.


Running the Tests Locally
-------------------------

	> gradle clean test


Building and Installing Locally
-------------------------------

    > gradle clean deployToLocal

will deploy the command line tools to `build/install/data_deposit`.  Within that folder there will be a `bin` directory containing Windows and *nix version of each of the command line tools.

Running Locally
---------------

The directory `build/install/data_deposit/bin` contains runner-scripts for all of the command line tools.  The tools take various arguments - please use the `-help` option.  There are both Windows and *nix versions of the tools (`.bat` vs no extension respectively). 

If you want to run the each of the deposit tools manually you will first need to create the observation in the database by running `observation_import`.  Please note that the command line tools do not update the deposit state of any of the depositables so running the tools manually in sequence is not the same as letting the deposit manager run them (as the deposit state of the observation and the various artefacts will not be updated).

Please note that when running `stage_artefact` the default local behaviour will be to attempt to copy the artefact onto the Dev NGAS server using Putty's implementation of `scp`.  This will fail without the right password for the NGAS server on the Dev box - the password can be obtained from the IM&T CASDA infrastructure team.  It is highly recommended that you only modify that password in the locally installed version, ie editing `build/install/data_deposit/config/config/application.properties` (that way you will not erroneously commit the password to the application properties file).

Running Within Eclipse
----------------------

You will need to create specific "Run Configuration"s for each tool where you supply the command line parameters as you would running the script from the command line.  (All file paths *must* be *nix-style paths.)

Clearing Your Local Database
----------------------------

If you need a clean database (eg: loading the same datafile twice will result in an error due to a conflict with 
the same observation ID), then you can clear it by running the following task in the **casda_deposit_manager** project:

    > gradle flywayClean flywayMigrate
    
Querying NGAS
-------------

To query status of file in NGAS:

    > curl http://casda-d-app.pawsey.org.au:7777/STATUS?file_id=<file_id>

To retrieve file from NGAS:

    > curl http://casda-d-app.pawsey.org.au:7777/RETRIEVE?file_id=<file_id>

where the `file_id` is of the format:

    observations-<sbid>[-<collection name>]-<filename>

eg:

    observations-11111-observation.xml
    observations-11111-image_cubes-image1.fits
    observations-11111-catalogues-selavy-results.components.xml
    observations-11111-evaluation_files-evaluation1.pdf
    observations-11111-measurement_sets-ms.tar

Configuration
-------------

### Logging

Log4J2 is configured using a log4j2.xml file.  When running locally or on a server, this file is deployed into the distribution's config directory, which is in the classpath of the application startup script.  The server build/deploy process will install the log4j2.xml with a `.template` extension and will not overwrite the existing file.  The intention of this deploy process was to allow a deployer to diff between the the deployed log4j configuration file and the template file to resolve any updates required.  In practice this has rarely ever happened in our dev or test environments and we have now decided to use a single log configuration file common to local and server environments.  Consequently, in your local environment, you will see log4j configuration errors as the logging system tries to talk to syslog and all logs be written to:
> `/CASDA/application/casda_deposit_tools/logs/data_deposit.log`
(Incidentally, the local build/deploy process also creates the `.template` file (as it's part of the build) but then copies that over the actual file to make local changes easier.)

When running tests under Eclipse, the 'log4j2-test.xml' takes priority over the 'log4j2.xml' file.  The test configuration file only uses console logging.

### Application Properties

The system uses Spring Boot's mechanism for loading application properties.  When running locally or on a server, the properties are drawn from two locations:

    * lib/datadeposit-X.Y.jar:application.properties
    * config/config/application.properties

The first contains common system properties but is overridden by the second which contains local environment-specific properties.  As with the logging configuration, the local application.properties file is deployed as a template file which can be used to create/update the existing application.properties - and during local installation the template file is again copied over the actual file.

Test cases can also be configured to load application properties - see 'TestAppConfig.java' and also 'src/test/local/test_config/application.properties'.  This file is kept in a somewhat odd location to avoid Eclipse 'overwriting' the normal application.properties file in the build directory when it builds the project.


Datafile Constraints
====================

###SBID


The observation metadata file must have an SBID that matches the '-sbid' parameter and must be **unique** in the database.  The SBID is contained under the '<identity>' element:

    <identity>
        ...
        <sbid>12345</sbid>
        ...

###Catalogue Exists

catalogue_import checks that the file that is being loaded has a corresponding Catalogue record in the database, using
the filename as a key (the Catalogue record and its filename key are created by importing an observation metadata file).  The catalogue filename is relative to the observation folder.

The element in the file that creates the record is the <catalogue> element, eg:

    <catalogues>
        <catalogue>
            <filename>selavy-results.components.xml</filename>
            ...
            ...
        </catalogue>
    </catalogues>

###Catalogue Image Exists

catalogue_import also associates the catalogue with a specific image, as specified in the catalogue data file through the 'imageFile' PARAM, eg:

    <PARAM 
        name="imageFile" 
        ucd="meta.file;meta.fits" 
        datatype="char" 
        arraysize="60" 
        value="src/test/resources/image/good/validFile.fits"/>

The value of that PARAM must identify a matching Image record for the Observation.  Again, these records are created through elements in the observation metadata file, and are keyed by the image filename, eg:

    <images>
        <image>
            <filename>image.i.clean.restored.fits</filename>
            ...
        </image>
    </images>

The PARAM's imageFile and the image's filename must match exactly.

###Image Cube Exists

observation_import checks that the file mentioned in the <image> tags exists in the directory where the observation is
being ingested from. 

		<image>
			<filename>validFile.fits</filename>
          ...
		</image>

###Image Cube Project Matches

observation_import checks project id mentioned in the <image> tags matcher the project id in the header of the fits file

		<image>
			...
      		<project>AS007</project>
		</image>

###Checksum File Exists and matches NGAS Checksum

stage_artefact will check that a checksum exists for each file to be staged, saved in a checksum file named after the original file with a
'.checksum' suffix, e.g.

>observation.xml

>observation.xml.checksum

And that this checksum matches the one generated by NGAS.

Deployed Project Structure
--------------------------

The 'deployToLocal' gradle tasks deploys the application locally into the 'build/install/data_deposit' directory using the following project structure.  The 'publish' gradle tasks produce and publish a 'zip' version of this structure so server installations will use the same structure.  The 'publish' gradle task also produces a 'jar' distribution which is used by the casda_deposit_manager project.

**Note**: Configuration files for the application are kept out of any packaged libraries (jars/zips) so that they can be configured with environment-specific settings.  To avoid the destruction of such files during an in-place update, configuration files are stored as 'templates' which must be copied after (initial) installation to create the actual configuration file. For local installations, this is handled by the 'installConfigurationFiles' gradle task, which is a dependency of 'deployToLocal'. For a server installation, see the 'deploy' gradle file.

    '-- data_deposit
        |-- bin                             scripts to run the tools go here
        |   |-- tool_name                   *nix script
        |   |-- tool_name.bat               windows script
        |   |-- ...
        |   |-- ...
        |   '-- ...
        |-- config                          project-specific configuration files go here
        |   |-- log4j2.xml                  local version of the log4j configuration file
        |   |-- log4j2.xml.template         distributed template for the log4j 
        |   |                               configuration file
        |   |-- ...
        |   |-- ...
        |-- '-- config                                  Spring Boot local 
        |       |                                       application.properties 
        |       |                                       sub-directory
        |       |-- application.properties              local version of the
        |       |                                       application.properties file
        |       |-- application.properties.template     distributed template for the 
        |       |                                       application.properties file
        |       |-- ...
        |       |-- ...
        |       '-- ...
        |-- lib                             all libs go here
        |   |-- lib1.jar
        |   |-- lib2.jar
        |   |-- ...
        |   |-- ...
        |   '-- ...
        '-- README.md                       project readme (yes, you're reading it!)
  
        
Updates the license header of the current project source files
--------------------------------------------------------------
Change the relevant value in pom.xml then run this command in command prompt:

$ mvn license:update-file-header
