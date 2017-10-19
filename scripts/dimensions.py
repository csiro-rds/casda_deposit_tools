import os
import subprocess
import re

##
# 
# CSIRO ASKAP Science Data Archive
#
# Copyright (C) 2016 Commonwealth Scientific and Industrial Research Organisation (CSIRO) ABN 41 687 119 230.
#
# Licensed under the CSIRO Open Source License Agreement (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License in the LICENSE file.
#
##

#code for white list
# run this against database and export results out to csv file (without column headings)
"""select IC.id, IC.filename, O.sbid from casda.image_cube IC, casda.observation O where IC.observation_id = O.id and IC.dimensions is null;"""

#change for each environment
test_directory = '/ASKAP/archive/dev/vol002/ASKAPArchive/'
cmd = "/ASKAP/access/dev/shared_tools/bin/imagegeom ";

#test_directory = '/ASKAP/archive/test/vol002/ASKAPArchive/'
#cmd = "/ASKAP/access/test/shared_tools/bin/imagegeom ";

#test_directory = '/ASKAP/archive/at/vol002/ASKAPArchive/'
#cmd = "/ASKAP/access/at/shared_tools/bin/imagegeom ";

#test_directory = '/ASKAP/prd-archive/prd/vol002/ASKAPArchive/'
#cmd = "/ASKAP/prd-access/prd/shared_tools/bin/imagegeom ";

if os.path.exists('result.sql'):
    os.remove('result.sql')

log_file = open('result.sql','a')
file_regex = '^observations-[0-9]+-image_cubes-[0-9]+.fits$'

generatedQueries = list()
nonFitsFiles = list()
skippedFiles = list()

#loops through the file structure {date}/{number}/{files} to find the fits files
for child in os.listdir(test_directory):
    date_path = os.path.join(test_directory, child)
    if os.path.isdir(date_path):
        for child2 in os.listdir(date_path):
            number_path = os.path.join(test_directory, child, child2)
            for child3 in os.listdir(number_path):
                file_path = os.path.join(test_directory, child, child2, child3)
                #excludes non-fits file types
                if file_path.endswith('.fits'):
                    file_name = file_path.split("/")[file_path.split("/").__len__()-1]
                    if file_name.startswith("observations") and file_name.split("-").__len__() > 3:
                        #processes the fits files using the wcs imagegeom script
                        sbid = file_name.split("-")[1]
                        name = file_name.split("-")[3]
                        p = subprocess.Popen(cmd + file_path, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
                        p.wait()

                        #get dimensions
                        dimensions = ""
                        for line in p.stdout.readlines():
                            dimensions += str(line)
                        if dimensions.startswith("{"):
                            #check for numbers(id) on end of file name. this means the file name won't match the one in the database
                            if re.match(file_regex, file_name):
                                #strip id from name
                                id = name.split("-")[name.split("-").__len__()-1].replace('.fits', '')
                                generatedQueries.append("update casda.image_cube set dimensions = '" + dimensions
                                           + "' where dimensions is null AND id = '" + id
                                           + "' AND observation_id  = (SELECT id from casda.observation where sbid = '" + sbid
                                           + "');\n")
                            else:
                                generatedQueries.append("update casda.image_cube set dimensions = '" + dimensions
                                           + "' where dimensions is null AND filename = '" + name
                                           + "' AND observation_id  = (SELECT id from casda.observation where sbid = '" + sbid
                                           + "');\n")
                        else:
                            skippedFiles.append(file_path) 
                    else:
                        skippedFiles.append(file_path)
                else:
                    nonFitsFiles.append(file_path)

#write results to file
total = len(generatedQueries) + len(nonFitsFiles) + len(skippedFiles)

log_file.write("--===============================\n")
log_file.write("--== Dimensions Script Results ==\n")
log_file.write("--===============================\n\n\n")

log_file.write("--Total Files found: " + str(total) + "\n\n")
log_file.write("--Queries Generated: " + str(len(generatedQueries)) + "\n\n")
log_file.write("--Skipped Files: " + str(len(skippedFiles)) + "\n")
log_file.write("--Non-Fits Files: " + str(len(nonFitsFiles)) + "\n\n\n")

log_file.write("--Generated Queries: \n")
log_file.write("---------------------\n\n")
for gq in generatedQueries:
    log_file.write(gq +"\n")
log_file.write("--Total: " + str(len(generatedQueries)) +"\n\n")

log_file.write("--Skipped Files: \n")
log_file.write("---------------\n\n")
for sf in skippedFiles:
    log_file.write("--" + sf + "\n")
log_file.write("\n--Total: " + str(len(skippedFiles)) +"\n\n")

log_file.write("--Non Fits Files: \n")
log_file.write("----------------\n\n")
for nff in nonFitsFiles:
    log_file.write("--" + nff + "\n")
log_file.write("--Total: " + str(len(nonFitsFiles)) +"\n\n")