package au.csiro.casda.datadeposit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.CharEncoding;
import org.junit.Test;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;

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
 * Common parent class for command line importer tests.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public abstract class AbstractArgumentsDrivenCommandLineToolTest
{
    /**
     * Test that the usage shows the same command name as the script that runs it.
     * 
     * @throws IOException
     */
    @Test
    public void testProgramNameShownInUsage() throws IOException
    {
        ArgumentsDrivenCommandLineTool<?> importer = createCommmandLineImporter();

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(new File("build.gradle")),
                        CharEncoding.UTF_8)))
        {
            String line = null;
            String applicationNameLine = null;
            String mainClassNameLineRegexp =
                    ".*mainClassName.*:.*" + importer.getClass().getCanonicalName().replace(".", "\\.") + ".*";
            String applicationNameLineRegexp = ".*applicationName.*:.*(?:'|\")(.*)(?:'|\").*";
            while ((line = reader.readLine()) != null)
            {
                if (line.matches(mainClassNameLineRegexp))
                {
                    applicationNameLine = reader.readLine();
                    if (!applicationNameLine.matches(applicationNameLineRegexp))
                    {
                        fail("Could not determine applicationName from build.gradle for "
                                + importer.getClass().getCanonicalName());
                    }
                    break;
                }
            }
            if (applicationNameLine == null)
            {
                fail("Could not determine applicationName from build.gradle for "
                        + importer.getClass().getCanonicalName());
            }
            Pattern mjdFormat = Pattern.compile(applicationNameLineRegexp);
            Matcher matcher = mjdFormat.matcher(applicationNameLine);
            matcher.matches(); // Required to be able to call group(n) on matcher.
            String expectedProgramName = matcher.group(1);

            StringBuilder usageStringBuilder = new StringBuilder();
            importer.writeUsage(usageStringBuilder);

            assertThat("Usage contains program name", usageStringBuilder.toString(), startsWith("Usage: "
                    + expectedProgramName + " [options]"));
        }

    }

    /**
     * Template method used by AbstractCommandLineImporterTest#testProgramName()
     * 
     * @return an instance of the CommandLineImporter under test
     */
    protected abstract ArgumentsDrivenCommandLineTool<?> createCommmandLineImporter();

    /**
     * @return the usage String for this test classes CommandLineImporter
     */
    protected String getCommandLineImporterUsage()
    {
        StringBuilder builder = new StringBuilder();
        createCommmandLineImporter().writeUsage(builder);
        return builder.toString();
    }

    /**
     * Test utility method that can be used to run an action expected to throw a CheckExitCalled exception, simulating a
     * system exit, and then run a set of checks when the 'exit' is called.
     * <p>
     * 
     * @param action
     *            a lambda for the action to run - it must call System.exit somewhere
     * @param checks
     *            a lambda for the checks to perform once System.exit has been called
     * @throws Exception
     */
    protected static void runAndCheckConditionsIgnoringExit(Runnable action, Runnable checks)
    {
        /*
         * Have to write it this way because of mockito's design of using after-the-fact asserts rather than
         * before-the-fact expectations.
         */
        try
        {
            action.run();
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            checks.run();
        }
    }
}
