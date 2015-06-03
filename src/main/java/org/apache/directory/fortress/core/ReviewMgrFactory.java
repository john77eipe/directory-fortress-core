/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.fortress.core;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.fortress.core.util.Config;
import org.apache.directory.fortress.core.util.ClassUtil;
import org.apache.directory.fortress.core.impl.ReviewMgrImpl;
import org.apache.directory.fortress.core.model.Session;
import org.apache.directory.fortress.core.rest.ReviewMgrRestImpl;
import org.apache.directory.fortress.core.util.VUtil;

/**
 * Creates an instance of the ReviewMgr object.
 * <p/>
 * The default implementation class is specified as {@link ReviewMgrImpl} but can be overridden by
 * adding the {@link GlobalIds#REVIEW_IMPLEMENTATION} config property.
 * <p/>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ReviewMgrFactory
{
    private static String reviewClassName = Config.getProperty(GlobalIds.REVIEW_IMPLEMENTATION);
    private static final String CLS_NM = ReviewMgrFactory.class.getName();

    /**
     * Create and return a reference to {@link ReviewMgr} object using HOME context.
     *
     * @return instance of {@link ReviewMgr}.
     * @throws SecurityException in the event of failure during instantiation.
     */
    public static ReviewMgr createInstance()
        throws SecurityException
    {
        return createInstance( GlobalIds.HOME );
    }

    /**
     * Create and return a reference to {@link ReviewMgr} object.
     *
     * @param contextId maps to sub-tree in DIT, for example ou=contextId, dc=jts, dc = com.
     * @return instance of {@link ReviewMgr}.
     * @throws SecurityException in the event of failure during instantiation.
     */
    public static ReviewMgr createInstance(String contextId)
        throws SecurityException
    {
        VUtil.assertNotNull(contextId, GlobalErrIds.CONTEXT_NULL, CLS_NM + ".createInstance");
        ReviewMgr reviewMgr;

        if ( StringUtils.isEmpty( reviewClassName ) )
        {
            if(GlobalIds.IS_REST)
            {
                reviewMgr = new ReviewMgrRestImpl();
            }
            else
            {
                reviewMgr = new ReviewMgrImpl();
            }
        }
        else
        {
            reviewMgr = ( ReviewMgr ) ClassUtil.createInstance(reviewClassName);
        }

        reviewMgr.setContextId(contextId);
        return reviewMgr;
    }

    /**
     * Create and return a reference to {@link ReviewMgr} object using HOME context.
     *
     * @param adminSess contains a valid Fortress A/RBAC Session object.
     * @return instance of {@link ReviewMgr}.
     * @throws SecurityException in the event of failure during instantiation.
     */
    public static ReviewMgr createInstance(Session adminSess)
        throws SecurityException
    {
        return createInstance( GlobalIds.HOME, adminSess );
    }

    /**
     * Create and return a reference to {@link ReviewMgr} object.
     *
     * @param contextId maps to sub-tree in DIT, for example ou=contextId, dc=jts, dc = com.
     * @param adminSess contains a valid Fortress A/RBAC Session object.
     * @return instance of {@link ReviewMgr}.
     * @throws SecurityException in the event of failure during instantiation.
     */
    public static ReviewMgr createInstance(String contextId, Session adminSess)
        throws SecurityException
    {
        ReviewMgr reviewMgr = createInstance(contextId);
        reviewMgr.setAdmin(adminSess);
        return reviewMgr;
    }
}

