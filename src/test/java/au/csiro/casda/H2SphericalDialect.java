package au.csiro.casda;

import java.sql.Types;

import org.hibernate.spatial.dialect.h2geodb.GeoDBDialect;

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
 * Dialect used for H2 in memory database instances.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class H2SphericalDialect extends GeoDBDialect
{
    private static final long serialVersionUID = -4396082809106275145L;

    /**
     * Constructor, maps the "OTHER" database type to text, because the H2 dialect doesn't include any mapping for
     * "OTHER".
     */
    public H2SphericalDialect()
    {
        super();
        registerColumnType(Types.OTHER, "text");
        registerHibernateType(Types.OTHER, "text");
    }
}
