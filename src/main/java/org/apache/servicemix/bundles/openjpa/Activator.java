/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.servicemix.bundles.openjpa;

import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.spi.PersistenceProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.openjpa.persistence.PersistenceProductDerivation;
import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;


/**
 * A bundle activator for OpenJPA framework to load default 
 * persistent configuration file
*/
public class Activator extends org.apache.servicemix.specs.locator.Activator {
    // following is so Aries can find and extend us for OSGi RFC 143
    public static final String PERSISTENCE_PROVIDER_ARIES = "javax.persistence.provider";
    // following would be set by Aries to expose their OSGi enabled provider
    public static final String PERSISTENCE_PROVIDER = PersistenceProvider.class.getName();
    public static final String OSGI_PERSISTENCE_PROVIDER = PersistenceProviderImpl.class.getName();
    private static ServiceRegistration svcReg = null;

    private static final transient Log LOG = LogFactory.getLog(Activator.class);
    private Map<Long, URL> persistenceFiles = new ConcurrentHashMap<Long, URL>();
    
    @Override
    public void start(BundleContext bundleContext) throws Exception {
        super.start(bundleContext);
        PersistenceProvider provider = new PersistenceProviderImpl();
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(PERSISTENCE_PROVIDER_ARIES, OSGI_PERSISTENCE_PROVIDER);
        svcReg = bundleContext.registerService(PERSISTENCE_PROVIDER, provider, props);
        LOG.info("OpenJPA activator starting");
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        super.stop(bundleContext);
        PersistenceProductDerivation.setDefaultPersistenceFiles(null);
        svcReg.unregister();
        LOG.info("OpenJPA activator stopping");
    }

    @Override
    protected void register(Bundle bundle) {
        URL url = bundle.getResource("META-INF/persistence.xml");
        if (url != null) {
            LOG.debug("found persistence file at " + url);
            persistenceFiles.put(bundle.getBundleId(), url);
            resetDefaultPersistenceFiles(); 
        }
    } 

    @Override
    protected void unregister(long bundleId) {
        URL file = persistenceFiles.remove(bundleId);
        if (file != null) {
            LOG.debug("removing persistence file URL at " + file);
            resetDefaultPersistenceFiles();
        }
    }
   
    private void resetDefaultPersistenceFiles() {
        ArrayList<URL> list = new ArrayList<URL>();
        list.addAll(persistenceFiles.values());
        PersistenceProductDerivation.setDefaultPersistenceFiles(list);
    }

}

