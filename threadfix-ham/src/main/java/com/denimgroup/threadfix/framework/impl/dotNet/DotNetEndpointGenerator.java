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
package com.denimgroup.threadfix.framework.impl.dotNet;

import com.denimgroup.threadfix.data.interfaces.Endpoint;
import com.denimgroup.threadfix.framework.engine.full.EndpointGenerator;
import com.denimgroup.threadfix.framework.impl.model.ModelField;
import com.denimgroup.threadfix.framework.impl.model.ModelFieldSet;
import com.denimgroup.threadfix.logging.SanitizedLogger;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.denimgroup.threadfix.CollectionUtils.list;
import static com.denimgroup.threadfix.framework.impl.dotNet.DotNetPathCleaner.cleanStringFromCode;

/**
 * Created by mac on 6/11/14.
 */
public class DotNetEndpointGenerator implements EndpointGenerator {

    private final List<DotNetControllerMappings> dotNetControllerMappings;
    private final DotNetRouteMappings            dotNetRouteMappings;
    private final DotNetModelMappings            dotNetModelMappings;
    private final List<Endpoint> endpoints = list();

    public static final SanitizedLogger LOG = new SanitizedLogger(DotNetEndpointGenerator.class);

    public DotNetEndpointGenerator(DotNetRouteMappings routeMappings,
                                   DotNetModelMappings modelMappings,
                                   DotNetControllerMappings... controllerMappings) {
        this(routeMappings, modelMappings, Arrays.asList(controllerMappings));
    }

    public DotNetEndpointGenerator(DotNetRouteMappings routeMappings,
                                   DotNetModelMappings modelMappings,
                                   List<DotNetControllerMappings> controllerMappings) {
        assert routeMappings != null : "routeMappings was null. Check route parsing code.";
        assert controllerMappings != null : "controllerMappings was null. Check controller parsing code.";
        assert controllerMappings.size() != 0 : "controllerMappings were empty. Check controller parsing code.";

        LOG.debug("Initializing EndpointGenerator with routeMappings: " + routeMappings + " and controllerMappings: " + controllerMappings);

        dotNetControllerMappings = controllerMappings;
        dotNetRouteMappings = routeMappings;
        dotNetModelMappings = modelMappings;

        assembleEndpoints();
    }

    private void assembleEndpoints() {
        if (dotNetRouteMappings == null || dotNetRouteMappings.routes == null) {
            LOG.error("No mappings found for project. Exiting.");
            return; // can't do anything without routes
        }
        else {
            LOG.info("Found " + dotNetRouteMappings.routes.size() + " MVC route format strings: ");
        }

        for (int i = 0; i < dotNetRouteMappings.routes.size(); i++) {
            DotNetRouteMappings.MapRoute route = dotNetRouteMappings.routes.get(i);
            LOG.info("[" + i + "]: " + route.url);
        }

        for (DotNetControllerMappings mappings : dotNetControllerMappings) {
            if (mappings.getControllerName() == null) {
                LOG.debug("Controller Name was null. Skipping to the next.");
                assert false;
                continue;
            }

            DotNetRouteMappings.MapRoute mapRoute = dotNetRouteMappings.getMatchingMapRoute(mappings.hasAreaName(), mappings.getControllerName());

            if (mapRoute == null)
                continue;

            for (Action action : mappings.getActions()) {
                if (action == null) {
                    LOG.debug("Action was null. Skipping to the next.");
                    assert false : "mappings.getActions() returned null. This shouldn't happen.";
                    continue;
                }

                String pattern = mapRoute.url;

                LOG.debug("Substituting patterns from route " + action + " into template " + pattern);

                String result = pattern
                        // substitute in controller name for {controller}
                        .replaceAll("\\{\\w*controller\\w*\\}", mappings.getControllerName());
                if(mappings.hasAreaName()){
                    result = result.replaceAll("\\{\\w*area\\w*\\}", mappings.getAreaName());
                }

                if (action.name.equals("Index")) {
                    result = result.replaceAll("/\\{\\w*action\\w*\\}", "");
                } else {
                    result = result.replaceAll("\\{\\w*action\\w*\\}", action.name);
                }

                boolean shouldReplaceParameterSection = true;
                if(action.parameters != null &&
                        mapRoute.defaultRoute != null &&
                        action.parameters.contains(mapRoute.defaultRoute.parameter)) {
                    String lowerCaseParameterName = mapRoute.defaultRoute.parameter.toLowerCase();
                    for (String parameter : action.parameters) {
                        if (parameter.toLowerCase().equals(lowerCaseParameterName)) {
                            shouldReplaceParameterSection = false;
                            break;
                        }
                    }
                }

                if (shouldReplaceParameterSection) {
                    result = result.replaceAll("/\\{[^\\}]*\\}", "");
                }

                result = cleanStringFromCode(result);

                if (!result.startsWith("/")) {
                    result = "/" + result;
                }

                expandParameters(action);

                LOG.debug("Got result " + result);

                endpoints.add(new DotNetEndpoint(result, mappings.getFilePath(), action));
            }
        }
    }

    private void expandParameters(Action action) {
        if (dotNetModelMappings != null) {

            for (ModelField field : action.parametersWithTypes) {
                ModelFieldSet parameters = dotNetModelMappings.getPossibleParametersForModelType(field.getType());
                if (!parameters.getFieldSet().isEmpty()) {
                    action.parameters.remove(field.getParameterKey());
                    for (ModelField possibleParameter : parameters) {
                        action.parameters.add(possibleParameter.getParameterKey());
                    }
                }
            }
        }
    }

    // TODO consider making this read-only with Collections.unmodifiableList() or returning a defensive copy
    @Nonnull
    @Override
    public List<Endpoint> generateEndpoints() {
        return endpoints;
    }

    @Override
    public Iterator<Endpoint> iterator() {
        return endpoints.iterator();
    }
}
