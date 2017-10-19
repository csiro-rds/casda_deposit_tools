#!/usr/bin/env python -u

# Output a plot of a fits file to a png file.
#

# Author James Dempsey
# Date 22 Aug 2017


from __future__ import print_function, division

import argparse
import os

from astropy.io import fits
import matplotlib
# Force matplotlib to not use any Xwindows backend.
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np


def parseargs():
    """
    Parse the command line arguments
    :return: An args map with the parsed arguments
    """
    parser = argparse.ArgumentParser(
        description="Plot the fits image to a png image")

    parser.add_argument("-i", "--input", help="The input FITS image or image cube", required=True)
    parser.add_argument("-o", "--output", help="The output PNG image", required=True)

    args = parser.parse_args()
    return args


def flatten_image(image):
    """
    Take an image cube and make it two dimensional by returning only the first plane.
    :param image: The image or image cube data array to be flattened to 2D
    :return:  The 2D image data array
    """
    orig_shape = image.shape
    num_dim = len(image.shape)
    if num_dim == 4:
        image = image[0,0,:,:]
    if num_dim == 3:
        image = image[0, 0, :, :]
    return image


def main():
    # Parse command line options
    args = parseargs()

    # Read the fits file
    if not os.path.exists(args.input):
        print ("Error: File {} does not exist".format(args.input))
        return 1
    hdulist = fits.open(args.input, memap=True)
    image = hdulist[0].data
    header = hdulist[0].header

    # Plot the fits data to an image
    image = flatten_image(image)
    fig = plt.figure(frameon=False, figsize=np.array(image.shape)/80, dpi=80)
    plt.imshow(image, cmap='gray')
    fig.subplots_adjust(left=0, bottom=0, right=1, top=1, wspace=None, hspace=None)
    plt.axis('off')
    plt.gca().get_xaxis().set_visible(False)
    plt.gca().get_yaxis().set_visible(False)
    plt.savefig(args.output, pad_inches=0)
    plt.close()

    return 0


if __name__ == '__main__':
    exit(main())
