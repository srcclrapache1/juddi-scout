/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

//
// This source code implements specifications defined by the Java
// Community Process. In order to remain compliant with the specification
// DO NOT add / change / or delete method signatures!
//
package javax.xml.registry.infomodel;

import javax.xml.registry.JAXRException;
import java.util.Collection;

/**
 * @version $Revision$ $Date$
 */
public interface ServiceBinding extends RegistryObject, URIValidator {
    void addSpecificationLink(SpecificationLink specificationLink) throws JAXRException;

    void addSpecificationLinks(Collection specificationLinks) throws JAXRException;

    String getAccessURI() throws JAXRException;

    Service getService() throws JAXRException;

    Collection getSpecificationLinks() throws JAXRException;

    ServiceBinding getTargetBinding() throws JAXRException;

    void removeSpecificationLink(SpecificationLink specificationLink) throws JAXRException;

    void removeSpecificationLinks(Collection specificationLinks) throws JAXRException;

    void setAccessURI(String uri) throws JAXRException;

    void setTargetBinding(ServiceBinding binding) throws JAXRException;
}