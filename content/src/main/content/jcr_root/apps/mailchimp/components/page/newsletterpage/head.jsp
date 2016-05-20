<%--
  Copyright 1997-2010 Day Management AG
  Barfuesserplatz 6, 4001 Basel, Switzerland
  All Rights Reserved.

  This software is the confidential and proprietary information of
  Day Management AG, ("Confidential Information"). You shall not
  disclose such Confidential Information and shall use it only in
  accordance with the terms of the license agreement you entered into
  with Day.

  ==============================================================================

  Default head script of newsletter pages.

  Draws the HTML head with some default content:
  - initialization of the WCM
  - sets the HTML title

  ==============================================================================

--%><%@include file="/libs/foundation/global.jsp" %><%
%><%@ page session="false"  import="com.day.cq.commons.Doctype,
                   com.day.cq.wcm.api.WCMMode,org.apache.commons.lang3.StringEscapeUtils" %><%
    String xs = Doctype.isXHTML(request) ? "/" : "";
%><head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"<%=xs%>>
    <meta name="keywords" content="<%= WCMUtils.getKeywords(currentPage) %>"<%=xs%>>
    <title><%= currentPage.getTitle() == null ? StringEscapeUtils.escapeHtml4(currentPage.getName()) : StringEscapeUtils.escapeHtml4(currentPage.getTitle()) %></title>
    <%
    if (WCMMode.fromRequest(request) != WCMMode.DISABLED) {
        %>
        <cq:include script="/libs/wcm/core/components/init/init.jsp"/>
        <cq:includeClientLib categories="cq.mcm"/>
        <%
    }
%>
</head>
