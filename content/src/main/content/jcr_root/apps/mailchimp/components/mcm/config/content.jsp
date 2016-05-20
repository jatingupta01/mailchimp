<%--
  MailChimp Configuration Component
--%>

<%@include file="/libs/foundation/global.jsp"%>
<%@page session="false" %>
<div>
    <h3>Mailchimp Settings</h3>   
</div>

<c:set value="${properties.accountDomain}" var="accountDomain" />
<div>
    <span>MailChimp Account Domain :
        <c:choose>
			<c:when test="${not empty accountDomain}">
        		${accountDomain}<br>
        		API Username : ${properties.apiUsername} <br>
        		API Key 	 : ${properties.apiKey}
			</c:when>
			<c:otherwise>
        		Please add the Mailchimp Account Domain
    		</c:otherwise>
        </c:choose>            
    </span>
</div>
