package org.aem.demo.core.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.component.annotations.Component;

import com.day.cq.commons.LabeledResource;

/**
 * @author Code & Theory
 */
@Component(service = Servlet.class,
           name = "Property Index SOLR Servlet",
           property = { "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                        "sling.servlet.resourceTypes=aem-solr/components/structure/page",
                        "sling.servlet.selectors=property",
                        "sling.servlet.extensions=index" })
public final class PropertyIndexSolrServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws
            ServletException,
            IOException {

        final LabeledResource lblResource = request.getResource()
                                                   .adaptTo(LabeledResource.class);
        final SolrInputDocument document = new SolrInputDocument();
        document.setField("id", lblResource.getPath());
        document.setField("title_s", lblResource.getTitle());
        document.setField("description_s", lblResource.getDescription());

        try (SolrClient client = getClient()) {

            new UpdateRequest().add(document)
                               .commit(client, "aemsolr");

        } catch (final SolrServerException e) {
            throw new ServletException(e);
        }
    }

    private static HttpSolrClient getClient() {

        return new HttpSolrClient.Builder().withBaseSolrUrl("http://localhost:8983/solr")
                                           .build();
    }
}