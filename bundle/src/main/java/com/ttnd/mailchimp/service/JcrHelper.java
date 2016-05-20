package com.ttnd.mailchimp.service;

import javax.jcr.Session;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;

/**
 * Created by Jatin on 3/30/2016.
 */

public interface JcrHelper {

	public ValueMap getConfigFromCloudService(String path);
	public Resource findResource(String path);
	public Session getJCRSession();
	public String getMailChimpConfigFromConfigs(String[] configs);
	public JSONArray getMailChimpListBasedOnUsername(String username);
	public String modifyHTMLLinksToExternal(String path, SlingHttpServletRequest request);
    
}
