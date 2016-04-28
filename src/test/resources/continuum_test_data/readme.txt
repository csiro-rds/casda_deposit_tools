You run this test data using the observation_import and continuum_import tools as described in the /README.md.

However note that the path to the ref_xy_votable.xml needs to be the same as what is in the metadata_xy.xml file 
so the votable files might need to be moved to a 'data' directory or the reference updated if neither of these 
steps has been done.

There is a perl script to clean up the data.  Its in here as data_deposit_cleanUp.pl.  See https://jira.csiro.au/browse/CASDA-2063.

And a spreadsheet used to generate the test data.  It is RADecToDegrees_TestData.xlsx and also see https://jira.csiro.au/browse/CASDA-2063 for more information.
