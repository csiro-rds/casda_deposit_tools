#!/bin/bash
shopt -s extglob # Allow exclude patterns
for each in $(ls $1/*.fits); do 
    echo "Generating image geometry for $each"
    /cygdrive/c/Projects/Casda/wcslibcli/bin/imagegeom "$each" | python -m json.tool > "$each.json"
done
shopt -u extglob
