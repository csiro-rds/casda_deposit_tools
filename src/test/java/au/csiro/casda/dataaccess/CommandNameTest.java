package au.csiro.casda.dataaccess;

import static org.mockito.Mockito.mock;
import au.csiro.casda.datadeposit.AbstractArgumentsDrivenCommandLineToolTest;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.service.NgasService;

/*
 * #%L
 * CSIRO ASKAP Science Data Archive
 * %%
 * Copyright (C) 2015 Commonwealth Scientific and Industrial Research Organisation (CSIRO) ABN 41 687 119 230.
 * %%
 * Licensed under the CSIRO Open Source License Agreement (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file.
 * #L%
 */

/**
 * Performs command line name test implemented in AbstractArgumentsDrivenCommandLineToolTest
 * 
 * Copyright 2015, CSIRO Australia All rights reserved.
 * 
 */
public class CommandNameTest extends AbstractArgumentsDrivenCommandLineToolTest
{
    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.casda.datadeposit.AbstractArgumentsDrivenCommandLineToolTest#createCommmandLineImporter()
     */
    @Override
    protected ArgumentsDrivenCommandLineTool<?> createCommmandLineImporter()
    {
        return new NgasDownloader("properties.version", new MiddlewareClientLocal(), mock(NgasService.class));
    }

}
