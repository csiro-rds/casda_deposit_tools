package au.csiro.casda.datadeposit.observation.validation;

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


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for arguments expected to not be null
 * 
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.SOURCE)
public @interface NonNull
{
}
