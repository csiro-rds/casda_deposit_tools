package au.csiro.casda.datadeposit.observation;

import static org.hamcrest.Matchers.containsInAnyOrder;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.hamcrest.CoreMatchers.is;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.xml.sax.SAXParseException;

import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.DepositState.Type;
import au.csiro.casda.datadeposit.DepositStateImpl;
import au.csiro.casda.datadeposit.DepositableArtefact;
import au.csiro.casda.datadeposit.observation.ObservationParser.MalformedFileException;
import au.csiro.casda.datadeposit.observation.jdbc.repository.SimpleJdbcRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ProjectRepository;
import au.csiro.casda.datadeposit.observation.parser.XmlObservationParserTest;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.EncapsulationFile;
import au.csiro.casda.entity.observation.EvaluationFile;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.MeasurementSet;
import au.csiro.casda.entity.observation.Observation;

/**
 * 
 * Tests for validations in the commandlineimporter class.
 * 
 */
@ContextConfiguration(classes = { TestAppConfig.class })
@ActiveProfiles("local")
public class ObservationParserTest
{
    @Autowired
    @Value("${fileIdMaxSize}")
    private int fileIdMaxSize;
    
    @Autowired
    @Value("${thumbnail.max.size.kilobytes}")
    private long thumbnailMaxSize;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ProjectRepository projectRepository;

    private ObservationRepository observationRepository;

    private SimpleJdbcRepository simpleJdbcRepository;

    public static Matcher<Throwable> malformedFileException(String expectedMessage)
    {
        return new TypeSafeMatcher<Throwable>()
        {

            @Override
            public void describeTo(Description description)
            {
                description.appendText("File Id longer than 80 char:");
            }

            @Override
            protected boolean matchesSafely(Throwable item)
            {
                if (!(item instanceof MalformedFileException))
                {
                    return false;
                }

                String message = ((MalformedFileException) item).getMessage();
                return message.matches(".*" + expectedMessage + ".*");
            }
        };
    }

    @Before
    public void setUp() throws Exception
    {
        projectRepository = mock(ProjectRepository.class);
        observationRepository = mock(ObservationRepository.class);
        simpleJdbcRepository = mock(SimpleJdbcRepository.class);
        when(observationRepository.save(any(Observation.class))).then(returnsFirstArg());
        when(simpleJdbcRepository.getImageTypes()).thenReturn(getValidImageTypes());
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
    }

    private List<String> getValidImageTypes()
    {
        List<String> types = new ArrayList<String>();
        types.add("Continuum - mask");
        types.add("cont_restored_T0");
        types.add("Polarisation - model");
        types.add("Polarisation - restored");

        return types;
    }
    
    public void testValidatethumbnailSize() throws Exception
    {
        ObservationParser parser =
                new ObservationParserImpl(projectRepository, observationRepository, simpleJdbcRepository);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException.expectMessage(
                "Supplied sbid [1] does not match the value in the " + "/dataset/identity/sbid element [12345]");
       // parser.parseFile(609, "src/test/resources/observation/good/metadata-v2-good01.xml");        
        parser.parseFile(609, "src/test/resources/deposit/609/observation.xml", false);
    }  
    

    /**
     * Tests that validateDataset(Dataset) returns false if sbid in dataset and cli do not match.
     * 
     * @throws Exception
     */
    @Test
    public void testValidateDatasetNotMatchingSbids() throws Exception
    {
        ObservationParser parser =
                new ObservationParserImpl(projectRepository, observationRepository, simpleJdbcRepository);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException.expectMessage(
                "Supplied sbid [1] does not match the value in the " + "/dataset/identity/sbid element [12345]");
        parser.parseFile(1, "src/test/resources/observation/good/metadata-v2-good01.xml", false);
    }

    /**
     * Tests that validateDataset(Dataset) throws an Exception if the catalogue has an invalid path
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testInvalidCatalogueName() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException.expectMessage(
                "Catalogue file name [/componentCatalogue.xml] must be a " + "relative path without a leading slash.");
        parser.parseFile(12345, "src/test/resources/observation/bad/observationValidBadCatalogueName.xml", false);
    }

    /**
     * Tests that validateDataset(Dataset) throws an Exception if the catalogue has an invalid path
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testInvalidCatalogueNameDoubleSlash() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException
                .expectMessage("Catalogue file name [src//componentCatalogue.xml] must not contain a double slash.");
        parser.parseFile(12345, "src/test/resources/observation/bad/observationValidBadCatalogueNameDoubleSlash.xml",
                false);
    }

    /**
     * Tests that validateDataset(Dataset) throws an Exception if the catalogue is missing a 'type'
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testCatalogueTypeRequired() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException
                .expectCause(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                        "The content of element 'catalogue' is not " + "complete. One of .* is expected."));
        parser.parseFile(12345, "src/test/resources/observation/bad/metadata-v2-catalogue_type_missing.xml", false);
    }

    /**
     * Tests that validateDataset(Dataset) throws an Exception if the catalogue has an unknown 'type'
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testUnknownCatalogueType() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException.expectCause(XmlObservationParserTest.unmarshallExceptionCausedByException(
                SAXParseException.class, "Value 'FOO' .*" + " must be a value from the enumeration."));
        parser.parseFile(12345, "src/test/resources/observation/bad/metadata-v2-catalogue_type_unknown.xml", false);
    }

    /**
     * Tests that validateDataset(Dataset) throws an Exception if the image has an invalid path
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testInvalidImageName() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException.expectMessage(
                "Image file name [/image.i.clean.restored.fits] must be a relative path without a " + "leading slash.");
        parser.parseFile(12345, "src/test/resources/observation/bad/observationValidBadImageCubeName.xml", false);
    }
    
    /**
     * Tests that validateDataset(Dataset) throws an Exception if the image has an invalid image-type
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testInvalidImageType() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException.expectMessage(
                "Image file name [image.i.clean.restored.fits] contains invalid image type [Incorrect - Type].");
        parser.parseFile(12345, "src/test/resources/observation/bad/observationValidBadImageCubeImageType.xml", false);
    }

    /**
     * Tests that validateDataset(Dataset) throws an Exception if the measurement set has an invalid path
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testInvalidMeasurementSetName() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException
                .expectMessage("Measurement set file name [/src/test/resources/measurement/good/no_file.ms.tar] must "
                        + "be a relative path without a leading slash.");
        parser.parseFile(12345, "src/test/resources/observation/bad/observationValidBadMeasurementSetName.xml", false);
    }

    /**
     * Tests that validateDataset(Dataset) throws an Exception if the measurement set has an invalid path
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testInvalidEvaluationFileName() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException.expectMessage("Evaluation file file name [/src/test/resources/evaluation/good/no_file.pdf] "
                + "must be a relative path without a leading slash.");
        parser.parseFile(12345, "src/test/resources/observation/bad/observationValidBadEvaluationFileName.xml", false);
    }

    /**
     * Tests that parseFile will populate file sizes
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testParseFilePopulatesFileSizes() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        Observation observation =
                parser.parseFile(12345, "src/test/resources/observation/good/metadata-v2-good01.xml", false);
        assertEquals(10, observation.getDepositableArtefacts().size());

        for (DepositableArtefact childDepositableArtefact : observation.getDepositableArtefacts())
        {
            if ("observation.xml".equals(childDepositableArtefact.getFilename()) || 
            		childDepositableArtefact instanceof EncapsulationFile)
            {
                assertNull(childDepositableArtefact.getFilesize());
            }
            else
            {
                assertEquals(
                        ObservationParser.calculateFileSizeKb(new File("src/test/resources/observation/good",
                                childDepositableArtefact.getFilename())),
                        childDepositableArtefact.getFilesize().longValue());
            }
        }
    }
    
    /**
     * Tests that parseFile will populate thumbnails
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testParseFileThumbnails() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        Observation observation =
                parser.parseFile(12345, "src/test/resources/observation/good/metadata-v2-good01.xml", false);
        assertEquals(10, observation.getDepositableArtefacts().size());
        assertEquals("validFile_fits_large_thumbnail.jpg",
                observation.getImageCubes().get(0).getLargeThumbnail().getFilename());
        assertEquals("validFile_fits_small_thumbnail.png",
                observation.getImageCubes().get(0).getSmallThumbnail().getFilename());
        assertEquals("jpg", observation.getImageCubes().get(0).getLargeThumbnail().getFormat());
        assertEquals("png", observation.getImageCubes().get(0).getSmallThumbnail().getFormat());

    }
    
    
    /**
     * Tests that validation of thumbnail size
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testValidateFileThumbnailSize() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException.expectMessage(
                "thumbnail file name [6.33MB-large-thumbnail.jpg] size [6488 KB] is larger than allowed size [3000 KB]");
        parser.parseFile(610, "src/test/resources/deposit/610/observation.xml", false);
    }

    /**
     * Tests that parseFile can handle multiple sbids
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testParseFileObservationWithMultipleSbids() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        Observation observation = parser.parseFile(609, "src/test/resources/deposit/609/observation.xml", false);
        assertEquals(13, observation.getDepositableArtefacts().size());
        assertEquals(5, observation.getSbids().size());
        assertTrue(observation.getSbid() == 609);
        assertEquals(observation.getSbids(), Arrays.asList(610, 611, 612, 613, 614));
    }

    /**
     * Tests that parseFile can handle updates to observations
     * 
     * @throws Exception
     *             IOException
     */
    @Test
    public void testParseFileUpdateObservation() throws Exception
    {
        when(observationRepository.findBySbid(66666)).thenReturn(null);
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        Observation observation = parser.parseFile(66666, "src/test/resources/deposit/66666/observation.xml", false);
        assertEquals(7, observation.getDepositableArtefacts().size());
        assertTrue(observation.getSbid() == 66666);
        assertThat(observation.getImageCubes().get(0).getFilename(), is("beta.image1.fits"));
        assertThat(observation.getImageCubes().size(), is(1));
        assertThat(observation.getDepositStateType(), is(Type.UNDEPOSITED));

        observation.setDepositState(new DepositStateImpl(Type.DEPOSITED, observation));
        when(observationRepository.findBySbid(66666)).thenReturn(observation);
        observation = parser.parseFile(66666, "src/test/resources/deposit/66666/observation-updated.xml", true);
        List<String> actualNames = new ArrayList<>();
        for (ImageCube imageCube : observation.getImageCubes())
        {
            actualNames.add(imageCube.getFilename());
        }
        assertThat(actualNames,
                containsInAnyOrder("beta.image1.fits", "beta.image2.fits", "emu.image.fits", "vlbi.image.fits"));
        
        actualNames.clear();
        for (Catalogue catalogue : observation.getCatalogues())
        {
            actualNames.add(catalogue.getFilename());
        }
        assertThat(actualNames, containsInAnyOrder("selavy-results.islands.xml", "selavy-results.components.xml",
                "selavy-results_polarisation.xml"));

        actualNames.clear();
        for (EvaluationFile evaluationFile : observation.getEvaluationFiles())
        {
            actualNames.add(evaluationFile.getFilename());
        }
        assertThat(actualNames, containsInAnyOrder("evaluations.pdf", "evaluations2.pdf"));

        actualNames.clear();
        for (MeasurementSet measurementSet : observation.getMeasurementSets())
        {
            actualNames.add(measurementSet.getFilename());
        }
        assertThat(actualNames, containsInAnyOrder("beta.ms.tar", "emu.ms.tar", "vlbi.ms.tar"));
        
        assertThat(observation.getObservationMetadataFileDepositable().getFilename(), is("observation.xml"));
        assertThat(observation.getDepositStateType(), is(Type.UNDEPOSITED));
        assertEquals(15, observation.getDepositableArtefacts().size());
        assertTrue(observation.getSbid() == 66666);
    }

    @Test
    public void testParseFileUpdateRequiresDeposited() throws Exception
    {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Asked to redeposit an observation that has not finished depositing");
        
        Observation observation = new Observation();
        observation.setDepositState(new DepositStateImpl(Type.DEPOSITING, observation));
        when(observationRepository.findBySbid(66666)).thenReturn(observation);

        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);

        parser.parseFile(66666, "src/test/resources/deposit/66666/observation-updated.xml", true);
    }

    @Test
    public void testParseFileUpdateRequiresExisting() throws Exception
    {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Asked to redeposit an observation that does not exist");

        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);

        parser.parseFile(66666, "src/test/resources/deposit/66666/observation-updated.xml", true);
    }
    
    @Test
    public void testCalculateFilesizeInKB() throws Exception
    {
        File file = mock(File.class);
        when(file.length()).thenReturn(0L);
        assertEquals(0, ObservationParser.calculateFileSizeKb(file));

        when(file.length()).thenReturn(1023L);
        assertEquals(1, ObservationParser.calculateFileSizeKb(file));

        when(file.length()).thenReturn(1025L);
        assertEquals(2, ObservationParser.calculateFileSizeKb(file));
    }

    /**
     * Tests that parseFile(Integer sbid, String observationMetadataFile) handles a long file name.
     * 
     * @throws Exception
     *             MalformedFileException
     */
    @Test
    public void testValidLongFileIdObservationArtifact() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException.expectMessage("image_cube file name ["
                + "validFilexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxd"
                + "Filexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxd"
                + "Filexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxd"
                + "Filexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxd"
                + "Filexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxd"
                + "Filexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.fits"
                + "] does not exist.");
        parser.parseFile(12345, "src/test/resources/observation/bad/observationValidLongFileId.xml", false);
    }

    @Test
    public void testFileNotExist() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException.expectMessage("image_cube file name [not_exist_file.fits] does not exist.");
        parser.parseFile(12345, "src/test/resources/observation/bad/observationBlankFilename.xml", false);
    }

    /**
     * tests that the start date must be before the end date in observations
     * @throws Exception an exception
     */
    @Test
    public void testDatesInOrder() throws Exception
    {
        ObservationParser parser = new ObservationParserImpl(projectRepository, observationRepository,
                simpleJdbcRepository, fileIdMaxSize, thumbnailMaxSize);
        expectedException.expect(ObservationParser.MalformedFileException.class);
        expectedException.expectMessage("Observation contains an observation End date/time "
                    + "which is earlier than or equal to the observation start time.");
        parser.parseFile(12345, "src/test/resources/observation/bad/observationEndBeforeStartDate.xml", false);
    }
}
