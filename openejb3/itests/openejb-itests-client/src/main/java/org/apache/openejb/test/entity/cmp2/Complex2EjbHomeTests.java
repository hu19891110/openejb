/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.test.entity.cmp2;

import org.apache.openejb.test.entity.cmp.ComplexCmpHome;

import javax.ejb.EJBMetaData;

/**
 * [3] Should be run as the third test suite of the BasicCmpTestClients
 */
public class Complex2EjbHomeTests extends ComplexCmp2TestClient {

    public Complex2EjbHomeTests() {
        super("EJBHome.");
    }

    protected void setUp() throws Exception {
        super.setUp();
        Object obj = initialContext.lookup("client/tests/entity/cmp2/ComplexCmpHome");
        ejbHome = (ComplexCmpHome) javax.rmi.PortableRemoteObject.narrow(obj, ComplexCmpHome.class);
        ejbObject = ejbHome.createObject("Second Bean");
        ejbPrimaryKey = ejbObject.getPrimaryKey();
    }

    //===============================
    // Test ejb home methods
    //
    public void test01_getEJBMetaData() {
        try {
            EJBMetaData ejbMetaData = ejbHome.getEJBMetaData();
            assertNotNull("The EJBMetaData is null", ejbMetaData);
        } catch (Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test02_getHomeHandle() {
        try {
            ejbHomeHandle = ejbHome.getHomeHandle();
            assertNotNull("The HomeHandle is null", ejbHomeHandle);
        } catch (Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test03_remove() {
        try {
            ejbHome.remove(ejbPrimaryKey);
            try {
                ejbObject.businessMethod("Should throw an exception");
                assertTrue("Calling business method after removing the EJBObject does not throw an exception", false);
            } catch (Exception e) {
                assertTrue(true);
            }
        } catch (Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void Xtest04_ejbHomeMethod() {
        try {
            assertEquals(8+9, ejbHome.sum(8, 9));
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }
    //
    // Test ejb home methods
    //===============================
}
