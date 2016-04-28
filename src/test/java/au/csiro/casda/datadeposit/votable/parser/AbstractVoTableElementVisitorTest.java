package au.csiro.casda.datadeposit.votable.parser;

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


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AbstractVoTableElementVisitorTest
{

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCanSuccessfullyLoadAllYmlFilesInSrcMainResourcesSchemas() throws IOException
    {
        String resourcesPath = "src/main/resources/schemas";

        Files.walk(Paths.get(resourcesPath)).forEach(
                filePath -> {
                    if (filePath.getFileName().toString().endsWith(".yml")
                            && !filePath.getFileName().toString().contains("level7"))
                    {
                        List<ParamConstraint> paramsMetadata = new ArrayList<>();
                        List<FieldConstraint> fieldsMetadata = new ArrayList<>();
                        try
                        {
                            AbstractVoTableElementVisitor.loadConstraintsFile("schemas/"
                                    + filePath.getFileName().toString(), paramsMetadata, fieldsMetadata);
                            assertTrue(paramsMetadata.size() > 0);
                            assertTrue(fieldsMetadata.size() > 0);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            fail("could not load the metadata file: " + filePath + " exception: " + e.getMessage());
                        }
                    }
                });
    }

    @Test
    public void testNoParamsInYmlIsOk()
    {
        List<ParamConstraint> paramsMetadata = new ArrayList<>();
        List<FieldConstraint> fieldsMetadata = new ArrayList<>();
        AbstractVoTableElementVisitor.loadConstraintsFile("invalid/yml/no_params.yml", paramsMetadata, fieldsMetadata);
    }

    @Test
    public void testEmptyParamsInYmlIsOk()
    {
        List<ParamConstraint> paramsMetadata = new ArrayList<>();
        List<FieldConstraint> fieldsMetadata = new ArrayList<>();
        AbstractVoTableElementVisitor.loadConstraintsFile("invalid/yml/empty_params.yml", paramsMetadata, fieldsMetadata);
    }

    @Test
    public void testNoFieldsInYmlIsOk()
    {
        List<ParamConstraint> paramsMetadata = new ArrayList<>();
        List<FieldConstraint> fieldsMetadata = new ArrayList<>();
        AbstractVoTableElementVisitor.loadConstraintsFile("invalid/yml/no_fields.yml", paramsMetadata, fieldsMetadata);
    }

    @Test
    public void testEmptyFieldsInYmlIsOk()
    {
        List<ParamConstraint> paramsMetadata = new ArrayList<>();
        List<FieldConstraint> fieldsMetadata = new ArrayList<>();
        AbstractVoTableElementVisitor.loadConstraintsFile("invalid/yml/empty_fields.yml", paramsMetadata, fieldsMetadata);
    }

    @Test
    public void testNonCharParamWithMaxarraysizeInYmlThrowsException()
    {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Error reading invalid/yml/non_char_param_with_maxarraysize.yml");

        List<ParamConstraint> paramsMetadata = new ArrayList<>();
        List<FieldConstraint> fieldsMetadata = new ArrayList<>();
        AbstractVoTableElementVisitor.loadConstraintsFile("invalid/yml/non_char_param_with_maxarraysize.yml",
                paramsMetadata, fieldsMetadata);
    }

    @Test
    public void testNonCharFieldWithMaxarraysizeInYmlThrowsException()
    {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Error reading invalid/yml/non_char_field_with_maxarraysize.yml");

        List<ParamConstraint> paramsMetadata = new ArrayList<>();
        List<FieldConstraint> fieldsMetadata = new ArrayList<>();
        AbstractVoTableElementVisitor.loadConstraintsFile("invalid/yml/non_char_field_with_maxarraysize.yml",
                paramsMetadata, fieldsMetadata);
    }

    @Test
    public void testCharParamWithoutMaxarraysizeInYmlThrowsException()
    {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Error reading invalid/yml/char_param_without_maxarraysize.yml");

        List<ParamConstraint> paramsMetadata = new ArrayList<>();
        List<FieldConstraint> fieldsMetadata = new ArrayList<>();
        AbstractVoTableElementVisitor.loadConstraintsFile("invalid/yml/char_param_without_maxarraysize.yml",
                paramsMetadata, fieldsMetadata);
    }

    @Test
    public void testCharFieldWithoutMaxarraysizeInYmlThrowsException()
    {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Error reading invalid/yml/char_field_without_maxarraysize.yml");

        List<ParamConstraint> paramsMetadata = new ArrayList<>();
        List<FieldConstraint> fieldsMetadata = new ArrayList<>();
        AbstractVoTableElementVisitor.loadConstraintsFile("invalid/yml/char_field_without_maxarraysize.yml",
                paramsMetadata, fieldsMetadata);
    }
}
