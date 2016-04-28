package au.csiro.casda.datadeposit.copy;

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


import javax.transaction.Transactional;

import org.junit.Test;

import au.csiro.casda.datadeposit.FunctionalTestBase;

/**
 * Functional test for registering artefacts.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class RegisterArtefactFunctionalTest extends FunctionalTestBase
{
    public RegisterArtefactFunctionalTest() throws Exception
    {
        super();
    }

    @Test
    @Transactional
    public void testHelp()
    {
        exit.equals(0);
        RegisterArtefactCommandLineTool tool =
                context.getBeanFactory().createBean(RegisterArtefactCommandLineTool.class);
        tool.run("-help");
    }

}
