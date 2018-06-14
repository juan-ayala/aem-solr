package org.aem.demo.core.servlets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.Jsoup;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.wcm.api.WCMMode;
import com.google.common.collect.ImmutableMap;

/**
 * @author Code & Theory
 */
@Component(service = Servlet.class,
           name = "Rendering Index SOLR Servlet",
           property = { "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                        "sling.servlet.resourceTypes=aem-solr/components/structure/page",
                        "sling.servlet.selectors=rendering",
                        "sling.servlet.extensions=index" })
public final class RenderingIndexSolrServlet extends SlingSafeMethodsServlet {

    @Reference
    private RequestResponseFactory requestResponseFactory;

    @Reference
    private SlingRequestProcessor requestProcessor;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws
            ServletException,
            IOException {

        final Resource resource = request.getResource();
        final SolrInputDocument document = new SolrInputDocument();
        document.setField("id", resource.getPath());
        document.addField("body_s", ImmutableMap.of("set", getText(resource)));

        try (SolrClient client = getClient()) {

            new UpdateRequest().add(document)
                               .commit(client, "aemsolr");

        } catch (final SolrServerException e) {
            throw new ServletException(e);
        }
    }

    private String getText(final Resource resource)
            throws
            ServletException,
            IOException {

        final String uri = String.format("%s.html", resource.getPath());
        final HttpServletRequest request = this.requestResponseFactory.createRequest(HttpConstants.METHOD_GET, uri);
        WCMMode.DISABLED.toRequest(request);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final HttpServletResponse response = this.requestResponseFactory.createResponse(out);
            final ResourceResolver resourceResolver = resource.getResourceResolver();
            this.requestProcessor.processRequest(request, response, resourceResolver);
            final String html = out.toString("UTF-8");
            return Jsoup.parse(html)
                        .text();
        }
    }

    private static HttpSolrClient getClient() {

        return new HttpSolrClient.Builder().withBaseSolrUrl("http://localhost:8983/solr")
                                           .build();
    }
}