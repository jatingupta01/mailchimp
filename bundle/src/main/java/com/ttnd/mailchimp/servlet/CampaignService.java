package com.ttnd.mailchimp.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import com.ttnd.mailchimp.service.JcrHelper;
import com.ttnd.mailchimp.util.MailChimpUtil;

/**
 * Created by Jatin on 3/3/2016.
 */

@SlingServlet(paths = "/services/mailchimp/campaigns")
public class CampaignService extends SlingAllMethodsServlet {

	private Logger log = Logger.getLogger(CampaignService.class.getName());

	@Reference
	private JcrHelper jcrHelper;

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		if (("fetchAll").equalsIgnoreCase(request.getParameter("action"))) {
			JSONObject campaigns = new JSONObject();// MailChimpUtil.getCampaigns(mailChimpConfig.getConfigDictionary());
			if (campaigns != null) {
				response.getWriter().write(campaigns.toString());
			}
		} else
			response.getWriter().write("No Action found for Campaign service");

	}

	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		String configs = request.getParameter("configs");
		JSONObject obj = new JSONObject();
		if (configs != null && configs.length() > 0) {
			String configURL = getConfigURLFromConfigs(configs);
			if (configURL != null && jcrHelper != null) {
				ValueMap map = jcrHelper.getConfigFromCloudService(configURL);
				if (("send").equalsIgnoreCase(request.getParameter("action"))) {
					String campaignID = request.getParameter("campaignID");
					if (campaignID != null && campaignID.trim().length() > 0) {
						if (map != null) {
							JSONObject checkListResponseObj = MailChimpUtil.getSendCheckList(map, campaignID);
							if (checkListResponseObj != null) {
								try {
									boolean isReady = checkListResponseObj.getBoolean("is_ready");
									if (isReady) {
										JSONObject responseObj = MailChimpUtil.sendCampaign(map, campaignID);
										if (responseObj != null) {
											obj.put("message", "success");
											response.getWriter().write(obj.toString());
											response.setStatus(HttpServletResponse.SC_OK);
										}else{
											obj.put("message", "Email has already been sent.");
											response.getWriter().write(obj.toString());
											response.setStatus(HttpServletResponse.SC_OK);
										}
									} else {
										response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									}
								} catch (JSONException je) {
									je.printStackTrace();
									response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								}

							}

						}
					}
				} else if (("export").equalsIgnoreCase(request.getParameter("action"))) {
					String pagePath = request.getParameter("pagePath");
					String pageURL = pagePath;
					try {
						if (pagePath != null && map != null) {
							Resource resource = jcrHelper.findResource(pagePath);
							if (resource != null) {
								ValueMap campaignConfigMap = resource.adaptTo(ValueMap.class);
								if (campaignConfigMap != null) {
									String fromName = campaignConfigMap.get("FromName").toString();
									String replyTo = campaignConfigMap.get("ReplyTo").toString();
									String from = campaignConfigMap.get("from").toString();
									String subject = campaignConfigMap.get("subject").toString();
									String title = campaignConfigMap.get("jcr:title").toString();
									String recipientList = campaignConfigMap.get("default-list").toString();
									// String onTime =
									// campaignConfigMap.get("onTime").toString();

									JSONObject params = new JSONObject();
									params.put("type", "regular");

									JSONObject recipientObj = new JSONObject();
									recipientObj.put("list_id", recipientList);
									params.put("recipients", recipientObj);

									JSONObject settingsObj = new JSONObject();
									settingsObj.put("subject_line", subject);
									settingsObj.put("title", title);
									settingsObj.put("from_name", fromName);
									settingsObj.put("reply_to", replyTo);
									settingsObj.put("auto_footer", true);
									settingsObj.put("inline_css", true);
									params.put("settings", settingsObj);
									JSONObject campaignCreationResponse = MailChimpUtil.createCampaign(map, params);
									if (campaignCreationResponse != null) {
										Object id = campaignCreationResponse.get("id");
										Node node = resource.adaptTo(Node.class);
										node.setProperty("campaignID", id != null ? id.toString() : "");
										node.getSession().save();
										if (id != null) {
											JSONObject contentParams = new JSONObject();
											String html = jcrHelper.modifyHTMLLinksToExternal(
													pagePath.replace("/jcr:content", ".html"), request);
											if (html != null) {
												contentParams.put("html", html);
												JSONObject contentJSONResponse = MailChimpUtil
														.updateCampaignContent(map, id.toString(), contentParams);
												if (contentJSONResponse != null) {
													obj.put("message", "success");
													response.getWriter().write(obj.toString());
													response.setStatus(HttpServletResponse.SC_OK);
												}

											}

										}

									}
								}
							}

						}
					} catch (JSONException je) {
						je.printStackTrace();
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					} catch (ValueFormatException e) {
						e.printStackTrace();
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					} catch (VersionException e) {
						e.printStackTrace();
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					} catch (LockException e) {
						e.printStackTrace();
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					} catch (ConstraintViolationException e) {
						e.printStackTrace();
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					} catch (RepositoryException e) {
						e.printStackTrace();
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					}
					catch (NullPointerException e) {
						e.printStackTrace();
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					} 
				}
			}
		}

	}

	private String getConfigURLFromConfigs(String configs) {
		String configURL = null;
		if (configs.indexOf(",") > -1) {
			String[] configArray = configs.split(",");
			configURL = jcrHelper.getMailChimpConfigFromConfigs(configArray);
		} else {
			configURL = configs;
		}
		return configURL;
	}
}
