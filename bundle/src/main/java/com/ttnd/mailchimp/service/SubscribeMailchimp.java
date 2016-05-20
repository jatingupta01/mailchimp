package com.ttnd.mailchimp.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.ttnd.mailchimp.Constants;
import com.ttnd.mailchimp.model.SubscriptionList;

/**
 * Created by Jatin on 4/6/2016.
 */
@Component
@Service(SubscribeMailchimp.class)
public class SubscribeMailchimp {

    @Reference
    private JcrHelper jcrHelper;

    private List<SubscriptionList> listIDs;

    private void configureList(String path){
        if(jcrHelper != null){
            Resource resource = jcrHelper.findResource(path + "/jcr:content");
            if(resource != null){
            	InheritanceValueMap inheritedValueMap = new HierarchyNodeInheritanceValueMap(resource);
                try{
                    if(inheritedValueMap != null){
                        String[] configPaths = inheritedValueMap.getInherited(Constants.CQ_CLOUD_SERVICE_CONFIG_PROPERTY, String[].class);
                        if(configPaths != null){
                            String configPath = "";
                            if(configPaths.length > 0){
                                for(String value: configPaths){
                                    if(value.indexOf(Constants.MAILCHIMP_CONFIG_PATH) > -1){
                                        configPath =value;
                                        break;
                                    }
                                }
                            }else{
                                if(configPaths.length == 1 && configPaths[0].indexOf(Constants.MAILCHIMP_CONFIG_PATH) > -1){
                                    configPath = configPaths[0];
                                }
                            }
                            if(configPath != null && configPath.length() > 0){
                                ValueMap configMap = jcrHelper.getConfigFromCloudService(configPath);
                                Object username = configMap.get(Constants.METADATA_MAILCHIMP_USERNAME);
                                if(username != null){
                                	JSONArray listArray = jcrHelper.getMailChimpListBasedOnUsername(username.toString());
                                    for(int i =0; i <listArray.length();i++){
                                        JSONObject obj = listArray.getJSONObject(i);
                                        SubscriptionList list = new SubscriptionList();
                                        list.setName(obj.get("text").toString());
                                        list.setId(obj.get("value").toString());
                                        this.listIDs.add(list);
                                    }
                                }
                                
                            }
                        }
                    }
                }catch (JSONException je){
                    je.printStackTrace();
                }catch(NullPointerException ne){
                	ne.printStackTrace();
                }catch(Exception e){
                	e.printStackTrace();
                }
            }
        }else{
            System.out.println("JcrHelper Service Not Available");
        }
    }

    public List<SubscriptionList> getListIDs(String path){
        this.listIDs = new ArrayList<SubscriptionList>();
        configureList(path);
        return this.listIDs;
    }
}
