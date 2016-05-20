package com.ttnd.mailchimp.servlet;

import com.day.cq.search.QueryBuilder;
import com.ttnd.mailchimp.Constants;
import com.ttnd.mailchimp.service.JcrHelper;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by Jatin on 3/3/2016.
 */

@SlingServlet(paths = "/services/mailchimp/import/lists")
public class ImportList extends SlingSafeMethodsServlet {

    private Logger log = Logger.getLogger(ImportList.class.getName());

    @Reference
    private JcrHelper jcrHelper;

    @Reference
    private QueryBuilder queryBuilder;


    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException{
        if(jcrHelper != null){
            String source =  request.getParameter("source");
            String path =  request.getParameter("path");
            if(path != null && source != null){
                Resource resource = jcrHelper.findResource(path);
                if(resource != null){
                    if(("cloud").equalsIgnoreCase(source)){
                        ValueMap map = jcrHelper.getConfigFromCloudService(path);
                        if(map != null){
                            Object obj  = map.get(Constants.METADATA_MAILCHIMP_USERNAME);
                            if(obj != null){
                                String username = obj.toString();
                                if(username != null){
                                    JSONArray listArray = jcrHelper.getMailChimpListBasedOnUsername(username);
                                    response.getWriter().write(listArray.toString());
                                }
                            }
                        }
                    }
                }

            }

        }else{
            log.info("ImportListService : MailChimp Configuration Service Not Available");
        }
    }


}
