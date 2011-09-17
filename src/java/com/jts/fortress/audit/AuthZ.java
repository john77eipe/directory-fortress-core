/*
 * Copyright (c) 2009-2011. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress.audit;


import java.io.Serializable;

/**
 * This entity class contains OpenLDAP slapd access log records that correspond to authorization attempts made to the directory.
 * <p/>
 * The auditSearch Structural object class is used to store authorization events that can later be queried via ldap API.<br />
 * <p/>
 * <code>For  the  Search class the reqScope attribute contains the scope of the</code><br />
 * <code>original search request, using the values specified for  the  LDAP  URL</code><br />
 * <code>format. I.e.  base, one, sub, or subord.  The reqDerefAliases attribute</code><br />
 * <code>is one of never, finding, searching, or always,  denoting  how  aliases</code><br />
 * <code>will  be  processed during the search.  The reqAttrsOnly attribute is a</code><br />
 * <code>Boolean value showing TRUE if only attribute names were  requested,  or</code><br />
 * <code>FALSE  if  attributes  and  their values were requested.  The reqFilter</code><br />
 * <code>attribute carries the filter used in the search request.   The  reqAttr</code><br />
 * <code>attribute  lists  the  requested attributes if specific attributes were</code><br />
 * <code>requested.  The reqEntries attribute is the integer count of  how  many</code><br />
 * <code>entries  were  returned  by  this search request.  The reqSizeLimit and</code><br />
 * <code>reqTimeLimit attributes indicate what  limits  were  requested  on  the</code><br />
 * <code>search operation.</code><br />
 * <ul>
 * <li>  ------------------------------------------
 * <li> <code>objectclass  (  1.3.6.1.4.1.4203.666.11.5.2.11</code>
 * <li> <code>NAME 'auditSearch'</code>
 * <li> <code>DESC 'Search operation'</code>
 * <li> <code>SUP auditReadObject STRUCTURAL</code>
 * <li> <code>MUST ( reqScope $ reqDerefAliases $ reqAttrsOnly )</code>
 * <li> <code>MAY ( reqFilter $ reqAttr $ reqEntries $ reqSizeLimit $</code>
 * <li> <code>reqTimeLimit ) )</code>
 * <li> ------------------------------------------
 * </ul>
 * <p/>
 * Note this class uses descriptions pulled from man pages on slapd access log.
 * <p/>

 *
 * @author smckinn
 * @created April 1, 2010
 */
public class AuthZ implements Serializable
{
    private String createTimestamp;
    private String creatorsName;
    private String entryCSN;
    private String entryDN;
    private String entryUUID;
    private String hasSubordinates;
    private String modifiersName;
    private String modifyTimestamp;
    private String objectClass;
    private String reqAttr;
    private String reqAttrsOnly;
    private String reqAuthzID;
    private String reqControls;
    private String reqDN;
    private String reqDerefAliases;
    private String reqEnd;
    private String reqEntries;
    private String reqFilter;
    private String reqResult;
    private String reqScope;
    private String reqSession;
    private String reqSizeLimit;
    private String reqStart;
    private String reqTimeLimit;
    private String reqType;
    private String structuralObjectClass;
    private String subschemaSubentry;

    /**
     * Get the attribute that maps to 'reqStart' which provides the start time of the operation which is also the rDn for the node.
     * These time attributes use generalizedTime syntax. The reqStart attribute is also used as the RDN for each log entry.
     *
     * @return attribute that maps to 'reqStart' in 'auditSearch' object class.
     */
    public String getCreateTimestamp()
    {
        return createTimestamp;
    }

    /**
     * Set the attribute that maps to 'reqStart' which provides the start time of the operation which is also the rDn for the node.
     * These time attributes use generalizedTime syntax. The reqStart attribute is also used as the RDN for each log entry.
     *
     * @param createTimestamp attribute that maps to 'reqStart' in 'auditSearch' object class.
     */
    public void setCreateTimestamp(String createTimestamp)
    {
        this.createTimestamp = createTimestamp;
    }


    /**
     * Return the user dn containing the identity of log user who added the audit record.  This will be the system user that
     * is configured for performing slapd access log operations on behalf of Fortress.
     * The config property name {@link com.jts.fortress.ldap.PoolMgr#LDAP_LOG_POOL_UID} contains the audit log system user id.
     *
     * @return value that maps to 'creatorsName' attribute on 'auditSearch' object class.
     */
    public String getCreatorsName()
    {
        return creatorsName;
    }

    /**
     * Set the user dn containing the identity of log user who added the audit record.  This will be the system user that
     * is configured for performing slapd access log operations on behalf of Fortress.
     * The config property name {@link com.jts.fortress.ldap.PoolMgr#LDAP_LOG_POOL_UID} contains the audit log system user id.
     *
     * @param creatorsName maps to 'creatorsName' attribute on 'auditSearch' object class.
     */
    public void setCreatorsName(String creatorsName)
    {
        this.creatorsName = creatorsName;
    }


    /**
     * Return the Change Sequence Number (CSN) containing sequence number that is used for OpenLDAP synch replication functionality.
     *
     * @return attribute that maps to 'entryCSN' on 'auditSearch' object class.
     */
    public String getEntryCSN()
    {
        return entryCSN;
    }

    /**
     * Set the Change Sequence Number (CSN) containing sequence number that is used for OpenLDAP synch replication functionality.
     *
     * @param entryCSN maps to 'entryCSN' attribute on 'auditSearch' object class.
     */
    public void setEntryCSN(String entryCSN)
    {
        this.entryCSN = entryCSN;
    }

    /**
     * Get the entry dn for bind object stored in directory.  This attribute uses the 'reqStart' along with suffix for log.
     *
     * @return attribute that maps to 'entryDN' on 'auditSearch' object class.
     */
    public String getEntryDN()
    {
        return entryDN;
    }

    /**
     * Set the entry dn for bind object stored in directory.  This attribute uses the 'reqStart' along with suffix for log.
     *
     * @param entryDN attribute that maps to 'entryDN' on 'auditSearch' object class.
     */
    public void setEntryDN(String entryDN)
    {
        this.entryDN = entryDN;
    }


    /**
     * Get the attribute that contains the Universally Unique ID (UUID) of the corresponding 'auditSearch' record.
     *
     * @return value that maps to 'entryUUID' attribute on 'auditSearch' object class.
     */
    public String getEntryUUID()
    {
        return entryUUID;
    }

    /**
     * Set the attribute that contains the Universally Unique ID (UUID) of the corresponding 'auditSearch' record.
     *
     * @param entryUUID that maps to 'entryUUID' attribute on 'auditSearch' object class.
     */
    public void setEntryUUID(String entryUUID)
    {
        this.entryUUID = entryUUID;
    }

    /**
     * Get the attribute that corresponds to the boolean value hasSubordinates.
     *
     * @return value that maps to 'hasSubordinates' attribute on 'auditSearch' object class.
     */
    public String getHasSubordinates()
    {
        return hasSubordinates;
    }

    /**
     * Set the attribute that corresponds to the boolean value hasSubordinates.
     *
     * @param hasSubordinates maps to same name on 'auditSearch' object class.
     */
    public void setHasSubordinates(String hasSubordinates)
    {
        this.hasSubordinates = hasSubordinates;
    }

    /**
     * Return the user dn containing the identity of log user who last modified the audit record.  This will be the system user that
     * is configured for performing slapd access log operations on behalf of Fortress.
     * The config property name {@link com.jts.fortress.ldap.PoolMgr#LDAP_LOG_POOL_UID} contains the audit log system user id.
     *
     * @return value that maps to 'modifiersName' attribute on 'auditSearch' object class.
     */
    public String getModifiersName()
    {
        return modifiersName;
    }

    /**
     * Set the user dn containing the identity of log user who modified the audit record.  This will be the system user that
     * is configured for performing slapd access log operations on behalf of Fortress.
     * The config property name {@link com.jts.fortress.ldap.PoolMgr#LDAP_LOG_POOL_UID} contains the audit log system user id.
     *
     * @param modifiersName maps to 'modifiersName' attribute on 'auditSearch' object class.
     */
    public void setModifiersName(String modifiersName)
    {
        this.modifiersName = modifiersName;
    }

    /**
     * Get the attribute that maps to 'modifyTimestamp' which provides the last time audit record was changed.
     * The time attributes use generalizedTime syntax.
     *
     * @return attribute that maps to 'modifyTimestamp' in 'auditSearch' object class.
     */
    public String getModifyTimestamp()
    {
        return modifyTimestamp;
    }

    /**
     * Set the attribute that maps to 'modifyTimestamp' which provides the last time audit record was changed.
     * The time attributes use generalizedTime syntax.
     *
     * @param modifyTimestamp attribute that maps to same name in 'auditSearch' object class.
     */
    public void setModifyTimestamp(String modifyTimestamp)
    {
        this.modifyTimestamp = modifyTimestamp;
    }

    /**
     * Get the object class name of the audit record.  For this entity, this value will always be 'auditSearch'.
     *
     * @return value that maps to 'objectClass' attribute on 'auditSearch' obejct class.
     */
    public String getObjectClass()
    {
        return objectClass;
    }

    /**
     * Set the object class name of the audit record.  For this entity, this value will always be 'auditSearch'.
     *
     * @param objectClass value that maps to same name on 'auditSearch' obejct class.
     */
    public void setObjectClass(String objectClass)
    {
        this.objectClass = objectClass;
    }

    /**
     * The  reqAuthzID  attribute  is  the  distinguishedName of the user that
     * performed the operation.  This will usually be the  same  name  as  was
     * established  at  the  start of a session by a Bind request (if any) but
     * may be altered in various circumstances.
     * For Fortress bind operations this will map to {@link com.jts.fortress.rbac.User#userId}
     *
     * @return value that maps to 'reqAuthzID' on 'auditSearch' object class.
     */
    public String getReqAuthzID()
    {
        return reqAuthzID;
    }

    /**
     * The  reqAuthzID  attribute  is  the  distinguishedName of the user that
     * performed the operation.  This will usually be the  same  name  as  was
     * established  at  the  start of a session by a Bind request (if any) but
     * may be altered in various circumstances.
     * For Fortress bind operations this will map to {@link com.jts.fortress.rbac.User#userId}
     *
     * @return value that maps to 'reqAuthzID' on 'auditSearch' object class.
     */
    public void setReqAuthzID(String reqAuthzID)
    {
        this.reqAuthzID = reqAuthzID;
    }

    /**
     * The reqControls and reqRespControls attributes carry any controls  sent
     * by  the  client  on  the  request  and  returned  by  the server in the
     * response, respectively. The attribute  values  are  just  uninterpreted
     * octet strings.
     *
     * @return value that maps to 'reqControls' attribute on 'auditSearch' object class.
     */
    public String getReqControls()
    {
        return reqControls;
    }

    /**
     * The reqControls and reqRespControls attributes carry any controls  sent
     * by  the  client  on  the  request  and  returned  by  the server in the
     * response, respectively. The attribute  values  are  just  uninterpreted
     * octet strings.
     *
     * @param reqControls maps to same name attribute on 'auditSearch' object class.
     */
    public void setReqControls(String reqControls)
    {
        this.reqControls = reqControls;
    }

    /**
     * The reqDN attribute is the  distinguishedName  of  the  target  of  the
     * operation.  E.g.,  for  a Bind request, this is the Bind DN. For an Add
     * request, this is the DN of the entry being added. For a Search request,
     * this is the base DN of the search.
     *
     * @return value that map to 'reqDN' attribute on 'auditSearch' object class.
     */
    public String getReqDN()
    {
        return reqDN;
    }

    /**
     * The reqDN attribute is the  distinguishedName  of  the  target  of  the
     * operation.  E.g.,  for  a Bind request, this is the Bind DN. For an Add
     * request, this is the DN of the entry being added. For a Search request,
     * this is the base DN of the search.
     *
     * @param reqDN maps to 'reqDN' attribute on 'auditSearch' object class.
     */
    public void setReqDN(String reqDN)
    {
        this.reqDN = reqDN;
    }

    /**
     * reqEnd provide the end time of the operation. It uses generalizedTime syntax.
     *
     * @return value that maps to 'reqEnd' attribute on 'auditSearch' object class.
     */
    public String getReqEnd()
    {
        return reqEnd;
    }

    /**
     * reqEnd provide the end time of the operation. It uses generalizedTime syntax.
     *
     * @param reqEnd value that maps to same name on 'auditSearch' object class.
     */
    public void setReqEnd(String reqEnd)
    {
        this.reqEnd = reqEnd;
    }

    /**
     * The  reqResult  attribute  is  the  numeric  LDAP  result  code  of the
     * operation, indicating either success or a particular LDAP  error  code.
     * An  error code may be accompanied by a text error message which will be
     * recorded in the reqMessage attribute.
     *
     * @return value that maps to 'reqResult' attribute on 'auditSearch' object class.
     */
    public String getReqResult()
    {
        return reqResult;
    }

    /**
     * The  reqResult  attribute  is  the  numeric  LDAP  result  code  of the
     * operation, indicating either success or a particular LDAP  error  code.
     * An  error code may be accompanied by a text error message which will be
     * recorded in the reqMessage attribute.
     *
     * @param reqResult maps to same name on 'auditSearch' object class.
     */
    public void setReqResult(String reqResult)
    {
        this.reqResult = reqResult;
    }

    /**
     * The reqSession attribute is an implementation-specific identifier  that
     * is  common to all the operations associated with the same LDAP session.
     * Currently this is slapd's internal connection ID, stored in decimal.
     *
     * @return value that maps to 'reqSession' attribute on 'auditSearch' object class.
     */
    public String getReqSession()
    {
        return reqSession;
    }

    /**
     * The reqSession attribute is an implementation-specific identifier  that
     * is  common to all the operations associated with the same LDAP session.
     * Currently this is slapd's internal connection ID, stored in decimal.
     *
     * @param reqSession maps to same name on 'auditSearch' object class.
     */
    public void setReqSession(String reqSession)
    {
        this.reqSession = reqSession;
    }

    /**
     * reqStart provide the start of the operation,  They  use generalizedTime syntax.
     * The reqStart attribute is also used as the RDN for each log entry.
     *
     * @return value that maps to 'reqStart' attribute on 'auditSearch' object class.
     */
    public String getReqStart()
    {
        return reqStart;
    }

    /**
     * reqStart provide the start of the operation,  They  use generalizedTime syntax.
     * The reqStart attribute is also used as the RDN for each log entry.
     *
     * @param reqStart maps to same name on 'auditSearch' object class.
     */
    public void setReqStart(String reqStart)
    {
        this.reqStart = reqStart;
    }

    /**
     * The  reqType  attribute  is  a  simple  string  containing  the type of
     * operation being logged, e.g.  add, delete, search,  etc.  For  extended
     * operations,  the  type also includes the OID of the extended operation,
     * e.g.  extended(1.1.1.1)
     *
     * @return value that maps to 'reqType' attribute on 'auditSearch' object class.
     */
    public String getReqType()
    {
        return reqType;
    }

    /**
     * The  reqType  attribute  is  a  simple  string  containing  the type of
     * operation being logged, e.g.  add, delete, search,  etc.  For  extended
     * operations,  the  type also includes the OID of the extended operation,
     * e.g.  extended(1.1.1.1)
     *
     * @param reqType maps to same name on 'auditSearch' object class.
     */
    public void setReqType(String reqType)
    {
        this.reqType = reqType;
    }

    /**
     * Returns the name of the structural object class that is used to log the event.  For this entity
     * this value will always be 'auditSearch'.
     *
     * @return value that maps to 'structuralObjectClass' attribute that contains the name 'auditSearch'.
     */
    public String getStructuralObjectClass()
    {
        return structuralObjectClass;
    }

    /**
     * Returns the name of the structural object class that is used to log the event.  For this entity
     * this value will always be 'auditSearch'.
     *
     * @param structuralObjectClass maps to same name on 'auditSearch' object class.
     */
    public void setStructuralObjectClass(String structuralObjectClass)
    {
        this.structuralObjectClass = structuralObjectClass;
    }

    /**
     * The reqEntries attribute is the integer count of  how  many entries  were  returned  by  this search request.
     *
     * @return value that maps to 'reqEntries' attribute on 'auditSearch' object class
     */
    public String getReqEntries()
    {
        return reqEntries;
    }

    /**
     * The reqEntries attribute is the integer count of  how  many entries  were  returned  by  this search request.
     *
     * @param reqEntries maps to same name on 'auditSearch' object class
     */
    public void setReqEntries(String reqEntries)
    {
        this.reqEntries = reqEntries;
    }

    /**
     * The reqAttr attribute lists the requested attributes if specific attributes were requested.
     *
     * @return value maps to 'reqAttr' on 'auditSearch' object class.
     */
    public String getReqAttr()
    {
        return reqAttr;
    }

    /**
     * The reqAttr attribute lists the requested attributes if specific attributes were requested.
     *
     * @param reqAttr maps to same name on 'auditSearch' object class.
     */
    public void setReqAttr(String reqAttr)
    {
        this.reqAttr = reqAttr;
    }

    /**
     * The reqAttrsOnly attribute is a Boolean value showing TRUE if only attribute names
     * were  requested, or FALSE if attributes and their values were requested.
     * For Fortress authorization requests this value will always be TRUE.
     *
     * @return value maps to 'reqAttrsOnly' on 'auditSearch' object class.
     */
    public String getReqAttrsOnly()
    {
        return reqAttrsOnly;
    }

    /**
     * The reqAttrsOnly attribute is a Boolean value showing TRUE if only attribute names
     * were  requested, or FALSE if attributes and their values were requested.
     * For Fortress authorization requests this value will always be TRUE.
     *
     * @param reqAttrsOnly maps to same name on 'auditSearch' object class.
     */
    public void setReqAttrsOnly(String reqAttrsOnly)
    {
        this.reqAttrsOnly = reqAttrsOnly;
    }

    /**
     * The reqFilter attribute carries the filter used in the search request.
     * <p/>
     * For Fortress authorization events this will contain the following:
     * <ul>
     * <li>userId: {@link com.jts.fortress.rbac.User#userId}
     * <li>activated roles: {@link com.jts.fortress.rbac.UserRole#name}
     * <li>object name: {@link com.jts.fortress.rbac.Permission#objectName}
     * <li>operation name: {@link com.jts.fortress.rbac.Permission#opName}
     * </ul>
     *
     * @return value that maps to 'reqFilter' attribute on 'auditSearch' object class.
     */
    public String getReqFilter()
    {
        return reqFilter;
    }

    /**
     * The reqFilter attribute carries the filter used in the search request.
     * <p/>
     * For Fortress authorization events this will contain the following:
     * <ul>
     * <li>userId: {@link com.jts.fortress.rbac.User#userId}
     * <li>activated roles: {@link com.jts.fortress.rbac.UserRole#name}
     * <li>object name: {@link com.jts.fortress.rbac.Permission#objectName}
     * <li>operation name: {@link com.jts.fortress.rbac.Permission#opName}
     * </ul>
     *
     * @param reqFilter maps to same name on 'auditSearch' object class.
     */
    public void setReqFilter(String reqFilter)
    {
        this.reqFilter = reqFilter;
    }

    /**
     * The reqScope attribute contains the scope of the original search request, using
     * the values specified for the LDAP URL format. I.e. base, one, sub, or subord.
     *
     * @return value that maps to 'reqScope' attribute on 'auditSearch' object class.
     */
    public String getReqScope()
    {
        return reqScope;
    }

    /**
     * The reqScope attribute contains the scope of the original search request, using
     * the values specified for the LDAP URL format. I.e. base, one, sub, or subord.
     *
     * @param reqScope maps to same name on 'auditSearch' object class.
     */
    public void setReqScope(String reqScope)
    {
        this.reqScope = reqScope;
    }

    /**
     * The reqSizeLimit attribute indicate what limits were requested on the search operation.
     *
     * @return value that maps to 'reqSizeLimit' attribute on 'auditSearch' object class.
     */
    public String getReqSizeLimit()
    {
        return reqSizeLimit;
    }

    /**
     * The reqSizeLimit attribute indicate what limits were requested on the search operation.
     *
     * @param reqSizeLimit maps to same name on 'auditSearch' object class.
     */
    public void setReqSizeLimit(String reqSizeLimit)
    {
        this.reqSizeLimit = reqSizeLimit;
    }

    /**
     * The reqTimeLimit attribute indicate what limits were requested on the search operation.
     *
     * @return value that maps to 'reqTimeLimit' attribute on 'auditSearch' object class.
     */
    public String getReqTimeLimit()
    {
        return reqTimeLimit;
    }

    /**
     * The reqTimeLimit attribute indicate what limits were requested on the search operation.
     *
     * @param reqTimeLimit maps to same name on 'auditSearch' object class.
     */
    public void setReqTimeLimit(String reqTimeLimit)
    {
        this.reqTimeLimit = reqTimeLimit;
    }

    /**
     * Return the subschemaSubentry attribute from the audit entry.
     *
     * @return value that maps to 'subschemaSubentry' on 'auditSearch' object class.
     */
    public String getSubschemaSubentry()
    {
        return subschemaSubentry;
    }

    /**
     * Set the subschemaSubentry attribute from the audit entry.
     *
     * @param subschemaSubentry maps to same name on 'auditSearch' object class.
     */
    public void setSubschemaSubentry(String subschemaSubentry)
    {
        this.subschemaSubentry = subschemaSubentry;
    }

    /**
     * The reqDerefAliases attribute is on of never, finding, searching, or always, denoting how aliases
     * will be processed during the search.
     *
     * @returns value that maps to 'reqDerefAliases' on 'auditSearch' object class.
     */
    public String getReqDerefAliases()
    {
        return reqDerefAliases;
    }

    /**
     * The reqDerefAliases attribute is on of never, finding, searching, or always, denoting how aliases
     * will be processed during the search.
     *
     * @param reqDerefAliases maps to same name on 'auditSearch' object class.
     */
    public void setReqDerefAliases(String reqDerefAliases)
    {
        this.reqDerefAliases = reqDerefAliases;
    }
}

