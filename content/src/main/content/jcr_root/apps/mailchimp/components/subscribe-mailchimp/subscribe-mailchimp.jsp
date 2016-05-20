<%--

  Subscribe Mailchimp component.

  Subscribe Mailchimp

--%><%
%><%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" import="com.ttnd.mailchimp.service.SubscribeMailchimp"%><%
%><%
	SubscribeMailchimp subscribeMailchimp = sling.getService(SubscribeMailchimp.class);
%>
<cq:includeClientLib js="mailchimp.subscribe"/>
<div id="subscribe-mailchimp-form">
    <c:forEach var="list" items="<%= subscribeMailchimp.getListIDs(currentPage.getPath())%>">
        <input type="checkbox" value="${list.id}" name="listID"> ${list.name}
    </c:forEach>
    <input type="text" name="emailID"/>
    <button class="subscribe-btn" id="subscribe-btn">Subscribe</button>
</div>
