////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2015 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s): Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////
package com.denimgroup.threadfix.framework.impl.jsp;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import javax.annotation.Nonnull;

import com.denimgroup.threadfix.framework.util.EndpointUtil;
import org.junit.Test;

import com.denimgroup.threadfix.framework.TestConstants;
import com.denimgroup.threadfix.data.interfaces.Endpoint;

public class JSPEndpointGeneratorTests {
    @Nonnull
    String[] pages = { "/root/about.jsp",
            "/root/admin.jsp",
            "/root/advanced.jsp",
            "/root/basket.jsp",
            "/root/contact.jsp",
            "/root/footer.jsp",
            "/root/header.jsp",
            "/root/home.jsp",
            "/root/init.jsp",
            "/root/login.jsp",
            "/root/logout.jsp",
            "/root/password.jsp",
            "/root/product.jsp",
            "/root/register.jsp",
            "/root/score.jsp",
            "/root/search.jsp", };

    @Test
    public void testSize() {
        JSPEndpointGenerator mappings = new JSPEndpointGenerator(new File(
                TestConstants.BODGEIT_SOURCE_LOCATION));

        int numExpected = 35;

        List<Endpoint> endpoints = EndpointUtil.flattenWithVariants(mappings.generateEndpoints());
        assertTrue("Size was " + endpoints.size()
                + " but should have been " + numExpected, endpoints
                .size() == numExpected);
    }

    @Test
    public void testKeys() {
        JSPEndpointGenerator mappings = new JSPEndpointGenerator(new File(
                TestConstants.BODGEIT_SOURCE_LOCATION));
        for (String page : pages) {
            assertTrue("Endpoint for " + page
                    + " shouldn't have been null but was.",
                    mappings.getEndpoints(page) != null);
        }
    }

    @Nonnull
    String[][] tests = {
        { "/root/basket.jsp", "debug", "63" },
        { "/root/basket.jsp", "update", "147" },
        { "/root/basket.jsp", "productid", "148" },
        { "/root/basket.jsp", "quantity", "160" },
    };

    @Test
    public void testParameters() {
        JSPEndpointGenerator mappings = new JSPEndpointGenerator(new File(
                TestConstants.BODGEIT_SOURCE_LOCATION));
        for (String[] test : tests) {
            JSPEndpoint endpoint = mappings.getEndpoints(test[0]).get(0);
            int result = endpoint.getLineNumberForParameter(test[1]);
            assertTrue("Line number for " + test[0] + ": " + test[1]
                    + " should have been " + test[2] + ", but was " + result,
                    Integer.valueOf(test[2]) == result);
        }
    }

    @Test
    public void testEndpointCSVCommas() {
        JSPEndpointGenerator mappings = new JSPEndpointGenerator(new File(
                TestConstants.BODGEIT_SOURCE_LOCATION));

        for (Endpoint endpoint : mappings.generateEndpoints()) {
            String csv = endpoint.getCSVLine();
            String toString = endpoint.toString();
            assertTrue("CSV was not equal to toString", csv.equals(toString));
            assertTrue("length of csv sections != 3", csv.split(",").length == 3);
        }
    }
}
