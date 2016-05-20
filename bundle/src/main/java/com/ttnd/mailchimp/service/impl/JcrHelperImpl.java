package com.ttnd.mailchimp.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.jcr.api.SlingRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.day.cq.commons.Externalizer;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.day.cq.wcm.api.WCMMode;
import com.ttnd.mailchimp.Constants;
import com.ttnd.mailchimp.service.JcrHelper;

@Component
@Service(value = JcrHelper.class)
public class JcrHelperImpl implements JcrHelper{
    @Reference
    private PageManagerFactory pageManagerFactory;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private SlingRepository repository;

    @Reference
    private QueryBuilder queryBuilder;
    
    @Reference
	Externalizer externalizer;
    
    @Reference
	private RequestResponseFactory requestResponseFactory;

	@Reference
	private SlingRequestProcessor requestProcessor;

	public ValueMap getConfigFromCloudService(String path){
        ValueMap map = null;
        if(path != null && resolverFactory != null && pageManagerFactory != null){
            try {
                ResourceResolver resolver = resolverFactory.getAdministrativeResourceResolver(null);
                if(resolver != null){
                    PageManager pageManager = pageManagerFactory.getPageManager(resolver);
                    if(pageManager != null){
                        Page page = pageManager.getPage(path);
                        if(page != null){
                            map = page.getContentResource().adaptTo(ValueMap.class);
                        }
                    }
                }
            }  catch(LoginException e){
                e.printStackTrace();
            }
        }
        return map;
    }

    public Resource findResource(String path){
        Resource resource = null;
        if(path != null && resolverFactory != null ){
            ResourceResolver resolver = null;
            try {
                resolver = resolverFactory.getAdministrativeResourceResolver(null);
                if(resolver != null){
                    resource = resolver.getResource(path);
                }
            } catch (LoginException e) {
                e.printStackTrace();
            }
        }
        return resource;
    }

    public Session getJCRSession(){
        Session session = null;
        try {
            if(repository != null){
                session = repository.loginAdministrative(null);
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return session;
    }

    public String getMailChimpConfigFromConfigs(String[] configs){
        String configURL = null;
        for(String config : configs){
            if(config.indexOf(Constants.MAILCHIMP_CONFIG_PATH) > -1){
                configURL = config;
                break;
            }
        }
        return configURL;
    }

    public JSONArray getMailChimpListBasedOnUsername(String username){
        JSONArray listArray = new JSONArray();
        Map<String,String> queryMap = new HashMap<String, String>();
        queryMap.put("path", "/home/groups");
        queryMap.put("1_property", "jcr:primaryType");
        queryMap.put("1_property.value", "rep:Group");
        queryMap.put("2_property", "profile/cq:authorizableCategory");
        queryMap.put("2_property.value", "mcm");
        queryMap.put("3_property", "profile/sling:resourceType");
        queryMap.put("3_property.value", "cq/security/components/profile");
        queryMap.put("4_property", "profile/account");
        queryMap.put("4_property.value", username);
        Session session = getJCRSession();
        if(session != null && !session.isLive()){
            session.logout();
            session = getJCRSession();
        }
        if(session != null && session.isLive()){
            Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
            SearchResult result = query.getResult();
            if(result != null){
                Iterator<Resource> resources = result.getResources();

                while(resources.hasNext()){
                    Resource res = resources.next();
                    if(res != null){
                        Resource profileResource = res.getChild("profile");
                        if(profileResource != null){
                            ValueMap profileMap = profileResource.adaptTo(ValueMap.class);
                            try{
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("text", profileMap.getOrDefault("givenName", "No Name"));
                                jsonObject.put("value", profileMap.getOrDefault("list-id", ""));
                                listArray.put(jsonObject);
                            }catch (JSONException je){
                                je.printStackTrace();
                                session.logout();
                            }

                        }
                    }
                }
                session.logout();
            }
        }
        return listArray;
    }

    public String modifyHTMLLinksToExternal(String path, SlingHttpServletRequest request){
    	String html = null;
    	try{
    		if(path != null && requestResponseFactory != null){
        		HttpServletRequest req = requestResponseFactory.createRequest("GET", path);
        		WCMMode.DISABLED.toRequest(req);
        		ByteArrayOutputStream out = new ByteArrayOutputStream();
        		HttpServletResponse resp = requestResponseFactory.createResponse(out);
        		String myExternalizedUrl = null;
        		/* Process request through Sling */
        		requestProcessor.processRequest(req, resp, request.getResourceResolver());
        		html = out.toString();
        		Document doc = Jsoup.parse(html);

        		Elements links = doc.select("a[href]");
        		Elements media = doc.select("[src]");
        		Elements imports = doc.select("link[href]");

        		for (Element src : media) {
        			if (src.tagName().equals("img")) {
        				myExternalizedUrl = externalizer.externalLink(request.getResourceResolver(), "mailchimp",
        						src.attr("src"));
        				src.attr("src", myExternalizedUrl);
        			} else
        			myExternalizedUrl = externalizer.externalLink(request.getResourceResolver(), "mailchimp", src.attr("src"));
        			src.attr("src", myExternalizedUrl);
        		}

        		for (Element link : imports) {
        			myExternalizedUrl = externalizer.externalLink(request.getResourceResolver(), "mailchimp", link.attr("src"));
        			link.attr("src", myExternalizedUrl);
        		}

        		for (Element link : links) {
        			myExternalizedUrl = externalizer.externalLink(request.getResourceResolver(), "mailchimp", link.attr("src"));
        			link.attr("src", myExternalizedUrl);
        		}
        		
        		html = doc.toString();
    		}	

    	}catch(IOException io){
    		io.printStackTrace();
    	}
    	catch(ServletException s){
    		s.printStackTrace();
    	}
    	    	
    	return html;
    }	

}
