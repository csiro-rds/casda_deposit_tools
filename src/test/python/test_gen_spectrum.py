# Set of unit tests for gen_spectrum written using the pytest framework.

# Run using py.test test_gen_spectrum.py

# Author James Dempsey
# Date 28 Aug 2016

import os
import sys
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../../main/python')))

from astropy.io import fits

import gen_spectrum


def test_get_beam_scale_factor_unresolved():
    """
    Test out a single pixel image comes back as unscaled (for unresolved sources, Jy/beam = Jy)
    """
    header = fits.Header()
    header['BMAJ'] = 10.0 / 3600.0 # degrees
    header['BMIN'] = 5.0 / 3600.0 # degrees
    header['BPA'] = 90.0 # degrees
    header['NAXIS1'] = 1
    header['NAXIS2'] = 1
    header['CDELT1'] = 1.0 / 3600.0
    header['CDELT2'] = 1.0 / 3600.0

    assert gen_spectrum.get_beam_scale_factor(header) == 1.0


def test_get_beam_scale_factor_resolved():
    """
    Test out a multiple pixel image comes back as scaled according to the beam size
    """
    header = fits.Header()
    header['BMAJ'] = 10.0 / 3600.0 # degrees
    header['BMIN'] = 5.0 / 3600.0 # degrees
    header['BPA'] = 90.0 # degrees
    header['NAXIS1'] = 30
    header['NAXIS2'] = 28
    header['CDELT1'] = 1.0 / 3600.0
    header['CDELT2'] = 1.0 / 3600.0

    assert gen_spectrum.get_beam_scale_factor(header) == 56.572874760076736


def test_get_beam_scale_factor_imspec():
    """
    Test out the scale factor calculation against a cutout verified against Miriad IMSPEC
    """
    header = fits.Header()
    header['BMAJ'] = 1.251171058400E-02
    header['BMIN'] = 9.086469873512E-03
    header['BPA'] = 8.932696347801E+01
    header['NAXIS1'] = 15
    header['NAXIS2'] = 15
    header['CDELT1'] = -2.083333333333E-03
    header['CDELT2'] = 2.083333333333E-03
    header['CTYPE1'] = 'RA---SIN'
    header['CTYPE2'] = 'DEC--SIN'
    header['CTYPE3'] = 'FREQ    '

    assert gen_spectrum.get_beam_scale_factor(header) == 29.58703282808615
