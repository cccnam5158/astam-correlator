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
//     Contributor(s):
//              Denim Group, Ltd.
//              Secure Decisions, a division of Applied Visions, Inc
//
////////////////////////////////////////////////////////////////////////
package com.denimgroup.threadfix.framework.impl.dotNet;

import com.denimgroup.threadfix.data.entities.ModelField;
import com.denimgroup.threadfix.data.entities.ModelFieldSet;
import com.denimgroup.threadfix.data.entities.RouteParameter;
import com.denimgroup.threadfix.data.entities.RouteParameterType;
import com.denimgroup.threadfix.data.interfaces.Endpoint;
import com.denimgroup.threadfix.framework.engine.full.EndpointGenerator;
import com.denimgroup.threadfix.framework.impl.dotNet.classDefinitions.CSharpAttribute;
import com.denimgroup.threadfix.framework.impl.dotNet.classDefinitions.CSharpClass;
import com.denimgroup.threadfix.framework.impl.dotNet.classDefinitions.CSharpParameter;
import com.denimgroup.threadfix.framework.util.EndpointUtil;
import com.denimgroup.threadfix.framework.util.EndpointValidationStatistics;
import com.denimgroup.threadfix.framework.util.FilePathUtils;
import com.denimgroup.threadfix.framework.util.ParameterMerger;
import com.denimgroup.threadfix.logging.SanitizedLogger;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.denimgroup.threadfix.CollectionUtils.list;
import static com.denimgroup.threadfix.CollectionUtils.map;

/**
 * Created by mac on 6/11/14.
 */
public class DotNetEndpointGenerator implements EndpointGenerator {

    private final List<DotNetControllerMappings> dotNetControllerMappings;
    private final DotNetRouteMappings dotNetRouteMappings;
    private final DotNetModelMappings            dotNetModelMappings;
    private final List<CSharpClass>              csharpClasses;
    private final List<Endpoint> endpoints = list();

    public static final SanitizedLogger LOG = new SanitizedLogger(DotNetEndpointGenerator.class);

    public DotNetEndpointGenerator(File rootDirectory,
                                   DotNetRouteMappings routeMappings,
                                   DotNetModelMappings modelMappings,
                                   List<CSharpClass> classes,
                                   DotNetControllerMappings... controllerMappings) {
        this(rootDirectory, routeMappings, modelMappings, classes, Arrays.asList(controllerMappings));
    }

    public DotNetEndpointGenerator(File rootDirectory,
                                   DotNetRouteMappings routeMappings,
                                   DotNetModelMappings modelMappings,
                                   List<CSharpClass> classes,
                                   List<DotNetControllerMappings> controllerMappings) {
        assert routeMappings != null : "routeMappings was null. Check route parsing code.";
        assert controllerMappings != null : "controllerMappings was null. Check controller parsing code.";
        assert controllerMappings.size() != 0 : "controllerMappings were empty. Check controller parsing code.";

        LOG.debug("Initializing EndpointGenerator with routeMappings: " + routeMappings + " and controllerMappings: " + controllerMappings);

        dotNetControllerMappings = controllerMappings;
        dotNetRouteMappings = routeMappings;
        dotNetModelMappings = modelMappings;
        csharpClasses = classes;

        assembleExplicitEndpoints(rootDirectory);
        assembleEndpoints(rootDirectory);
        expandAmbiguousEndpoints();

        fillImplicitParametrics(endpoints);

        ParameterMerger merger = new ParameterMerger();
        Map<Endpoint, Map<String, RouteParameter>> allMergedParameters = merger.mergeParametersIn(endpoints);

        for (Map.Entry<Endpoint, Map<String, RouteParameter>> endpointMapEntry : allMergedParameters.entrySet()) {
            DotNetEndpoint endpoint = (DotNetEndpoint)endpointMapEntry.getKey();
            Map<String, RouteParameter> mergedEndpointParameters = endpointMapEntry.getValue();
            Map<String, RouteParameter> currentParameters = endpoint.getParameters();

            for (Map.Entry<String, RouteParameter> mergedParameter : mergedEndpointParameters.entrySet()) {
                String paramName = mergedParameter.getKey();
                //  Known parametric endpoints take precedence over any merging recommendations
	            if (currentParameters.get(paramName).getParamType() == RouteParameterType.PARAMETRIC_ENDPOINT) {
	            	continue;
	            }

                RouteParameter mergedParam = mergedParameter.getValue();
                currentParameters.put(paramName, mergedParam);
            }
        }

        EndpointUtil.rectifyVariantHierarchy(endpoints);

        EndpointValidationStatistics.printValidationStats(endpoints);
    }

    private void assembleExplicitEndpoints(File rootDirectory) {
        //  Add actions with explicit endpoints
        for (DotNetControllerMappings mappings : dotNetControllerMappings) {
            if (mappings.getControllerName() == null) {
                LOG.debug("Controller Name was null. Skipping to the next.");
                assert false;
                continue;
            }

            for (Action action : mappings.getActions()) {
                if (action.explicitRoute == null || action.isMethodBasedAction) {
                    continue;
                }

                expandParameters(action);

                LOG.debug("Got explicit endpoint " + action.explicitRoute);

                String filePath = mappings.getFilePath();
                if (rootDirectory != null && filePath.startsWith(rootDirectory.getAbsolutePath())) {
                    filePath = FilePathUtils.getRelativePath(filePath, rootDirectory);
                }

                String endpoint = formatActionPath(action.explicitRoute, mappings.getControllerName(), action.name, mappings.getAreaName(), false);
                if (!endpoint.startsWith("/")) {
                    endpoint = "/" + endpoint;
                }
                endpoints.add(new DotNetEndpoint(endpoint, filePath, action));
            }
        }
    }

    private void assembleEndpoints(File rootDirectory) {
        if (dotNetRouteMappings == null) {
            LOG.error("No mappings found for project. Exiting.");
            return; // can't do anything without routes
        }

        List<DotNetRouteMappings.MapRoute> visitedRoutes = list();

        for (DotNetControllerMappings mappings : dotNetControllerMappings) {
            if (mappings.getControllerName() == null) {
                LOG.debug("Controller Name was null. Skipping to the next.");
                assert false;
                continue;
            }

            DotNetRouteMappings.MapRoute mapRoute = dotNetRouteMappings.getMatchingMapRoute(mappings.hasAreaName(), mappings.hasActionNames(), mappings.getControllerName(), mappings.getNamespace());

            if (mapRoute == null ||  mapRoute.url == null || mapRoute.url.equals(""))
                continue;

            for (Action action : mappings.getActions()) {
                if (action == null) {
                    LOG.debug("Action was null. Skipping to the next.");
                    assert false : "mappings.getActions() returned null. This shouldn't happen.";
                    continue;
                }

                boolean shouldReplaceParameterSection = true;

                if(action.parameters != null &&
                    mapRoute.defaultRoute != null &&
                    action.parameters.keySet().contains(mapRoute.defaultRoute.parameter)) {

                    String lowerCaseParameterName = mapRoute.defaultRoute.parameter.toLowerCase();
                    for (String parameter : action.parameters.keySet()) {
                        if (parameter.toLowerCase().equals(lowerCaseParameterName)) {
                            shouldReplaceParameterSection = false;
                            break;
                        }
                    }
                }

                //  Actions mapped via HTTP method instead of action name will have a explicitRoute assigned, which
                //  will be used as the action name instead. Non-method-mapped actions should not have an explicit
                //  route assigned
                if (action.explicitRoute != null && !action.isMethodBasedAction) {
                    continue;
                }

                String pattern = mapRoute.url;
                String result = null;
                //  If a specific action was set for this route, only create endpoints when we get to that action
                if (!pattern.contains("{action}") && !pattern.contains("[action]")) {
                    if (mapRoute.defaultRoute != null && !action.name.equals(mapRoute.defaultRoute.action)) {
                        continue;
                    } else if (action.isMethodBasedAction) {
                        result = formatActionPath(
                            mapRoute.url,
                            mappings.getControllerName(),
                            action.explicitRoute,
                            mappings.hasAreaName() ? mappings.getAreaName() : null,
                            shouldReplaceParameterSection
                        );
                    }
                }

                LOG.debug("Substituting patterns from route " + action + " into template " + pattern);

                if (result == null) {
                    boolean isDefaultAction = (mapRoute.defaultRoute != null && action.name.equals(mapRoute.defaultRoute.action));
                    result = formatActionPath(
                        mapRoute.url,
                        mappings.getControllerName(),
                        isDefaultAction ? null : action.name,
                        mappings.hasAreaName() ? mappings.getAreaName() : null,
                        shouldReplaceParameterSection
                    );
                }

                // Commented since this would remove valuable information regarding parametric routes
                //result = cleanStringFromCode(result);

                LOG.debug("Got result " + result);

                expandParameters(action);

                String filePath = mappings.getFilePath();
                if (rootDirectory != null && filePath.startsWith(rootDirectory.getAbsolutePath())) {
                    filePath = FilePathUtils.getRelativePath(filePath, rootDirectory);
                }

                endpoints.add(new DotNetEndpoint(result, filePath, action));

                if (!visitedRoutes.contains(mapRoute)) {
                    visitedRoutes.add(mapRoute);
                }
            }
        }

        //  Add routes that only have default controllers specified (which wouldn't have been
	    //  enumerated in the previous loop)

        List<DotNetRouteMappings.MapRoute> unvisitedRoutes = new ArrayList<DotNetRouteMappings.MapRoute>(dotNetRouteMappings.routes);
        unvisitedRoutes.removeAll(visitedRoutes);
        for (DotNetRouteMappings.MapRoute route : unvisitedRoutes) {
        	if (route.defaultRoute == null) {
        		continue;
	        }

	        DotNetRouteMappings.ConcreteRoute defaultRoute = route.defaultRoute;
        	String result = route.url;
        	if (!result.startsWith("/")) {
        		result = "/" + result;
	        }

	        DotNetControllerMappings controllerMappings = null;
        	Action action = null;
        	for (DotNetControllerMappings mappings : dotNetControllerMappings) {
        		if (controllerMappings != null) {
        			break;
		        }

        		if (mappings.getControllerName() != null && mappings.getControllerName().equals(defaultRoute.controller)) {
        			for (Action controllerAction : mappings.getActions()) {
        				if (controllerAction.explicitRoute == null && controllerAction.name.equals(defaultRoute.action)) {
					        controllerMappings = mappings;
					        action = controllerAction;
					        break;
				        }
			        }
		        }
	        }

	        if (controllerMappings == null || action == null) {
        		continue;
	        }

	        result = formatActionPath(
	            result,
                controllerMappings.getControllerName(),
                action.name,
                controllerMappings.hasAreaName() ? controllerMappings.getAreaName() : null,
                false
            );

	        String filePath = controllerMappings.getFilePath();
        	if (filePath.startsWith(rootDirectory.getAbsolutePath())) {
        		filePath = FilePathUtils.getRelativePath(filePath, rootDirectory);
	        }

	        endpoints.add(new DotNetEndpoint(result, filePath, action));
        }

    }

    private String formatActionPath(String actionPath, String controllerName, String actionName, String areaName, boolean shouldReplaceParameterSection) {
        String result = actionPath;
        result = result.replaceAll("[\\[\\{]controller[\\]\\}]", controllerName);
        result = result.replaceAll("[\\[\\{]action[\\]\\}]", actionName == null ? "" : actionName);
        if (areaName != null) {
            result = result.replaceAll("[{\\[]\\w*area\\w*[}\\]]", areaName);
        }
        if (shouldReplaceParameterSection) {
            result = result.replaceAll("/\\{[^\\}]*\\}", "");
        }

        if (!result.startsWith("/")) {
            result = "/" + result;
        }
        return result;
    }

    //  Find parametric endpoints and add embedded parameters if necessary
    private void fillImplicitParametrics(Collection<Endpoint> endpoints) {
        Pattern parametricPattern = Pattern.compile("\\{(\\w+)\\}");

        for (Endpoint endpoint : endpoints) {
            String urlPath = endpoint.getUrlPath();
            Matcher paramMatcher = parametricPattern.matcher(urlPath);
            List<String> paramNames = list();
            while (paramMatcher.find()) {
                paramNames.add(paramMatcher.group(1));
            }

            if (paramNames.isEmpty())
                continue;

            for (String paramName : paramNames) {
                if (!endpoint.getParameters().containsKey(paramName)) {
                    RouteParameter newParam = new RouteParameter(paramName);
                    newParam.setParamType(RouteParameterType.PARAMETRIC_ENDPOINT);
                    newParam.setDataType("String");
                    endpoint.getParameters().put(paramName, newParam);
                }
            }
        }
    }

    private void expandParameters(Action action) {
        if (dotNetModelMappings != null) {

            for (RouteParameter param : action.parametersWithTypes) {
                String dataTypeSource = param.getDataTypeSource();

                if (dataTypeSource == null) {
                    continue;
                }

                ModelFieldSet parameters = dotNetModelMappings.getPossibleParametersForModelType(dataTypeSource);
                if (parameters.getFieldSet().isEmpty() && dataTypeSource.contains(".")) {
                    String cleanedDataTypeSource = dataTypeSource.substring(dataTypeSource.lastIndexOf('.') + 1);
                    parameters = dotNetModelMappings.getPossibleParametersForModelType(cleanedDataTypeSource);
                }
                if (!parameters.getFieldSet().isEmpty()) {
                    List<String> includedParamNames = null;
                    CSharpParameter methodParameter = action.actionMethod.getParameter(param.getName());
                    CSharpAttribute bindAttribute = methodParameter.getAttribute("Bind");
                    if (bindAttribute != null) {
                        CSharpParameter includeParameter = bindAttribute.getParameterValue("Include", 0);
                        if (includeParameter != null) {
                            String includeString = includeParameter.getStringValue();
                            includedParamNames = list();
                            for (String paramName : includeString.split(",")) {
                                includedParamNames.add(paramName.trim());
                            }
                        }
                    }

                    action.parameters.remove(param.getName());
                    for (ModelField possibleParameter : parameters) {
                        if (includedParamNames != null && !includedParamNames.contains(possibleParameter.getParameterKey())) {
                            continue;
                        }

                        RouteParameter newParam = new RouteParameter(possibleParameter.getParameterKey());
                        newParam.setDataType(possibleParameter.getType());
                        newParam.setParamType(RouteParameterType.FORM_DATA); // All non-primitives are serialized as form data
                        action.parameters.put(possibleParameter.getParameterKey(), newParam);
                    }
                }
            }
        }
    }

    private void expandAmbiguousEndpoints() {
        List<DotNetEndpoint> ambiguousEndpoints = list();
        List<DotNetEndpoint> dedicatedEndpoints = list();
        for (Endpoint endpoint : endpoints) {
            DotNetEndpoint dotEndpoint = (DotNetEndpoint)endpoint;
            if (dotEndpoint.hasMultipleMethods()) {
                ambiguousEndpoints.add(dotEndpoint);
                List<DotNetEndpoint> splitEndpoints = dotEndpoint.splitByMethods();
                DotNetEndpoint primaryEndpoint = splitEndpoints.get(0);
                for (DotNetEndpoint subEndpoint : splitEndpoints) {
                    if (subEndpoint != primaryEndpoint) {
                        primaryEndpoint.addVariant(subEndpoint);
                    }
                }
                dedicatedEndpoints.add(primaryEndpoint);
            }
        }

        endpoints.removeAll(ambiguousEndpoints);
        endpoints.addAll(dedicatedEndpoints);
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
