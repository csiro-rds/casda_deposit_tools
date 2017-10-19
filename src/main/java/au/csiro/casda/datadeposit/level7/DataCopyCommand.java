package au.csiro.casda.datadeposit.level7;



import java.io.File;
import java.time.Instant;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import au.csiro.casda.AppConfig;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.DataDepositMessageBuilder;
import au.csiro.casda.datadeposit.copy.CopyDataException;
import au.csiro.casda.datadeposit.exception.CreateChecksumException;
import au.csiro.casda.entity.observation.Level7Collection;
import au.csiro.casda.logging.CasdaLogMessageBuilderFactory;
import au.csiro.casda.logging.DataLocation;
import au.csiro.logging.CasdaDataDepositEvents;

/**
 * DataCopyCommandLineImporter implements a Spring CommandLineRunner that can be used to copy the file associated with a
 * level 7 collection to a staging area and record entries for each file.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Import(AppConfig.class)
public class DataCopyCommand extends
        ArgumentsDrivenCommandLineTool<DataCopyCommandLineArgumentsParser.CommandLineArguments>
{
    /**
     * The name of this tool.
     */
    public static final String TOOL_NAME = "data_copy";

    private static final Logger logger = LoggerFactory.getLogger(DataCopyCommand.class);

    private Level7CollectionDataCopier observationParser;

    private DataCopyCommandLineArgumentsParser commandLineArgumentsParser =
            new DataCopyCommandLineArgumentsParser();

    /**
     * main method used to run this CommandLineImporter
     * 
     * @param args
     *            the command-line arguments
     */
    public static void main(String[] args)
    {
        runCommandLineTool(DataCopyCommand.class, logger, args);
    }

    /**
     * Constructor
     * 
     * @param observationParser
     *            a au.csiro.casda.datadeposit.observation.ObservationParser used to perform the import of an
     *            Observation metadata file.
     */
    @Autowired
    public DataCopyCommand(Level7CollectionDataCopier observationParser)
    {
        super();
        this.observationParser = observationParser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String... args)
    {
        // use a JCommander to process the command line arguments
        parseCommandLineArguments(logger, args);

        logger.info("Starting data copy for level 7 image collection");

        long parentId = commandLineArgumentsParser.getArgs().getParentId();
        String collectionFolder = commandLineArgumentsParser.getArgs().getCollectionFolder();

        Instant startTime = Instant.now();

        Level7Collection level7Collection = null;
        try
        {
            level7Collection = observationParser.copyData(parentId, collectionFolder);
        }
        catch (CopyDataException | CreateChecksumException e)
        {
            //TODO: Need new event code
            logger.error(CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(CasdaDataDepositEvents.E003).add(parentId)
                    .toString(), e);
            System.exit(1);
        }

        Instant endTime = Instant.now();

        long filesizeInBytes = FileUtils.sizeOfDirectory(new File(collectionFolder));

        //TODO: Change event code
        DataDepositMessageBuilder messageBuilder = CasdaDataDepositEvents.E012.messageBuilder() //
                .add(parentId) //
                .addStartTime(startTime) //
                .addEndTime(endTime) //
                .addSource(DataLocation.RTC) //
                .addDestination(DataLocation.CASDA_DB) //
                .addVolumeBytes(filesizeInBytes) //
                .addFileId(level7Collection.getUniqueIdentifier().replace("/", "-"));
        logger.info(messageBuilder.toString());

        System.exit(0);
    }

    /**
     * Gets the arguments parser. Mostly useful for unit testing.
     * 
     * @return ObservationCommandLineArgumentsParser the parser
     */
    public DataCopyCommandLineArgumentsParser getCommandLineArgumentsParser()
    {
        return commandLineArgumentsParser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CasdaDataDepositEvents getMalformedParametersEvent()
    {
        //TODO: Change event code
        return CasdaDataDepositEvents.E033;
    }
}
