#!/bin/bash
shopt -s extglob # Allow eclude patterns
for each in $(ls $1/!(*.checksum)); do 
    echo "Verifying checksums for $each"
    ./verify_checksum.sh "$each"
done
shopt -u extglob
