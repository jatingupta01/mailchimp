package com.ttnd.mailchimp.service;

/**
 * Created by Arpit on 30/03/2016.
 */

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.polling.importer.ImportException;
import com.day.cq.polling.importer.Importer;
import com.ttnd.mailchimp.Constants;
import com.ttnd.mailchimp.model.SimplePrincipal;
import com.ttnd.mailchimp.model.SubscriptionList;
import com.ttnd.mailchimp.util.MailChimpUtil;

@Service(value = Importer.class)
@Component
@Property(name = Importer.SCHEME_PROPERTY, value = "mailchimpListData", propertyPrivate = true)
public class MailchimpListImporter implements Importer {
	private final String GROUP_PATH = "/home/groups/ttnd";
	private final String PROFILE_NODE = "profile";
	private final String PROPERTY_CQ_AUTHORIZABLE_CATEGORY = "cq:authorizableCategory";
	private final String PROPERTY_GIVENNAME = "givenName";
	private final String PROPERTY_SLING_RESOURCE_TYPE = "sling:resourceType";	
	private final String PROPERTY_ACCOUNT_EMAIL = "account";	
	private final String PROPERTY_LIST_ID = "list-id";	

	private Session session = null;

	@Reference
	private SlingRepository slingRepository;

	private final static Logger LOGGER = LoggerFactory.getLogger(MailchimpListImporter.class);

	public void importData(String scheme, String dataSource, Resource resource) throws ImportException {
		LOGGER.info("Importer Service Started");
    	JSONObject responseObj = new JSONObject();
    	
		try {
			responseObj.put("status", "SUCCESS");
			String paths = resource.adaptTo(Node.class).getParent().getPath();
			Resource resource2 = resource.getResourceResolver().getResource(paths);
			ValueMap configuration = resource2.adaptTo(ValueMap.class);
			String username = configuration.get(Constants.METADATA_MAILCHIMP_USERNAME, String.class);
			
			List<SubscriptionList> lists = MailChimpUtil.getMailChimpList(configuration);
			
			if(lists != null){
	    			if(slingRepository != null){
	    				session = slingRepository.loginAdministrative(null);
	    				UserManager userManager = AccessControlUtil.getUserManager(session);
	    				if(userManager != null){
	    					for(SubscriptionList list : lists){
	    						if(null != userManager.getAuthorizable(list.getId()))
	    							continue;
	    						SimplePrincipal principal = new SimplePrincipal(list.getId());
	    						if(principal != null){
	    							Group group = userManager.createGroup(list.getId(), principal, GROUP_PATH);
	    							Value authorizableVal = session.getValueFactory().createValue("mcm");
	    							group.setProperty(PROPERTY_CQ_AUTHORIZABLE_CATEGORY, authorizableVal);
	    							if(group != null){
	    								Value givenNameVal = session.getValueFactory().createValue(list.getName());    	    							
    	    							Value resourceTypeVal = session.getValueFactory().createValue("cq/security/components/profile");
    	    							Value accountEmail = session.getValueFactory().createValue(username);
    	    							Value listId = session.getValueFactory().createValue(list.getId());
    	    							
    	    							group.setProperty(PROFILE_NODE + "/" + PROPERTY_GIVENNAME, givenNameVal);
    	    							group.setProperty(PROFILE_NODE + "/" + PROPERTY_SLING_RESOURCE_TYPE, resourceTypeVal);
    	    							group.setProperty(PROFILE_NODE + "/" + PROPERTY_CQ_AUTHORIZABLE_CATEGORY, authorizableVal);
    	    							group.setProperty(PROFILE_NODE + "/" + PROPERTY_ACCOUNT_EMAIL, accountEmail);
    	    							group.setProperty(PROFILE_NODE + "/" + PROPERTY_LIST_ID, listId);
	    							}
	    						}
	    					}
	    				}
	    				session.save();
	    			}    		
			}
		}catch(RepositoryException e){
			LOGGER.info("Repository Exception during List Creation {}",e);
	    } catch (JSONException e) {
	    	LOGGER.info("JSON Excepion {}",e);
		}
	}

	public void importData(String arg0, String arg1, Resource arg2, String arg3, String arg4) throws ImportException {
		LOGGER.info("MyImporter started...");
	}

}