/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.ws.scout.registry.qa;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.registry.BulkResponse;
import javax.xml.registry.BusinessLifeCycleManager;
import javax.xml.registry.JAXRException;
import javax.xml.registry.JAXRResponse;
import javax.xml.registry.RegistryService;
import javax.xml.registry.infomodel.Concept;
import javax.xml.registry.infomodel.ExternalLink;
import javax.xml.registry.infomodel.InternationalString;
import javax.xml.registry.infomodel.Key;
import javax.xml.registry.infomodel.Organization;
import javax.xml.registry.infomodel.Service;
import javax.xml.registry.infomodel.ServiceBinding;
import javax.xml.registry.infomodel.SpecificationLink;

import junit.framework.JUnit4TestAdapter;

import org.apache.ws.scout.BaseTestCase;
import org.apache.ws.scout.Finder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
Tests Publish, Delete (and indirectly, find) for service bindings.
 * 
 * You can comment out the deletion portion and use 
 * Open source UDDI Browser  <http://www.uddibrowser.org>
 * to check your intermediate results
 *
 * Based on query/publish tests written by 
 * <a href="mailto:anil@apache.org">Anil Saldhana</a>.
 *
 * @author <a href="mailto:dbhole@redhat.com">Deepak Bhole</a>
 * @author <a href="mailto:anil@apache.org">Anil Saldhana</a>
 *
 * @since Sep 27, 2005
 */
public class JAXR050ServiceBindingTest extends BaseTestCase
{
    
    String serviceBindingName = "Apache JAXR Service Binding -- APACHE SCOUT TEST";
    String serviceName = "Apache JAXR Service -- APACHE SCOUT TEST";
	String tempOrgName = "Apache JAXR Service Org -- APACHE SCOUT TEST";

    @Before
    public void setUp()
    {
       super.setUp();
    }
    @After
    public void tearDown()
    {
        super.tearDown();
    }

	/**
	 * Tests publishing and deleting of service bindings.
	 * 
	 * Do not break this into testPublish(), testDelete(), etc. Order is
	 * important, and not all jvms can guarantee order since the JUnit framework
	 * uses getMethods() to gather test methods, and getMethods() does not
	 * guarantee order.
	 */
    @Test
    public void testPublishFindAndDeleteServiceBinding()
    {
        login();
        try
        {
            RegistryService rs = connection.getRegistryService();
            blm = rs.getBusinessLifeCycleManager();
            bqm = rs.getBusinessQueryManager();
            Finder finder = new Finder(bqm, uddiversion);

            System.out.println("\nCreating temporary organization...\n");
            Organization tmpOrg = createTempOrg();
            Key tmpOrgKey = tmpOrg.getKey();
            
            System.out.println("\nCreating service...\n");
            Service tmpSvc = createTempService(tmpOrg);
            Key tmpSvcKey = tmpSvc.getKey();
            
            System.out.println("\nCreating service binding...\n");
            Key sbKey = createServiceBinding(tmpSvc);
                       
            
            SpecificationLink specLink = blm.createSpecificationLink();            
            Concept concept = null;
            if ("3.0".equals(uddiversion)) {
            	concept = (Concept)bqm.getRegistryObject("uddi:uddi.org:findqualifier:orlikekeys", BusinessLifeCycleManager.CONCEPT);
            } else {
            	concept = (Concept)bqm.getRegistryObject("uuid:AD61DE98-4DB8-31B2-A299-A2373DC97212",BusinessLifeCycleManager.CONCEPT);
            }
            specLink.setSpecificationObject(concept);
            
            //find serviceBinding
            Collection<ServiceBinding> serviceBindings2 = finder.findServiceBindings(tmpSvcKey, specLink);
            @SuppressWarnings("unused")
			ServiceBinding serviceBinding2 = serviceBindings2.iterator().next();
            
            // All created ... now try to delete.
            deleteServiceBinding(sbKey);
            deleteTempService(tmpSvcKey);
            deleteTempOrg(tmpOrgKey);
            // No find service binding.. search by name is not currently supported. 
            
        } catch (JAXRException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }


    private Key createServiceBinding(Service tmpSvc) throws JAXRException {
    	Key key = null;
        ServiceBinding serviceBinding = blm.createServiceBinding();
        serviceBinding.setName(getIString(serviceBindingName));
        serviceBinding.setDescription(getIString("UDDI service binding"));
        tmpSvc.addServiceBinding(serviceBinding);
        
        SpecificationLink specLink = blm.createSpecificationLink();
        ExternalLink externalLink = blm.createExternalLink("http://localhost:8080/jmx-console", "Scout test");
        Collection<ExternalLink> externalLinks = new ArrayList<ExternalLink>();
        externalLinks.add(externalLink);
        specLink.setExternalLinks(externalLinks);
        
        RegistryService rs = connection.getRegistryService();
        bqm = rs.getBusinessQueryManager();
        Concept concept = null;
        if ("3.0".equals(uddiversion)) {        
        	concept = (Concept)bqm.getRegistryObject("uddi:uddi.org:findqualifier:orlikekeys", BusinessLifeCycleManager.CONCEPT);
        } else {
        	concept = (Concept)bqm.getRegistryObject("uuid:AD61DE98-4DB8-31B2-A299-A2373DC97212",BusinessLifeCycleManager.CONCEPT);
        }
        
        specLink.setSpecificationObject(concept);
        
        serviceBinding.addSpecificationLink(specLink);
        ArrayList<ServiceBinding> serviceBindings = new ArrayList<ServiceBinding>();
        serviceBindings.add(serviceBinding);

        BulkResponse br = blm.saveServiceBindings(serviceBindings);
        if (br.getStatus() == JAXRResponse.STATUS_SUCCESS)
        {
            System.out.println("Service Binding Saved");
            key = (Key) br.getCollection().iterator().next();
            System.out.println("Saved Key=" + key.getId());
        } else
        {
            System.err.println("JAXRExceptions " +
                    "occurred during save:");
            Collection exceptions = br.getExceptions();
            Iterator iter = exceptions.iterator();
            while (iter.hasNext())
            {
                Exception e = (Exception) iter.next();
                System.err.println(e.toString());
                fail(e.toString());
            }
        }
        
        return key;
    }

    private void deleteServiceBinding(Key key) throws JAXRException {

    	String id = key.getId();

    	System.out.println("\nDeleting service binding with id " + id + "\n");

    	Collection<Key> keys = new ArrayList<Key>();
    	keys.add(key);
    	BulkResponse response = blm.deleteServiceBindings(keys);

    	Collection exceptions = response.getExceptions();
    	if (exceptions == null) {
    		Collection retKeys = response.getCollection();
    		Iterator keyIter = retKeys.iterator();
    		javax.xml.registry.infomodel.Key orgKey = null;
    		if (keyIter.hasNext()) {
    			orgKey = 
    				(javax.xml.registry.infomodel.Key) keyIter.next();
    			id = orgKey.getId();
    			System.out.println("Service binding with ID=" + id + " was deleted");
    		}
    	}
    }

    private Service createTempService(Organization tmpOrg) throws JAXRException {
    	
        Service service = blm.createService(getIString(serviceName));
        service.setDescription(getIString("Services in UDDI Registry"));
        service.setProvidingOrganization(tmpOrg);

        ArrayList<Service> services = new ArrayList<Service>();
        services.add(service);

        BulkResponse br = blm.saveServices(services);
        if (br.getStatus() == JAXRResponse.STATUS_SUCCESS)
        {
            System.out.println("Service Saved");
            Key key = (Key) br.getCollection().iterator().next();
            System.out.println("Saved Key=" + key.getId());
            service.setKey(key);
        } else
        {
            System.err.println("JAXRExceptions " +
                    "occurred during save:");
            Collection exceptions = br.getExceptions();
            Iterator iter = exceptions.iterator();
            while (iter.hasNext())
            {
                Exception e = (Exception) iter.next();
                System.err.println(e.toString());
            }
        }
        
        return service;
    }

    private void deleteTempService(Key key) throws JAXRException {

    	String id = key.getId();

    	System.out.println("\nDeleting service with id " + id + "\n");

    	Collection<Key> keys = new ArrayList<Key>();
    	keys.add(key);
    	BulkResponse response = blm.deleteServices(keys);

    	Collection exceptions = response.getExceptions();
    	if (exceptions == null) {
    		Collection retKeys = response.getCollection();
    		Iterator keyIter = retKeys.iterator();
    		javax.xml.registry.infomodel.Key orgKey = null;
    		if (keyIter.hasNext()) {
    			orgKey = 
    				(javax.xml.registry.infomodel.Key) keyIter.next();
    			id = orgKey.getId();
    			System.out.println("Service with ID=" + id + " was deleted");
    		}
    	}
    }
    
    private Organization createTempOrg() throws JAXRException {

        Key orgKey = null;
        Organization org = blm.createOrganization(getIString(tempOrgName));
        org.setDescription(getIString("Temporary organization to test saveService()"));

        Collection<Organization> orgs = new ArrayList<Organization>();
        orgs.add(org);
        BulkResponse br = blm.saveOrganizations(orgs);
        
        if (br.getStatus() == JAXRResponse.STATUS_SUCCESS)
        {
        	orgKey = (Key) br.getCollection().iterator().next();
            System.out.println("Temporary Organization Created with id=" + orgKey.getId());
            org.setKey(orgKey);
        }  else
        {
            System.err.println("JAXRExceptions " +
                    "occurred during creation of temporary organization:");
            
            Iterator iter = br.getCollection().iterator();
            
            while (iter.hasNext()) {
            	Exception e = (Exception) iter.next();
            	System.err.println(e.toString());
            }
            
            fail();
        }
        
        return org;
    }
    
    private void deleteTempOrg(Key key) throws JAXRException {

    	if (key == null) {
    		return;
    	}
    	
    	String id = key.getId();

    	System.out.println("\nDeleting temporary organization with id " + id + "\n");

    	Collection<Key> keys = new ArrayList<Key>();
    	keys.add(key);
    	BulkResponse response = blm.deleteOrganizations(keys);

    	Collection exceptions = response.getExceptions();
    	if (exceptions == null) {
    		Collection retKeys = response.getCollection();
    		Iterator keyIter = retKeys.iterator();
    		Key orgKey = null;
    		if (keyIter.hasNext()) {
    			orgKey = 
    				(javax.xml.registry.infomodel.Key) keyIter.next();
    			id = orgKey.getId();
    			System.out.println("Organization with ID=" + id + " was deleted");
    		}
    	}
    }
    
    private InternationalString getIString(String str)
    throws JAXRException
    {
        return blm.createInternationalString(str);
    }
    
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(JAXR050ServiceBindingTest.class);
    }
}
