#!/bin/bash
shopt -s extglob # Allow exclude patterns
for each in $(ls $1/!(*.checksum)); do 
    echo "Generating checksums for $each"
    ./calc_checksum.sh "$each" > "$each.checksum"
done
shopt -u extglob
