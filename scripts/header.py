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
##
#	A script for parsing all image cubes in an archive, extracting the header data and outputting the data to an SQL file
#	in the format of a series of sql update commands to enter this information to the database.
##

#code for white list
# run this against database and export results out to csv file (without column headings)
"""select IC.id, IC.filename, O.sbid from casda.image_cube IC, casda.observation O where IC.observation_id = O.id and IC.header is null;"""

# change for each environment
#test_directory = '/ASKAP/archive/dev/vol002/ASKAPArchive/'
#cmd = "/ASKAP/access/dev/shared_tools/bin/imhead ";

#test_directory = '/ASKAP/archive/test/vol002/ASKAPArchive/'
#cmd = "/ASKAP/access/test/shared_tools/bin/imhead ";

test_directory = '/ASKAP/archive/at/vol002/ASKAPArchive/'
cmd = "/ASKAP/access/at/shared_tools/bin/imhead ";

#test_directory = '/ASKAP/prd-archive/prd/vol002/ASKAPArchive/'
#cmd = "/ASKAP/prd-access/prd/shared_tools/bin/imhead ";

file_regex = '^observations-[0-9]+-image_cubes-[0-9]+.fits$'


# method for checking fits file against white-list
# file name is the full filename
def is_in_list(file_name):
    sbid = file_name.split("-")[1]
    name = re.sub('observations-[0-9]+-image_cubes-', '', file_name)
    value = ''
    position = 0

    if re.match(file_regex, file_name):
        value = name.split("-")[name.split("-").__len__()-1].replace('.fits', '')
        position = 0
    else:
        value = name
        position = 1

    for line in whitelist:
        comp_array = line.split(';')
        if comp_array[2] == sbid and comp_array[position] == value:
            return True
    return False

if __name__ == '__main__':
    if os.path.exists('result.sql'):
        os.remove('result.sql')

    whitelist_file = open('whitelist.csv', 'r')
    log_file = open('result.sql','a')

    whitelist = list()

    # create white-list collection
    for line in whitelist_file.readlines():
        whitelist.append(line.rstrip())

    log_file.write("--===============================\n")
    log_file.write("--== Header Script Results ==\n")
    log_file.write("--===============================\n\n\n")
    log_file.write("--Generated Queries: \n")
    log_file.write("---------------------\n\n")

    generatedQueries = 0
    assumedDone = list()
    nonFitsFiles = list()
    skippedFiles = list()

    # loops through the file structure {date}/{number}/{files} to find the fits files
    for child in os.listdir(test_directory):
        date_path = os.path.join(test_directory, child)
        if os.path.isdir(date_path) and re.match("^[0-9]{4}-[0-9]{2}-[0-9]{2}$", child):
            for child2 in os.listdir(date_path):
                number_path = os.path.join(test_directory, child, child2)
                if os.path.isdir(number_path) and re.match("^[0-9]+$", child2):
                    for child3 in os.listdir(number_path):
                        file_path = os.path.join(test_directory, child, child2, child3)
                        # excludes non-fits file types
                        if file_path.endswith('.fits'):
                            file_name = file_path.split("/")[len(file_path.split("/"))-1]
                            if file_name.startswith("observations") and len(file_name.split("-")) > 3:
                                if is_in_list(file_name):
                                    # processes the fits files using the imhead tool
                                    sbid = file_name.split("-")[1]
                                    name = re.sub('observations-[0-9]+-image_cubes-', '', file_name)
                                    p = subprocess.Popen(cmd + file_path, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
                                    p.wait()

                                    # get dimensions
                                    header = ""
                                    for line in p.stdout.readlines():
                                        header += str(line)

                                    # check for numbers(id) on end of file name. this means the file name won't match the one in the database
                                    if re.match(file_regex, file_name):
                                        # strip id from name
                                        id = name.split("-")[len(name.split("-"))-1].replace('.fits', '')
                                        log_file.write("update casda.image_cube set header = '" + header.replace("'", "''")
                                                   + "' where header is null AND id = '" + id
                                                   + "' AND observation_id  = (SELECT id from casda.observation where sbid = '" + sbid
                                                   + "');\n")
                                    else:
                                        log_file.write("update casda.image_cube set header = '" + header.replace("'", "''")
                                                   + "' where header is null AND filename = '" + name
                                                   + "' AND observation_id  = (SELECT id from casda.observation where sbid = '" + sbid
                                                   + "');\n")
                                    generatedQueries += 1

                                else:
                                    assumedDone.append(file_path)
                            else:
                                skippedFiles.append(file_path)
                        else:
                            nonFitsFiles.append(file_path)

    # write results to file
    total = generatedQueries + len(nonFitsFiles) + len(skippedFiles) + len(assumedDone)

    log_file.write("\n\n--Files assumed to have header: \n")
    log_file.write("----------------\n\n")
    for nff in assumedDone:
        log_file.write("--" + nff + "\n")

    log_file.write("\n\n--Skipped Files: \n")
    log_file.write("---------------\n\n")
    for sf in skippedFiles:
        log_file.write("--" + sf + "\n")

    log_file.write("\n\n--Non Fits Files: \n")
    log_file.write("----------------\n\n")
    for nff in nonFitsFiles:
        log_file.write("--" + nff + "\n")

    log_file.write("\n--Total Files found: " + str(total) + "\n\n")
    log_file.write("--Queries Generated: " + str(generatedQueries) + "\n")
    log_file.write("--Files assumed to have header: " + str(len(assumedDone)) + "\n")
    log_file.write("--Skipped Files: " + str(len(skippedFiles)) + "\n")
    log_file.write("--Non-Fits Files: " + str(len(nonFitsFiles)) + "\n")