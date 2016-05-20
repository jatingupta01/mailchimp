$(document).ready(function(){

    $("#subscribe-btn").on("click", function(){
        var params = {};
		var form = $("#subscribe-mailchimp-form");
        if(form && form.length > 0){
            params.listID = $("#subscribe-mailchimp-form [name=listID]:checked").map(function(){return $(this).val();}).get();
            params.emailID = $("#subscribe-mailchimp-form [name=emailID]").val();
            var path = location.pathname;
            if(path && path.length > 0){
                var contentPath = path.substring(0, path.indexOf("."));
				var url = CQ.HTTP.noCaching(contentPath + ".cloudservices.json");
                var response = CQ.HTTP.get(url);
                if(response){
                    var inheritData = CQ.HTTP.eval(response); 
                    if(inheritData){
                        var configs = inheritData["cq:cloudserviceconfigs"];
                        var configURL = "";
                        if(configs){
                            if($.isArray(configs)){
                                configURL = configs.join(",");
                            }
                            else{
                                configURL = configs;
                            }
                            params.path = configURL;
                        }
                    }
                }
            }

            if(!validateFormParamaters(params)){
				return;
            }
			$.ajax({
				url: '/services/mailchimp/lists/subscribe',
                type: 'POST',
                data: params,
                success: function(data){
                    console.log(data);
                    alert("Subscribed Successfully. You will be shortly recieving mail for your confirmation");
                }, error: function(data){
                    console.log(data);
                    alert("Cannot be subscribed");
                }
        	});
        }


	});
});

function validateFormParamaters(params){
	if(!params.listID || params.listID.length == 0){
        alert("Please select the List");
        return false;
	}
    if(!params.emailID || params.emailID.length == 0){
        alert("Please enter the emailID");
        return false;
	}
    if(!params.path || params.path.length == 0){
        alert("Please config the Cloud Service for this page");
        return false;
	}
    return true; 
}