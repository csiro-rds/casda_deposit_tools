package au.csiro.casda.datadeposit.catalogue.level7;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.CharEncoding;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import au.csiro.TestUtils;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.DatabaseException;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.MalformedFileException;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.Mode;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.ValidationModeSignal;
import au.csiro.casda.datadeposit.observation.jdbc.repository.SimpleJdbcRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.CatalogueType;
import au.csiro.casda.entity.observation.Level7Collection;
import au.csiro.casda.entity.observation.Project;

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
 * Functional test for Level 7 catalogue import.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class Level7CatalogueParserFunctionalTest
{
    private BufferedReader goldenMasterReader;

    @Mock
    private SimpleJdbcRepository repository;

    @Mock
    private Level7CollectionRepository level7CollectionRepository;

    @Mock
    private CatalogueRepository catalogueRepository;

    private static Answer<Level7Collection> returnSavedValue = new Answer<Level7Collection>()
    {
        // this updates the ids of the records for the level7 collection and its catalogues
        @Override
        public Level7Collection answer(InvocationOnMock invocation) throws Throwable
        {
            Level7Collection level7Collection = (Level7Collection) invocation.getArguments()[0];
            level7Collection.setId(15L);
            long catalogueId = 14L;
            for (Catalogue catalogue : level7Collection.getCatalogues())
            {
                catalogue.setId(catalogueId++);
            }
            return level7Collection;
        }
    };

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws IOException
    {
        if (goldenMasterReader != null)
        {
            goldenMasterReader.close();
        }
    }

    @Test
    public void testAgainstGoldenMaster() throws Exception
    {
        doReturn(false).when(repository).tableExists(anyString());

        Level7Collection level7Collection = new Level7Collection(123456);
        level7Collection.setProject(new Project("AS007"));

        Catalogue catalogue = new Catalogue(CatalogueType.LEVEL7);
        catalogue.setFilename("atlas_sources.xml");
        catalogue.setId(11L);
        level7Collection.addCatalogue(catalogue);

        Level7VoTableVisitor visitor =
                new Level7VoTableVisitor(repository, new SimpleDateFormat("yyyy-MM-dd").parse("2015-04-07"), 255);
        Level7CatalogueParser parser = new Level7CatalogueParser(visitor, level7CollectionRepository);

        when(level7CollectionRepository.findByDapCollectionId(123456L)).thenReturn(level7Collection);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(repository).executeStatement(queryCaptor.capture());

        parser.parseFile(123456, "atlas_sources.xml", "src/test/resources/level7/good/atlas_sources.xml", Mode.NORMAL);

        goldenMasterReader =
                new BufferedReader(new InputStreamReader(new FileInputStream(
                        TestUtils.getResourceAsFile("level7/good/atlas_sources.ddl.txt")),
                        Charset.forName(CharEncoding.UTF_8)));

        String args = StringUtils.join(queryCaptor.getAllValues(), "\n");
        BufferedReader outputReader = new BufferedReader(new StringReader(args));

        int i = 1;
        String goldenMasterLine = goldenMasterReader.readLine();
        while (goldenMasterLine != null)
        {
            String outputLine = outputReader.readLine();
            Assert.assertNotNull("Output missing line at line" + i, outputLine);
            assertEquals("Difference in line " + i, goldenMasterLine, outputLine);
            goldenMasterLine = goldenMasterReader.readLine();
            i++;
        }
        assertNull("Output has additional lines from line " + i, outputReader.readLine());
    }

    @Test
    public void testAgainstUnusualFieldsMaster() throws Exception
    {
        doReturn(false).when(repository).tableExists(anyString());

        Level7Collection level7Collection = new Level7Collection(123456);
        level7Collection.setProject(new Project("AS031"));

        Catalogue catalogue = new Catalogue(CatalogueType.LEVEL7);
        catalogue.setFilename("unusual_types_catalogue.xml");
        catalogue.setFormat("VOTABLE");
        catalogue.setParent(level7Collection);
        catalogue.setId(13L);

        Level7VoTableVisitor visitor =
                new Level7VoTableVisitor(repository, new SimpleDateFormat("yyyy-MM-dd").parse("2015-04-07"), 255);
        Level7CatalogueParser parser = new Level7CatalogueParser(visitor, level7CollectionRepository);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(repository).executeStatement(queryCaptor.capture());

        parser.parseCatalogueDatafile("src/test/resources/level7/good/unusual_types_catalogue.xml", level7Collection,
                CatalogueParser.Mode.NORMAL);

        goldenMasterReader =
                new BufferedReader(new InputStreamReader(new FileInputStream(
                        TestUtils.getResourceAsFile("level7/good/unusual_types.ddl.txt")),
                        Charset.forName(CharEncoding.UTF_8)));

        String args = StringUtils.join(queryCaptor.getAllValues(), "\n");
        BufferedReader outputReader = new BufferedReader(new StringReader(args));

        int i = 1;
        String goldenMasterLine = goldenMasterReader.readLine();
        while (goldenMasterLine != null)
        {
            String outputLine = outputReader.readLine();
            Assert.assertNotNull("Output missing line at line" + i, outputLine);
            assertEquals("Difference in line " + i, goldenMasterLine, outputLine);
            goldenMasterLine = goldenMasterReader.readLine();
            i++;
        }
        assertNull("Output has additional lines from line " + i, outputReader.readLine());
    }

    @Test
    public void testValidationModeForGoodFile() throws Exception
    {
        testValidationMessages("good/atlas_sources.xml");
    }

    @Test
    public void testValidationModeForCatalogueNameMissing() throws Exception
    {
        testValidationMessages("bad/catalogue_name.missing.xml",
                "Error in TABLE : Missing PARAM matching name: 'Catalogue Name', datatype: 'char'");
    }

    @Test
    public void testValidationModeForCatalogueNameEmpty() throws Exception
    {
        testValidationMessages("bad/catalogue_name.empty.xml",
                "Error in PARAM 'Catalogue Name' : value cannot be blank");
    }

    @Test
    public void testValidationModeForCatalogueNameInvalid() throws Exception
    {
        testValidationMessages("bad/catalogue_name.invalid.xml",
                "Error in PARAM 'Catalogue Name' : value contains forbidden characters "
                        + "(it must contain only letters, numbers and underscores)");
    }

    @Test
    public void testValidationModeForCatalogueNameDuplicate() throws Exception
    {
        String parentId = "12345";

        String catalogueDatafile = "src/test/resources/level7/bad/catalogue_name.duplicate.xml";

        Level7VoTableVisitor visitor =
                new Level7VoTableVisitor(repository, new SimpleDateFormat("yyyy-MM-dd").parse("2015-04-07"), 255);
        Level7CatalogueParser parser = new Level7CatalogueParser(visitor, level7CollectionRepository);

        when(repository.tableExists("my_level7_catalogue")).thenReturn(true);

        when(level7CollectionRepository.save(any(Level7Collection.class))).then(returnSavedValue);

        try
        {
            parser.parseFile(Integer.parseInt(parentId), "catalogue_name.empty.xml", catalogueDatafile,
                    Mode.VALIDATE_ONLY);
            fail("Validation mode must throw an exception of type ValidationModeSignal");
        }
        catch (ValidationModeSignal e)
        {
            List<String> failureMessages = e.getValidationFailureMessages();
            assertEquals(1, failureMessages.size());
            assertEquals("Error in PARAM 'Catalogue Name' : catalogue with name 'my_level7_catalogue' already exists",
                    failureMessages.get(0));
        }
    }

    @Test
    public void testNormalModeForLevel7CollectionDoesnotExist() throws Exception
    {
        String parentId = "12345";

        String catalogueDatafile = "src/test/resources/level7/good/atlas_sources.xml";

        Level7VoTableVisitor visitor =
                new Level7VoTableVisitor(repository, new SimpleDateFormat("yyyy-MM-dd").parse("2015-04-07"), 255);
        Level7CatalogueParser parser = new Level7CatalogueParser(visitor, level7CollectionRepository);

        Catalogue catalogue = new Catalogue(CatalogueType.LEVEL7);
        catalogue.setProject(new Project("AS007"));
        catalogue.setFilename("atlas_sources.xml");
        catalogue.setId(11L);

        when(catalogueRepository.findByLevel7CollectionIdAndFilename(12345L, "atlas_sources.xml"))
                .thenReturn(catalogue);
        when(level7CollectionRepository.findByDapCollectionId(12345L)).thenReturn(null);

        try
        {
            parser.parseFile(Integer.parseInt(parentId), "atlas_sources.xml", catalogueDatafile, Mode.NORMAL);
            fail("An exception should be thrown because the catalogue has already been loaded");
        }
        catch (CatalogueParser.DatabaseException e)
        {
            assertEquals("Level 7 Collection record with collection id 12345 does not exist", e.getMessage());
        }
    }

    @Test
    public void testValidationModeForIndexedFieldsMissing() throws Exception
    {
        testValidationMessages("bad/indexed_fields.missing.xml",
                "Error in TABLE : Missing PARAM matching name: 'Indexed Fields', datatype: 'char'");
    }

    @Test
    public void testValidationModeForIndexedFieldsUnknown() throws Exception
    {
        testValidationMessages("bad/indexed_fields.unknown_field.xml", "Error in TABLE : Unknown indexed fields 'foo'");
    }

    @Test
    public void testValidationModeForPrincipalFieldsMissing() throws Exception
    {
        testValidationMessages("bad/principal_fields.missing.xml",
                "Error in TABLE : Missing PARAM matching name: 'Principal Fields', datatype: 'char'");
    }

    @Test
    public void testValidationModeForPrincipalFieldsUnknown() throws Exception
    {
        testValidationMessages("bad/principal_fields.unknown_field.xml",
                "Error in TABLE : Unknown principal fields 'foo'");
    }

    @Test
    public void testValidationModeForMissingFieldWithIdUcd() throws Exception
    {
        testValidationMessages("bad/ucd.id.missing.xml",
                "Error in TABLE : Missing FIELD matching ucd: 'meta.id;meta.main'");
    }

    @Test
    public void testValidationModeForMissingFieldWithRaUcd() throws Exception
    {
        testValidationMessages("bad/ucd.ra.missing.xml",
                "Error in TABLE : Missing FIELD matching ucd: 'pos.eq.ra;meta.main'");
    }

    @Test
    public void testValidationModeForMissingFieldWithDecUcd() throws Exception
    {
        testValidationMessages("bad/ucd.dec.missing.xml",
                "Error in TABLE : Missing FIELD matching ucd: 'pos.eq.dec;meta.main'");
    }

    @Test
    public void testValidationModeFailsForCharWiderThanMaxarraysize() throws Exception
    {
        testValidationMessages("bad/comment.arraysize.too_large.xml",
                "Error in FIELD 'comment' : Attribute 'arraysize' ('1025') exceeds maximum of '1024'");
    }

    @Test
    public void testAgainstMissingDescriptionMasterValidate() throws Exception
    {
        testMissingDescription(Mode.VALIDATE_ONLY);
    }

    @Test
    public void testAgainstMissingDescriptionMasterNormal() throws Exception
    {
        testMissingDescription(Mode.NORMAL);
    }

    private void testMissingDescription(CatalogueParser.Mode mode) throws Exception
    {
        // description is missing on the component name field in the sample
        doReturn(false).when(repository).tableExists(anyString());

        Level7Collection level7Collection = new Level7Collection(123456);
        level7Collection.setProject(new Project("C009"));

        Catalogue catalogue = new Catalogue(CatalogueType.LEVEL7);
        catalogue.setFilename("description.missing.xml");
        catalogue.setFormat("VOTABLE");
        catalogue.setId(12L);
        catalogue.setParent(level7Collection);

        Level7VoTableVisitor visitor =
                new Level7VoTableVisitor(repository, new SimpleDateFormat("yyyy-MM-dd").parse("2015-04-07"), 255);
        Level7CatalogueParser parser = new Level7CatalogueParser(visitor, level7CollectionRepository);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(repository).executeStatement(queryCaptor.capture());

        parser.parseCatalogueDatafile("src/test/resources/level7/good/description.missing.xml", level7Collection, mode);

        goldenMasterReader =
                new BufferedReader(new InputStreamReader(new FileInputStream(
                        TestUtils.getResourceAsFile("level7/good/description.missing.ddl.txt")),
                        Charset.forName(CharEncoding.UTF_8)));

        String args = StringUtils.join(queryCaptor.getAllValues(), "\n");
        BufferedReader outputReader = new BufferedReader(new StringReader(args));

        int i = 1;
        String goldenMasterLine = goldenMasterReader.readLine();
        while (goldenMasterLine != null)
        {
            String outputLine = outputReader.readLine();
            Assert.assertNotNull("Output missing line at line" + i, outputLine);
            assertEquals("Difference in line " + i, goldenMasterLine, outputLine);
            goldenMasterLine = goldenMasterReader.readLine();
            i++;
        }
        assertNull("Output has additional lines from line " + i, outputReader.readLine());
    }
    
    @Test
    public void testAgainstQuotesInCommentsMasterValidate() throws Exception
    {
        testQuotesInComments(Mode.VALIDATE_ONLY);
    }
    
    @Test
    public void testAgainstQuotesInCommentsMasterNormal() throws Exception
    {
        testQuotesInComments(Mode.NORMAL);
    }

    private void testQuotesInComments(CatalogueParser.Mode mode) throws Exception
    {
        // description is missing on the component name field in the sample
        doReturn(false).when(repository).tableExists(anyString());

        Level7Collection level7Collection = new Level7Collection(123456);
        level7Collection.setProject(new Project("C009"));

        Catalogue catalogue = new Catalogue(CatalogueType.LEVEL7);
        catalogue.setFilename("quotes.in.comments.xml");
        catalogue.setFormat("VOTABLE");
        catalogue.setId(12L);
        catalogue.setParent(level7Collection);

        Level7VoTableVisitor visitor =
                new Level7VoTableVisitor(repository, new SimpleDateFormat("yyyy-MM-dd").parse("2015-04-07"), 255);
        Level7CatalogueParser parser = new Level7CatalogueParser(visitor, level7CollectionRepository);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(repository).executeStatement(queryCaptor.capture());

        parser.parseCatalogueDatafile("src/test/resources/level7/good/quotes.in.comments.xml", level7Collection, mode);

        goldenMasterReader =
                new BufferedReader(new InputStreamReader(new FileInputStream(
                        TestUtils.getResourceAsFile("level7/good/quotes.in.comments.ddl.txt")),
                        Charset.forName(CharEncoding.UTF_8)));

        String args = StringUtils.join(queryCaptor.getAllValues(), "\n");
        BufferedReader outputReader = new BufferedReader(new StringReader(args));

        int i = 1;
        String goldenMasterLine = goldenMasterReader.readLine();
        while (goldenMasterLine != null)
        {
            String outputLine = outputReader.readLine();
            Assert.assertNotNull("Output missing line at line" + i, outputLine);
            assertEquals("Difference in line " + i, goldenMasterLine, outputLine);
            goldenMasterLine = goldenMasterReader.readLine();
            i++;
        }
        assertNull("Output has additional lines from line " + i, outputReader.readLine());
    }

    @Test
    public void testValidationModeForFieldDescriptionTooLong() throws Exception
    {
        testValidationMessages("bad/description.too.long.xml",
                "Error in FIELD 'ra_deg_cont' : Description element must be no bigger than 255 characters");
    }

    @Test
    public void testValidationModeForTableDescriptionTooLong() throws Exception
    {
        testValidationMessages("bad/table.description.too.long.xml",
                "Error in TABLE : Description element must be no bigger than 255 characters");
    }

    @Test
    public void testValidationModeForFieldNameEmpty() throws Exception
    {
        testValidationMessages("bad/field.name.empty.xml",
                "Error in TABLE : Table has one or more FIELDs with a blank 'name' attribute");
    }

    @Test
    public void testValidationModeForFieldNameInvalid() throws Exception
    {
        testValidationMessages("bad/field.name.invalid.xml",
                "Error in FIELD 'ra_deg_-' : Attribute 'name' ('ra_deg_-') contains forbidden characters ("
                        + "it must contain only letters, numbers and underscores)");
    }

    @Test
    public void testValidationModeForCellsInvalid() throws Exception
    {
        testValidationMessages("bad/cells.bad.xml", //
                "Error in 5th TD (FIELD 'comment') of 1st TR : " //
                        + "Value 'looks like a group in irac 1' is wider than 18 chars", //
                "Error in 2nd TR : Missing TD", //
                "Error in 3rd TR : Additional TD", //
                "Error in 2nd TD (FIELD 'ra_deg_cont') of 4th TR : Value 'A51.5289789999999996' is not a 'double'", //
                "Error in 5th TR : Missing TD", //
                "Error in 6th TR : Additional TD", //
                "Error in 6th TD (FIELD 'veryLongField') of 9th TR : " //
                        + "Value '0123456789012345678901234567890123456789012345678901234567890123456789" //
                        + "01234567890123456789012345678901234567890123456789012345678901234567890123456789" //
                        + "01234567890123456789012345678901234567890123456789012345678901234567890123456789" //
                        + "01234567890123456789012345678901234567890123456789012345678901234567890123456789" //
                        + "01234567890123456789012345678901234567890123456789012345678901234567890123456789" //
                        + "01234567890123456789012345678901234567890123456789012345678901234567890123456789" //
                        + "01234567890123456789012345678901234567890123456789012345678901234567890123456789" //
                        + "01234567890123456789012345678901234567890123456789012345678901234567890123456789" //
                        + "01234567890123456789012345678901234567890123456789012345678901234567890123456789" //
                        + "01234567890123456789012345678901234567890123456789012345678901234567890123456789" //
                        + "01234567890123456789012345678901234567890123456789012345678901234567890123456789" //
                        + "01234567890123456789012345678901234567890123456789012345678901234567890123456789" //
                        + "012345678901234567890123456789012345678901234567890123456789012345678901234' " //
                        + "is wider than maximum 1024 chars", //
                "Error in 4th TD (FIELD 'type') of 10th TR : Value 'A8' is not a 'int'");
    }

    @Test
    public void testValidationModeForUnparseableCatalogueFile() throws Exception
    {
        testValidationMessages("bad/unparseable.xml",
                "Error in TABLE: cvc-elt.1: Cannot find the declaration of element 'VOTABLE'.");
    }
    
    @Test
    public void testValidationModeForColumnId() throws Exception
    {
        testValidationMessages("bad/column.named.id.xml",
                "Error in TABLE : Table contains a FIELD named 'id'");
    }

    private void testValidationMessages(String testFile, String... expectedFailureMessages) throws ParseException,
            SQLException, FileNotFoundException, MalformedFileException, DatabaseException
    {
        String parentId = "12345";

        String catalogueDatafile = "src/test/resources/level7/" + testFile;

        Level7VoTableVisitor visitor =
                new Level7VoTableVisitor(repository, new SimpleDateFormat("yyyy-MM-dd").parse("2015-04-07"), 255);
        Level7CatalogueParser parser = new Level7CatalogueParser(visitor, level7CollectionRepository);

        when(repository.tableExists("my_level7_catalogue")).thenReturn(false);

        when(level7CollectionRepository.save(any(Level7Collection.class))).then(returnSavedValue);

        try
        {
            parser.parseFile(Integer.parseInt(parentId), "catalogue_name.empty.xml", catalogueDatafile,
                    Mode.VALIDATE_ONLY);
            fail("Validation mode must throw an exception of type ValidationModeSignal");
        }
        catch (ValidationModeSignal e)
        {
            assertThat(e.getValidationFailureMessages(), equalTo(Arrays.asList(expectedFailureMessages)));
        }
    }

}
