Test folders:

* 11111
** Simple valid observation containing:
*** one image cube, 
*** one continuum island catalogue,
*** one continuum component catalogue (referencing the continuum island catalogue),
*** one continuum polarisation catalogue,
*** one measurement set with two scans, and 
*** one evaluation file. 
* 11112
** Used for unit tests only - please ignore.
* 22222
** Invalid observation containing one image cube, one continuum catalogue, one measurement set, and one evaluation file but the image cube and catalogue files have bad checksums.  The deposit will fail on Copying the image cube and catalogue due to the mismatched checksums.
* 22223
** Invalid observation containing one image cube, one continuum catalogue, one measurement set, and one evaluation file but the image cube and catalogue files are missing their checksums.  The deposit will fail on Copying the image cube and catalogue due to the missing files.
* 22224
** Invalid observation containing one image cube, one continuum catalogue, one measurement set, and one evaluation file but the image cube and catalogue files (and their checksums) are missing.  The deposit will fail on Processing the image cube and catalogue due to the missing files.
* 22225
** Invalid observation containing one image file, a *malformed* catalogue, one measurement set, and one evaluation file.  The deposit will fail on Processing the catalogue.
* 22226
** Invalid observation containing one image file, one continuum catalogue, one measurement set, and one *empty* evaluation file.  The deposit will fail on Archiving the evaluation file. (Note: later this will fail at an earlier stage once we validate for empty files.)
* 33333
** Valid Observation with:
** Simple valid observation containing:
*** *two* image cubes, 
*** one continuum island catalogue for image1,
*** one continuum component catalogue (referencing the continuum island catalogue) for image1,
*** one continuum polarisation catalogue,
*** one measurement set with two scans, and 
*** one evaluation file. 
