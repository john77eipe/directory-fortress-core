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
package org.apache.directory.fortress.core.impl;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicy;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapAttributeInUseException;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.fortress.core.model.Address;
import org.apache.directory.fortress.core.model.AdminRole;
import org.apache.directory.fortress.core.model.OrgUnit;
import org.apache.directory.fortress.core.model.PwMessage;
import org.apache.directory.fortress.core.model.Role;
import org.apache.directory.fortress.core.model.Session;
import org.apache.directory.fortress.core.model.User;
import org.apache.directory.fortress.core.model.UserAdminRole;
import org.apache.directory.fortress.core.model.UserRole;
import org.apache.directory.fortress.core.model.Warning;
import org.apache.directory.fortress.core.util.ObjUtil;
import org.apache.directory.fortress.core.util.attr.AttrHelper;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.directory.fortress.core.CreateException;
import org.apache.directory.fortress.core.FinderException;
import org.apache.directory.fortress.core.GlobalErrIds;
import org.apache.directory.fortress.core.GlobalIds;
import org.apache.directory.fortress.core.model.ObjectFactory;
import org.apache.directory.fortress.core.PasswordException;
import org.apache.directory.fortress.core.RemoveException;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.UpdateException;
import org.apache.directory.fortress.core.util.Config;
import org.apache.directory.fortress.core.ldap.ApacheDsDataProvider;
import org.apache.directory.fortress.core.util.time.CUtil;


/**
 * Data access class for LDAP User entity.
 * <p/>
 * <p/>
 * The Fortress User LDAP schema follows:
 * <p/>
 * <h4>1. InetOrgPerson Structural Object Class </h4>
 * <code># The inetOrgPerson represents people who are associated with an</code><br />
 * <code># organization in some way.  It is a structural class and is derived</code><br />
 * <code># from the organizationalPerson which is defined in X.521 [X521].</code><br />
 * <ul>
 * <li>  ------------------------------------------
 * <li> <code>objectclass ( 2.16.840.1.113730.3.2.2</code>
 * <li> <code>NAME 'inetOrgPerson'</code>
 * <li> <code>DESC 'RFC2798: Internet Organizational Person'</code>
 * <li> <code>SUP organizationalPerson</code>
 * <li> <code>STRUCTURAL</code>
 * <li> <code>MAY ( audio $ businessCategory $ carLicense $ departmentNumber $</code>
 * <li> <code>displayName $ employeeNumber $ employeeType $ givenName $</code>
 * <li> <code>homePhone $ homePostalAddress $ initials $ jpegPhoto $</code>
 * <li> <code>labeledURI $ mail $ manager $ mobile $ o $ pager $ photo $</code>
 * <li> <code>roomNumber $ secretary $ uid $ userCertificate $</code>
 * <li> <code>x500uniqueIdentifier $ preferredLanguage $</code>
 * <li> <code>userSMIMECertificate $ userPKCS12 ) )</code>
 * <li>  ------------------------------------------
 * </ul>
 * <h4>2. ftProperties AUXILIARY Object Class is used to store client specific name/value pairs on target entity</h4>
 * <code># This aux object class can be used to store custom attributes.</code><br />
 * <code># The properties collections consist of name/value pairs and are not constrainted by Fortress.</code><br />
 * <ul>
 * <li>  ------------------------------------------
 * <li> <code>objectclass ( 1.3.6.1.4.1.38088.3.2</code>
 * <li> <code>NAME 'ftProperties'</code>
 * <li> <code>DESC 'Fortress Properties AUX Object Class'</code>
 * <li> <code>AUXILIARY</code>
 * <li> <code>MAY ( ftProps ) ) </code>
 * <li>  ------------------------------------------
 * </ul>
 * <p/>
 * <h4>3. ftUserAttrs is used to store user RBAC and Admin role assignment and other security attributes on User
 * entity</h4>
 * <ul>
 * <li>  ------------------------------------------
 * <li> <code>objectclass ( 1.3.6.1.4.1.38088.3.1</code>
 * <li> <code>NAME 'ftUserAttrs'</code>
 * <li> <code>DESC 'Fortress User Attribute AUX Object Class'</code>
 * <li> <code>AUXILIARY</code>
 * <li> <code>MUST ( ftId )</code>
 * <li> <code>MAY ( ftRC $ ftRA $ ftARC $ ftARA $ ftCstr</code>
 * <li>  ------------------------------------------
 * </ul>
 * <h4>4. ftMods AUXILIARY Object Class is used to store Fortress audit variables on target entity.</h4>
 * <ul>
 * <li> <code>objectclass ( 1.3.6.1.4.1.38088.3.4</code>
 * <li> <code>NAME 'ftMods'</code>
 * <li> <code>DESC 'Fortress Modifiers AUX Object Class'</code>
 * <li> <code>AUXILIARY</code>
 * <li> <code>MAY (</code>
 * <li> <code>ftModifier $</code>
 * <li> <code>ftModCode $</code>
 * <li> <code>ftModId ) )</code>
 * <li>  ------------------------------------------
 * </ul>
 * <p/>
 * This class is thread safe.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @created August 30, 2009
 */
final class UserDAO extends ApacheDsDataProvider
{
    private static final String CLS_NM = UserDAO.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger( CLS_NM );

    /*
      *  *************************************************************************
      *  **  Fortress USER STATICS
      *  ************************************************************************
      */
    private static final String USERS_AUX_OBJECT_CLASS_NAME = "ftUserAttrs";
    private static final String USER_OBJECT_CLASS = "user.objectclass";
    private static final String USERS_EXTENSIBLE_OBJECT = "extensibleObject";
    //private static final String POSIX_ACCOUNT_OBJECT_CLASS_NAME = "posixAccount";

    // The Fortress User entity attributes are stored within standard LDAP object classes along with custom auxiliary
    // object classes:
    private static final String USER_OBJ_CLASS[] =
        { SchemaConstants.TOP_OC, Config.getProperty( USER_OBJECT_CLASS ),
            USERS_AUX_OBJECT_CLASS_NAME, GlobalIds.PROPS_AUX_OBJECT_CLASS_NAME, GlobalIds
            .FT_MODIFIER_AUX_OBJECT_CLASS_NAME, USERS_EXTENSIBLE_OBJECT,
        //            POSIX_ACCOUNT_OBJECT_CLASS_NAME
    };

    private static final String objectClassImpl = Config.getProperty( USER_OBJECT_CLASS );
    private static final String SYSTEM_USER = "ftSystem";

    /**
     * Constant contains the  attribute name used within inetorgperson ldap object classes.
     */
    private static final String DEPARTMENT_NUMBER = "departmentNumber";

    /**
     * Constant contains the  attribute name used within inetorgperson ldap object classes.
     */
    private static final String ROOM_NUMBER = "roomNumber";

    /**
     * Constant contains the mobile attribute values used within iNetOrgPerson ldap object classes.
     */
    private static final String MOBILE = "mobile";

    /**
     * Constant contains the  attribute name for jpeg images to be stored within inetorgperson ldap object classes.
     */
    private static final String JPEGPHOTO = "jpegPhoto";

    /**
     * Constant contains the employeeType attribute within iNetOrgPerson ldap object classes.
     */
    private static final String EMPLOYEE_TYPE = "employeeType";

    // RFC2307bis:
    private static final String UID_NUMBER = "uidNumber";
    private static final String GID_NUMBER = "gidNumber";
    private static final String HOME_DIRECTORY = "homeDirectory";
    private static final String LOGIN_SHELL = "loginShell";
    private static final String GECOS = "gecos";

    private static final String OPENLDAP_POLICY_SUBENTRY = "pwdPolicySubentry";
    private static final String OPENLDAP_PW_RESET = "pwdReset";
    private static final String OPENLDAP_PW_LOCKED_TIME = "pwdAccountLockedTime";
    private static final String OPENLDAP_ACCOUNT_LOCKED_TIME = "pwdAccountLockedTime";
    private static final String LOCK_VALUE = "000001010000Z";
    private static final String[] USERID =
        { SchemaConstants.UID_AT };
    private static final String[] ROLES =
        { GlobalIds.USER_ROLE_ASSIGN };

    private static final String[] USERID_ATRS =
        { SchemaConstants.UID_AT };

    // These will be loaded in static initializer that follows:
    private static String[] authnAtrs = null;
    private static String[] defaultAtrs = null;

    static
    {
        LOG.debug( "GlobalIds.IS_OPENLDAP: " + GlobalIds.IS_OPENLDAP );
        LOG.debug( "GlobalIds.IS_OPENLDAP ? OPENLDAP_PW_RESET : null: " + ( GlobalIds.IS_OPENLDAP ? OPENLDAP_PW_RESET
            : null ) );
        LOG.debug( "GlobalIds.IS_OPENLDAP: " + GlobalIds.IS_OPENLDAP );

        if ( GlobalIds.IS_OPENLDAP )
        {
            // This default set of attributes contains all and is used for search operations.
            defaultAtrs = new String[]
                {
                    GlobalIds.FT_IID,
                    SchemaConstants.UID_AT,
                    SchemaConstants.USER_PASSWORD_AT,
                    SchemaConstants.DESCRIPTION_AT,
                    SchemaConstants.OU_AT,
                    SchemaConstants.CN_AT,
                    SchemaConstants.SN_AT,
                    GlobalIds.USER_ROLE_DATA,
                    GlobalIds.CONSTRAINT,
                    GlobalIds.USER_ROLE_ASSIGN,
                    OPENLDAP_PW_RESET,
                    OPENLDAP_PW_LOCKED_TIME,
                    OPENLDAP_POLICY_SUBENTRY,
                    GlobalIds.PROPS,
                    GlobalIds.USER_ADMINROLE_ASSIGN,
                    GlobalIds.USER_ADMINROLE_DATA,
                    SchemaConstants.POSTAL_ADDRESS_AT,
                    SchemaConstants.L_AT,
                    SchemaConstants.POSTALCODE_AT,
                    SchemaConstants.POSTOFFICEBOX_AT,
                    SchemaConstants.ST_AT,
                    SchemaConstants.PHYSICAL_DELIVERY_OFFICE_NAME_AT,
                    DEPARTMENT_NUMBER,
                    ROOM_NUMBER,
                    SchemaConstants
                    .TELEPHONE_NUMBER_AT,
                    MOBILE,
                    SchemaConstants.MAIL_AT,
                    EMPLOYEE_TYPE,
                    SchemaConstants.TITLE_AT,
                    SYSTEM_USER,
                    JPEGPHOTO,
                /*
                            TODO: add for RFC2307Bis
                            UID_NUMBER,
                            GID_NUMBER,
                            HOME_DIRECTORY,
                            LOGIN_SHELL,
                            GECOS
                */};

            // This smaller result set of attributes are needed for user validation and authentication operations.
            authnAtrs = new String[]
                {
                    GlobalIds.FT_IID,
                    SchemaConstants.UID_AT,
                    SchemaConstants.USER_PASSWORD_AT,
                    SchemaConstants.DESCRIPTION_AT,
                    SchemaConstants.OU_AT,
                    SchemaConstants.CN_AT,
                    SchemaConstants.SN_AT,
                    GlobalIds.CONSTRAINT,
                    OPENLDAP_PW_RESET,
                    OPENLDAP_PW_LOCKED_TIME,
                    GlobalIds.PROPS };
        }

        else
        {
            defaultAtrs = new String[]
                {
                    GlobalIds.FT_IID,
                    SchemaConstants.UID_AT,
                    SchemaConstants.USER_PASSWORD_AT,
                    SchemaConstants.DESCRIPTION_AT,
                    SchemaConstants.OU_AT,
                    SchemaConstants.CN_AT,
                    SchemaConstants.SN_AT,
                    GlobalIds.USER_ROLE_DATA,
                    GlobalIds.CONSTRAINT,
                    GlobalIds.USER_ROLE_ASSIGN,
                    GlobalIds.PROPS,
                    GlobalIds.USER_ADMINROLE_ASSIGN,
                    GlobalIds.USER_ADMINROLE_DATA,
                    SchemaConstants.POSTAL_ADDRESS_AT,
                    SchemaConstants.L_AT,
                    SchemaConstants.POSTALCODE_AT,
                    SchemaConstants.POSTOFFICEBOX_AT,
                    SchemaConstants.ST_AT,
                    SchemaConstants.PHYSICAL_DELIVERY_OFFICE_NAME_AT,
                    DEPARTMENT_NUMBER,
                    ROOM_NUMBER,
                    SchemaConstants.TELEPHONE_NUMBER_AT,
                    MOBILE,
                    SchemaConstants.MAIL_AT,
                    EMPLOYEE_TYPE,
                    SchemaConstants.TITLE_AT,
                    SYSTEM_USER,
                    JPEGPHOTO, };

            // This smaller result set of attributes are needed for user validation and authentication operations.
            authnAtrs = new String[]
                {
                    GlobalIds.FT_IID,
                    SchemaConstants.UID_AT,
                    SchemaConstants.USER_PASSWORD_AT,
                    SchemaConstants.DESCRIPTION_AT,
                    SchemaConstants.OU_AT,
                    SchemaConstants.CN_AT,
                    SchemaConstants.SN_AT,
                    GlobalIds.CONSTRAINT,
                    GlobalIds.PROPS };
        }

    }

    // This default set of attributes contains all and is used for search operations.
    /*
        private static final String[] defaultAtrs =
            {
                GlobalIds.FT_IID,
                SchemaConstants.UID_AT, SchemaConstants.USER_PASSWORD_AT,
                SchemaConstants.DESCRIPTION_AT,
                SchemaConstants.OU_AT,
                SchemaConstants.CN_AT,
                SchemaConstants.SN_AT,
                GlobalIds.USER_ROLE_DATA,
                GlobalIds.CONSTRAINT,
                GlobalIds.USER_ROLE_ASSIGN,
                GlobalIds.IS_OPENLDAP ? OPENLDAP_PW_RESET : "",
                GlobalIds.IS_OPENLDAP ? OPENLDAP_PW_LOCKED_TIME : "",
                GlobalIds.IS_OPENLDAP ? OPENLDAP_POLICY_SUBENTRY : "",
                GlobalIds.PROPS,
                GlobalIds.USER_ADMINROLE_ASSIGN,
                GlobalIds.USER_ADMINROLE_DATA,
                SchemaConstants.POSTAL_ADDRESS_AT,
                SchemaConstants.L_AT,
                SchemaConstants.POSTALCODE_AT,
                SchemaConstants.POSTOFFICEBOX_AT,
                SchemaConstants.ST_AT,
                SchemaConstants.PHYSICAL_DELIVERY_OFFICE_NAME_AT,
                DEPARTMENT_NUMBER,
                ROOM_NUMBER,
                SchemaConstants.TELEPHONE_NUMBER_AT,
                MOBILE,
                SchemaConstants.MAIL_AT,
                EMPLOYEE_TYPE,
                SchemaConstants.TITLE_AT,
                SYSTEM_USER,
                JPEGPHOTO,

    */
    /*
                TODO: add for RFC2307Bis
                UID_NUMBER,
                GID_NUMBER,
                HOME_DIRECTORY,
                LOGIN_SHELL,
                GECOS
    *//*

        };
      */

    private static final String[] ROLE_ATR =
        { GlobalIds.USER_ROLE_DATA };

    private static final String[] AROLE_ATR =
        { GlobalIds.USER_ADMINROLE_DATA };


    /**
     * @param entity
     * @return
     * @throws CreateException
     */
    User create( User entity ) throws CreateException
    {
        LdapConnection ld = null;

        try
        {
            entity.setInternalId();

            String dn = getDn( entity.getUserId(), entity.getContextId() );

            Entry myEntry = new DefaultEntry( dn );

            myEntry.add( SchemaConstants.OBJECT_CLASS_AT, USER_OBJ_CLASS );
            myEntry.add( GlobalIds.FT_IID, entity.getInternalId() );
            myEntry.add( SchemaConstants.UID_AT, entity.getUserId() );

            // CN is required on inetOrgPerson object class, if caller did not set, use the userId:
            if ( StringUtils.isEmpty( entity.getCn() ) )
            {
                entity.setCn( entity.getUserId() );
            }

            myEntry.add( SchemaConstants.CN_AT, entity.getCn() );

            // SN is required on inetOrgPerson object class, if caller did not set, use the userId:
            if ( StringUtils.isEmpty( entity.getSn() ) )
            {
                entity.setSn( entity.getUserId() );
            }

            myEntry.add( SchemaConstants.SN_AT, entity.getSn() );

            // guard against npe
            myEntry.add( SchemaConstants.USER_PASSWORD_AT, ObjUtil.isNotNullOrEmpty( entity.getPassword() ) ? new
                String( entity.getPassword() ) : new String( new char[]
                    {} ) );
            myEntry.add( SchemaConstants.DISPLAY_NAME_AT, entity.getCn() );

            if ( StringUtils.isNotEmpty( entity.getTitle() ) )
            {
                myEntry.add( SchemaConstants.TITLE_AT, entity.getTitle() );
            }

            if ( StringUtils.isNotEmpty( entity.getEmployeeType() ) )
            {
                myEntry.add( EMPLOYEE_TYPE, entity.getEmployeeType() );
            }

            /*
                        TODO: add RFC2307BIS
                        if ( StringUtils.isNotEmpty( entity.getUidNumber() ) )
                        {
                            myEntry.add( UID_NUMBER, entity.getUidNumber() );
                        }

                        if ( StringUtils.isNotEmpty( entity.getGidNumber() ) )
                        {
                            myEntry.add( GID_NUMBER, entity.getGidNumber() );
                        }

                        if ( StringUtils.isNotEmpty( entity.getHomeDirectory() ) )
                        {
                            myEntry.add( HOME_DIRECTORY, entity.getHomeDirectory() );
                        }

                        if ( StringUtils.isNotEmpty( entity.getLoginShell() ) )
                        {
                            myEntry.add( LOGIN_SHELL, entity.getLoginShell() );
                        }

                        if ( StringUtils.isNotEmpty( entity.getGecos() ) )
                        {
                            myEntry.add( GECOS, entity.getGecos() );
                        }
            */

            // These are multi-valued attributes, use the util function to load.
            // These items are optional.  The utility function will return quietly if item list is empty:
            loadAttrs( entity.getPhones(), myEntry, SchemaConstants.TELEPHONE_NUMBER_AT );
            loadAttrs( entity.getMobiles(), myEntry, MOBILE );
            loadAttrs( entity.getEmails(), myEntry, SchemaConstants.MAIL_AT );

            // The following attributes are optional:
            if ( ObjUtil.isNotNullOrEmpty( entity.isSystem() ) )
            {
                myEntry.add( SYSTEM_USER, entity.isSystem().toString().toUpperCase() );
            }

            if ( GlobalIds.IS_OPENLDAP && StringUtils.isNotEmpty( entity.getPwPolicy() ) )
            {
                String pwdPolicyDn = GlobalIds.POLICY_NODE_TYPE + "=" + entity.getPwPolicy() + "," + getRootDn(
                    entity.getContextId(), GlobalIds.PPOLICY_ROOT );
                myEntry.add( OPENLDAP_POLICY_SUBENTRY, pwdPolicyDn );
            }

            if ( StringUtils.isNotEmpty( entity.getOu() ) )
            {
                myEntry.add( SchemaConstants.OU_AT, entity.getOu() );
            }

            if ( StringUtils.isNotEmpty( entity.getDescription() ) )
            {
                myEntry.add( SchemaConstants.DESCRIPTION_AT, entity.getDescription() );
            }

            // props are optional as well:
            // Add "initial" property here.
            entity.addProperty( "init", "" );
            loadProperties( entity.getProperties(), myEntry, GlobalIds.PROPS );
            // map the userid to the name field in constraint:
            entity.setName( entity.getUserId() );
            myEntry.add( GlobalIds.CONSTRAINT, CUtil.setConstraint( entity ) );
            loadAddress( entity.getAddress(), myEntry );

            if ( ObjUtil.isNotNullOrEmpty( entity.getJpegPhoto() ) )
            {
                myEntry.add( JPEGPHOTO, entity.getJpegPhoto() );
            }

            ld = getAdminConnection();
            add( ld, myEntry, entity );
            entity.setDn( dn );
        }
        catch ( LdapException e )
        {
            String error = "create userId [" + entity.getUserId() + "] caught LDAPException=" + e.getMessage();
            throw new CreateException( GlobalErrIds.USER_ADD_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return entity;
    }


    /**
     * @param entity
     * @return
     * @throws UpdateException
     */
    User update( User entity ) throws UpdateException
    {
        LdapConnection ld = null;
        String userDn = getDn( entity.getUserId(), entity.getContextId() );

        try
        {
            List<Modification> mods = new ArrayList<Modification>();

            if ( StringUtils.isNotEmpty( entity.getCn() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants.CN_AT,
                    entity.getCn() ) );
            }

            if ( StringUtils.isNotEmpty( entity.getSn() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants.SN_AT,
                    entity.getSn() ) );
            }

            if ( StringUtils.isNotEmpty( entity.getOu() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants.OU_AT,
                    entity.getOu() ) );
            }

            if ( ObjUtil.isNotNullOrEmpty( entity.getPassword() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants
                    .USER_PASSWORD_AT, new String( entity.getPassword() ) ) );
            }

            if ( StringUtils.isNotEmpty( entity.getDescription() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants
                    .DESCRIPTION_AT, entity.getDescription() ) );
            }

            if ( StringUtils.isNotEmpty( entity.getEmployeeType() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, EMPLOYEE_TYPE, entity
                    .getEmployeeType() ) );
            }

            if ( StringUtils.isNotEmpty( entity.getTitle() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants.TITLE_AT,
                    entity.getTitle() ) );
            }

            if ( GlobalIds.IS_OPENLDAP && StringUtils.isNotEmpty( entity.getPwPolicy() ) )
            {
                String szDn = GlobalIds.POLICY_NODE_TYPE + "=" + entity.getPwPolicy() + "," + getRootDn( entity
                    .getContextId(), GlobalIds.PPOLICY_ROOT );
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, OPENLDAP_POLICY_SUBENTRY,
                    szDn ) );
            }

            if ( ObjUtil.isNotNullOrEmpty( entity.isSystem() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SYSTEM_USER, entity
                    .isSystem().toString().toUpperCase() ) );
            }

            if ( entity.isTemporalSet() )
            {
                // map the userid to the name field in constraint:
                entity.setName( entity.getUserId() );
                String szRawData = CUtil.setConstraint( entity );

                if ( StringUtils.isNotEmpty( szRawData ) )
                {
                    mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, GlobalIds.CONSTRAINT,
                        szRawData ) );
                }
            }

            if ( ObjUtil.isNotNullOrEmpty( entity.getProperties() ) )
            {
                loadProperties( entity.getProperties(), mods, GlobalIds.PROPS, true );
            }

            loadAddress( entity.getAddress(), mods );

            // These are multi-valued attributes, use the util function to load:
            loadAttrs( entity.getPhones(), mods, SchemaConstants.TELEPHONE_NUMBER_AT );
            loadAttrs( entity.getMobiles(), mods, MOBILE );
            loadAttrs( entity.getEmails(), mods, SchemaConstants.MAIL_AT );

            if ( ObjUtil.isNotNullOrEmpty( entity.getJpegPhoto() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, JPEGPHOTO, entity
                    .getJpegPhoto() ) );
            }

            if ( mods.size() > 0 )
            {
                ld = getAdminConnection();
                modify( ld, userDn, mods, entity );
                entity.setDn( userDn );
            }

            entity.setDn( userDn );
        }
        catch ( LdapException e )
        {
            String error = "update userId [" + entity.getUserId() + "] caught LDAPException=" + e.getMessage();
            throw new UpdateException( GlobalErrIds.USER_UPDATE_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return entity;
    }


    /**
     * @param entity
     * @param replace
     * @return
     * @throws UpdateException
     */
    User updateProps( User entity, boolean replace ) throws UpdateException
    {
        LdapConnection ld = null;
        String userDn = getDn( entity.getUserId(), entity.getContextId() );

        try
        {
            List<Modification> mods = new ArrayList<Modification>();

            if ( ObjUtil.isNotNullOrEmpty( entity.getProperties() ) )
            {
                loadProperties( entity.getProperties(), mods, GlobalIds.PROPS, replace );
            }

            if ( mods.size() > 0 )
            {
                ld = getAdminConnection();
                modify( ld, userDn, mods, entity );
                entity.setDn( userDn );
            }

            entity.setDn( userDn );
        }
        catch ( LdapException e )
        {
            String error = "updateProps userId [" + entity.getUserId() + "] isReplace [" + replace + "] caught " +
                "LDAPException=" + e.getMessage();
            throw new UpdateException( GlobalErrIds.USER_UPDATE_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return entity;
    }


    /**
     * @param user
     * @throws RemoveException
     */
    String remove( User user ) throws RemoveException
    {
        LdapConnection ld = null;
        String userDn = getDn( user.getUserId(), user.getContextId() );

        try
        {
            ld = getAdminConnection();
            delete( ld, userDn, user );
        }
        catch ( LdapException e )
        {
            String error = "remove userId [" + user.getUserId() + "] caught LDAPException=" + e.getMessage();
            throw new RemoveException( GlobalErrIds.USER_DELETE_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userDn;
    }


    /**
     * @param user
     * @throws org.apache.directory.fortress.core.UpdateException
     */
    void lock( User user ) throws UpdateException
    {
        LdapConnection ld = null;
        String userDn = getDn( user.getUserId(), user.getContextId() );

        try
        {
            List<Modification> mods = new ArrayList<Modification>();
            mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, OPENLDAP_PW_LOCKED_TIME,
                LOCK_VALUE ) );
            ld = getAdminConnection();
            modify( ld, userDn, mods, user );
        }
        catch ( LdapException e )
        {
            String error = "lock user [" + user.getUserId() + "] caught LDAPException=" + e.getMessage();
            throw new UpdateException( GlobalErrIds.USER_PW_LOCK_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
    }


    /**
     * @param user
     * @throws UpdateException
     */
    void unlock( User user ) throws UpdateException
    {
        LdapConnection ld = null;
        String userDn = getDn( user.getUserId(), user.getContextId() );

        try
        {
            //ld = getAdminConnection();
            List<Modification> mods = new ArrayList<Modification>();

            mods.add( new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, OPENLDAP_PW_LOCKED_TIME ) );
            ld = getAdminConnection();
            modify( ld, userDn, mods, user );
        }
        catch ( LdapNoSuchAttributeException e )
        {
            LOG.info( "unlock user [" + user.getUserId() + "] no such attribute:" + OPENLDAP_ACCOUNT_LOCKED_TIME );
        }
        catch ( LdapException e )
        {
            String error = "unlock user [" + user.getUserId() + "] caught LDAPException= " + e.getMessage();
            throw new UpdateException( GlobalErrIds.USER_PW_UNLOCK_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
    }


    /**
     * @param user
     * @return
     * @throws org.apache.directory.fortress.core.FinderException
     */
    User getUser( User user, boolean isRoles ) throws FinderException
    {
        User entity = null;
        LdapConnection ld = null;
        String userDn = getDn( user.getUserId(), user.getContextId() );

        String[] uATTRS;
        // Retrieve role attributes?

        if ( isRoles )
        {
            // Retrieve the User's assigned RBAC and Admin Role attributes from directory.
            uATTRS = defaultAtrs;

        }
        else
        {
            // Do not retrieve the User's assigned RBAC and Admin Role attributes from directory.
            uATTRS = authnAtrs;
        }

        Entry findEntry = null;

        try
        {
            ld = getAdminConnection();
            findEntry = read( ld, userDn, uATTRS );
        }
        catch ( LdapNoSuchObjectException e )
        {
            String warning = "getUser COULD NOT FIND ENTRY for user [" + user.getUserId() + "]";
            throw new FinderException( GlobalErrIds.USER_NOT_FOUND, warning );
        }
        catch ( LdapException e )
        {
            String error = "getUser [" + userDn + "]= caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.USER_READ_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        try
        {
            if ( findEntry != null )
            {
                entity = unloadLdapEntry( findEntry, 0, user.getContextId() );
            }
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            entity = null;
        }

        if ( entity == null )
        {
            String warning = "getUser userId [" + user.getUserId() + "] not found, Fortress rc=" + GlobalErrIds
                .USER_NOT_FOUND;
            throw new FinderException( GlobalErrIds.USER_NOT_FOUND, warning );
        }

        return entity;
    }


    /**
     * @param user
     * @return
     * @throws org.apache.directory.fortress.core.FinderException
     */
    List<UserAdminRole> getUserAdminRoles( User user ) throws FinderException
    {
        List<UserAdminRole> roles = null;
        LdapConnection ld = null;
        String userDn = getDn( user.getUserId(), user.getContextId() );

        try
        {
            ld = getAdminConnection();
            Entry findEntry = read( ld, userDn, AROLE_ATR );
            roles = unloadUserAdminRoles( findEntry, user.getUserId(), user.getContextId() );
        }
        catch ( LdapNoSuchObjectException e )
        {
            String warning = "getUserAdminRoles COULD NOT FIND ENTRY for user [" + user.getUserId() + "]";
            throw new FinderException( GlobalErrIds.USER_NOT_FOUND, warning );
        }
        catch ( LdapException e )
        {
            String error = "getUserAdminRoles [" + userDn + "]= caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.USER_READ_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return roles;
    }


    /**
     * @param user
     * @return
     * @throws org.apache.directory.fortress.core.FinderException
     */
    List<String> getRoles( User user ) throws FinderException
    {
        List<String> roles = null;
        LdapConnection ld = null;
        String userDn = getDn( user.getUserId(), user.getContextId() );

        try
        {
            ld = getAdminConnection();
            Entry findEntry = read( ld, userDn, ROLES );

            if ( findEntry == null )
            {
                String warning = "getRoles userId [" + user.getUserId() + "] not found, Fortress rc=" + GlobalErrIds
                    .USER_NOT_FOUND;
                throw new FinderException( GlobalErrIds.USER_NOT_FOUND, warning );
            }

            roles = getAttributes( findEntry, GlobalIds.USER_ROLE_ASSIGN );
        }
        catch ( LdapNoSuchObjectException e )
        {
            String warning = "getRoles COULD NOT FIND ENTRY for user [" + user.getUserId() + "]";
            throw new FinderException( GlobalErrIds.USER_NOT_FOUND, warning );
        }
        catch ( LdapException e )
        {
            String error = "getRoles [" + userDn + "]= caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.URLE_SEARCH_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return roles;
    }


    /**
     * @param user
     * @return
     * @throws org.apache.directory.fortress.core.FinderException,  org.apache.directory.fortress.core.PasswordException
     */
    Session checkPassword( User user ) throws FinderException, PasswordException
    {
        Session session = null;
        LdapConnection ld = null;
        String userDn = getDn( user.getUserId(), user.getContextId() );

        try
        {
            session = new ObjectFactory().createSession();
            session.setAuthenticated( false );
            session.setUserId( user.getUserId() );
            ld = getUserConnection();
            BindResponse bindResponse = bind( ld, userDn, user.getPassword() );
            String info = null;

            if ( bindResponse.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS )
            {
                info = "PASSWORD INVALID for userId [" + user.getUserId() + "], resultCode [" +
                    bindResponse.getLdapResult().getResultCode() + "]";
                session.setMsg( info );
                session.setErrorId( GlobalErrIds.USER_PW_INVLD );
            }

            PasswordPolicy respCtrl = getPwdRespCtrl( bindResponse );

            if ( respCtrl != null )
            {
                // check IETF password policies here
                checkPwPolicies( session, respCtrl );
            }

            if ( session.getErrorId() == 0 )
            {
                session.setAuthenticated( true );
            }
            else
            {
                // pw invalid or pw policy violation:
                throw new PasswordException( session.getErrorId(), session.getMsg() );
            }
        }
        catch ( LdapAuthenticationException e )
        {
            String info = "checkPassword INVALID PASSWORD for userId [" + user.getUserId() + "] exception [" + e + "]";
            throw new PasswordException( GlobalErrIds.USER_PW_INVLD, info );
        }
        catch ( LdapException e )
        {
            String error = "checkPassword userId [" + user.getUserId() + "] caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.USER_READ_FAILED, error, e );
        }
        finally
        {
            closeUserConnection( ld );
        }

        return session;
    }


    private void checkPwPolicies( PwMessage pwMsg, PasswordPolicy respCtrl )
    {
        int rc = 0;
        boolean result = false;
        String msgHdr = "checkPwPolicies for userId [" + pwMsg.getUserId() + "] ";
        if ( respCtrl != null )
        {
            // LDAP has notified of password violation:
            if ( respCtrl.hasResponse() )
            {
                String errMsg = null;
                if ( respCtrl.getResponse() != null )
                {
                    if ( respCtrl.getResponse().getTimeBeforeExpiration() > 0 )
                    {
                        pwMsg.setExpirationSeconds( respCtrl.getResponse().getTimeBeforeExpiration() );
                        pwMsg.setWarning( new ObjectFactory().createWarning( GlobalPwMsgIds
                            .PASSWORD_EXPIRATION_WARNING, "PASSWORD WILL EXPIRE", Warning.Type.PASSWORD ) );
                    }
                    if ( respCtrl.getResponse().getGraceAuthNRemaining() > 0 )
                    {
                        pwMsg.setGraceLogins( respCtrl.getResponse().getGraceAuthNRemaining() );
                        pwMsg.setWarning( new ObjectFactory().createWarning( GlobalPwMsgIds.PASSWORD_GRACE_WARNING,
                            "PASSWORD IN GRACE", Warning.Type.PASSWORD ) );
                    }

                    if ( respCtrl.getResponse().getPasswordPolicyError() != null )
                    {

                        switch ( respCtrl.getResponse().getPasswordPolicyError() )
                        {

                            case CHANGE_AFTER_RESET:
                                // Don't throw exception if authenticating in J2EE Realm - The Web application must
                                // give user a chance to modify their password.
                                if ( !GlobalIds.IS_REALM )
                                {
                                    errMsg = msgHdr + "PASSWORD HAS BEEN RESET BY LDAP_ADMIN_POOL_UID";
                                    rc = GlobalErrIds.USER_PW_RESET;
                                }
                                else
                                {
                                    errMsg = msgHdr + "PASSWORD HAS BEEN RESET BY LDAP_ADMIN_POOL_UID BUT ALLOWING TO" +
                                        " CONTINUE DUE TO REALM";
                                    result = true;
                                    pwMsg.setWarning( new ObjectFactory().createWarning( GlobalErrIds.USER_PW_RESET,
                                        errMsg, Warning.Type.PASSWORD ) );
                                }
                                break;

                            case ACCOUNT_LOCKED:
                                errMsg = msgHdr + "ACCOUNT HAS BEEN LOCKED";
                                rc = GlobalErrIds.USER_PW_LOCKED;
                                break;

                            case PASSWORD_EXPIRED:
                                errMsg = msgHdr + "PASSWORD HAS EXPIRED";
                                rc = GlobalErrIds.USER_PW_EXPIRED;
                                break;

                            case PASSWORD_MOD_NOT_ALLOWED:
                                errMsg = msgHdr + "PASSWORD MOD NOT ALLOWED";
                                rc = GlobalErrIds.USER_PW_MOD_NOT_ALLOWED;
                                break;

                            case MUST_SUPPLY_OLD_PASSWORD:
                                errMsg = msgHdr + "MUST SUPPLY OLD PASSWORD";
                                rc = GlobalErrIds.USER_PW_MUST_SUPPLY_OLD;
                                break;

                            case INSUFFICIENT_PASSWORD_QUALITY:
                                errMsg = msgHdr + "PASSWORD QUALITY VIOLATION";
                                rc = GlobalErrIds.USER_PW_NSF_QUALITY;
                                break;

                            case PASSWORD_TOO_SHORT:
                                errMsg = msgHdr + "PASSWORD TOO SHORT";
                                rc = GlobalErrIds.USER_PW_TOO_SHORT;
                                break;

                            case PASSWORD_TOO_YOUNG:
                                errMsg = msgHdr + "PASSWORD TOO YOUNG";
                                rc = GlobalErrIds.USER_PW_TOO_YOUNG;
                                break;

                            case PASSWORD_IN_HISTORY:
                                errMsg = msgHdr + "PASSWORD IN HISTORY VIOLATION";
                                rc = GlobalErrIds.USER_PW_IN_HISTORY;
                                break;

                            default:
                                errMsg = msgHdr + "PASSWORD CHECK FAILED";
                                rc = GlobalErrIds.USER_PW_CHK_FAILED;
                                break;
                        }

                    }
                }
                if ( rc != 0 )
                {
                    pwMsg.setMsg( errMsg );
                    pwMsg.setErrorId( rc );
                    pwMsg.setAuthenticated( result );
                    LOG.debug( errMsg );
                }
            }
        }
    }


    /**
     * @param user
     * @return
     * @throws FinderException
     */
    List<User> findUsers( User user ) throws FinderException
    {
        List<User> userList = new ArrayList<>();
        LdapConnection ld = null;
        String userRoot = getRootDn( user.getContextId(), GlobalIds.USER_ROOT );

        try
        {
            //String filter;
            StringBuilder filterbuf = new StringBuilder();
            if ( StringUtils.isNotEmpty( user.getUserId() ) )
            {
                // place a wild card after the input userId:
                String searchVal = encodeSafeText( user.getUserId(), GlobalIds.USERID_LEN );
                filterbuf.append( GlobalIds.FILTER_PREFIX );
                filterbuf.append( objectClassImpl );
                filterbuf.append( ")(" );
                filterbuf.append( SchemaConstants.UID_AT );
                filterbuf.append( "=" );
                filterbuf.append( searchVal );
                filterbuf.append( "*))" );
            }
            else if ( StringUtils.isNotEmpty( user.getInternalId() ) )
            {
                // internalUserId search
                String searchVal = encodeSafeText( user.getInternalId(), GlobalIds.USERID_LEN );
                // this is not a wildcard search. Must be exact match.
                filterbuf.append( GlobalIds.FILTER_PREFIX );
                filterbuf.append( objectClassImpl );
                filterbuf.append( ")(" );
                filterbuf.append( GlobalIds.FT_IID );
                filterbuf.append( "=" );
                filterbuf.append( searchVal );
                filterbuf.append( "))" );
            }
            else
            {
                // Beware - returns ALL users!!:"
                filterbuf.append( "(objectclass=" );
                filterbuf.append( objectClassImpl );
                filterbuf.append( ")" );
            }

            ld = getAdminConnection();
            SearchCursor searchResults = search( ld, userRoot, SearchScope.ONELEVEL, filterbuf.toString(), defaultAtrs, false,
                GlobalIds.BATCH_SIZE );
            long sequence = 0;

            while ( searchResults.next() )
            {
                userList.add( unloadLdapEntry( searchResults.getEntry(), sequence++, user.getContextId() ) );
            }
        }
        catch ( LdapException e )
        {
            String warning = "findUsers userRoot [" + userRoot + "] caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.USER_SEARCH_FAILED, warning, e );
        }
        catch ( CursorException e )
        {
            String warning = "findUsers userRoot [" + userRoot + "] caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.USER_SEARCH_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userList;
    }


    /**
     * @param user
     * @param limit
     * @return
     * @throws FinderException
     */
    List<String> findUsers( User user, int limit ) throws FinderException
    {
        List<String> userList = new ArrayList<>();
        LdapConnection ld = null;
        String userRoot = getRootDn( user.getContextId(), GlobalIds.USER_ROOT );

        try
        {
            String searchVal = encodeSafeText( user.getUserId(), GlobalIds.USERID_LEN );
            StringBuilder filterbuf = new StringBuilder();
            filterbuf.append( GlobalIds.FILTER_PREFIX );
            filterbuf.append( objectClassImpl );
            filterbuf.append( ")(" );
            filterbuf.append( SchemaConstants.UID_AT );
            filterbuf.append( "=" );
            filterbuf.append( searchVal );
            filterbuf.append( "*))" );

            ld = getAdminConnection();
            SearchCursor searchResults = search( ld, userRoot, SearchScope.ONELEVEL, filterbuf.toString(), USERID,
                false, GlobalIds
                .BATCH_SIZE, limit );

            while ( searchResults.next() )
            {
                Entry entry = searchResults.getEntry();
                userList.add( getAttribute( entry, SchemaConstants.UID_AT ) );
            }
        }
        catch ( LdapException e )
        {
            String warning = "findUsers caught LdapException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.USER_SEARCH_FAILED, warning, e );
        }
        catch ( CursorException e )
        {
            String warning = "findUsers caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.USER_SEARCH_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userList;
    }


    /**
     * @param role
     * @return
     * @throws FinderException
     */
    List<User> getAuthorizedUsers( Role role ) throws FinderException
    {
        List<User> userList = new ArrayList<>();
        LdapConnection ld = null;
        String userRoot = getRootDn( role.getContextId(), GlobalIds.USER_ROOT );

        try
        {
            String roleVal = encodeSafeText( role.getName(), GlobalIds.USERID_LEN );
            StringBuilder filterbuf = new StringBuilder();
            filterbuf.append( GlobalIds.FILTER_PREFIX );
            filterbuf.append( USERS_AUX_OBJECT_CLASS_NAME );
            filterbuf.append( ")(" );

            Set<String> roles = RoleUtil.getDescendants( role.getName(), role.getContextId() );

            if ( ObjUtil.isNotNullOrEmpty( roles ) )
            {
                filterbuf.append( "|(" );
                filterbuf.append( GlobalIds.USER_ROLE_ASSIGN );
                filterbuf.append( "=" );
                filterbuf.append( roleVal );
                filterbuf.append( ")" );

                for ( String uRole : roles )
                {
                    filterbuf.append( "(" );
                    filterbuf.append( GlobalIds.USER_ROLE_ASSIGN );
                    filterbuf.append( "=" );
                    filterbuf.append( uRole );
                    filterbuf.append( ")" );
                }

                filterbuf.append( ")" );
            }
            else
            {
                filterbuf.append( GlobalIds.USER_ROLE_ASSIGN );
                filterbuf.append( "=" );
                filterbuf.append( roleVal );
                filterbuf.append( ")" );
            }

            filterbuf.append( ")" );
            ld = getAdminConnection();
            SearchCursor searchResults = search( ld, userRoot, SearchScope.ONELEVEL, filterbuf.toString(), defaultAtrs, false,
                GlobalIds.BATCH_SIZE );
            long sequence = 0;

            while ( searchResults.next() )
            {
                userList.add( unloadLdapEntry( searchResults.getEntry(), sequence++, role.getContextId() ) );
            }
        }
        catch ( LdapException e )
        {
            String warning = "getAuthorizedUsers role name [" + role.getName() + "] caught LDAPException=" + e
                .getMessage();
            throw new FinderException( GlobalErrIds.URLE_SEARCH_FAILED, warning, e );
        }
        catch ( CursorException e )
        {
            String warning = "getAuthorizedUsers role name [" + role.getName() + "] caught LDAPException=" + e
                .getMessage();
            throw new FinderException( GlobalErrIds.URLE_SEARCH_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userList;
    }


    /**
     * @param role
     * @return
     * @throws FinderException
     */
    List<User> getAssignedUsers( Role role ) throws FinderException
    {
        List<User> userList = new ArrayList<>();
        LdapConnection ld = null;
        String userRoot = getRootDn( role.getContextId(), GlobalIds.USER_ROOT );

        try
        {
            String roleVal = encodeSafeText( role.getName(), GlobalIds.USERID_LEN );
            StringBuilder filterbuf = new StringBuilder();
            filterbuf.append( GlobalIds.FILTER_PREFIX );
            filterbuf.append( USERS_AUX_OBJECT_CLASS_NAME );
            filterbuf.append( ")(" );
            filterbuf.append( GlobalIds.USER_ROLE_ASSIGN );
            filterbuf.append( "=" );
            filterbuf.append( roleVal );
            filterbuf.append( "))" );

            ld = getAdminConnection();
            SearchCursor searchResults = search( ld, userRoot, SearchScope.ONELEVEL, filterbuf.toString(), defaultAtrs, false,
                GlobalIds.BATCH_SIZE );
            long sequence = 0;

            while ( searchResults.next() )
            {
                userList.add( unloadLdapEntry( searchResults.getEntry(), sequence++, role.getContextId() ) );
            }
        }
        catch ( LdapException e )
        {
            String warning = "getAssignedUsers role name [" + role.getName() + "] caught LDAPException=" + e
                .getMessage();
            throw new FinderException( GlobalErrIds.URLE_SEARCH_FAILED, warning, e );
        }
        catch ( CursorException e )
        {
            String warning = "getAssignedUsers role name [" + role.getName() + "] caught LDAPException=" + e
                .getMessage();
            throw new FinderException( GlobalErrIds.URLE_SEARCH_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userList;
    }


    /**
     * @param roles
     * @return
     * @throws FinderException
     */
    Set<String> getAssignedUsers( Set<String> roles, String contextId ) throws FinderException
    {
        Set<String> userSet = new HashSet<>();
        LdapConnection ld = null;
        String userRoot = getRootDn( contextId, GlobalIds.USER_ROOT );

        try
        {
            StringBuilder filterbuf = new StringBuilder();
            filterbuf.append( GlobalIds.FILTER_PREFIX );
            filterbuf.append( USERS_AUX_OBJECT_CLASS_NAME );
            filterbuf.append( ")(|" );

            if ( ObjUtil.isNotNullOrEmpty( roles ) )
            {
                for ( String roleVal : roles )
                {
                    String filteredVal = encodeSafeText( roleVal, GlobalIds.USERID_LEN );
                    filterbuf.append( "(" );
                    filterbuf.append( GlobalIds.USER_ROLE_ASSIGN );
                    filterbuf.append( "=" );
                    filterbuf.append( filteredVal );
                    filterbuf.append( ")" );
                }
            }
            else
            {
                return null;
            }

            filterbuf.append( "))" );
            ld = getAdminConnection();
            SearchCursor searchResults = search( ld, userRoot, SearchScope.ONELEVEL, filterbuf.toString(), USERID_ATRS,
                false,
                GlobalIds.BATCH_SIZE );

            while ( searchResults.next() )
            {
                userSet.add( getAttribute( searchResults.getEntry(), SchemaConstants.UID_AT ) );
            }
        }
        catch ( LdapException e )
        {
            String warning = "getAssignedUsers caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.URLE_SEARCH_FAILED, warning, e );
        }
        catch ( CursorException e )
        {
            String warning = "getAssignedUsers caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.URLE_SEARCH_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userSet;
    }


    /**
     * @param role
     * @return
     * @throws FinderException
     */
    List<User> getAssignedUsers( AdminRole role ) throws FinderException
    {
        List<User> userList = new ArrayList<>();
        LdapConnection ld = null;
        String userRoot = getRootDn( role.getContextId(), GlobalIds.USER_ROOT );

        try
        {
            String roleVal = encodeSafeText( role.getName(), GlobalIds.USERID_LEN );
            StringBuilder filterbuf = new StringBuilder();
            filterbuf.append( GlobalIds.FILTER_PREFIX );
            filterbuf.append( USERS_AUX_OBJECT_CLASS_NAME );
            filterbuf.append( ")(" );
            filterbuf.append( GlobalIds.USER_ADMINROLE_ASSIGN );
            filterbuf.append( "=" );
            filterbuf.append( roleVal );
            filterbuf.append( "))" );

            ld = getAdminConnection();
            SearchCursor searchResults = search( ld, userRoot, SearchScope.ONELEVEL, filterbuf.toString(), defaultAtrs, false,
                GlobalIds.BATCH_SIZE );
            long sequence = 0;

            while ( searchResults.next() )
            {
                userList.add( unloadLdapEntry( searchResults.getEntry(), sequence++, role.getContextId() ) );
            }
        }
        catch ( LdapException e )
        {
            String warning = "getAssignedUsers admin role name [" + role.getName() + "] caught LDAPException=" + e
                .getMessage();
            throw new FinderException( GlobalErrIds.ARLE_USER_SEARCH_FAILED, warning, e );
        }
        catch ( CursorException e )
        {
            String warning = "getAssignedUsers admin role name [" + role.getName() + "] caught LDAPException=" + e
                .getMessage();
            throw new FinderException( GlobalErrIds.ARLE_USER_SEARCH_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userList;
    }


    /**
     * @param role
     * @param limit
     * @return
     * @throws FinderException
     */
    List<String> getAuthorizedUsers( Role role, int limit ) throws FinderException
    {
        List<String> userList = new ArrayList<>();
        LdapConnection ld = null;
        String userRoot = getRootDn( role.getContextId(), GlobalIds.USER_ROOT );

        try
        {
            String roleVal = encodeSafeText( role.getName(), GlobalIds.USERID_LEN );
            StringBuilder filterbuf = new StringBuilder();
            filterbuf.append( GlobalIds.FILTER_PREFIX );
            filterbuf.append( USERS_AUX_OBJECT_CLASS_NAME );
            filterbuf.append( ")(" );
            filterbuf.append( GlobalIds.USER_ROLE_ASSIGN );
            filterbuf.append( "=" );
            filterbuf.append( roleVal );
            filterbuf.append( "))" );

            ld = getAdminConnection();
            SearchCursor searchResults = search( ld, userRoot, SearchScope.ONELEVEL, filterbuf.toString(), USERID,
                false, GlobalIds
                .BATCH_SIZE, limit );

            while ( searchResults.next() )
            {
                Entry entry = searchResults.getEntry();
                userList.add( getAttribute( entry, SchemaConstants.UID_AT ) );
            }
        }
        catch ( LdapException e )
        {
            String warning = "getAuthorizedUsers role name [" + role.getName() + "] caught LDAPException=" + e
                .getMessage();
            throw new FinderException( GlobalErrIds.URLE_SEARCH_FAILED, warning, e );
        }
        catch ( CursorException e )
        {
            String warning = "getAuthorizedUsers role name [" + role.getName() + "] caught LDAPException=" + e
                .getMessage();
            throw new FinderException( GlobalErrIds.URLE_SEARCH_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userList;
    }


    /**
     * @param searchVal
     * @return
     * @throws FinderException
     */
    List<String> findUsersList( String searchVal, String contextId ) throws FinderException
    {
        List<String> userList = new ArrayList<>();
        LdapConnection ld = null;
        String userRoot = getRootDn( contextId, GlobalIds.USER_ROOT );

        try
        {
            searchVal = encodeSafeText( searchVal, GlobalIds.USERID_LEN );
            StringBuilder filterbuf = new StringBuilder();
            filterbuf.append( GlobalIds.FILTER_PREFIX );
            filterbuf.append( objectClassImpl );
            filterbuf.append( ")(" );
            filterbuf.append( SchemaConstants.UID_AT );
            filterbuf.append( "=" );
            filterbuf.append( searchVal );
            filterbuf.append( "*))" );

            ld = getAdminConnection();
            SearchCursor searchResults = search( ld, userRoot, SearchScope.ONELEVEL, filterbuf.toString(), defaultAtrs, false,
                GlobalIds.BATCH_SIZE );
            long sequence = 0;

            while ( searchResults.next() )
            {
                userList.add( ( unloadLdapEntry( searchResults.getEntry(), sequence++, contextId ) ).getUserId() );
            }
        }
        catch ( LdapException e )
        {
            String warning = "findUsersList caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.USER_SEARCH_FAILED, warning, e );
        }
        catch ( CursorException e )
        {
            String warning = "findUsersList caught CursorException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.USER_SEARCH_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userList;
    }


    /**
     * @param ou
     * @return
     * @throws FinderException
     */
    List<User> findUsers( OrgUnit ou, boolean limitSize ) throws FinderException
    {
        List<User> userList = new ArrayList<>();
        LdapConnection ld = null;
        String userRoot = getRootDn( ou.getContextId(), GlobalIds.USER_ROOT );

        try
        {
            String szOu = encodeSafeText( ou.getName(), GlobalIds.OU_LEN );
            StringBuilder filterbuf = new StringBuilder();
            filterbuf.append( GlobalIds.FILTER_PREFIX );
            filterbuf.append( objectClassImpl );
            filterbuf.append( ")(" );
            filterbuf.append( SchemaConstants.OU_AT );
            filterbuf.append( "=" );
            filterbuf.append( szOu );
            filterbuf.append( "))" );
            int maxLimit;

            if ( limitSize )
            {
                maxLimit = 10;
            }
            else
            {
                maxLimit = 0;
            }

            ld = getAdminConnection();
            SearchCursor searchResults = search( ld, userRoot, SearchScope.ONELEVEL, filterbuf.toString(), defaultAtrs, false,
                GlobalIds.BATCH_SIZE, maxLimit );
            long sequence = 0;

            while ( searchResults.next() )
            {
                userList.add( unloadLdapEntry( searchResults.getEntry(), sequence++, ou.getContextId() ) );
            }
        }
        catch ( LdapException e )
        {
            String warning = "findUsers caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.USER_SEARCH_FAILED, warning, e );
        }
        catch ( CursorException e )
        {
            String warning = "findUsers caught CursorException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.USER_SEARCH_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userList;
    }


    /**
     * @param entity
     * @param newPassword
     * @return
     * @throws UpdateException
     * @throws SecurityException
     * @throws PasswordException
     */
    boolean changePassword( User entity, char[] newPassword ) throws SecurityException
    {
        boolean rc = true;
        LdapConnection ld = null;
        List<Modification> mods;
        String userDn = getDn( entity.getUserId(), entity.getContextId() );

        try
        {
            ld = getUserConnection();
            bind( ld, userDn, entity.getPassword() );
            mods = new ArrayList<Modification>();

            mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants
                .USER_PASSWORD_AT, new String( newPassword ) ) );

            modify( ld, userDn, mods );

            // The 2nd modify is to update audit attributes on the User entry:
            if ( GlobalIds.IS_AUDIT && ( entity.getAdminSession() != null ) )
            {
                // Because the user modified their own password, set their userId here:
                //(entity.getAdminSession()).setInternalUserId(entity.getUserId());
                mods = new ArrayList<Modification>();
                modify( ld, userDn, mods, entity );
            }
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            String warning = User.class.getName() + ".changePassword user [" + entity.getUserId() + "] ";

            warning += " constraint violation, ldap rc=" + e.getMessage() + " Fortress rc=" + GlobalErrIds
                .PSWD_CONST_VIOLATION;

            throw new PasswordException( GlobalErrIds.PSWD_CONST_VIOLATION, warning );
        }
        catch ( LdapNoPermissionException e )
        {
            String warning = User.class.getName() + ".changePassword user [" + entity.getUserId() + "] ";
            warning += " user not authorized to change password, ldap rc=" + e.getMessage() + " Fortress rc=" +
                GlobalErrIds.USER_PW_MOD_NOT_ALLOWED;
            throw new UpdateException( GlobalErrIds.USER_PW_MOD_NOT_ALLOWED, warning );
        }
        catch ( LdapException e )
        {
            String warning = User.class.getName() + ".changePassword user [" + entity.getUserId() + "] ";
            warning += " caught LDAPException rc=" + e.getMessage();
            throw new UpdateException( GlobalErrIds.USER_PW_CHANGE_FAILED, warning, e );
        }
        finally
        {
            closeUserConnection( ld );
        }

        return rc;
    }


    /**
     * @param user
     * @throws UpdateException
     */
    void resetUserPassword( User user ) throws UpdateException
    {
        LdapConnection ld = null;
        String userDn = getDn( user.getUserId(), user.getContextId() );

        try
        {
            List<Modification> mods = new ArrayList<Modification>();

            mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants
                .USER_PASSWORD_AT, new String( user.getPassword() ) ) );

            mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, OPENLDAP_PW_RESET, "TRUE" ) );

            ld = getAdminConnection();
            modify( ld, userDn, mods, user );
        }
        catch ( LdapException e )
        {
            String warning = "resetUserPassword userId [" + user.getUserId() + "] caught LDAPException=" + e
                .getMessage();
            throw new UpdateException( GlobalErrIds.USER_PW_RESET_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
    }


    /**
     * @param uRole
     * @return
     * @throws UpdateException
     * @throws FinderException
     */
    String assign( UserRole uRole ) throws UpdateException, FinderException
    {
        LdapConnection ld = null;
        String userDn = getDn( uRole.getUserId(), uRole.getContextId() );

        try
        {
            List<Modification> mods = new ArrayList<Modification>();
            String szUserRole = uRole.getRawData();

            mods.add( new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, GlobalIds.USER_ROLE_DATA,
                szUserRole ) );

            mods.add( new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, GlobalIds.USER_ROLE_ASSIGN, uRole
                .getName() ) );

            ld = getAdminConnection();
            modify( ld, userDn, mods, uRole );
        }
        catch ( LdapAttributeInUseException e )
        {
            String warning = "assign userId [" + uRole.getUserId() + "] name [" + uRole.getName() + "] ";

            warning += "assignment already exists.";
            throw new FinderException( GlobalErrIds.URLE_ASSIGN_EXIST, warning );
        }
        catch ( LdapException e )
        {
            String warning = "assign userId [" + uRole.getUserId() + "] name [" + uRole.getName() + "] ";

            warning += "caught LDAPException=" + e.getMessage();
            throw new UpdateException( GlobalErrIds.URLE_ASSIGN_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userDn;
    }


    /**
     * @param uRole
     * @return
     * @throws UpdateException
     * @throws FinderException
     */
    String deassign( UserRole uRole ) throws UpdateException, FinderException
    {
        LdapConnection ld = null;
        String userDn = getDn( uRole.getUserId(), uRole.getContextId() );

        try
        {
            // read the user's RBAC role assignments to locate target record.  Need the raw data before attempting
            // removal:
            List<UserRole> roles = getUserRoles( uRole.getUserId(), uRole.getContextId() );
            int indx = -1;

            // Does the user have any roles assigned?
            if ( roles != null )
            {
                // function call will set indx to -1 if name not found:
                indx = roles.indexOf( uRole );

                // Is the targeted name assigned to user?
                if ( indx > -1 )
                {
                    // Retrieve the targeted name:
                    UserRole fRole = roles.get( indx );
                    // delete the name assignment attribute using the raw name data:
                    List<Modification> mods = new ArrayList<Modification>();

                    mods.add( new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, GlobalIds
                        .USER_ROLE_DATA, fRole.getRawData() ) );

                    mods.add( new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, GlobalIds
                        .USER_ROLE_ASSIGN, fRole.getName() ) );
                    ld = getAdminConnection();
                    modify( ld, userDn, mods, uRole );
                }
            }
            // target name not found:
            if ( indx == -1 )
            {
                // The user does not have the target name assigned,
                String warning = "deassign userId [" + uRole.getUserId() + "] name [" + uRole.getName() + "] " +
                    "assignment does not exist.";
                throw new FinderException( GlobalErrIds.URLE_ASSIGN_NOT_EXIST, warning );
            }
        }
        catch ( LdapException e )
        {
            String warning = "deassign userId [" + uRole.getUserId() + "] name [" + uRole.getName() + "] caught " +
                "LDAPException=" + e.getMessage();
            throw new UpdateException( GlobalErrIds.URLE_DEASSIGN_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userDn;
    }


    /**
     * @param uRole
     * @return
     * @throws UpdateException
     * @throws FinderException
     */
    String assign( UserAdminRole uRole ) throws UpdateException, FinderException
    {
        LdapConnection ld = null;
        String userDn = getDn( uRole.getUserId(), uRole.getContextId() );

        try
        {
            List<Modification> mods = new ArrayList<Modification>();
            String szUserRole = uRole.getRawData();
            mods.add( new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, GlobalIds.USER_ADMINROLE_DATA,
                szUserRole ) );

            mods.add( new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, GlobalIds.USER_ADMINROLE_ASSIGN,
                uRole.getName() ) );

            ld = getAdminConnection();
            modify( ld, userDn, mods, uRole );
        }
        catch ( LdapAttributeInUseException e )
        {
            String warning = "assign userId [" + uRole.getUserId() + "] name [" + uRole.getName() + "] assignment " +
                "already exists.";
            throw new FinderException( GlobalErrIds.ARLE_ASSIGN_EXIST, warning );
        }
        catch ( LdapException e )
        {
            String warning = "assign userId [" + uRole.getUserId() + "] name [" + uRole.getName() + "] caught " +
                "LDAPException=" + e.getMessage();
            throw new UpdateException( GlobalErrIds.ARLE_ASSIGN_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userDn;
    }


    /**
     * @param uRole
     * @return
     * @throws UpdateException
     * @throws FinderException
     */
    String deassign( UserAdminRole uRole ) throws UpdateException, FinderException
    {
        LdapConnection ld = null;
        String userDn = getDn( uRole.getUserId(), uRole.getContextId() );

        try
        {
            // read the user's ARBAC roles to locate record.  Need the raw data before attempting removal:
            User user = new User( uRole.getUserId() );
            user.setContextId( uRole.getContextId() );
            List<UserAdminRole> roles = getUserAdminRoles( user );

            int indx = -1;

            // Does the user have any roles assigned?
            if ( roles != null )
            {
                // function call will set index to -1 if name not found:
                indx = roles.indexOf( uRole );

                // Is the targeted name assigned to user?
                if ( indx > -1 )
                {
                    // Retrieve the targeted name:
                    UserRole fRole = roles.get( indx );
                    // delete the name assignment attribute using the raw name data:
                    List<Modification> mods = new ArrayList<Modification>();

                    mods.add( new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, GlobalIds
                        .USER_ADMINROLE_DATA, fRole.getRawData() ) );

                    mods.add( new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, GlobalIds
                        .USER_ADMINROLE_ASSIGN, fRole.getName() ) );

                    ld = getAdminConnection();
                    modify( ld, userDn, mods, uRole );
                }
            }

            // target name not found:
            if ( indx == -1 )
            {
                // The user does not have the target name assigned,
                String warning = "deassign userId [" + uRole.getUserId() + "] name [" + uRole.getName() + "] " +
                    "assignment does not exist.";
                throw new FinderException( GlobalErrIds.ARLE_DEASSIGN_NOT_EXIST, warning );
            }
        }
        catch ( LdapException e )
        {
            String warning = "deassign userId [" + uRole.getUserId() + "] name [" + uRole.getName() + "] caught " +
                "LDAPException=" + e.getMessage();
            throw new UpdateException( GlobalErrIds.ARLE_DEASSIGN_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userDn;
    }


    /**
     * @param user
     * @return
     * @throws UpdateException
     * @throws Exception
     */
    String deletePwPolicy( User user ) throws UpdateException
    {
        LdapConnection ld = null;
        String userDn = getDn( user.getUserId(), user.getContextId() );

        try
        {
            List<Modification> mods = new ArrayList<Modification>();

            mods.add( new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, OPENLDAP_POLICY_SUBENTRY ) );
            ld = getAdminConnection();
            modify( ld, userDn, mods, user );
        }
        catch ( LdapException e )
        {
            String warning = "deletePwPolicy userId [" + user.getUserId() + "] caught LDAPException=" + e.getMessage
                () + " msg=" + e.getMessage();
            throw new UpdateException( GlobalErrIds.USER_PW_PLCY_DEL_FAILED, warning, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return userDn;
    }


    /**
     * @param entry
     * @return
     * @throws LdapInvalidAttributeValueException
     */
    private User unloadLdapEntry( Entry entry, long sequence, String contextId )
        throws LdapInvalidAttributeValueException
    {
        User entity = new ObjectFactory().createUser();
        entity.setSequenceId( sequence );
        entity.setInternalId( getAttribute( entry, GlobalIds.FT_IID ) );
        entity.setDescription( getAttribute( entry, SchemaConstants.DESCRIPTION_AT ) );
        entity.setUserId( getAttribute( entry, SchemaConstants.UID_AT ) );
        entity.setCn( getAttribute( entry, SchemaConstants.CN_AT ) );
        entity.setName( entity.getCn() );
        entity.setSn( getAttribute( entry, SchemaConstants.SN_AT ) );
        entity.setOu( getAttribute( entry, SchemaConstants.OU_AT ) );
        entity.setDn( entry.getDn().getName() );
        entity.setTitle( getAttribute( entry, SchemaConstants.TITLE_AT ) );
        entity.setEmployeeType( getAttribute( entry, EMPLOYEE_TYPE ) );
        unloadTemporal( entry, entity );
        entity.setRoles( unloadUserRoles( entry, entity.getUserId(), contextId ) );
        entity.setAdminRoles( unloadUserAdminRoles( entry, entity.getUserId(), contextId ) );
        entity.setAddress( unloadAddress( entry ) );
        entity.setPhones( getAttributes( entry, SchemaConstants.TELEPHONE_NUMBER_AT ) );
        entity.setMobiles( getAttributes( entry, MOBILE ) );
        entity.setEmails( getAttributes( entry, SchemaConstants.MAIL_AT ) );
        String szBoolean = getAttribute( entry, SYSTEM_USER );
        if ( szBoolean != null )
        {
            entity.setSystem( Boolean.valueOf( szBoolean ) );
        }

        /*
                TODO: Add for RFC2307BIS
                entity.setUidNumber( getAttribute( entry, UID_NUMBER ) );
                entity.setGidNumber( getAttribute( entry, GID_NUMBER ) );
                entity.setHomeDirectory( getAttribute( entry, HOME_DIRECTORY ) );
                entity.setLoginShell( getAttribute( entry, LOGIN_SHELL ) );
                entity.setGecos( getAttribute( entry, GECOS ) );
        */

        entity.addProperties( AttrHelper.getProperties( getAttributes( entry, GlobalIds.PROPS ) ) );

        if ( GlobalIds.IS_OPENLDAP )
        {
            szBoolean = getAttribute( entry, OPENLDAP_PW_RESET );
            if ( szBoolean != null && szBoolean.equalsIgnoreCase( "true" ) )
            {
                entity.setReset( true );
            }
            String szPolicy = getAttribute( entry, OPENLDAP_POLICY_SUBENTRY );
            if ( StringUtils.isNotEmpty( szPolicy ) )
            {
                entity.setPwPolicy( getRdn( szPolicy ) );
            }

            szBoolean = getAttribute( entry, OPENLDAP_PW_LOCKED_TIME );

            if ( szBoolean != null && szBoolean.equals( LOCK_VALUE ) )
            {
                entity.setLocked( true );
            }
        }

        entity.setJpegPhoto( getPhoto( entry, JPEGPHOTO ) );

        return entity;
    }


    /**
     * @param userId
     * @return
     * @throws FinderException
     */
    private List<UserRole> getUserRoles( String userId, String contextId ) throws FinderException
    {
        List<UserRole> roles = null;
        LdapConnection ld = null;
        String userDn = getDn( userId, contextId );
        try
        {
            ld = getAdminConnection();
            Entry findEntry = read( ld, userDn, ROLE_ATR );
            roles = unloadUserRoles( findEntry, userId, contextId );
        }
        catch ( LdapNoSuchObjectException e )
        {
            String warning = "getUserRoles COULD NOT FIND ENTRY for user [" + userId + "]";
            throw new FinderException( GlobalErrIds.USER_NOT_FOUND, warning );
        }
        catch ( LdapException e )
        {
            String error = "getUserRoles [" + userDn + "]= caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.USER_READ_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }

        return roles;
    }


    /**
     * Given a collection of ARBAC roles, {@link UserAdminRole}, convert to raw data format and load into ldap
     * attribute set in preparation for ldap add.
     *
     * @param list  contains List of type {@link UserAdminRole} targeted for adding to ldap.
     * @param entry collection of ldap attributes containing ARBAC role assignments in raw ldap format.
     * @throws LdapException
     */
    private void loadUserAdminRoles( List<UserAdminRole> list, Entry entry ) throws LdapException
    {
        if ( list != null )
        {
            Attribute userAdminRoleData = new DefaultAttribute( GlobalIds.USER_ADMINROLE_DATA );
            Attribute userAdminRoleAssign = new DefaultAttribute( GlobalIds.USER_ADMINROLE_ASSIGN );

            for ( UserAdminRole userRole : list )
            {
                userAdminRoleData.add( userRole.getRawData() );
                userAdminRoleAssign.add( userRole.getName() );
            }

            if ( userAdminRoleData.size() != 0 )
            {
                entry.add( userAdminRoleData );
                entry.add( userAdminRoleAssign );
            }
        }
    }


    /**
     * Given a collection of RBAC roles, {@link UserRole}, convert to raw data format and load into ldap modification
     * set in preparation for ldap modify.
     *
     * @param list contains List of type {@link UserRole} targeted for updating into ldap.
     * @param mods contains ldap modification set containing RBAC role assignments in raw ldap format to be updated.
     * @throws LdapInvalidAttributeValueException
     */
    private void loadUserRoles( List<UserRole> list, List<Modification> mods )
        throws LdapInvalidAttributeValueException
    {
        Attribute userRoleData = new DefaultAttribute( GlobalIds.USER_ROLE_DATA );
        Attribute userRoleAssign = new DefaultAttribute( GlobalIds.USER_ROLE_ASSIGN );

        if ( list != null )
        {
            for ( UserRole userRole : list )
            {
                userRoleData.add( userRole.getRawData() );
                userRoleAssign.add( userRole.getName() );
            }

            if ( userRoleData.size() != 0 )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, userRoleData ) );
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, userRoleAssign ) );
            }
        }
    }


    /**
     * Given a collection of ARBAC roles, {@link UserAdminRole}, convert to raw data format and load into ldap
     * modification set in preparation for ldap modify.
     *
     * @param list contains List of type {@link UserAdminRole} targeted for updating to ldap.
     * @param mods contains ldap modification set containing ARBAC role assignments in raw ldap format to be updated.
     * @throws LdapInvalidAttributeValueException
     */
    private void loadUserAdminRoles( List<UserAdminRole> list, List<Modification> mods ) throws
        LdapInvalidAttributeValueException
    {
        Attribute userAdminRoleData = new DefaultAttribute( GlobalIds.USER_ADMINROLE_DATA );
        Attribute userAdminRoleAssign = new DefaultAttribute( GlobalIds.USER_ADMINROLE_ASSIGN );

        if ( list != null )
        {
            boolean nameSeen = false;

            for ( UserAdminRole userRole : list )
            {
                userAdminRoleData.add( userRole.getRawData() );

                if ( !nameSeen )
                {
                    userAdminRoleAssign.add( userRole.getName() );
                    nameSeen = true;
                }
            }

            if ( userAdminRoleData.size() != 0 )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, userAdminRoleData ) );
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, userAdminRoleAssign ) );
            }
        }
    }


    /**
     * Given a collection of RBAC roles, {@link UserRole}, convert to raw data format and load into ldap attribute
     * set in preparation for ldap add.
     *
     * @param list  contains List of type {@link UserRole} targeted for adding to ldap.
     * @param entry ldap entry containing attributes mapping to RBAC role assignments in raw ldap format.
     * @throws LdapException
     */
    private void loadUserRoles( List<UserRole> list, Entry entry ) throws LdapException
    {
        if ( list != null )
        {
            Attribute userRoleData = new DefaultAttribute( GlobalIds.USER_ROLE_DATA );
            Attribute userRoleAssign = new DefaultAttribute( GlobalIds.USER_ROLE_ASSIGN );

            for ( UserRole userRole : list )
            {
                userRoleData.add( userRole.getRawData() );
                userRoleAssign.add( userRole.getName() );
            }

            if ( userRoleData.size() != 0 )
            {
                entry.add( userRoleData, userRoleAssign );
            }
        }
    }


    /**
     * Given a User address, {@link org.apache.directory.fortress.core.model.Address}, load into ldap attribute set in preparation for ldap add.
     *
     * @param address contains User address {@link org.apache.directory.fortress.core.model.Address} targeted for adding to ldap.
     * @param entry   collection of ldap attributes containing RBAC role assignments in raw ldap format.
     * @throws org.apache.directory.api.ldap.model.exception.LdapException
     */
    private void loadAddress( Address address, Entry entry ) throws LdapException
    {
        if ( address != null )
        {
            if ( ObjUtil.isNotNullOrEmpty( address.getAddresses() ) )
            {
                for ( String val : address.getAddresses() )
                {
                    entry.add( SchemaConstants.POSTAL_ADDRESS_AT, val );
                }
            }

            if ( StringUtils.isNotEmpty( address.getCity() ) )
            {
                entry.add( SchemaConstants.L_AT, address.getCity() );
            }

            //if(StringUtils.isNotEmpty(address.getCountry()))
            //{
            //    attrs.add(GlobalIds.COUNTRY, address.getAddress1());
            //}

            if ( StringUtils.isNotEmpty( address.getPostalCode() ) )
            {
                entry.add( SchemaConstants.POSTALCODE_AT, address.getPostalCode() );
            }

            if ( StringUtils.isNotEmpty( address.getPostOfficeBox() ) )
            {
                entry.add( SchemaConstants.POSTOFFICEBOX_AT, address.getPostOfficeBox() );
            }

            if ( StringUtils.isNotEmpty( address.getState() ) )
            {
                entry.add( SchemaConstants.ST_AT, address.getState() );
            }

            if ( StringUtils.isNotEmpty( address.getBuilding() ) )
            {
                entry.add( SchemaConstants.PHYSICAL_DELIVERY_OFFICE_NAME_AT, address.getBuilding() );
            }

            if ( StringUtils.isNotEmpty( address.getDepartmentNumber() ) )
            {
                entry.add( DEPARTMENT_NUMBER, address.getDepartmentNumber() );
            }

            if ( StringUtils.isNotEmpty( address.getRoomNumber() ) )
            {
                entry.add( ROOM_NUMBER, address.getRoomNumber() );
            }
        }
    }


    /**
     * Given an address, {@link Address}, load into ldap modification set in preparation for ldap modify.
     *
     * @param address contains entity of type {@link Address} targeted for updating into ldap.
     * @param mods    contains ldap modification set contains attributes to be updated in ldap.
     */
    private void loadAddress( Address address, List<Modification> mods )
    {
        if ( address != null )
        {
            if ( ObjUtil.isNotNullOrEmpty( address.getAddresses() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants
                    .POSTAL_ADDRESS_AT ) );

                for ( String val : address.getAddresses() )
                {
                    mods.add( new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, SchemaConstants
                        .POSTAL_ADDRESS_AT, val ) );
                }
            }

            if ( StringUtils.isNotEmpty( address.getCity() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants.L_AT,
                    address.getCity() ) );
            }

            if ( StringUtils.isNotEmpty( address.getPostalCode() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants
                    .POSTALCODE_AT, address.getPostalCode() ) );
            }

            if ( StringUtils.isNotEmpty( address.getPostOfficeBox() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants
                    .POSTOFFICEBOX_AT, address.getPostOfficeBox() ) );
            }

            if ( StringUtils.isNotEmpty( address.getState() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants.ST_AT,
                    address.getState() ) );
            }

            if ( StringUtils.isNotEmpty( address.getBuilding() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants
                    .PHYSICAL_DELIVERY_OFFICE_NAME_AT, address.getBuilding() ) );
            }

            if ( StringUtils.isNotEmpty( address.getDepartmentNumber() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, DEPARTMENT_NUMBER,
                    address.getDepartmentNumber() ) );
            }

            if ( StringUtils.isNotEmpty( address.getRoomNumber() ) )
            {
                mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, ROOM_NUMBER, address
                    .getRoomNumber() ) );
            }
        }
    }


    /**
     * Given an ldap entry containing organzationalPerson address information, convert to {@link Address}
     *
     * @param entry contains ldap entry to retrieve admin roles from.
     * @return entity of type {@link Address}.
     * @throws LdapInvalidAttributeValueException
     * @throws org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException in the event of ldap
     * client error.
     */
    private Address unloadAddress( Entry entry ) throws LdapInvalidAttributeValueException
    {
        Address addr = new ObjectFactory().createAddress();
        List<String> pAddrs = getAttributes( entry, SchemaConstants.POSTAL_ADDRESS_AT );

        if ( pAddrs != null )
        {
            for ( String pAddr : pAddrs )
            {
                addr.setAddress( pAddr );
            }
        }

        addr.setCity( getAttribute( entry, SchemaConstants.L_AT ) );
        addr.setState( getAttribute( entry, SchemaConstants.ST_AT ) );
        addr.setPostalCode( getAttribute( entry, SchemaConstants.POSTALCODE_AT ) );
        addr.setPostOfficeBox( getAttribute( entry, SchemaConstants.POSTOFFICEBOX_AT ) );
        addr.setBuilding( getAttribute( entry, SchemaConstants.PHYSICAL_DELIVERY_OFFICE_NAME_AT ) );
        addr.setDepartmentNumber( getAttribute( entry, DEPARTMENT_NUMBER ) );
        addr.setRoomNumber( getAttribute( entry, ROOM_NUMBER ) );
        // todo: add support for country attribute
        //addr.setCountry(getAttribute(le, GlobalIds.COUNTRY));

        return addr;
    }


    /**
     * Given an ldap entry containing ARBAC roles assigned to user, retrieve the raw data and convert to a collection
     * of {@link UserAdminRole}
     * including {@link org.apache.directory.fortress.core.util.time.Constraint}.
     *
     * @param entry     contains ldap entry to retrieve admin roles from.
     * @param userId    attribute maps to {@link UserAdminRole#userId}.
     * @param contextId
     * @return List of type {@link UserAdminRole} containing admin roles assigned to a particular user.
     */
    private List<UserAdminRole> unloadUserAdminRoles( Entry entry, String userId, String contextId )
    {
        List<UserAdminRole> uRoles = null;
        List<String> roles = getAttributes( entry, GlobalIds.USER_ADMINROLE_DATA );

        if ( roles != null )
        {
            long sequence = 0;
            uRoles = new ArrayList<>();

            for ( String raw : roles )
            {
                UserAdminRole ure = new ObjectFactory().createUserAdminRole();
                ure.load( raw, contextId, new RoleUtil() );
                ure.setSequenceId( sequence++ );
                ure.setUserId( userId );
                uRoles.add( ure );
            }
        }

        return uRoles;
    }


    /**
     * @param userId
     * @param contextId
     * @return
     */
    private String getDn( String userId, String contextId )
    {
        return SchemaConstants.UID_AT + "=" + userId + "," + getRootDn( contextId, GlobalIds.USER_ROOT );
    }


    /**
     * Given an ldap entry containing RBAC roles assigned to user, retrieve the raw data and convert to a collection
     * of {@link UserRole}
     * including {@link org.apache.directory.fortress.core.util.time.Constraint}.
     *
     * @param entry     contains ldap entry to retrieve roles from.
     * @param userId    attribute maps to {@link UserRole#userId}.
     * @param contextId
     * @return List of type {@link UserRole} containing RBAC roles assigned to a particular user.
     */
    private List<UserRole> unloadUserRoles( Entry entry, String userId, String contextId )
    {
        List<UserRole> uRoles = null;
        List<String> roles = getAttributes( entry, GlobalIds.USER_ROLE_DATA );

        if ( roles != null )
        {
            long sequence = 0;
            uRoles = new ArrayList<>();

            for ( String raw : roles )
            {
                UserRole ure = new ObjectFactory().createUserRole();
                ure.load( raw, contextId, new RoleUtil() );
                ure.setUserId( userId );
                ure.setSequenceId( sequence++ );
                uRoles.add( ure );
            }
        }

        return uRoles;
    }
}
