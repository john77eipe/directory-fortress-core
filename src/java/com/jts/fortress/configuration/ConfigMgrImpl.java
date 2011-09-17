/*
 * Copyright (c) 2009-2011. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress.configuration;

import com.jts.fortress.SecurityException;

import java.util.Properties;


/**
 * This Manager impl supplies CRUD methods used to manage properties stored within the ldap directory.
 * The Fortress config nodes are used to remotely share Fortress client specific properties between processes.
 * Fortress places no limits on the number of unique configurations that can be present at one time in the directory.
 * The Fortress client will specify the preferred configuration node by name via a property named, {@link com.jts.fortress.constants.GlobalIds#CONFIG_REALM}.
 * Each process using Fortress client is free to share an existing node with other processes or create its own unique config
 * instance using the methods within this class.<BR>
 * <p/>
 * This object is thread safe.
 * <p/>

 *
 * @author smckinn
 * @created February 5, 2011
 */
public class ConfigMgrImpl implements ConfigMgr
{
    private static final ConfigP cfgP = new ConfigP();

    /**
     * Create a new configuration node with given name and properties.  The name is required.  If node already exists,
     * a {@link com.jts.fortress.SecurityException} with error {@link com.jts.fortress.constants.GlobalErrIds#FT_CONFIG_ALREADY_EXISTS} will be thrown.
     *
     * @param name    attribute is required and maps to 'cn' attribute in 'device' object class.
     * @param inProps contains {@link Properties} with list of name/value pairs to remove from existing config node.
     * @return {@link Properties} containing the collection of name/value pairs just added.
     * @throws com.jts.fortress.SecurityException in the event entry already present or other system error.
     */
    public Properties add(String name, Properties inProps) throws com.jts.fortress.SecurityException
    {
        return cfgP.add(name, inProps);
    }


    /**
     * Update existing configuration node with additional properties, or, replace existing properties.  The name is required.  If node does not exist,
     * a {@link com.jts.fortress.SecurityException} with error {@link com.jts.fortress.constants.GlobalErrIds#FT_CONFIG_NOT_FOUND} will be thrown.
     *
     * @param name    attribute is required and maps to 'cn' attribute in 'device' object class.
     * @param inProps contains {@link Properties} with list of name/value pairs to add or udpate from existing config node.
     * @return {@link Properties} containing the collection of name/value pairs to be added to existing node.
     * @throws com.jts.fortress.SecurityException in the event entry not present or other system error.
     */
    public Properties update(String name, Properties inProps) throws com.jts.fortress.SecurityException
    {
        return cfgP.update(name, inProps);
    }

    /**
     * <font size="3" color="red">
     * Completely removes named configuration node from the directory.
     * <p/>
     * This method is destructive and will remove the configuration node completely from directory.<BR>
     * Care should be taken during execution to ensure target name is correct and permanent removal of all parameters located
     * there is intended.  There is no 'undo' for this operation.
     * <p/>
     * </font>
     *
     * @param name is required and maps to 'cn' attribute on 'device' object class of node targeted for operation.
     * @throws com.jts.fortress.SecurityException in the event of system error.
     */
    public void delete(String name) throws SecurityException
    {
        cfgP.delete(name);
    }

    /**
     * Delete existing configuration node with additional properties, or, replace existing properties.  The name is required.  If node does not exist,
     * a {@link com.jts.fortress.SecurityException} with error {@link com.jts.fortress.constants.GlobalErrIds#FT_CONFIG_NOT_FOUND} will be thrown.
     *
     * @param name attribute is required and maps to 'cn' attribute in 'device' object class.
     * @return {@link Properties} containing the collection of name/value pairs to be added to existing node.
     * @throws com.jts.fortress.SecurityException in the event entry not present or other system error.
     */
    public void delete(String name, Properties inProps) throws SecurityException
    {
        cfgP.delete(name, inProps);
    }

    /**
     * Read an existing configuration node with given name and return to caller.  The name is required.  If node doesn't exist,
     * a {@link com.jts.fortress.SecurityException} with error {@link com.jts.fortress.constants.GlobalErrIds#FT_CONFIG_NOT_FOUND} will be thrown.
     *
     * @param name attribute is required and maps to 'cn' attribute in 'device' object class.
     * @return {@link Properties} containing the collection of name/value pairs just added. Maps to 'ftProps' attribute in 'ftProperties' object class.
     * @throws SecurityException in the event entry doesn't exist or other system error.
     */
    public Properties read(String name) throws com.jts.fortress.SecurityException
    {
        return cfgP.read(name);
    }
}
