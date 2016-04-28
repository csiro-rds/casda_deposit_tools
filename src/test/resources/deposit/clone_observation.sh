#!/bin/bash
OLD_SBID="$1"
NEW_SBID="$2"

if [ -z "$OLD_SBID" ]; then echo "Usage: clone_observation.sh OLD_SBID NEW_SBID" && exit 1; fi
if [ -z "$NEW_SBID" ]; then echo "Usage: clone_observation.sh OLD_SBID NEW_SBID" && exit 1; fi

if [ ! -d "$OLD_SBID" ]; then echo "No such observation folder $OLD_SBID" && exit 1; fi
if [ -d "$NEW_SBID" ]; then echo "Observation folder $NEW_SBID already exists" && exit 1; fi

echo "Coping $OLD_SBID"
cp -r "$OLD_SBID" "$NEW_SBID"

echo "Replacing SBID in observation.xml"
cat "$OLD_SBID/observation.xml" | sed -e "s/<sbid>$OLD_SBID<\/sbid>/<sbid>$NEW_SBID<\/sbid>/" > "$NEW_SBID/observation.xml"

# Replace sbid in all catalogues (this technique will process any non-observation.xml xml file which is hacky but probably ok)
echo "Replacing SBID-based object IDs in component and island catalogues"
for each in $(ls $NEW_SBID/*.xml | grep -v observation.xml | sed -e "s/$NEW_SBID\///")
do 
    echo "Processing $NEW_SBID/$each"
	./convert_component_catalogue_component_ids.sh -o "$OLD_SBID" -s "$NEW_SBID" -c "$each"
	./convert_island_catalogue_island_ids.sh -o "$OLD_SBID" -s "$NEW_SBID" -c "$each"
done

echo "(Re)generating checksums."
rm "$NEW_SBID/*.checksum"
./generate_checksums.sh "$NEW_SBID"


