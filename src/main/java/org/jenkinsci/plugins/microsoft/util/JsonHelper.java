package org.jenkinsci.plugins.microsoft.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonHelper {
    public static ArrayList<Integer> getHostPorts(String marathonConfigFile) 
    		throws FileNotFoundException, IOException {
    	ArrayList<Integer> hostPorts = new ArrayList<Integer>();
    	try(InputStream marathonFile = new java.io.FileInputStream(marathonConfigFile)) {
    		final ObjectMapper mapper = new ObjectMapper();
    		JsonNode parentNode = mapper.readTree(marathonFile);
    		JsonNode node = parentNode.get("container").get("docker").get("portMappings");
    		Iterator<JsonNode> elements = node.elements();
    		while(elements.hasNext()) {
    			JsonNode element = elements.next();
    			hostPorts.add((Integer)element.get("hostPort").asInt());
    		}
    	}
    	
    	return hostPorts;
    }

    public static String getId(String marathonConfigFile) 
    		throws FileNotFoundException, IOException {
    	try(InputStream marathonFile = new java.io.FileInputStream(marathonConfigFile)) {
    		final ObjectMapper mapper = new ObjectMapper();
    		JsonNode parentNode = mapper.readTree(marathonFile);
    		return parentNode.get("id").toString();
    	}
    }
}
