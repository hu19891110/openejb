//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.1-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2006.07.08 at 06:01:06 PM PDT 
//


package org.openejb.jee2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 * 	
 * 
 * 	  The query-method specifies the method for a finder or select
 * 	  query.
 * 
 * 	  The method-name element specifies the name of a finder or select
 * 	  method in the entity bean's implementation class.
 * 
 * 	  Each method-param must be defined for a query-method using the
 * 	  method-params element.
 * 
 * 	  It is used by the query-method element.
 * 
 * 	  Example:
 * 
 * 	  <query>
 * 	      <description>Method finds large orders</description>
 * 	      <query-method>
 * 		  <method-name>findLargeOrders</method-name>
 * 		  <method-params></method-params>
 * 	      </query-method>
 * 	      <ejb-ql>
 * 		SELECT OBJECT(o) FROM Order o
 * 		  WHERE o.amount &gt; 1000
 * 	      </ejb-ql>
 * 	  </query>
 * 
 * 	  
 *       
 * 
 * <p>Java class for query-methodType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="query-methodType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="method-name" type="{http://java.sun.com/xml/ns/javaee}method-nameType"/>
 *         &lt;element name="method-params" type="{http://java.sun.com/xml/ns/javaee}method-paramsType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "query-methodType", propOrder = {
    "methodName",
    "methodParams"
})
public class QueryMethodType {

    @XmlElement(name = "method-name", required = true)
    protected String methodName;
    @XmlElement(name = "method-params", required = true)
    protected MethodParamsType methodParams;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;

    /**
     * Gets the value of the methodName property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     *  contains a name of an enterprise
     * 	bean method or the asterisk (*) character. The asterisk is
     * 	used when the element denotes all the methods of an
     * 	enterprise bean's client view interfaces.
     */
    public void setMethodName(String value) {
        this.methodName = value;
    }

    /**
     * Gets the value of the methodParams property.
     * 
     * @return
     *     possible object is
     *     {@link MethodParamsType }
     *     
     */
    public MethodParamsType getMethodParams() {
        return methodParams;
    }

    /**
     * Sets the value of the methodParams property.
     * 
     * @param value
     *     allowed object is
     *     {@link MethodParamsType }
     *     
     */
    public void setMethodParams(MethodParamsType value) {
        this.methodParams = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

}
