#############################################################################################
#
# Python script to convert ATLAS Tables 4 and 6 from ASCII format
# into VOTable format and add decimal equatorial position fields. 
#
# Author: James Dempsey on 10 Dec 2015
#
# Note: Required PARAMs and DESCRIPTION must be added to the votable files after conversion. 
#       The ID fields must also be renamed and have a UCD of "meta.id;meta.main" set  
#
#############################################################################################

from astropy.io import ascii
from astropy.io.votable.tree import Field
from astropy.io.votable import from_table
from astropy.coordinates import SkyCoord
import numpy as np
from numpy.lib import recfunctions
import re

TargetFolder = 'C:\\temp\\'
BaseT4Filename = TargetFolder + "CDFS_published_Table4"
BaseT6Filename = TargetFolder + "CDFS_published_Table6"

def sanitiseColNames(table):
	"Ensure the column names have only allowed characters"
	print "Original column names:", table.colnames
	allowedName = re.compile("^[A-Za-z0-9_]+$")
	invalidChars = re.compile("[^A-Za-z0-9_]")
	for name in table.colnames:
		if (name == "DE-"):
			table.rename_column(name, "DE_sign")
		elif (not(allowedName.match(name))):
			newName = invalidChars.sub("_", name)
			table.rename_column(name, newName)
	print "Final column names:   ", table.colnames

	
def addCoords(table):
	"Take the fragmented ra and dec and create a decimal ra and dec field pair for each row"
	print "Adding coords to table"
	print "Original first table row: ", table.array[0]
	#print table.array['RAh']
	raField = Field(votable, name="ra_deg", datatype="float",precision="6", unit="deg",ucd="pos.eq.ra;meta.main",width="12")
	decField = Field(votable, name="dec_deg", datatype="float",precision="6", unit="deg",ucd="pos.eq.dec;meta.main",width="13")
	raVal = np.zeros(table.array.size)
	decVal = np.zeros(table.array.size)
	table.array = recfunctions.append_fields(table.array, "ra_deg", raVal)
	table.array = recfunctions.append_fields(table.array, "dec_deg", decVal)
	table.fields.append(raField)
	table.fields.append(decField)
	
	#print table.array[0]
	for row in table.array:
		raStr = "%sh%sm%ss" % (row['RAh'], row['RAm'], row['RAs'])
		decStr = "%s%sd%sm%ss" % (row['DE_sign'], row['DEd'], row['DEm'], row['DEs'])
		coord=SkyCoord(raStr, decStr, frame='icrs')
		row['ra_deg']=coord.ra.deg
		row['dec_deg']=coord.dec.degree
		
	print "Final first table row: ", table.array[0]

# Table 4
print "------ Starting table 4 ----------"
SourceFilename = BaseT4Filename+".txt"
DestFilename = BaseT4Filename+".xml"
data = ascii.read(SourceFilename)
sanitiseColNames(data)
print data
votable = from_table(data)
votable.version = 1.3
addCoords(votable.get_first_table())
for field in votable.get_first_table().fields:
	print "Field %s %s %s %s %s" %(field.ID, field.name, field.datatype, field.width, field.ucd)
votable.to_xml(DestFilename)
print "Written to ", DestFilename


# Table 6
print "\n\n------ Starting table 6 ----------"
SourceFilename = BaseT6Filename+".txt"
DestFilename = BaseT6Filename+".xml"
data = ascii.read(SourceFilename)
sanitiseColNames(data)
print data
votable = from_table(data)
votable.version = 1.3
addCoords(votable.get_first_table())
for field in votable.get_first_table().fields:
	print "Field %s %s %s %s %s" %(field.ID, field.name, field.datatype, field.width, field.ucd)
votable.to_xml(DestFilename)
print "Written to ", DestFilename

