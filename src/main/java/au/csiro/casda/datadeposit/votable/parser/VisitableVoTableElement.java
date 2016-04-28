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

/**
 * Classes should implement this interface if they support being visited by
 * a {@link VoTableElementVisitor}.
 * <p>
 * Copyright 2014, CSIRO Australia
 * All rights reserved.
 */
public interface VisitableVoTableElement
{
    /**
     * Accepts the visitor.  Implementations should call the visitors's 'visit', 
     * method, passing themselves as the argument, and also should call 'accept' 
     * on any 'child' elements that would also need to be visited.  The root
     * element should also call the visitor's 'stop' when the last child element 
     * has been visited.
     * <p>
     * @param visitor
     *          the visitor to accept.
     */
    public void accept(VoTableElementVisitor visitor);
}
