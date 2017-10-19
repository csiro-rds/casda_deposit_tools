#!/usr/bin/env python -u

# Convert a 3D image cube into a 1D spectrum by integrating the data for each spatial plane
# and then convert the value from Jy/beam to Jy by dividing by the beam area.

# Author James Dempsey
# Date 28 Mar 2017


from __future__ import print_function, division

from astropy.io import fits
from astropy import units

import argparse
import math
import numpy as np
import os
import sys
import time


def parseargs():
    """
    Parse the command line arguments
    :return: An args map with the parsed arguments
    """
    parser = argparse.ArgumentParser(
        description="Build a 1D spectrum from a 3D or 4D image cube.")
    parser.add_argument("-i", "--input", help="The input FITS image cube", required=True)
    parser.add_argument("-o", "--output", help="The file name of the spectrum result.", required=True)
    parser.add_argument("-f", "--format", help="The format to output the result in. " +
                                               "Can be fits, votable or text (only fits is supported currently)",
                        default='fits')

    args = parser.parse_args()
    return args


def get_beam_scale_factor(header):
    """
    Given a FITS file header map, calculate the scaling factor to be applied to the integrated values.

    The basic approach is to convert the major/minor axes to sigma or standard deviation values, then
    use them to calculate the value of the Gaussian at each point in the extracted region.

    :param header: The header map of a FITS file.
    :return: The scaling factor to apply to convert from Jy/beam to Jy
    """
    beam_maj = header['BMAJ']  # degrees
    beam_min = header['BMIN']  # degrees
    beam_pa = (header['BPA'] * units.deg).to(units.rad).value

    ra_pixel_size = header['CDELT1']  # degrees
    dec_pixel_size = header['CDELT2']  # degrees

    fwhm_maj_pix = beam_maj / abs(ra_pixel_size)
    fwhm_min_pix = beam_min / abs(dec_pixel_size)

    cos_theta = math.cos(beam_pa)
    sin_theta = math.sin(beam_pa)

    maj_sd_sq = fwhm_maj_pix * fwhm_maj_pix / 8. / math.log(2)
    min_sd_sq = fwhm_min_pix * fwhm_min_pix / 8. / math.log(2)

    ra_half_width = (header['NAXIS1'] - 1) // 2
    dec_half_width = (header['NAXIS2'] - 1) // 2
    scale_factor = 0.

    for y in range(-dec_half_width, dec_half_width+1):
        for x in range(-ra_half_width, ra_half_width+1):
            u = x * cos_theta + y * sin_theta
            v = x * sin_theta - y * cos_theta
            scale_factor += math.exp(-0.5 * (u * u / maj_sd_sq + v * v / min_sd_sq))

    return scale_factor


def build_spectrum(image, header):
    """
    Build a scaled 1D spectrum from a 3D or 4D image cube. The file is assumed to be in
    RA, Dec, Freq order with potentially a single plane fourth axis. The data is summed
    over the spatial axis (assumed to be the first two axes) to give a value for each
    spatial point. This value is then converted from Jy/beam to Jy by scaling it to the beam.

    :param image: The image data from the FITS image cube file
    :param header: The FITS header of the image cube
    :return: A data array of the 1D spectrum
    """
    beam_scale_factor = get_beam_scale_factor(header)
    naxis = header['NAXIS']
    spectrum = np.nansum(image, axis=(naxis - 2, naxis - 1))
    spectrum /= beam_scale_factor
    if naxis == 4:
        spectrum = spectrum.reshape((spectrum.shape[0], spectrum.shape[1], 1, 1))
    else:
        spectrum = spectrum.reshape((spectrum.shape[0], 1, 1))
    print(image.shape, spectrum.shape, beam_scale_factor)
    return spectrum


def build_header(oldheader):
    """
    Create a header for the spectrum file based on the header of the input cube. This will include a history entry
    for the spectrum extraction.
    :param oldheader: The header of the input cube
    :return: The header of the new specturm file.
    """
    new_header = oldheader.copy()
    new_header['BUNIT'] = 'Jy'
    new_header.add_history("CASDA: Extracted spectrum on " + time.strftime('%Y-%m-%dT%H:%M:%S+0000', time.gmtime()))
    return new_header


def write_spectrum(output, spectrum, header):
    """
    Write out the spectrum data to a fits file.
    :param output: The name of the output file.
    :param spectrum: The spectrum data as a numpy array
    :param header: The header of the original fits file.
    :return: None
    """
    new_header = build_header(header)
    prihdu = fits.PrimaryHDU(data=spectrum, header=new_header)
    hdulist = fits.HDUList([prihdu])
    if os.path.exists(output):
        os.remove(output)
    hdulist.writeto(output, output_verify="warn")
    return


def report_error(message):
    """
    Report an error and exit the program.
    :param message: The text of the error message
    :return: None, the system will exit.
    """
    sys.exit('%s: error: %s\n' % ("gen_spectrum", message))


def validate_fits_file(header):
    """
    Check that the input FITS file is suitable for producing a spectrum. This method wil report the first issue
    and exit if the file is not valid.

    :param header: The header of the input FITS file.
    :return: None
    """
    if header['NAXIS'] < 3:
        report_error("The file must have 2 spatial and 1 spectral axes.")

    if not header['CTYPE1'].startswith("RA") and not header['CTYPE1'].startswith("GLAT"):
        report_error("The file must have the spatial axes as the first two axes.")

    if not header['CTYPE2'].startswith("DEC") and not header['CTYPE2'].startswith("GLON"):
        report_error("The file must have the spatial axes as the first two axes.")

    if not header['CTYPE2'].startswith("DEC") and not header['CTYPE2'].startswith("GLON"):
        report_error("The file must have the spatial axes as the first two axes.")

    if not header['BMAJ'] or not header['BMIN'] or not header['BPA']:
        report_error("The FITS file must define the beam dimensions using the BMAJ, BMIN and BPA keys")


def main():
    """
    Main program logic method.
    :return: exit code, 0 if successful, 1 otherwise.
    """
    args = parseargs()

    start = time.time()

    # Read the fits file
    if not os.path.exists(args.input):
        print("Error: file %s does not exist." % args.input)
        return 1
    hdulist = fits.open(args.input, memmap=True)
    image = hdulist[0].data
    header = hdulist[0].header

    # Need to validate the image cube - beam details, axis order
    validate_fits_file(header)

    # Extract the spectrum
    spectrum = build_spectrum(image, header)

    # Write the 1D fits file
    write_spectrum(args.output, spectrum, header)

    end = time.time()
    print('Built spectrum in %.02f s' % (end - start))

    return 0


if __name__ == '__main__':
    exit(main())
