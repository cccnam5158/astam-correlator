package com.denimgroup.threadfix.framework.impl.jsp;

import java.util.List;

public class JSPWebXmlServletMapping {

    private String servletName;
    private List<String> urlPatterns;
    private JSPWebXmlServlet mappedServlet;

    public JSPWebXmlServletMapping(String servletName, List<String> urlPatterns) {
        this.servletName = servletName;
        this.urlPatterns = urlPatterns;
    }

    public void setMappedServlet(JSPWebXmlServlet servlet) {
        mappedServlet = servlet;
    }

    public JSPWebXmlServlet getMappedServlet() {
        return mappedServlet;
    }

    public String getServletName() {
        return servletName;
    }

    public List<String> getUrlPatterns() {
        return urlPatterns;
    }

    //  TODO - Servlets can have multiple URL bindings, when mapping a finding need to iterate through bindings and find best match
    public boolean matchesUrlPattern(String url) {
        return false;
    }
}