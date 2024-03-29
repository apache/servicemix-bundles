/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id$
 */
package org.apache.xml.serializer;

import java.io.IOException;

import org.w3c.dom.Node;

/**
 * Interface for a DOM serializer implementation.
 * <p>
 * The DOMSerializer is a facet of a serializer and is obtained from the
 * asDOMSerializer() method of the ({@link Serializer}) interface. 
 * A serializer may or may not support a DOM serializer, if it does not then the 
 * return value from asDOMSerializer() is null.
 * <p>
 * Example:
 * <pre>
 * // Create a document to be serialized
 * org.w3c.dom.Document doc = ...;
 *
 * // Create a Serializer that will be used
 * // to serialize the document  
 * org.apache.xml.serializer.Serializer ser = ...;
 *
 * // Set the Writer to write output to, and 
 * // serialize the DOM using that Serializer
 * java.io.StringWriter sw = new java.io.StringWriter();
 * ser.setWriter(sw);
 * DOMSerialzier dser = ser.asDOMSerializer();
 * dser.serialize(doc);
 *
 * // Write out the serialized XML in the String.
 * System.out.println(sw.toString());
 * </pre>
 *
 * @see OutputPropertiesFactory
 * @see SerializerFactory
 * @see Serializer
 *
 * @xsl.usage general
 *
 */
public interface DOMSerializer
{
    /**
     * Serializes the DOM node. Throws an exception only if an I/O
     * exception occured while serializing.
     *
     * This interface is a public API.
     *
     * @param node the DOM node to serialize
     * @throws IOException if an I/O exception occured while serializing
     */
    public void serialize(Node node) throws IOException;
}