package org.jenkinsci.plugins.microsoft.util;

import java.util.Calendar;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.microsoft.services.AzureManagementServiceDelegate;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class JellyUtil {
    public static FormValidation checkNotNullValue(String value, String errorMessage) {
		if(value == null || value.length() == 0)
		{
			return FormValidation.error(errorMessage);
		}
		
		return FormValidation.ok();
    }
    
    public static FormValidation checkForIntValue(String value, String errorMessage) {
		int val = 0;
		try {
			val = Integer.parseInt(value);
			if(val < 1 || val > 40) {
				throw new Exception(errorMessage);
			}
		}catch(Exception ex) {
			return FormValidation.error("An integer value is required.");
		}
		
		return FormValidation.ok();
    }
    
    public static FormValidation checkSubscriptionInformation(String subscriptionId,
            String clientId,
            String clientSecret,
            String oauth2TokenEndpoint) {
        if (StringUtils.isBlank(subscriptionId)) {
            return FormValidation.error("Error: Subscription ID is missing");
        }
        if (StringUtils.isBlank(clientId)) {
            return FormValidation.error("Error: Native Client ID is missing");
        }
        if (StringUtils.isBlank(clientSecret)) {
            return FormValidation.error("Error: Azure Password is missing");
        }
        if (StringUtils.isBlank(oauth2TokenEndpoint)) {
            return FormValidation.error("Error: OAuth 2.0 Token Endpoint is missing");
        }
        
        String response = AzureManagementServiceDelegate.verifyConfiguration(
                subscriptionId,
                clientId,
                clientSecret,
                oauth2TokenEndpoint,
                null);

        if (Constants.OP_SUCCESS.equalsIgnoreCase(response)) {
            return FormValidation.ok("Success");
        } else {
            return FormValidation.error(response);
        }            
    }

    public static String generateUniqueName(String prefix) {
		String dnsNamePrefix = UUID.randomUUID().toString().replace("-",  "");
		long millis = Calendar.getInstance().getTimeInMillis();
		long datePart = millis % 1000000000;
		return prefix + dnsNamePrefix.toLowerCase().substring(0, 8) + datePart;	  	
    }
    
    public static void addLocationsToModel(ListBoxModel model) {
        model.add("West US");
        model.add("East US");
        model.add("North Central US");
        model.add("South Central US");
        model.add("North Europe");
        model.add("West Europe");
        model.add("East Asia");
        model.add("Southeast Asia");
        model.add("Japan East");
        model.add("Japan West");
        model.add("Brazil South");
    }    
}

