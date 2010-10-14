//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-661 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.07.19 at 09:49:41 PM CDT 
//


package org.apache.ws.scout.model.uddi.v2;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for direction.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="direction">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *     &lt;enumeration value="fromKey"/>
 *     &lt;enumeration value="toKey"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "direction")
@XmlEnum
public enum Direction {

    @XmlEnumValue("fromKey")
    FROM_KEY("fromKey"),
    @XmlEnumValue("toKey")
    TO_KEY("toKey");
    private final String value;

    Direction(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Direction fromValue(String v) {
        for (Direction c: Direction.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}