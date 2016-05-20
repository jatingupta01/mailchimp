CQ.cloudservices.MailchimpCloudServicePanel = CQ.Ext.extend(CQ.Ext.Panel, {

    inheritField: null,

    inheritFlag: null,

    serviceFieldSet: null,

    addServiceButton: null,

    dataView: null,

    data: null,

    constructor: function(config) {
        var dlg = this;

        this.inheritField = new CQ.Static({
            "fieldLabel": CQ.I18n.getMessage("Inherited from"),
            "html": ""
        });

        this.inheritFlag = new CQ.Ext.form.Hidden({
        	name: ":cancelInheritance",
        	value: false
        });

        this.serviceFieldSet = new CQ.form.DialogFieldSet({
            title: CQ.I18n.getMessage("Cloud Service Configurations"),
            collapsed: false,
            autoHeight: true,
            autoScroll: true,
            items: [
                this.inheritFlag,
                {
                    xtype: "hidden",
                    name: "./cq:cloudserviceconfigs" + CQ.Sling.DELETE_SUFFIX,
                    value: true
                }
            ]
        });


        this.addServiceButton = new CQ.Ext.Button({
            "text": CQ.I18n.getMessage("Add Service 1"),
            "style": "float:right",
            "handler": function() {
                var parentDlg = this.findParentByType("dialog");
                var serviceDlg = new CQ.cloudservices.CloudServiceDialog({
                    path: parentDlg.path,
                    data: dlg.data,
                    buttons: [
                        {
                            text: CQ.I18n.getMessage("OK"),
                            handler: function() {
                                dlg.addService(serviceDlg.getSelectedService());
                                var configs = dlg.data['cq:cloudserviceconfigs'];
                                if(!configs) {
                                    configs = [];
                                }
                                if(typeof(configs) == "string") {
                                    configs = [configs];
                                }
                                dlg.data["cq:cloudserviceconfigs"] = configs;
                                configs.push(serviceDlg.getSelectedService().path);
                                serviceDlg.close();
                                dlg.toggleListSelectionBasedOnConfig(configs);

                            }
                        },
                        {
                            text: CQ.I18n.getMessage("Cancel"),
                            handler: function() {
                                serviceDlg.close();
                            }
                        }
                    ]
                });
                serviceDlg.show();
            }
        });

        CQ.Util.applyDefaults(config, {
			"items": [
                dlg.inheritField,
                dlg.serviceFieldSet,
                {
                    "xtype": "panel",
                    "border": false,
                    "items": [
                        dlg.addServiceButton,
                        {
                            xtype: "static",
                            style: "float:right;margin:3px 10px 0px 0px;text-decoration:underline;",
                            html: '<a href="' + CQ.HTTP.externalize('/miscadmin#/etc/cloudservices') + '" target="_blank">' + CQ.I18n.getMessage("Manage configurations") + '</a>'
                        }
                    ]
                },
                {
                    
                    xtype: "selection",
                    type: "select",
                    fieldLabel : "Default List",
                    fieldDescription : "Default List to which Newsletter will be sent",
					name: "./default-list"
                    

                }
            ],
            "listeners": {
                "beforeshow": function(comp) {
                    comp.doLayout();
                }
            }
        });
        
        CQ.cloudservices.MailchimpCloudServicePanel.superclass.constructor.call(this, config);
    },
    
    initComponent: function() {
        CQ.cloudservices.MailchimpCloudServicePanel.superclass.initComponent.call(this);
        var parentDialog = this.findParentByType("dialog");
        parentDialog.on("loadcontent", this.postProcessRecords, this);
    },
    
    postProcessRecords: function(dialog, records, opts, sucess) {
        //#38153: already initialized
        if (this.data) {        
            return;
        }
        //check for inheritance
        var dlg = this.findParentByType('dialog');
        var dlgPath = dlg.path.replace("/jcr:content","");
        var showParent = !records[0].data["cq:cloudserviceconfigs"] ? "" : "?showparent=true"; 
        var url = CQ.HTTP.noCaching(dlgPath + ".cloudservices.json" + showParent)
        var response = CQ.HTTP.get(url);
        var inheritData = CQ.HTTP.eval(response);    
        var recordData = records[0].data;
        var isInherited = inheritData["jcr:path"] != undefined; 
        var isOverridden = (isInherited && recordData["cq:cloudserviceconfigs"] != undefined);
        
        this.data = recordData;
        if(isInherited && !isOverridden) {
            this.data = inheritData;
        }
        
        //fill store with configured services
        if(this.data["cq:cloudserviceconfigs"]) {       
            var url = CQ.HTTP.noCaching("/libs/cq/cloudservices/services.json")
            var response = CQ.HTTP.get(url);
            var data = CQ.HTTP.eval(response);
            
            var configs = this.data["cq:cloudserviceconfigs"];
            if(typeof(configs) == "string") {
                configs = [configs];
            }
            for(var i=0; i<configs.length; i++) {
                var service = this.getServiceForConfigPath(data.services, configs[i]);
                if(service) {
                    this.addService(service, configs[i]);
                }
            }
        }
        
        if( (this.data["jcr:path"] || dlgPath) ) {
            if(isInherited) {
                var inheritPath = inheritData["jcr:path"].replace("/jcr:content","");
                var tpl = new CQ.Ext.Template('{path}');
                this.inheritField.updateHtml(tpl.apply({path: inheritPath}));
            }
            
            this.inheritField.setVisible((isOverridden || isInherited));
            this.setConfigurationsEnabled((isOverridden || !isInherited));
            
            var editLock = isOverridden ? false : true;
            this.handleLock(this.inheritField, editLock);
        }
    },
    
    setConfigurationsEnabled: function(enable) {
        var tab = this.findParentByType('tabpanel');
        var fields = tab.findByType('compositefield');
        for(var i = 0; i < fields.length; i++) {
            enable ? fields[i].enable() : fields[i].disable();
            var removeBtn = fields[i].items.items[0].ownerCt.findByType('button')[0];
            enable ? removeBtn.enable() : removeBtn.disable();
        }
        enable ? this.addServiceButton.enable() :  this.addServiceButton.disable();
    },
    
    getServiceForConfigPath: function(services, path) {
        for(var i=0; i<services.length; i++) {
            if(path.indexOf(services[i].path) > -1) {
                return services[i];
            }
        }
    },
    
    handleLock: function(field, editLock) {
        try {
            var dlg = this;
            var iconCls = (editLock ? "cq-dialog-locked" : "cq-dialog-unlocked");
            field.editLock = editLock;
            
            this.inheritFlag.setValue(!editLock);
            
            field.fieldEditLockBtn = new CQ.TextButton({
                "tooltip": editLock ? CQ.Dialog.CANCEL_INHERITANCE : CQ.Dialog.REVERT_INHERITANCE,
                "cls": "cq-dialog-editlock",
                "iconCls": iconCls,
                "handleMouseEvents": false,
                "handler": function() {                     
                    dlg.switchInheritance(field, function(field, iconCls, editLock) {
                            field.fieldEditLockBtn.setIconClass(iconCls);
                            field.fieldEditLockBtn.setTooltip(iconCls == "cq-dialog-unlocked" ?
                                    CQ.Dialog.REVERT_INHERITANCE : CQ.Dialog.CANCEL_INHERITANCE);
                            field.setDisabled(editLock);
                            field.editLock = editLock;
                            },
                            dlg);
                }
            });
            var formEl = CQ.Ext.get('x-form-el-' + field.id);
            var label = formEl.parent().first();
            // narrow the field label
            formEl.parent().first().dom.style.width =
                    (parseInt(label.dom.style.width) - CQ.themes.Dialog.LOCK_WIDTH) + "px";
            if (field.rendered) {
                field.fieldEditLockBtn.render(formEl.parent(), label.next());
            } else {
                this.on("render", function(comp) {
                    field.fieldEditLockBtn.render(formEl.parent(), label.next());
                });
            }
        }
        catch (e) {
            // skip (formEl is null)
        }       
    },
    
    switchInheritance: function(field, callback, scope) {
        CQ.Ext.Msg.confirm(
            field.editLock ? CQ.I18n.getMessage("Cancel inheritance") : CQ.I18n.getMessage("Revert inheritance"),
            field.editLock ? CQ.I18n.getMessage("Do you really want to cancel the inheritance?") : CQ.I18n.getMessage("Do you really want to revert the inheritance?"),
            function(btnId) {
                if (btnId == "yes") {
                    var editLock = (field.editLock ? false : true);
                    var iconCls = (field.editLock ? "cq-dialog-unlocked" : "cq-dialog-locked");
                    if (callback) {
                        callback.call(this, field, iconCls, editLock);
                    }
                    this.inheritFlag.setValue(!editLock);
                    this.setConfigurationsEnabled(!editLock);
                }
            },
            scope || this
        );
    },

    toggleListSelectionBasedOnConfig: function(configs){
        var dlg = this;
        var parentDialog = dlg.findParentByType("dialog");
        var listSelect = parentDialog.getField("./default-list");
        if(listSelect){
            if(configs && configs.length > 0){
                for(var k=0; k < configs.length; k++){
                    if(configs[k].indexOf("/etc/cloudservices/mailchimp") > -1){
						listSelect.show();
                        return;
                    }

                }

            }
            listSelect.reset();
            listSelect.setOptions("");

			listSelect.doLayout(false, false);
            listSelect.hide();

        }
    },

    addService: function(service, value) {
        if(service && service.title && service.path) {
            var dlg = this;
            var parentDialog = dlg.findParentByType("dialog");
            var listSelect = parentDialog.getField("./default-list");
			var fld = {
                "xtype": "compositefield",
                "items": [
                    {
                        "xtype": "cloudservicescombo",
                        "fieldLabel": service.title,
                        "name": "./cq:cloudserviceconfigs",
                        "rootPath": service.path,
                        "templatePath": service.templatePath,
                        "value": value ? value : "",
                        "flex": 1,
                         listeners : {
                            select : function(comp, result, index){
                                if(result && result.data && result.data.path){
									var path =  result.data.path;
                                    if(result.data.templatePath == "/apps/mailchimp/templates/mailchimp"){
										console.log(path);
                                        $.ajax({
                                            url: '/services/mailchimp/import/lists?source=cloud&path=' + path,
                                            type: 'GET',
                                            success: function(data){
                                                if(data){
                                            		var jsonData = JSON.parse(data);
                                            		listSelect.reset();
                                                    listSelect.setOptions(jsonData);
                                            		listSelect.doLayout(false,false);
                                                }
                                        	},
                                            error:function(data){
                                            	console.log(data);
                                        	}
                                        });
                                    }	
                                }

                            }
    					}

                    }
                ]
            };
            
            var linkHtml;
            if(service.serviceUrl) {
                linkHtml = '<a href="' + service.serviceUrl + '" target="_blank"><img ext:qtip="' + (service.serviceUrlLabel || "Link to Service")
                           + '" src="' + CQ.HTTP.externalize(service.iconPath || '/libs/cq/ui/widgets/themes/default/icons/16x16/siteadmin.png') + '" /></a>';
            } else {
                linkHtml = '<img src="' + CQ.HTTP.externalize('/etc/designs/default/0.gif') + '" />';
            }



            fld.items.push({
                xtype: 'static',
                html: linkHtml,
                width: '16px'
            });

            fld.items.push({
                "xtype":"panel",
                "border":false,
                "items": [
                    {
                        "xtype":"button",
                        "iconCls": "cq-multifield-remove",
                        "template": new CQ.Ext.Template('<span><button class="x-btn" type="{0}"></button></span>'),
                        "handler":function(b,e) {
                            var field = b.findParentByType("panel").ownerCt.initialConfig.ownerCt;
                            // remove the field from the fieldset
                            field.ownerCt.remove(field);
                            // remove from the configs so the service appears when listing the services in the dialog
                            var rootPath = field.items.items[0].rootPath;
                            var configs = dlg.data['cq:cloudserviceconfigs'];
                            if(configs && typeof(configs) == "string") {
                                configs = [configs];
                            }
                            if(configs){
								var idx;
                                for(var x=0; x < configs.length ; x++){
                                    if(configs[x] && configs[x].trim().length > 0 && configs[x].indexOf(rootPath) > -1){
                                        idx = x;
                                    }
                                }
                                if(idx >= 0){
                                    configs.splice(idx,1);
                                }
                                dlg.data['cq:cloudserviceconfigs'] = configs;
                                dlg.toggleListSelectionBasedOnConfig(configs);
                            }    

                        }
                    }
                ]
            });

			this.serviceFieldSet.add(fld);

            this.serviceFieldSet.doLayout();
        }
    }
    
});
CQ.Ext.reg("mailchimpCloudServicePanel", CQ.cloudservices.MailchimpCloudServicePanel);