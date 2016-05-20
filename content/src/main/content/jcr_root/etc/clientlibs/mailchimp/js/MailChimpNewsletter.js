function performAction(ref, action){
    if(ref && ref.path){
        var dlgPath = ref.path.replace("/jcr:content","");
        var url = CQ.HTTP.noCaching(dlgPath + ".cloudservices.json");
        var response = CQ.HTTP.get(url);
        if(response){
            var inheritData = CQ.HTTP.eval(response); 
            var campaignID = inheritData["campaignID"];
            if(inheritData){
                var listID = inheritData["default-list"];
                var configs = inheritData["cq:cloudserviceconfigs"];
				var configURL = "";
                if(configs){
                    if($.isArray(configs)){
						configURL = configs.join(",");
                    }
                    else{
						configURL = configs;
                    }
                }
                if(listID && action && action.length > 0){
                    var params = {};
					params["listID"] = listID;
                    params["configs"] = configURL;
                    params["pagePath"] = ref.path;
                    params["campaignID"] = campaignID;
                    $.ajax({
                        url: '/services/mailchimp/campaigns?action=' + action,
                        type: 'POST',
                        data: params,
                        dataType: "json",
                        success: function(resObj){
                        	if(action=="export"){
                        		alert("Newsletter created successfully.");	
                        	}else if(action=="send"){
                        		alert("Newsletter sent successfully.");
                        	}else{
                        		alert("Operation done successfully.");
                        	}
						},
                        error:function(data){
                        	console.log
                        	if(action=="export"){
                        		alert("Some issue occured. Newsletter Cannot be created. Please try later");	
                        	}else if(action=="send"){
                        		alert("Some issue occured. Newsletter Cannot be sent. Please try later");
                        	}else{
                        		alert("Some internal server error occured.");
                        	}
                        	
                    	}
                	});
                }
            }
        }    
    }
}