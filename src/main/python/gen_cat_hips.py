#!/usr/bin/env python -u

# Extract the catalogue entries and create a HiPS collection base don the acatlogue.

from __future__ import print_function, division

import argparse
import casda
import os
import shutil
import subprocess
import time

# Database query and field declarations
queries = {
    'continuum_component': 'SELECT component_id, island_id, component_name, ra_deg_cont, dec_deg_cont, flux_peak, '
                           'flux_int, maj_axis, min_axis, pos_ang, image_id '
                           'FROM casda.continuum_component cc, casda.catalogue cat '
                           'WHERE cc.catalogue_id = cat.id',
    'continuum_island': 'SELECT island_id, island_name, ra_deg_cont, dec_deg_cont, flux_peak, flux_int, maj_axis, '
                        'min_axis, pos_ang, image_id '
                        'FROM casda.continuum_island ci, casda.catalogue cat '
                           'WHERE ci.catalogue_id = cat.id',
    'polarisation_component':   'SELECT polc.component_id, polc.component_name, polc.ra_deg_cont, polc.dec_deg_cont, flux_I_median, '
                                'pol_peak, fd_peak, pol_ang_zero, pol_frac, cc.maj_axis, cc.min_axis, cc.pos_ang, image_id '
                                'FROM casda.polarisation_component polc, casda.catalogue cat, casda.continuum_component cc '
                                'WHERE polc.catalogue_id = cat.id and polc.component_id = cc.component_id',
    'spectral_line_absorption': 'SELECT object_id, object_name, ra_deg_cont, dec_deg_cont, freq_w, z_hi_w, w50, '
                                'opt_depth_peak, opt_depth_int '
                                'FROM casda.spectral_line_absorption sla, casda.catalogue cat '
                                'WHERE sla.catalogue_id = cat.id',
    'spectral_line_emission':   'SELECT object_id, object_name, ra_deg_w, dec_deg_w, vel_w, integ_flux, w50_vel, '
                                'maj_axis, min_axis, pos_ang '
                                'FROM casda.spectral_line_emission sle, casda.catalogue cat '
                                'WHERE sle.catalogue_id = cat.id'
}

ra_field = {
    'continuum_component': 'ra_deg_cont', 'continuum_island': 'ra_deg_cont',
    'polarisation_component': 'ra_deg_cont', 'spectral_line_absorption': 'ra_deg_cont',
    'spectral_line_emission': 'ra_deg_w'}
dec_field = {
    'continuum_component': 'dec_deg_cont', 'continuum_island': 'dec_deg_cont',
    'polarisation_component': 'dec_deg_cont', 'spectral_line_absorption': 'dec_deg_cont',
    'spectral_line_emission': 'dec_deg_w'}
score_field = {
    'continuum_component': 'flux_peak', 'continuum_island': 'flux_peak',
    'polarisation_component': 'flux_I_median', 'spectral_line_absorption': 'z_hi_w',
    'spectral_line_emission': 'integ_flux'}


def parseargs():
    """
    Parse the command line arguments
    :return: An args map with the parsed arguments
    """
    parser = argparse.ArgumentParser(
        description="Extract the entries for a catalogue and generate a HiPS collection for it.")
    parser.add_argument("catalogue", choices=['continuum_component', 'continuum_island', 'polarisation_component',
                                              'spectral_line_absorption', 'spectral_line_emission'],
                        help="The CASDA catalogue to be converted")
    parser.add_argument("destination_directory",
                        help="The directory where the resulting files will be stored")
    parser.add_argument("--work_directory", "-work",
                        help="The directory where intermediate files will be stored")
    parser.add_argument("--environment", "--env", choices=['dev', 'test', 'at', 'prod'], default='prod',
                        help="The CASDA environment to be queried")

    args = parser.parse_args()
    return args


def download_catalogue(catalogue_type, work_folder):
    query = queries[catalogue_type]
    job_location = casda.create_async_tap_job()
    print("Job is ", job_location)
    casda.add_param_to_async_job(job_location, 'query', query)
    casda.add_param_to_async_job(job_location, 'RESPONSEFORMAT', 'votable')
    casda.add_param_to_async_job(job_location, 'request', 'doQuery')
    casda.add_param_to_async_job(job_location, 'lang', 'ADQL')
    job_status = casda.run_async_job(job_location)
    print('\nJob finished with status %s address is %s\n\n' % (job_status, job_location))
    if job_status != 'ERROR':
        filenames = casda.download_all(job_location, work_folder)
        return filenames[0]


def generate_hips(catalogue_filename, work_folder, catalogue_type):
    out_folder = work_folder + "/" + catalogue_type
    backup_path = out_folder + "_prev"
    if os.path.exists(backup_path):
        shutil.rmtree(backup_path)
    if os.path.exists(out_folder):
        os.rename(out_folder, backup_path)

    ra = ra_field[catalogue_type]
    dec = dec_field[catalogue_type]
    score = score_field[catalogue_type]
    desc = "" if catalogue_type == 'spectral_line_absorption' else "-desc";
    hipsgen_cmd = 'java -Xmx4G -jar /CASDA/application/shared_tools/Hipsgen-cat.jar -in {} -f VOT -out {} -cat {} -ra {} -dec {} -score {} {} -lM 5'.format(
        catalogue_filename, out_folder, catalogue_type, ra, dec, score, desc)
    print('\nStarting generation of HiPS file\n\n')
    retcode = subprocess.call(hipsgen_cmd, shell=True)
    if retcode != 0:
        message = "Command '" + hipsgen_cmd + "' failed with code " + str(retcode)
        raise SystemError(message)

    return out_folder


def main():
    args = parseargs()

    # Choose which environment to use, prod is the default
    if args.environment == 'dev':
        casda.use_dev()
    elif args.environment == 'test':
        casda.use_test()
    elif args.environment == 'at':
        casda.use_at()
    else:
        casda.use_prod()

    start = time.time()
    if not os.path.exists(args.destination_directory):
        os.makedirs(args.destination_directory)
    work_folder = args.work_directory if args.work_directory else 'work'
    if not os.path.exists(work_folder):
        os.makedirs(work_folder)

    # Download the catalogue
    filename = download_catalogue(args.catalogue, work_folder)

    # Run the hipsgen
    hips_folder = generate_hips(filename, work_folder, args.catalogue)

    # Move the result to its final destination
    final_dest = args.destination_directory + "/" + args.catalogue
    backup_path = final_dest + "_prev"
    if os.path.exists(backup_path):
        shutil.rmtree(backup_path)
    if os.path.exists(final_dest):
        os.rename(final_dest, backup_path)
    os.rename(hips_folder, final_dest)

    # Ensure the Norder1 directory exists
    norder1 = final_dest + "/Norder1"
    if not os.path.exists(norder1):
        shutil.copytree(final_dest + "/Norder2", norder1)

    # Report
    end = time.time()
    print('#### HiPS generation completed at %s ####'
          % (time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(end))))
    print('Full run took %.02f s' % (end - start))
    return 0


if __name__ == '__main__':
    exit(main())
