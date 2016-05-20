package com.ttnd.mailchimp.servlet;

import com.ttnd.mailchimp.service.JcrHelper;
import com.ttnd.mailchimp.util.MailChimpUtil;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by Jatin on 3/3/2016.
 */

@SlingServlet(paths = "/services/mailchimp/lists/subscribe")
public class SubscribeUser extends SlingAllMethodsServlet {

    private Logger log = Logger.getLogger(SubscribeUser.class.getName());

    @Reference
    private JcrHelper jcrHelper;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException{
        if(jcrHelper != null){
            String path =  request.getParameter("path");
            String emailID = request.getParameter("emailID");
            String[] listIDs = request.getParameterValues("listID");
            if(path != null){
                Resource resource = jcrHelper.findResource(path);
                if(resource != null && emailID != null 
                		&& listIDs != null && listIDs.length > 0 && path != null){
                	String configURL = "";
                    if(path.indexOf(",") > -1){
                       String[] configArray = path.split(",");
                        configURL = jcrHelper.getMailChimpConfigFromConfigs(configArray);
                    }else{
                        configURL = path;
                    }
                    ValueMap map = jcrHelper.getConfigFromCloudService(configURL);
                    if(map != null){
                        JSONObject responseObj = MailChimpUtil.subscribeUser(emailID, listIDs, map);
                        if(responseObj != null){
                        	response.getWriter().write(responseObj.toString());
                        }
                    }else{
                    	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    }
                }else{
                	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }

        }else{
            log.info("ImportListService : MailChimp Configuration Service Not Available");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
