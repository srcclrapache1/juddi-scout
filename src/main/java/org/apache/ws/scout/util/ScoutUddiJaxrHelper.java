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
package org.apache.ws.scout.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.xml.registry.JAXRException;
import javax.xml.registry.LifeCycleManager;
import javax.xml.registry.infomodel.Association;
import javax.xml.registry.infomodel.Classification;
import javax.xml.registry.infomodel.ClassificationScheme;
import javax.xml.registry.infomodel.Concept;
import javax.xml.registry.infomodel.EmailAddress;
import javax.xml.registry.infomodel.ExternalIdentifier;
import javax.xml.registry.infomodel.ExternalLink;
import javax.xml.registry.infomodel.InternationalString;
import javax.xml.registry.infomodel.Organization;
import javax.xml.registry.infomodel.PostalAddress;
import javax.xml.registry.infomodel.RegistryObject;
import javax.xml.registry.infomodel.Service;
import javax.xml.registry.infomodel.ServiceBinding;
import javax.xml.registry.infomodel.SpecificationLink;
import javax.xml.registry.infomodel.TelephoneNumber;
import javax.xml.registry.infomodel.User;

import org.apache.ws.scout.model.uddi.v2.AccessPoint;
import org.apache.ws.scout.model.uddi.v2.Address;
import org.apache.ws.scout.model.uddi.v2.AddressLine;
import org.apache.ws.scout.model.uddi.v2.BindingTemplate;
import org.apache.ws.scout.model.uddi.v2.BindingTemplates;
import org.apache.ws.scout.model.uddi.v2.BusinessDetail;
import org.apache.ws.scout.model.uddi.v2.BusinessEntity;
import org.apache.ws.scout.model.uddi.v2.BusinessService;
import org.apache.ws.scout.model.uddi.v2.BusinessServices;
import org.apache.ws.scout.model.uddi.v2.CategoryBag;
import org.apache.ws.scout.model.uddi.v2.Contact;
import org.apache.ws.scout.model.uddi.v2.Contacts;
import org.apache.ws.scout.model.uddi.v2.Description;
import org.apache.ws.scout.model.uddi.v2.DiscoveryURL;
import org.apache.ws.scout.model.uddi.v2.DiscoveryURLs;
import org.apache.ws.scout.model.uddi.v2.Email;
import org.apache.ws.scout.model.uddi.v2.HostingRedirector;
import org.apache.ws.scout.model.uddi.v2.IdentifierBag;
import org.apache.ws.scout.model.uddi.v2.InstanceDetails;
import org.apache.ws.scout.model.uddi.v2.KeyedReference;
import org.apache.ws.scout.model.uddi.v2.Name;
import org.apache.ws.scout.model.uddi.v2.OverviewDoc;
import org.apache.ws.scout.model.uddi.v2.Phone;
import org.apache.ws.scout.model.uddi.v2.ServiceInfo;
import org.apache.ws.scout.model.uddi.v2.TModel;
import org.apache.ws.scout.model.uddi.v2.TModelDetail;
import org.apache.ws.scout.model.uddi.v2.TModelInfo;
import org.apache.ws.scout.model.uddi.v2.TModelInstanceDetails;
import org.apache.ws.scout.model.uddi.v2.TModelInstanceInfo;
import org.apache.ws.scout.registry.infomodel.AssociationImpl;
import org.apache.ws.scout.registry.infomodel.ClassificationImpl;
import org.apache.ws.scout.registry.infomodel.ClassificationSchemeImpl;
import org.apache.ws.scout.registry.infomodel.ConceptImpl;
import org.apache.ws.scout.registry.infomodel.EmailAddressImpl;
import org.apache.ws.scout.registry.infomodel.ExternalIdentifierImpl;
import org.apache.ws.scout.registry.infomodel.ExternalLinkImpl;
import org.apache.ws.scout.registry.infomodel.InternationalStringImpl;
import org.apache.ws.scout.registry.infomodel.KeyImpl;
import org.apache.ws.scout.registry.infomodel.OrganizationImpl;
import org.apache.ws.scout.registry.infomodel.PersonNameImpl;
import org.apache.ws.scout.registry.infomodel.PostalAddressImpl;
import org.apache.ws.scout.registry.infomodel.ServiceBindingImpl;
import org.apache.ws.scout.registry.infomodel.ServiceImpl;
import org.apache.ws.scout.registry.infomodel.SpecificationLinkImpl;
import org.apache.ws.scout.registry.infomodel.TelephoneNumberImpl;
import org.apache.ws.scout.registry.infomodel.UserImpl;

/**
 * Helper class that does UDDI->Jaxr Mapping
 *
 * @author <a href="mailto:anil@apache.org">Anil Saldhana</a>
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 */
public class ScoutUddiJaxrHelper
{
   public static Association getAssociation(Collection orgs,
                                            LifeCycleManager lcm)
           throws JAXRException
   {
      Association asso = new AssociationImpl(lcm);
      Object[] arr = orgs.toArray();
      asso.setSourceObject((RegistryObject)arr[0]);
      asso.setTargetObject((RegistryObject)arr[1]);
      return asso;
   }

   public static Organization getOrganization(BusinessEntity businessEntity,
                                              LifeCycleManager lifeCycleManager)
           throws JAXRException
   {
      List<Name> namesList = businessEntity.getName();
      Name n = null;
      if (namesList.size()>0) n = namesList.get(0);
      
      List<Description> descriptionList = businessEntity.getDescription();
      Description desc =null;
      if (descriptionList.size()>0) desc = descriptionList.get(0);

      Organization org = new OrganizationImpl(lifeCycleManager);
      if(n != null ) {
          org.setName(getIString(n.getLang(), n.getValue(), lifeCycleManager));
      }
      if( desc != null) {
          org.setDescription(getIString(desc.getLang(), desc.getValue(), lifeCycleManager));
      }
      org.setKey(lifeCycleManager.createKey(businessEntity.getBusinessKey()));

      //Set Services also
      BusinessServices services = businessEntity.getBusinessServices();
      if(services != null)
      {
    	  List<BusinessService> bizServiceList = services.getBusinessService();
    	  for (BusinessService businessService : bizServiceList) {
    		  org.addService(getService(businessService, lifeCycleManager));
    	  }
      }

      /*
       *  Users
       *
       *  we need to take the first contact and designate as the
       *  'primary contact'.  Currently, the OrganizationImpl
       *  class does that automatically as a safety in case
       *  user forgets to set - lets be explicit here as to not
       *  depend on that behavior
       */

      Contacts contacts = businessEntity.getContacts();
      if(contacts != null)
      {
    	  List<Contact> contactList = contacts.getContact();
    	  if (contactList!=null) {
    		  boolean isFirst=true;
    		  for (Contact contact : contactList) {
    			  User user = new UserImpl(null);
    			  String pname = contact.getPersonName();
    			  user.setPersonName(new PersonNameImpl(pname));
    			  if (isFirst) {
    				  isFirst=false;
    				  org.setPrimaryContact(user);
    			  } else {
    				  org.addUser(user);
    			  }
			}
    	  }
      }
       
      //External Links
      DiscoveryURLs durls = businessEntity.getDiscoveryURLs();
      if (durls != null)
      {
    	  List<DiscoveryURL> discoveryURL_List = durls.getDiscoveryURL();
    	  for (DiscoveryURL discoveryURL : discoveryURL_List) {
    		  ExternalLink link = new ExternalLinkImpl(lifeCycleManager);
    		  link.setExternalURI(discoveryURL.getValue());
    		  org.addExternalLink(link);
    	  }
      }

      org.addExternalIdentifiers(getExternalIdentifiers(businessEntity.getIdentifierBag(), lifeCycleManager));
      org.addClassifications(getClassifications(businessEntity.getCategoryBag(), lifeCycleManager));
      
      return org;
   }


   public static Organization getOrganization(BusinessDetail bizdetail,
                                              LifeCycleManager lifeCycleManager)
           throws JAXRException
   {
	  List<BusinessEntity> bizEntityList = bizdetail.getBusinessEntity();
      BusinessEntity[] bz = new BusinessEntity[bizEntityList.size()];
      bizEntityList.toArray(bz);

      BusinessEntity entity = bz[0];
      Name n = null;
      if (entity.getName().size()>0) n = entity.getName().get(0);
      
      List<Description> descriptionList = entity.getDescription();
      Description desc =null;
      if (descriptionList.size()>0) desc = descriptionList.get(0);

      Organization org = new OrganizationImpl(lifeCycleManager);
      if( n != null ) {
          org.setName(getIString(n.getLang(), n.getValue(), lifeCycleManager));
      }
      if( desc != null ) {
          org.setDescription(getIString(desc.getLang(), desc.getValue(), lifeCycleManager));
      }
      org.setKey(lifeCycleManager.createKey(entity.getBusinessKey()));

      //Set Services also
      BusinessServices services = entity.getBusinessServices();
      List<BusinessService> bizServiceList = services.getBusinessService();
      for (BusinessService businessService : bizServiceList) {
    	  org.addService(getService(businessService, lifeCycleManager));
	  }

      /*
       *  Users
       *
       *  we need to take the first contact and designate as the
       *  'primary contact'.  Currently, the OrganizationImpl
       *  class does that automatically as a safety in case
       *  user forgets to set - lets be explicit here as to not
       *  depend on that behavior
       */
      Contacts contacts = entity.getContacts();
      List<Contact> contactList = contacts.getContact();
      for (Contact contact : contactList) {
         User user = new UserImpl(null);
         String pname = contact.getPersonName();
         user.setType(contact.getUseType());
         user.setPersonName(new PersonNameImpl(pname));
         
         List<Email> emailList = contact.getEmail();
         ArrayList<EmailAddress> tempEmails = new ArrayList<EmailAddress>();
         for (Email email : emailList) {
        	 tempEmails.add(new EmailAddressImpl(email.getValue(), null));
		 }
         user.setEmailAddresses(tempEmails);
         
         List<Address> addressList = contact.getAddress();
         ArrayList<PostalAddress> tempAddresses = new ArrayList<PostalAddress>();
         for (Address address : addressList) {
        	 ArrayList<AddressLine> addressLineList = new ArrayList<AddressLine>(address.getAddressLine());
        	 AddressLine[] alines = new AddressLine[addressLineList.size()];
        	 addressLineList.toArray(alines);
        	 
        	 PostalAddress pa = getPostalAddress(alines);
        	 tempAddresses.add(pa);
		 }
         user.setPostalAddresses(tempAddresses);
         
         List<Phone> phoneList = contact.getPhone();
         ArrayList<TelephoneNumber> tempPhones = new ArrayList<TelephoneNumber>();
         boolean isFirst=true;
         for (Phone phone : phoneList) {
        	 TelephoneNumberImpl tni = new TelephoneNumberImpl();
        	 tni.setType(phone.getUseType());
        	 tni.setNumber(phone.getValue());
        	 tempPhones.add(tni);
	         user.setTelephoneNumbers(tempPhones);
	         if (isFirst) {
	        	 isFirst=false;
	            org.setPrimaryContact(user);
	         } else {
	            org.addUser(user);
	         }
         }
      }

      //External Links
      DiscoveryURLs durls = entity.getDiscoveryURLs();
      if (durls != null)
      {
    	 List<DiscoveryURL> discoveryURL_List = durls.getDiscoveryURL();
         for (DiscoveryURL discoveryURL : discoveryURL_List) {
            ExternalLink link = new ExternalLinkImpl(lifeCycleManager);
            link.setExternalURI(discoveryURL.getValue());
            org.addExternalLink(link);
         }
      }

      org.addExternalIdentifiers(getExternalIdentifiers(entity.getIdentifierBag(), lifeCycleManager));
      org.addClassifications(getClassifications(entity.getCategoryBag(), lifeCycleManager));
      
      return org;
   }

   private static PostalAddress getPostalAddress(AddressLine[] addressLineArr) throws JAXRException {
	   PostalAddress pa = new PostalAddressImpl();
	   HashMap<String, String> hm = new HashMap<String, String>();
	   for (int y = 0; y < addressLineArr.length; y++) {
		   hm.put(addressLineArr[y].getKeyName(), addressLineArr[y].getKeyValue());
	   }        	 
	   
	   if (hm.containsKey("STREET_NUMBER")) {
		   pa.setStreetNumber(hm.get("STREET_NUMBER"));
	   }

	   if (hm.containsKey("STREET")) {
		   pa.setStreet(hm.get("STREET"));
	   }
	   
	   if (hm.containsKey("CITY")) {
		   pa.setCity(hm.get("CITY"));
	   }
	   
	   if (hm.containsKey("COUNTRY")) {
		   pa.setCountry(hm.get("COUNTRY"));
	   }
	
	   if (hm.containsKey("POSTALCODE")) {
		   pa.setPostalCode(hm.get("POSTALCODE"));
	   }
	   
	   if (hm.containsKey("STATE")) {
		   pa.setStateOrProvince(hm.get("STATE"));
	   }
	   
	   return pa;
   }
   
   private static InternationalString getIString(String lang, String str, LifeCycleManager lifeCycleManager)
       throws JAXRException
   {
	   if (str!=null) {
		   return lifeCycleManager.createInternationalString(getLocale(lang), str);
	   } else {
		   return null;
	   }
   }
   
   public static InternationalString getIString(String str, LifeCycleManager lifeCycleManager)
           throws JAXRException
   {
      return lifeCycleManager.createInternationalString(str);
   }

   public static Service getService(BusinessService businessService, LifeCycleManager lifeCycleManager)
           throws JAXRException
   {
      Service serve = new ServiceImpl(lifeCycleManager);

      String keystr = businessService.getServiceKey();

      if (keystr != null)
      {
         serve.setKey(lifeCycleManager.createKey(keystr));
      }

      Name n = null;
      if (businessService.getName().size()>0) n = businessService.getName().get(0);

      if (n != null) {
    	  String name = n.getValue();
    	  serve.setName(lifeCycleManager.createInternationalString(getLocale(n.getLang()), name));
      }

      Description desc =null;
      if (businessService.getDescription().size()>0) desc = businessService.getDescription().get(0);
      if (desc != null ) {
          serve.setDescription(lifeCycleManager.createInternationalString(getLocale(desc.getLang()), desc.getValue()));
      }
      
      //Populate the ServiceBindings for this Service
      BindingTemplates bts = businessService.getBindingTemplates();
      List<BindingTemplate> bindingTemplateList = bts.getBindingTemplate();
      for (BindingTemplate bindingTemplate : bindingTemplateList) {
    	  serve.addServiceBinding(getServiceBinding(bindingTemplate, lifeCycleManager));
	  }
      serve.addClassifications(getClassifications(businessService.getCategoryBag(), lifeCycleManager));
      
      return serve;
   }

   public static Service getService(ServiceInfo serviceInfo, LifeCycleManager lifeCycleManager)
           throws JAXRException
   {
      Service service = new ServiceImpl(lifeCycleManager);

      String keystr = serviceInfo.getServiceKey();

      if (keystr != null)
      {
         service.setKey(lifeCycleManager.createKey(keystr));
      }

      Name n = null;
      if (serviceInfo.getName().size()>0) n = serviceInfo.getName().get(0);
      if (n != null) {
    	  String name = n.getValue();
    	  service.setName(lifeCycleManager.createInternationalString(getLocale(n.getLang()), name));
      }
      return service;
   }

   public static ServiceBinding getServiceBinding(BindingTemplate businessTemplate, LifeCycleManager lifeCycleManager)
           throws JAXRException
   {
      ServiceBinding serviceBinding = new ServiceBindingImpl(lifeCycleManager);

      String keystr = businessTemplate.getServiceKey();
      if (keystr != null)
      {
         Service svc = new ServiceImpl(lifeCycleManager);
         svc.setKey(lifeCycleManager.createKey(keystr));
         ((ServiceBindingImpl)serviceBinding).setService(svc);
      }
      String bindingKey = businessTemplate.getBindingKey();
      if(bindingKey != null) serviceBinding.setKey(new KeyImpl(bindingKey));
     
      //Access URI
      AccessPoint access = businessTemplate.getAccessPoint();
      if (access != null) serviceBinding.setAccessURI(access.getValue());

      //Description
      Description desc = null;
      if (businessTemplate.getDescription().size()>0) desc = businessTemplate.getDescription().get(0);
      if (desc!=null) {
         serviceBinding.setDescription(new InternationalStringImpl(desc.getValue()));
      }
      /**Section D.10 of JAXR 1.0 Specification */
      
      TModelInstanceDetails details = businessTemplate.getTModelInstanceDetails();
      List<TModelInstanceInfo> tmodelInstanceInfoList = details.getTModelInstanceInfo();
      TModelInstanceInfo[] tmodelInstanceInfoArray = new TModelInstanceInfo[tmodelInstanceInfoList.size()];
      tmodelInstanceInfoList.toArray(tmodelInstanceInfoArray);
      
      for (int i = 0; tmodelInstanceInfoArray != null && i < tmodelInstanceInfoArray.length; i++)
      {
         TModelInstanceInfo info = (TModelInstanceInfo)tmodelInstanceInfoArray[i];
         if (info!=null && info.getInstanceDetails()!=null) {
	         InstanceDetails idetails = info.getInstanceDetails();
	         Collection<ExternalLink> elinks = getExternalLinks(idetails.getOverviewDoc(),lifeCycleManager);
	         SpecificationLink slink = new SpecificationLinkImpl(lifeCycleManager);
	         slink.addExternalLinks(elinks);
	         serviceBinding.addSpecificationLink(slink); 
	         
	         ConceptImpl c = new ConceptImpl(lifeCycleManager);
	         c.setExternalLinks(elinks);
	         c.setKey(lifeCycleManager.createKey(info.getTModelKey())); 
	         c.setName(lifeCycleManager.createInternationalString(idetails.getInstanceParms()));
	         c.setValue(idetails.getInstanceParms());
	         
	         slink.setSpecificationObject(c);
         }
      }
      
      HostingRedirector hr = businessTemplate.getHostingRedirector();
      if(hr != null)
      {
         ServiceBinding sb = lifeCycleManager.createServiceBinding();
         sb.setKey(new KeyImpl(hr.getBindingKey()));
         serviceBinding.setTargetBinding(sb);
      }

      return serviceBinding;
   }

   public static Concept getConcept(TModelDetail tModelDetail, LifeCycleManager lifeCycleManager)
           throws JAXRException
   {
      Concept concept = new ConceptImpl(lifeCycleManager);
      List<TModel> tmodelList = tModelDetail.getTModel();
      for (TModel tmodel : tmodelList) {
    	  concept.setKey(lifeCycleManager.createKey(tmodel.getTModelKey()));
    	  concept.setName(lifeCycleManager.createInternationalString(getLocale(tmodel.getName().getLang()),
    			  tmodel.getName().getValue()));

    	  Description desc = getDescription(tmodel);
    	  if( desc != null ) {
    	      concept.setDescription(lifeCycleManager.createInternationalString(getLocale(desc.getLang()), 
    	    		  desc.getValue()));
    	  }

          concept.addExternalIdentifiers(getExternalIdentifiers(tmodel.getIdentifierBag(), lifeCycleManager));
          concept.addClassifications(getClassifications(tmodel.getCategoryBag(), lifeCycleManager));
      }
      return concept;
   }

   public static Concept getConcept(TModel tmodel, LifeCycleManager lifeCycleManager)
           throws JAXRException
   {
      Concept concept = new ConceptImpl(lifeCycleManager);
      concept.setKey(lifeCycleManager.createKey(tmodel.getTModelKey()));
      concept.setName(lifeCycleManager.createInternationalString(getLocale(tmodel.getName().getLang()),
    		  tmodel.getName().getValue()));

      Description desc = getDescription(tmodel);
      if (desc != null) {
          concept.setDescription(lifeCycleManager.createInternationalString(getLocale(desc.getLang()), 
        		  desc.getValue()));
      }
      
      concept.addExternalIdentifiers(getExternalIdentifiers(tmodel.getIdentifierBag(), lifeCycleManager));
      concept.addClassifications(getClassifications(tmodel.getCategoryBag(), lifeCycleManager));

      return concept;
   }

   public static Concept getConcept(TModelInfo tModelInfo, LifeCycleManager lifeCycleManager)
           throws JAXRException
   {
      Concept concept = new ConceptImpl(lifeCycleManager);
      concept.setKey(lifeCycleManager.createKey(tModelInfo.getTModelKey()));
      concept.setName(lifeCycleManager.createInternationalString(getLocale(tModelInfo.getName().getLang()), 
    		  tModelInfo.getName().getValue()));

      return concept;
   }

   private static Description getDescription( TModel tmodel )
   {
	   Description desc = null;
	   if (tmodel.getDescription().size()>0) desc=tmodel.getDescription().get(0);
       return desc;
   }

   /**
    * Classifications - going to assume all are external since UDDI does not use "Concepts".
    * 
    * @param categoryBag
    * @param destinationObj
    * @param lifeCycleManager
    * @throws JAXRException
    */
   public static Collection getClassifications(CategoryBag categoryBag, LifeCycleManager lifeCycleManager) 
   throws JAXRException {
	   Collection<Classification> classifications = null;
	   if (categoryBag != null) {
		    classifications = new ArrayList<Classification>();
		    List<KeyedReference> keyedReferenceList = categoryBag.getKeyedReference();
		    for (KeyedReference keyedReference : keyedReferenceList) {
				Classification classification = new ClassificationImpl(lifeCycleManager);
				classification.setValue(keyedReference.getKeyValue());
				classification.setName(new InternationalStringImpl(keyedReference.getKeyName()));
				String tmodelKey = keyedReference.getTModelKey();
				if (tmodelKey != null) {
					ClassificationScheme scheme = new ClassificationSchemeImpl(lifeCycleManager);
					scheme.setKey(new KeyImpl(tmodelKey));
					classification.setClassificationScheme(scheme);
				}
				classifications.add(classification);
			}
		}
	    return classifications;
	}
   
   public static Collection<ExternalLink> getExternalLinks(OverviewDoc overviewDoc , LifeCycleManager lifeCycleManager)
   throws JAXRException
   {
       ArrayList<ExternalLink> alist = new ArrayList<ExternalLink>(1);
       if(overviewDoc != null)
       {
    	   String descStr = "";
    	   Description desc = null;
    	   if (overviewDoc.getDescription().size()>0) desc = overviewDoc.getDescription().get(0);
           if (desc !=null) descStr = desc.getValue();
           alist.add(lifeCycleManager.createExternalLink(overviewDoc.getOverviewURL(),descStr));
       }
       
       return alist;
   }
   
   /**
    * External Identifiers
    * 
    * @param identifierBag
    * @param destinationObj
    * @param lifeCycleManager
    * @throws JAXRException
    */
   public static Collection<ExternalIdentifier> getExternalIdentifiers(IdentifierBag identifierBag, LifeCycleManager lifeCycleManager) 
   throws JAXRException {
	  Collection<ExternalIdentifier> extidentifiers = null;
      if (identifierBag != null) {
    	  extidentifiers = new ArrayList<ExternalIdentifier>();
    	  
    	  List<KeyedReference> keyedReferenceList = identifierBag.getKeyedReference();
          for (KeyedReference keyedReference : keyedReferenceList) {
             ExternalIdentifier extId = new ExternalIdentifierImpl(lifeCycleManager);
             extId.setValue(keyedReference.getKeyValue());
             extId.setName(new InternationalStringImpl(keyedReference.getKeyName()));
             
             String tmodelKey = keyedReference.getTModelKey();
             if (tmodelKey != null) {
            	 ClassificationScheme scheme = new ClassificationSchemeImpl(lifeCycleManager);
            	 scheme.setKey(new KeyImpl(tmodelKey));
            	 extId.setIdentificationScheme(scheme);
             }
             extidentifiers.add(extId);
          }
      }
      return extidentifiers;
   }
   
   private static Locale getLocale(String lang) {
       if (lang == null || lang.trim().length() == 0) {
           return Locale.getDefault();
       } else if (lang.equalsIgnoreCase(Locale.getDefault().getLanguage())) {
           return Locale.getDefault();
       } else {
           return new Locale(lang);
       } 
   }
   
}