#!/usr/bin/env python -u

# Plot a multi-order coverage (MOC) to a map image.

# Author James Dempsey
# Date 29 May 2017

from __future__ import print_function, division

import matplotlib
# Force matplotlib to not use any Xwindows backend.
matplotlib.use('Agg')

import argparse
from pymoc import MOC
import pymoc.io.fits as pymocfits
import pymoc.util.plot as pymocplt


def parseargs():
    """
    Parse the command line arguments
    :return: An args map with the parsed arguments
    """
    parser = argparse.ArgumentParser(
    description="Plot a multi-order coverage (MOC) as a map")
    parser.add_argument("input", help="The input moc in fits format")
    parser.add_argument("output", help="The name of the file the plot will be written to")
    parser.add_argument("--title", help="The title of the plot", default="")

    args = parser.parse_args()
    return args


def plot_moc(moc_file, plot_file, title):
    """
    Output the coverage map to an image file.
    :param moc_file: The name of the input multi-order coverage (MOC) fits file.
    :param plot_file: The name of the image file to be produced.
    :param title: The title of the plot.
    :return: None
    """
    moc = MOC()
    pymocfits.read_moc_fits(moc, moc_file)
    pymocplt.plot_moc(moc, projection='moll', title=title, filename=plot_file)


def main():
    args = parseargs()
    plot_moc(args.input, args.output, args.title)
    return 0


if __name__ == '__main__':
    exit(main())

