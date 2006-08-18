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
package org.apache.ws.scout.registry;

import org.apache.juddi.IRegistry;
import org.apache.juddi.datatype.CategoryBag;
import org.apache.juddi.datatype.Description;
import org.apache.juddi.datatype.KeyedReference;
import org.apache.juddi.datatype.Name;
import org.apache.juddi.datatype.assertion.PublisherAssertion;
import org.apache.juddi.datatype.binding.BindingTemplate;
import org.apache.juddi.datatype.business.BusinessEntity;
import org.apache.juddi.datatype.request.FindQualifiers;
import org.apache.juddi.datatype.response.*;
import org.apache.juddi.datatype.service.BusinessService;
import org.apache.juddi.datatype.tmodel.TModel;
import org.apache.juddi.error.RegistryException;
import org.apache.log4j.Logger;
import org.apache.ws.scout.registry.infomodel.ClassificationSchemeImpl;
import org.apache.ws.scout.registry.infomodel.ConceptImpl;
import org.apache.ws.scout.registry.infomodel.InternationalStringImpl;
import org.apache.ws.scout.registry.infomodel.KeyImpl;
import org.apache.ws.scout.registry.infomodel.ServiceBindingImpl;
import org.apache.ws.scout.registry.infomodel.AssociationImpl;
import org.apache.ws.scout.util.EnumerationHelper;
import org.apache.ws.scout.util.ScoutUddiJaxrHelper;

import javax.xml.registry.BulkResponse;
import javax.xml.registry.BusinessLifeCycleManager;
import javax.xml.registry.BusinessQueryManager;
import javax.xml.registry.FindQualifier;
import javax.xml.registry.InvalidRequestException;
import javax.xml.registry.JAXRException;
import javax.xml.registry.LifeCycleManager;
import javax.xml.registry.RegistryService;
import javax.xml.registry.UnsupportedCapabilityException;
import javax.xml.registry.infomodel.Association;
import javax.xml.registry.infomodel.ClassificationScheme;
import javax.xml.registry.infomodel.Concept;
import javax.xml.registry.infomodel.Key;
import javax.xml.registry.infomodel.LocalizedString;
import javax.xml.registry.infomodel.Organization;
import javax.xml.registry.infomodel.RegistryObject;
import javax.xml.registry.infomodel.Service;
import javax.xml.registry.infomodel.ServiceBinding;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * Implements the JAXR BusinessQueryManager Interface
 * For futher details, look into the JAXR API Javadoc.
 *
 * @author <a href="mailto:anil@apache.org">Anil Saldhana</a>
 * @author <a href="mailto:jboynes@apache.org">Jeremy Boynes</a>
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 */
public class BusinessQueryManagerImpl implements BusinessQueryManager
{
    private final RegistryServiceImpl registryService;
    
    private static Logger log = Logger.getLogger(BusinessQueryManagerImpl.class);


    public BusinessQueryManagerImpl(RegistryServiceImpl registry)
    {
        this.registryService = registry;
    }

    public RegistryService getRegistryService()
    {
        return registryService;
    }

    /**
     * Finds organizations in the registry that match the specified parameters
     *
     * @param findQualifiers
     * @param namePatterns
     * @param classifications
     * @param specifications
     * @param externalIdentifiers
     * @param externalLinks
     * @return
     * @throws JAXRException
     */
    public BulkResponse findOrganizations(Collection findQualifiers,
                                          Collection namePatterns,
                                          Collection classifications,
                                          Collection specifications,
                                          Collection externalIdentifiers,
                                          Collection externalLinks) throws JAXRException
    {
        IRegistry registry = registryService.getRegistry();
        try
        {
            FindQualifiers juddiFindQualifiers = mapFindQualifiers(findQualifiers);
            Vector nameVector = mapNamePatterns(namePatterns);
            BusinessList result = registry.findBusiness(nameVector,
                    null, null, null, null,
                    juddiFindQualifiers,
                    registryService.getMaxRows());
            Vector v = result.getBusinessInfos().getBusinessInfoVector();

            Collection orgs = new ArrayList();
            int len = 0;
            if (v != null)
            {
                len = v.size();
                orgs = new ArrayList(len);
            }
            for (int i = 0; i < len; i++)
            {
                BusinessInfo info = (BusinessInfo) v.elementAt(i);
                //Now get the details on the individual biz
                BusinessDetail detail = registry.getBusinessDetail(info.getBusinessKey());

                orgs.add(registryService.getLifeCycleManagerImpl().createOrganization(detail));
            }
            return new BulkResponseImpl(orgs);
        } catch (RegistryException e)
        {
            throw new JAXRException(e);
        }
    }

    public BulkResponse findAssociations(Collection findQualifiers,
                                         String sourceObjectId,
                                         String targetObjectId,
                                         Collection associationTypes) throws JAXRException
    {
        //TODO: Currently we just return all the Association objects owned by the caller
        IRegistry registry = registryService.getRegistry();
        try
        {  
            ConnectionImpl con = ((RegistryServiceImpl)getRegistryService()).getConnection();
            AuthToken auth = this.getAuthToken(con,registry);
            PublisherAssertions result =
                    registry.getPublisherAssertions(auth.getAuthInfo());
            Vector v = result.getPublisherAssertionVector();

            Collection col = null;
            int len = 0;
            if (v != null)
            {
                len = v.size();
                col = new ArrayList(len);
            }
            for (int i = 0; i < len; i++)
            {
                PublisherAssertion pas = (PublisherAssertion) v.elementAt(i);
                String sourceKey = pas.getFromKey();
                String targetKey = pas.getToKey();
                Collection orgcol = new ArrayList();
                orgcol.add(new KeyImpl(sourceKey));
                orgcol.add(new KeyImpl(targetKey));
                BulkResponse bl = getRegistryObjects(orgcol, LifeCycleManager.ORGANIZATION);
                Association asso = ScoutUddiJaxrHelper.getAssociation(bl.getCollection(),
                                             registryService.getBusinessLifeCycleManager());
                KeyedReference keyr = pas.getKeyedReference();
                Concept c = new ConceptImpl(getRegistryService().getBusinessLifeCycleManager());
                c.setName(new InternationalStringImpl(keyr.getKeyName()));
                c.setKey( new KeyImpl(keyr.getTModelKey()) );
                c.setValue(keyr.getKeyValue());
                asso.setAssociationType(c);
                col.add(asso);
            }
            return new BulkResponseImpl(col);
        } catch (RegistryException e)
        {
            throw new JAXRException(e);
        }
    }

    public BulkResponse findCallerAssociations(Collection findQualifiers,
                                               Boolean confirmedByCaller,
                                               Boolean confirmedByOtherParty,
                                               Collection associationTypes) throws JAXRException
    {
        //TODO: Currently we just return all the Association objects owned by the caller
        IRegistry registry = registryService.getRegistry();
        try
        { 
            ConnectionImpl con = ((RegistryServiceImpl)getRegistryService()).getConnection();
            AuthToken auth = this.getAuthToken(con,registry);
           
            AssertionStatusReport report = null;
            String confirm = "";
            boolean caller = confirmedByCaller.booleanValue();
            boolean other = confirmedByOtherParty.booleanValue();

            if(caller  && other   )
                        confirm = CompletionStatus.COMPLETE;
            else
              if(!caller  && other  )
                        confirm = CompletionStatus.FROMKEY_INCOMPLETE;
           else
                 if(caller  && !other   )
                        confirm = CompletionStatus.TOKEY_INCOMPLETE;

            report = registry.getAssertionStatusReport(auth.getAuthInfo(),confirm);
            Vector v = report.getAssertionStatusItemVector();
            Collection col = new ArrayList();
            int len = 0;
            if (v != null)
            {
                len = v.size();
                col = new ArrayList(len);
            }
            for (int i = 0; i < len; i++)
            {
                AssertionStatusItem asi = (AssertionStatusItem) v.elementAt(i);
                String sourceKey = asi.getFromKey();
                String targetKey = asi.getToKey();
                Collection orgcol = new ArrayList();
                orgcol.add(new KeyImpl(sourceKey));
                orgcol.add(new KeyImpl(targetKey));
                BulkResponse bl = getRegistryObjects(orgcol, LifeCycleManager.ORGANIZATION);
                Association asso = ScoutUddiJaxrHelper.getAssociation(bl.getCollection(),
                                             registryService.getBusinessLifeCycleManager());
                //Set Confirmation
                ((AssociationImpl)asso).setConfirmedBySourceOwner(caller);
                ((AssociationImpl)asso).setConfirmedByTargetOwner(other);

                if(confirm != CompletionStatus.COMPLETE)
                     ((AssociationImpl)asso).setConfirmed(false);

                Concept c = new ConceptImpl(getRegistryService().getBusinessLifeCycleManager());
                KeyedReference keyr = asi.getKeyedReference();
                c.setKey(new KeyImpl(keyr.getTModelKey()));
                c.setName(new InternationalStringImpl(keyr.getKeyName()));
                c.setValue(keyr.getKeyValue());
                asso.setKey(new KeyImpl(keyr.getTModelKey())); //TODO:Validate this
                asso.setAssociationType(c);
                col.add(asso);
            }


            return new BulkResponseImpl(col);
        } catch (RegistryException e)
        {
            throw new JAXRException(e);
        }
    }

    /**
     *  TODO - need to support the qualifiers
     *
     * @param findQualifiers
     * @param namePatterns
     * @return
     * @throws JAXRException
     */
    public ClassificationScheme findClassificationSchemeByName(Collection findQualifiers,
                                                               String namePatterns) throws JAXRException
    {
        ClassificationScheme scheme = null;

        if (namePatterns.indexOf("uddi-org:types") != -1) {

            scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());
            scheme.setName(new InternationalStringImpl(namePatterns));
            scheme.setKey(new KeyImpl(TModel.TYPES_TMODEL_KEY));
        }
        else if (namePatterns.indexOf("dnb-com:D-U-N-S") != -1) {

            scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());
            scheme.setName(new InternationalStringImpl(namePatterns));
            scheme.setKey(new KeyImpl(TModel.D_U_N_S_TMODEL_KEY));
        }
        else if (namePatterns.indexOf("uddi-org:iso-ch:3166:1999") != -1)
        {
            scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());
            scheme.setName(new InternationalStringImpl(namePatterns));
            scheme.setKey(new KeyImpl(TModel.ISO_CH_TMODEL_KEY));
        }
        else if (namePatterns.indexOf("uddi-org:iso-ch:3166-1999") != -1)
        {
            scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());
            scheme.setName(new InternationalStringImpl(namePatterns));
            scheme.setKey(new KeyImpl(TModel.ISO_CH_TMODEL_KEY));
        }
        else if (namePatterns.indexOf("iso-ch:3166:1999") != -1)
        {
            scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());
            scheme.setName(new InternationalStringImpl(namePatterns));
            scheme.setKey(new KeyImpl(TModel.ISO_CH_TMODEL_KEY));
        }
        else if (namePatterns.indexOf("iso-ch:3166-1999") != -1)
        {
            scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());
            scheme.setName(new InternationalStringImpl(namePatterns));
            scheme.setKey(new KeyImpl(TModel.ISO_CH_TMODEL_KEY));
        }
        else if (namePatterns.indexOf("unspsc-org:unspsc") != -1) {

            scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());
            scheme.setName(new InternationalStringImpl(namePatterns));
            scheme.setKey(new KeyImpl(TModel.UNSPSC_TMODEL_KEY));
        }
        else if (namePatterns.indexOf("ntis-gov:naics") != -1) {

            scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());
            scheme.setName(new InternationalStringImpl(namePatterns));
            scheme.setKey(new KeyImpl(TModel.NAICS_TMODEL_KEY));
        }
        else
        {   //TODO:Before going to the registry, check if it a predefined Enumeration

            /*
             * predefined Enumerations
             */

            if ("AssociationType".equals(namePatterns)) {
                scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());

                scheme.setName(new InternationalStringImpl(namePatterns));

                scheme.setKey(new KeyImpl(TModel.UNSPSC_TMODEL_KEY));

                addChildConcept((ClassificationSchemeImpl)scheme, "RelatedTo");
                addChildConcept((ClassificationSchemeImpl)scheme, "HasChild");
                addChildConcept((ClassificationSchemeImpl)scheme, "HasMember");
                addChildConcept((ClassificationSchemeImpl)scheme, "HasParent");
                addChildConcept((ClassificationSchemeImpl)scheme, "ExternallyLinks");
                addChildConcept((ClassificationSchemeImpl)scheme, "Contains");
                addChildConcept((ClassificationSchemeImpl)scheme, "EquivalentTo");
                addChildConcept((ClassificationSchemeImpl)scheme, "Extends");
                addChildConcept((ClassificationSchemeImpl)scheme, "Implements");
                addChildConcept((ClassificationSchemeImpl)scheme, "InstanceOf");
                addChildConcept((ClassificationSchemeImpl)scheme, "Supersedes");
                addChildConcept((ClassificationSchemeImpl)scheme, "Uses");
                addChildConcept((ClassificationSchemeImpl)scheme, "Replaces");
                addChildConcept((ClassificationSchemeImpl)scheme, "ResponsibleFor");
                addChildConcept((ClassificationSchemeImpl)scheme, "SubmitterOf");
            }
            else if ("ObjectType".equals(namePatterns)) {
                scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());

                scheme.setName(new InternationalStringImpl(namePatterns));

                scheme.setKey(new KeyImpl(TModel.UNSPSC_TMODEL_KEY));

                addChildConcept((ClassificationSchemeImpl)scheme, "CPP");
                addChildConcept((ClassificationSchemeImpl)scheme, "CPA");
                addChildConcept((ClassificationSchemeImpl)scheme, "Process");
                addChildConcept((ClassificationSchemeImpl)scheme, "WSDL");
                addChildConcept((ClassificationSchemeImpl)scheme, "Association");
                addChildConcept((ClassificationSchemeImpl)scheme, "AuditableEvent");
                addChildConcept((ClassificationSchemeImpl)scheme, "Classification");
                addChildConcept((ClassificationSchemeImpl)scheme, "Concept");
                addChildConcept((ClassificationSchemeImpl)scheme, "ExternalIdentifier");
                addChildConcept((ClassificationSchemeImpl)scheme, "ExternalLink");
                addChildConcept((ClassificationSchemeImpl)scheme, "ExtrinsicObject");
                addChildConcept((ClassificationSchemeImpl)scheme, "Organization");
                addChildConcept((ClassificationSchemeImpl)scheme, "Package");
                addChildConcept((ClassificationSchemeImpl)scheme, "Service");
                addChildConcept((ClassificationSchemeImpl)scheme, "ServiceBinding");
                addChildConcept((ClassificationSchemeImpl)scheme, "User");
            }
            else if ("PhoneType".equals(namePatterns)) {
                scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());

                scheme.setName(new InternationalStringImpl(namePatterns));

                scheme.setKey(new KeyImpl(TModel.UNSPSC_TMODEL_KEY));

                addChildConcept((ClassificationSchemeImpl)scheme, "OfficePhone");
                addChildConcept((ClassificationSchemeImpl)scheme, "HomePhone");
                addChildConcept((ClassificationSchemeImpl)scheme, "MobilePhone");
                addChildConcept((ClassificationSchemeImpl)scheme, "Beeper");
                addChildConcept((ClassificationSchemeImpl)scheme, "FAX");
            }
            else if ("URLType".equals(namePatterns)) {
                scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());

                scheme.setName(new InternationalStringImpl(namePatterns));

                scheme.setKey(new KeyImpl(TModel.UNSPSC_TMODEL_KEY));

                addChildConcept((ClassificationSchemeImpl)scheme, "HTTP");
                addChildConcept((ClassificationSchemeImpl)scheme, "HTTPS");
                addChildConcept((ClassificationSchemeImpl)scheme, "SMTP");
                addChildConcept((ClassificationSchemeImpl)scheme, "PHONE");
                addChildConcept((ClassificationSchemeImpl)scheme, "FAX");
                addChildConcept((ClassificationSchemeImpl)scheme, "OTHER");
            }
            else if ("PostalAddressAttributes".equals(namePatterns)) {
                scheme = new ClassificationSchemeImpl(registryService.getLifeCycleManagerImpl());

                scheme.setName(new InternationalStringImpl(namePatterns));

                scheme.setKey(new KeyImpl(TModel.UNSPSC_TMODEL_KEY));

                addChildConcept((ClassificationSchemeImpl)scheme, "StreetNumber");
                addChildConcept((ClassificationSchemeImpl)scheme, "Street");
                addChildConcept((ClassificationSchemeImpl)scheme, "City");
                addChildConcept((ClassificationSchemeImpl)scheme, "State");
                addChildConcept((ClassificationSchemeImpl)scheme, "PostalCode");
                addChildConcept((ClassificationSchemeImpl)scheme, "Country");
            }
            else {

                //Lets ask the uddi registry if it has the TModels
                IRegistry registry = registryService.getRegistry();
                FindQualifiers juddiFindQualifiers = mapFindQualifiers(findQualifiers);
                Vector nameVector = new Vector();
                nameVector.add(namePatterns);
                try
                {
                    //We are looking for one exact match, so getting upto 3 records is fine
                    TModelList list = registry.findTModel(namePatterns, null, null, juddiFindQualifiers, 3);
                    TModelInfos infos = null;
                    Vector tmvect = null;
                    if (list != null) infos = list.getTModelInfos();
                    if (infos != null) tmvect = infos.getTModelInfoVector();
                    if (tmvect != null)
                    {
                        if (tmvect.size() > 1)
                            throw new InvalidRequestException("Multiple matches found");

                        TModelInfo info = (TModelInfo) tmvect.elementAt(0);
                        scheme.setName(new InternationalStringImpl(info.getName().getValue()));
                        scheme.setKey(new KeyImpl(info.getTModelKey()));
                    }

                } catch (RegistryException e)
                {
                    log.error("Exception ::",e);
                    throw new JAXRException(e.getLocalizedMessage());
                }
            }
        }
        return scheme;
    }

    /**
     * Creates a new Concept, associates w/ parent scheme, and then
     * adds as decendent.
     * @param scheme
     * @param name
     * @throws JAXRException
     */
    private void addChildConcept(ClassificationSchemeImpl scheme, String name)
        throws JAXRException {
        Concept c = new ConceptImpl(registryService.getLifeCycleManagerImpl());

        c.setName(new InternationalStringImpl(name));
        c.setValue(name);
        ((ConceptImpl)c).setScheme((ClassificationSchemeImpl)scheme);

        scheme.addChildConcept(c);
    }

    public BulkResponse findClassificationSchemes(Collection findQualifiers,
                                                  Collection namePatterns,
                                                  Collection classifications,
                                                  Collection externalLinks) throws JAXRException
    {
        //TODO: Handle this better
        Collection col = new ArrayList();
        Iterator iter = namePatterns.iterator();
        String name = "";
        while(iter.hasNext())
        {
          name = (String)iter.next();
          break;
        }

        col.add(this.findClassificationSchemeByName(findQualifiers,name));
        return new BulkResponseImpl(col);

    }

    public Concept findConceptByPath(String path) throws JAXRException
    {
        //We will store the enumerations datastructure in the util package
        /**
         * I am not clear about how this association type enumerations
         * are to be implemented.
         */
        return EnumerationHelper.getConceptByPath(path);
    }

    public BulkResponse findConcepts(Collection findQualifiers,
                                     Collection namePatterns,
                                     Collection classifications,
                                     Collection externalIdentifiers,
                                     Collection externalLinks) throws JAXRException
    {
        Collection col = new ArrayList();

        //Lets ask the uddi registry if it has the TModels
        IRegistry registry = registryService.getRegistry();
        FindQualifiers juddiFindQualifiers = mapFindQualifiers(findQualifiers);
        Iterator iter = null;
        if (namePatterns != null) iter = namePatterns.iterator();
        while (iter.hasNext())
        {
            String namestr = (String) iter.next();
            try
            {
                TModelList list = registry.findTModel(namestr, null, null, juddiFindQualifiers, 10);
                TModelInfos infos = null;
                Vector tmvect = null;
                if (list != null) infos = list.getTModelInfos();
                if (infos != null) tmvect = infos.getTModelInfoVector();
                for (int i = 0; tmvect != null && i < tmvect.size(); i++)
                {
                    TModelInfo info = (TModelInfo) tmvect.elementAt(i);
                    col.add(ScoutUddiJaxrHelper.getConcept(info, this.registryService.getBusinessLifeCycleManager()));
                }

            } catch (RegistryException e)
            {
                log.error("RegistryException::",e);
                throw new JAXRException(e.getLocalizedMessage());
            }
        }

        return new BulkResponseImpl(col);
    }

    public BulkResponse findRegistryPackages(Collection findQualifiers,
                                             Collection namePatterns,
                                             Collection classifications,
                                             Collection externalLinks) throws JAXRException
    {
        throw new UnsupportedCapabilityException();
    }

    public BulkResponse findServiceBindings(Key serviceKey,
                                            Collection findQualifiers,
                                            Collection classifications,
                                            Collection specifications) throws JAXRException
    {
        BulkResponseImpl blkRes = new BulkResponseImpl();

        IRegistry iRegistry = registryService.getRegistry();
        FindQualifiers juddiFindQualifiers = mapFindQualifiers(findQualifiers);

        try
        {
 
            BindingDetail l = iRegistry.findBinding(serviceKey.getId(),null,null,juddiFindQualifiers,registryService.getMaxRows());

            /*
             * now convert  from jUDDI ServiceInfo objects to JAXR Services
             */
            if (l != null) {

                Vector bindvect= l.getBindingTemplateVector();
                Collection col = new ArrayList();

                for (int i=0; bindvect != null && i < bindvect.size(); i++) {
                    BindingTemplate si = (BindingTemplate) bindvect.elementAt(i);
                    ServiceBinding sb =  ScoutUddiJaxrHelper.getServiceBinding(si,
                            registryService.getBusinessLifeCycleManager());
                    col.add(sb);
                   //Fill the Service object by making a call to registry
                   Service s = (Service)getRegistryObject(serviceKey.getId(), LifeCycleManager.SERVICE);
                   ((ServiceBindingImpl)sb).setService(s);
                }

                blkRes.setCollection(col);
            }
        }
        catch (RegistryException e) {
            log.error("RegistryException::",e);
            throw new JAXRException(e.getLocalizedMessage());
        }

        return blkRes;
    }


    /**
     * Finds all Service objects that match all of the criteria specified by
     * the parameters of this call.  This is a logical AND operation between
     * all non-null parameters
     *
     * TODO - support findQualifiers, classifications and specifications
     *
     * @param orgKey
     * @param findQualifiers
     * @param namePatterns
     * @param classifications
     * @param specificationa
     * @return
     * @throws JAXRException
     */
    public BulkResponse findServices(Key orgKey, Collection findQualifiers,
                                     Collection namePatterns,
                                     Collection classifications,
                                     Collection specificationa) throws JAXRException
    {
        BulkResponseImpl blkRes = new BulkResponseImpl();

        IRegistry iRegistry = registryService.getRegistry();
        FindQualifiers juddiFindQualifiers = mapFindQualifiers(findQualifiers);
        Vector juddiNames = mapNamePatterns(namePatterns);

        try
        {
            /*
             * hit the registry.  The key is not required for UDDI2
             */

            String id = null;

            if (orgKey != null) {
                id = orgKey.getId();
            }

            ServiceList l = iRegistry.findService(id, juddiNames,
                    null, null, juddiFindQualifiers, registryService.getMaxRows());

            /*
             * now convert  from jUDDI ServiceInfo objects to JAXR Services
             */
            if (l != null) {

                ServiceInfos serviceInfos = l.getServiceInfos();

                Vector v = (serviceInfos != null ? serviceInfos.getServiceInfoVector() : null);

                Collection col = new ArrayList();

                for (int i=0; v != null && i < v.size(); i++) {
                    ServiceInfo si = (ServiceInfo) v.elementAt(i);
                    col.add(ScoutUddiJaxrHelper.getService(si,
                            registryService.getBusinessLifeCycleManager()));
                }

                blkRes.setCollection(col);
            }
        }
        catch (RegistryException e) {
            log.error("RegistryException::",e);
            throw new JAXRException(e.getLocalizedMessage());
        }

        return blkRes;
    }

    public RegistryObject getRegistryObject(String id) throws JAXRException
    {
        throw new UnsupportedCapabilityException();
    }

    public RegistryObject getRegistryObject(String id, String objectType) throws JAXRException
    {
        IRegistry registry = registryService.getRegistry();
        BusinessLifeCycleManager lcm = registryService.getBusinessLifeCycleManager();

        if (LifeCycleManager.CLASSIFICATION_SCHEME.equalsIgnoreCase(objectType)) {

            try {

                TModelDetail tmodeldetail = registry.getTModelDetail(id);
                if(tmodeldetail != null && tmodeldetail.getTModelVector().size() > 0)
                {
                	TModel tmodel = (TModel)tmodeldetail.getTModelVector().elementAt(0);
                	ClassificationSchemeImpl scheme = new ClassificationSchemeImpl(lcm);
                	boolean uddiBased = isUDDIBasedTModel(tmodel);
                    if(uddiBased)
                    {
                    	scheme.setName(new InternationalStringImpl(tmodel.getName()));
                    	Vector descVect = tmodel.getDescriptionVector();
                    	if(descVect != null && descVect.size() > 0)
                    	{
                    		Description d = (Description)descVect.elementAt(0);
                    		scheme.setDescription(new InternationalStringImpl(d.getValue()));
                    	}
                    	scheme.setExternalLinks(ScoutUddiJaxrHelper.getExternalLinks(tmodel.getOverviewDoc()
                    			                              ,lcm));
                    	scheme.setExternalIdentifiers(ScoutUddiJaxrHelper.getExternalIdentifiers(tmodel.getIdentifierBag()
                    			                              ,lcm));  
                    	scheme.setClassifications(ScoutUddiJaxrHelper.getClassifications(tmodel.getCategoryBag(),lcm));
                    }
                    else
                    {
                    	Concept c = ScoutUddiJaxrHelper.getConcept(tmodeldetail, lcm);

                        /*
                         * now turn into a concrete ClassificationScheme
                         */ 
                        scheme.setName(c.getName());
                        scheme.setDescription(c.getDescription());
                        scheme.setKey(c.getKey()); 
                    }

                    return scheme;
                } 
            }
            catch (RegistryException e) {
            	log.error("RegistryException::",e);
                throw new JAXRException(e.getLocalizedMessage());
            }
        }
        else if (LifeCycleManager.ORGANIZATION.equalsIgnoreCase(objectType)) {

            try
            {
                BusinessDetail orgdetail = registry.getBusinessDetail(id);
                return ScoutUddiJaxrHelper.getOrganization(orgdetail, lcm);
            }
            catch (RegistryException e) {
            	log.error("RegistryException::",e);
                throw new JAXRException(e.getLocalizedMessage());
            }
        }
        else if (LifeCycleManager.CONCEPT.equalsIgnoreCase(objectType)) {

            try
            {
                TModelDetail tmodeldetail = registry.getTModelDetail(id);
                return ScoutUddiJaxrHelper.getConcept(tmodeldetail, lcm);
            }
            catch (RegistryException e) {
            	log.error("RegistryException::",e);
                throw new JAXRException(e.getLocalizedMessage());
            }
        }
        else if (LifeCycleManager.SERVICE.equalsIgnoreCase(objectType)) {

            try {

               
                ServiceDetail sd = registry.getServiceDetail(id);

                if (sd != null) {

                    Vector v = sd.getBusinessServiceVector();

                    if (v.size() != 0) {
                        Service service = getServiceFromBusinessService(
                                (BusinessService) v.elementAt(0), lcm);

                        return service;
                    }
                }
            }
            catch (RegistryException e) {
            	log.error("RegistryException::",e);
            }
        }

        return null;
    }

    /**
     *  Helper routine to take a jUDDI business service and turn into a useful
     *  Service.  Needs to go back to the registry to get the organization to
     *  properly hydrate the Service
     *
     * @param bs  BusinessService object to turn in to a Service
     * @param lcm manager to use
     * @return new Service object
     * @throws JAXRException
     */
    protected Service getServiceFromBusinessService(BusinessService bs, LifeCycleManager lcm)
        throws JAXRException {

        Service service  = ScoutUddiJaxrHelper.getService(bs, lcm);

        /*
         * now get the Organization if we can
         */

        String busKey = bs.getBusinessKey();

        if (busKey != null) {
            Organization o = (Organization) getRegistryObject(busKey,
                    LifeCycleManager.ORGANIZATION);
            service.setProvidingOrganization(o);
        }

        return service;
    }

    /**
     * Gets the RegistryObjects owned by the caller. The objects
     * are returned as their concrete type (e.g. Organization, User etc.)
     *
     *  TODO - need to figure out what the set are.  This is just to get some
     *  basic functionality
     *
     * @return
     * @throws JAXRException
     */
    public BulkResponse getRegistryObjects() throws JAXRException
    {
        String types[] = {
            LifeCycleManager.ORGANIZATION,
            LifeCycleManager.SERVICE};

        Collection c = new ArrayList();

        for (int i = 0; i < types.length; i++) {
            try {
                BulkResponse bk = getRegistryObjects(types[i]);

                if (bk.getCollection() != null) {
                    c.addAll(bk.getCollection());
                }
            }
            catch(JAXRException e) {
                // ignore - just a problem with that type?
            }
        }

        return new BulkResponseImpl(c);
    }

    public BulkResponse getRegistryObjects(Collection objectKeys) throws JAXRException
    {
        throw new UnsupportedCapabilityException();
    }

    public BulkResponse getRegistryObjects(Collection objectKeys, String objectType) throws JAXRException
    {
        IRegistry registry = registryService.getRegistry();
        //Convert into a vector of strings
        Vector keys = new Vector();
        Iterator iter = objectKeys.iterator();
        while(iter.hasNext())
        {
            Key key = (Key)iter.next();
            keys.add(key.getId());
        }
        Collection col = new ArrayList();
        LifeCycleManager lcm = registryService.getLifeCycleManagerImpl();

        if (LifeCycleManager.CLASSIFICATION_SCHEME.equalsIgnoreCase(objectType))
        {
            try
            {
                TModelDetail tmodeldetail = registry.getTModelDetail(keys);
                Vector tmvect = tmodeldetail.getTModelVector();
                for (int i = 0; tmvect != null && i < tmvect.size(); i++)
                {
                    col.add(ScoutUddiJaxrHelper.getConcept((TModel) tmvect.elementAt(i), lcm));
                }

            } catch (RegistryException e)
            {
            	log.error("RegistryException::",e);
                throw new JAXRException(e.getLocalizedMessage());
            }
        }
        else if (LifeCycleManager.ORGANIZATION.equalsIgnoreCase(objectType))
        {
            //Get the Organization from the uddi registry
            try
            {
                BusinessDetail orgdetail = registry.getBusinessDetail(keys);
                Vector bizvect = orgdetail.getBusinessEntityVector();
                for (int i = 0; bizvect != null && i < bizvect.size(); i++)
                {
                    col.add(ScoutUddiJaxrHelper.getOrganization((BusinessEntity) bizvect.elementAt(i), lcm));
                }
            } catch (RegistryException e)
            {
                throw new JAXRException(e.getLocalizedMessage());
            }
        }
        else if (LifeCycleManager.CONCEPT.equalsIgnoreCase(objectType))
        {
            try {
                TModelDetail tmodeldetail = registry.getTModelDetail(keys);
                Vector tmvect = tmodeldetail.getTModelVector();
                for (int i = 0; tmvect != null && i < tmvect.size(); i++)
                {
                    col.add(ScoutUddiJaxrHelper.getConcept((TModel) tmvect.elementAt(i), lcm));
                }

            }
            catch (RegistryException e)
            {
            	log.error("RegistryException::",e);
                throw new JAXRException(e.getLocalizedMessage());
            }
        }
        else if (LifeCycleManager.SERVICE.equalsIgnoreCase(objectType)) {

            try {
                ServiceDetail serviceDetail = registry.getServiceDetail(keys);

                if (serviceDetail != null) {

                    Vector v = serviceDetail.getBusinessServiceVector();

                    for (int i=0; v != null && i < v.size(); i++) {

                        Service service = getServiceFromBusinessService(
                                (BusinessService) v.elementAt(i), lcm);

                        col.add(service);
                    }
                }
            }
            catch (RegistryException e) {
                throw new JAXRException(e);
            }
        }
        else {
            throw new JAXRException("Unsupported type " + objectType +
                    " for getRegistryObjects() in Apache Scout");
        }

        return new BulkResponseImpl(col);

    }

    public BulkResponse getRegistryObjects(String id) throws JAXRException
    {
        if (LifeCycleManager.ORGANIZATION.equalsIgnoreCase(id)) {
            List a = new ArrayList();
            a.add("%");

            BulkResponse br = findOrganizations(null, a, null, null, null, null);

            return br;
        }
        else if (LifeCycleManager.SERVICE.equalsIgnoreCase(id)) {
            List a = new ArrayList();
            a.add("%");

            BulkResponse br = this.findServices(null,null, a, null, null);

            return br;
        }
        else
        {
            throw new JAXRException("Unsupported type for getRegistryObjects() :" + id);
        }

    }

    private static final Map findQualifierMapping;

    static
    {
        findQualifierMapping = new HashMap();
        findQualifierMapping.put(FindQualifier.AND_ALL_KEYS, new org.apache.juddi.datatype.request.FindQualifier(org.apache.juddi.datatype.request.FindQualifier.AND_ALL_KEYS));
        findQualifierMapping.put(FindQualifier.CASE_SENSITIVE_MATCH, new org.apache.juddi.datatype.request.FindQualifier(org.apache.juddi.datatype.request.FindQualifier.CASE_SENSITIVE_MATCH));
        findQualifierMapping.put(FindQualifier.COMBINE_CLASSIFICATIONS, new org.apache.juddi.datatype.request.FindQualifier(org.apache.juddi.datatype.request.FindQualifier.COMBINE_CATEGORY_BAGS));
        findQualifierMapping.put(FindQualifier.EXACT_NAME_MATCH, new org.apache.juddi.datatype.request.FindQualifier(org.apache.juddi.datatype.request.FindQualifier.EXACT_NAME_MATCH));
        findQualifierMapping.put(FindQualifier.OR_ALL_KEYS, new org.apache.juddi.datatype.request.FindQualifier(org.apache.juddi.datatype.request.FindQualifier.OR_ALL_KEYS));
        findQualifierMapping.put(FindQualifier.OR_LIKE_KEYS, new org.apache.juddi.datatype.request.FindQualifier(org.apache.juddi.datatype.request.FindQualifier.OR_LIKE_KEYS));
        findQualifierMapping.put(FindQualifier.SERVICE_SUBSET, new org.apache.juddi.datatype.request.FindQualifier(org.apache.juddi.datatype.request.FindQualifier.SERVICE_SUBSET));
        findQualifierMapping.put(FindQualifier.SORT_BY_DATE_ASC, new org.apache.juddi.datatype.request.FindQualifier(org.apache.juddi.datatype.request.FindQualifier.SORT_BY_DATE_ASC));
        findQualifierMapping.put(FindQualifier.SORT_BY_DATE_DESC, new org.apache.juddi.datatype.request.FindQualifier(org.apache.juddi.datatype.request.FindQualifier.SORT_BY_DATE_DESC));
        findQualifierMapping.put(FindQualifier.SORT_BY_NAME_ASC, new org.apache.juddi.datatype.request.FindQualifier(org.apache.juddi.datatype.request.FindQualifier.SORT_BY_NAME_ASC));
        findQualifierMapping.put(FindQualifier.SORT_BY_NAME_DESC, new org.apache.juddi.datatype.request.FindQualifier(org.apache.juddi.datatype.request.FindQualifier.SORT_BY_NAME_DESC));
//        findQualifierMapping.put(FindQualifier.SOUNDEX, null);
    }

    static FindQualifiers mapFindQualifiers(Collection jaxrQualifiers) throws UnsupportedCapabilityException
    {
        if (jaxrQualifiers == null)
        {
            return null;
        }
        FindQualifiers result = new FindQualifiers(jaxrQualifiers.size());
        for (Iterator i = jaxrQualifiers.iterator(); i.hasNext();)
        {
            String jaxrQualifier = (String) i.next();
            org.apache.juddi.datatype.request.FindQualifier juddiQualifier =
                    (org.apache.juddi.datatype.request.FindQualifier) findQualifierMapping.get(jaxrQualifier);
            if (juddiQualifier == null)
            {
                throw new UnsupportedCapabilityException("jUDDI does not support FindQualifer: " + jaxrQualifier);
            }
            result.addFindQualifier(juddiQualifier);
        }
        return result;
    }

    static Vector mapNamePatterns(Collection namePatterns)
    throws JAXRException
    {
        if (namePatterns == null)
            return null;
        Vector result = new Vector(namePatterns.size());
        for (Iterator i = namePatterns.iterator(); i.hasNext();)
        {
        	Name name = null;
        	Object obj = i.next();
        	Locale locale = Locale.getDefault();
        	if(obj instanceof String)
        	{ 
        		name = new Name((String)obj, locale.getLanguage()); 
        	}
        	else
        	 if(obj instanceof LocalizedString)
        	 {
        		LocalizedString ls = (LocalizedString)obj;
        		name = new Name(ls.getValue(),ls.getLocale().getLanguage()); 
        	 } 
            result.add(name);
        }
        return result;
    }

   /**
     * Get the Auth Token from the registry
     *
     * @param connection
     * @param ireg
     * @return auth token
     * @throws JAXRException
     */
    private AuthToken getAuthToken(ConnectionImpl connection, IRegistry ireg)
            throws JAXRException {
        Set creds = connection.getCredentials();
        Iterator it = creds.iterator();
        String username = "", pwd = "";
        while (it.hasNext()) {
            PasswordAuthentication pass = (PasswordAuthentication) it.next();
            username = pass.getUserName();
            pwd = new String(pass.getPassword());
        }
        AuthToken token = null;
        try {
            token = ireg.getAuthToken(username, pwd);
        }
        catch (Exception e) {
            throw new JAXRException(e);
        }
        return token;
    }
    
    private boolean isUDDIBasedTModel(TModel tmodel)
    {
    	/**
    	 * JAXR 1.0 Specification: Section 6.4.3
    	 */
    	String uddi_org_types = "uuid:C1ACF26D-9672-4404-9D70-39B756E62AB4";
    	
    	boolean result = false;
    	CategoryBag cbag = tmodel.getCategoryBag();
    	if(cbag != null)
    	{
    		Vector vect  = cbag.getKeyedReferenceVector();
    		if(vect != null)
    		{
    			Iterator iter = vect.iterator();
    			while(iter.hasNext())
    			{
    				KeyedReference kr = (KeyedReference)iter.next();
    				if(kr != null)
    	    		{
    	    			String key = kr.getTModelKey();
    	    			String tval = kr.getKeyValue();
    	    			result = key.equals(uddi_org_types); 
    	    			//Check the taxonomy values
    	    			result = checkTaxonomyValue(tval);
    	    			if(result)
    	    				break;
    	    		}
    			}
    		} 
    	}
    	return result;
    }
    
    private boolean checkTaxonomyValue(String tval)
    {
    	return (tval.equalsIgnoreCase("Identifier") || 
				tval.equalsIgnoreCase("Namespace") || 
				tval.equalsIgnoreCase("Categorization") ||
				tval.equalsIgnoreCase("PostalAddress"));
    }
}
