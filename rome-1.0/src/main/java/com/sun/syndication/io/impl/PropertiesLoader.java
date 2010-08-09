/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sun.syndication.io.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

/**
 * <p>
 * Properties loader that aggregates a master properties file and several extra properties files,
 * all from the current classpath.
 * </p>
 * <p>
 * The master properties file has to be in a distinct location than the extra properties file.
 * Firs the master properties file is loaded, then all the extra properties files in their order
 * of appearance in the classpath.
 * </p>
 * <p>
 * Current use cases (plugin manager for parsers/converters/generators for feeds and modules and date
 * formats) assume properties are list of tokens, that why the only method to get property values
 * is the getTokenizedProperty().
 * </p>
 * 
 * @author willem
 * @author jbonofre
 *
 */
public class PropertiesLoader {
    
    private static final String MASTER_PLUGIN_FILE = "com/sun/syndication/rome.properties";
    private static final String EXTRA_PLUGIN_FILE = "rome.properties";
    
    private static Map clMap = new WeakHashMap();
    
    /**
     * <p>
     * Returns the PropertiesLoader singleton used by ROME to load plugin components.
     * </p>
     * 
     * @return PropertiesLoader singleton.
     */
    public static PropertiesLoader getPropertiesLoader() {
        synchronized(PropertiesLoader.class) {
            PropertiesLoader loader = (PropertiesLoader) clMap.get(Thread.currentThread().getContextClassLoader());
            if (loader == null) {
                try {
                    loader = new PropertiesLoader(MASTER_PLUGIN_FILE, EXTRA_PLUGIN_FILE);
                    clMap.put(Thread.currentThread().getContextClassLoader(), loader);
                } catch (IOException ioException) {
                    throw new RuntimeException(ioException);
                }
            }
            return loader;
        }
    }
    
    private Properties[] _properties;
    
    /**
     * <p>
     * Creates a PropertiesLoader.
     * </p>
     * 
     * @param masterFileLocation master file location, there must be only one.
     * @param extraFileLocation extra file location, there may be many.
     * @throws IOException thrown if one of the properties file could not be read.
     */
    private PropertiesLoader(String masterFileLocation, String extraFileLocation) throws IOException {
        List propertiesList = new ArrayList();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        InputStream is = classLoader.getResourceAsStream(masterFileLocation);
        if (is == null) {
            classLoader = this.getClass().getClassLoader();
            is = classLoader.getResourceAsStream(masterFileLocation);
        }
        if (is == null) {
            throw new IOException("Could not load ROME master plugins file [" + masterFileLocation + "]");
        }
        Properties p = new Properties();
        p.load(is);
        is.close();
        propertiesList.add(p);
        
        classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration urls = classLoader.getResources(extraFileLocation);
        while (urls.hasMoreElements()) {
            URL url = (URL) urls.nextElement();
            p = new Properties();
            try {
                is = url.openStream();
                p.load(is);
                is.close();
            } catch (IOException ioException) {
                IOException thrown = new IOException("Could not load ROME extensions plugins file [" + url.toString() + "], " + ioException.getMessage());
                thrown.setStackTrace(ioException.getStackTrace());
                throw thrown;
            }
            propertiesList.add(p);
        }
        
        _properties = new Properties[propertiesList.size()];
        propertiesList.toArray(_properties);   
    }
    
    /**
     * <p>
     * Returns an array of tokenized values stored under a property key in all properties files.
     * If the master file has this property its tokens will be the first ones in the array.
     * </p>
     * 
     * @param key property key to retrieve values.
     * @param separator String will all separator characters to tokenize from the values in all
     * properties files.
     * @return all the tokens for the given property key from all the properties files.
     */
    public String[] getTokenizedProperty(String key, String separator) {
        List entriesList = new ArrayList();
        for (int i = 0; i < _properties.length; i++) {
            String values = _properties[i].getProperty(key);
            if (values != null) {
                StringTokenizer st = new StringTokenizer(values, separator);
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    entriesList.add(token);
                }
            }
        }
        String[] entries = new String[entriesList.size()];
        entriesList.toArray(entries);
        return entries;
    }
    
    /**
     * <p>
     * Returns an array of values stored under a property key in all properties files.
     * If the master file has this property it will be the first ones in the array.
     * </p>
     * 
     * @param key property key to retrieve values.
     * @return all the values for the given property key from all the properties files.
     */
    public String[] getProperty(String key) {
        List entriesList = new ArrayList();
        for (int i =0; i < _properties.length; i++) {
            String values = _properties[i].getProperty(key);
            if (values != null) {
                entriesList.add(values);
            }
        }
        String[] entries = new String[entriesList.size()];
        entriesList.toArray(entries);
        return entries;
    }

}
