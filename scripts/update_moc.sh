#!/bin/bash
#
# Purpose: bash script for updating the project and global coverage maps with the footprint of a fits image.
# Author: James Dempsey <james.dempsey@csiro.au>

if [ "$#" -ne 2 ]; then
    echo "Usage: ${0} imageFile projectCode"
    exit 1
fi

PATH=/CASDA/application/casda_deposit_tools/../shared_tools/bin:$PATH
ALADIN="/CASDA/application/shared_tools/AladinBeta.jar"

IN_FILE="$1"
PROJECT="$2"
MOC_BASE="/ASKAP/archive/dev/vol002/maps/active/"
PROJ_MOC_FILE="${MOC_BASE}${PROJECT}/Moc.fits"
PROJ_MOC_PREVIEW="${MOC_BASE}${PROJECT}/Moc_preview.png"
ASKAP_MOC_FILE="${MOC_BASE}ASKAP/Moc.fits"
ASKAP_MOC_PREVIEW="${MOC_BASE}ASKAP/Moc_preview.png"

# Ensure the folders exist
if [ ! -e ${MOC_BASE}${PROJECT} ]
then
mkdir ${MOC_BASE}${PROJECT}
fi

# Create or update the moc files
if [ -f $PROJ_MOC_FILE ]
then
java -Xmx4000m -jar $ALADIN -mocgen in=$IN_FILE out=${PROJ_MOC_FILE}.tmp previous=$PROJ_MOC_FILE
rm $PROJ_MOC_FILE
mv ${PROJ_MOC_FILE}.tmp $PROJ_MOC_FILE
else
java -Xmx4000m -jar $ALADIN -mocgen in=$IN_FILE out=${PROJ_MOC_FILE}
fi

if [ -f $ASKAP_MOC_FILE ]
then
java -Xmx4000m -jar $ALADIN -mocgen in=$IN_FILE out=${ASKAP_MOC_FILE}.tmp previous=$ASKAP_MOC_FILE
rm $ASKAP_MOC_FILE
mv ${ASKAP_MOC_FILE}.tmp $ASKAP_MOC_FILE
else
java -Xmx4000m -jar $ALADIN -mocgen in=$IN_FILE out=${ASKAP_NOC_FILE}
fi


# Produce preview images of both mocs
/CASDA/application/shared_tools/bin/python /CASDA/application/casda_deposit_tools/data_deposit/bin/plot_moc.py $PROJ_MOC_FILE $PROJ_MOC_PREVIEW
/CASDA/application/shared_tools/bin/python /CASDA/application/casda_deposit_tools/data_deposit/bin/plot_moc.py $ASKAP_MOC_FILE $ASKAP_MOC_PREVIEW

exit 0
