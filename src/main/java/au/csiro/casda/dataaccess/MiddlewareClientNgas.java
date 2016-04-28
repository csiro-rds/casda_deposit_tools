package au.csiro.casda.dataaccess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;


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
 * An implementation of the middleware client interface for interacting with NGAS.
 * 
 * Copyright 2013, CSIRO Australia All rights reserved.
 * 
 */
@Component
@Profile("ngas")
public class MiddlewareClientNgas implements MiddlewareClient
{
    private static final Logger logger = LoggerFactory.getLogger(MiddlewareClientNgas.class);

    private final URI ngasBaseUrl;
    private final CloseableHttpClient client;

    /**
     * Ngas commands
     * 
     * Copyright 2015, CSIRO Australia All rights reserved.
     * 
     */
    private enum Command
    {
        ARCHIVE, CHECKFILE, RETRIEVE, STATUS;

        private final String command;

        Command()
        {
            this.command = "/" + toString();
        }

        Command(String command)
        {
            this.command = command;
        }
    }

    /**
     * Create a new MiddlewareClientNgas that uses the given httpClient and baseUrl
     * 
     * @param baseUrl
     *            The base URL to use for the NGAS Server.
     * @throws URISyntaxException
     *             if unable to create ngas client
     */
    @Autowired
    public MiddlewareClientNgas(@Value("${ngas.baseurl}") String baseUrl) throws URISyntaxException
    {
        this.client = HttpClientBuilder.create().setUserAgent("CASDA NGAS Middleware Client").useSystemProperties()
                .build();
        this.ngasBaseUrl = new URI(baseUrl);
    }

    @Override
    public File retrieve(String fileId, RetrieveHandler callback) throws MiddlewareClientException
    {
        try
        {
            HttpGet get = createNgasGet(Command.RETRIEVE, 
                    ListUtils.unmodifiableList(Arrays.asList(pair("file_id", fileId))));
            logger.debug("Retrieving from NGAS file_id={}", fileId);
            HttpResponse response = client.execute(get);
            HttpEntity entity = checkResponse(get.getURI(), response);
            logger.debug("Response from NGAS: {}", response.getStatusLine());
            
            try (InputStream is = entity.getContent())
            {
                String filename = extractFilenameFromDisposition(response.getHeaders("Content-Disposition"));
                return callback.onRetrieve(is, filename);
            }
        }
        catch (IOException e)
        {
            throw new MiddlewareClientException(e);
        }
    }

    /**
     * Check Content-Disposition headers for a filename and return it. If a filename isn't found, returns "".
     * 
     * @param contentDispositions
     *            The array of "Content-Disposition" headers from a HTTP Response.
     * @return The filename, or the empty string if no filename exists.
     */
    private String extractFilenameFromDisposition(Header[] contentDispositions)
    {
        if (contentDispositions.length > 1)
        {
            logger.warn("More than one Content-Disposition header ({}), only using the first",
                    Arrays.toString(contentDispositions));
        }
        String filename = "";
        if (contentDispositions.length > 0)
        {
            for (HeaderElement element : contentDispositions[0].getElements())
            {
                NameValuePair filenameParam = element.getParameterByName("filename");
                if (filenameParam != null)
                {
                    filename = filenameParam.getValue();
                    break;
                }
            }
        }
        return filename;
    }

    /**
     * Factory method that creates a {@link NameValuePair}, used purely as syntactic sugar.
     * 
     * @param name
     *            The name
     * @param value
     *            The value
     * @return The new {@link NameValuePair} for the name and value.
     */
    private static NameValuePair pair(String name, String value)
    {
        return new BasicNameValuePair(name, value);
    }

    private HttpGet createNgasGet(final Command command, final List<NameValuePair> params)
            throws MiddlewareClientException
    {
        final URI url;
        try
        {
            url = new URIBuilder(ngasBaseUrl).setPath(command.command).addParameters(params).build();
        }
        catch (URISyntaxException e)
        {
            throw new MiddlewareClientException(e);
        }
        return new HttpGet(url);
    }

    private HttpEntity checkResponse(final URI uri, final HttpResponse response) throws HttpResponseException,
            ClientProtocolException
    {
        StatusLine statusLine = response.getStatusLine();
        HttpEntity entity = response.getEntity();
        if (entity == null)
        {
            throw new ClientProtocolException("Response to " + uri + " contains no content");
        }
        if (HttpStatus.valueOf(statusLine.getStatusCode()).is4xxClientError())
        {
            logger.error("Failed to process query {}", uri);
            throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
        }
        return entity;
    }

}
