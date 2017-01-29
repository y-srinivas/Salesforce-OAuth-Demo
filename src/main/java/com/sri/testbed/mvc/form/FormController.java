package com.sri.testbed.mvc.form;



import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.ws.ConnectorConfig;

@Controller
@RequestMapping("/test")
public class FormController {

	// Invoked on every request

	// Invoked initially to create the "form" attribute
	// Once created the "form" attribute comes from the HTTP session (see @SessionAttributes)

	@ModelAttribute("formBean")
	public FormBean createFormBean() {
		return new FormBean();
	}
	
	@RequestMapping(method=RequestMethod.GET)
	public @ResponseBody String form(Model model) throws Exception {
		model.addAttribute("formBean", new FormBean());
		
		return connectSalesforce();
	}
	

	  static final String USERNAME = "srinivas@yellapantula.com";
	  static final String PASSWORD = "Password#234DxvNq8jWd0hTmrtmG3UgP7ts";
	  static PartnerConnection connection;

	  public  String connectSalesforce() throws Exception {

	    
	  
	    //config.setTraceMessage(true);
		 String message = null;
	    
	    try {
	     Response response =connectWithOAuth();
	   
	     if(response.isSuccess() && response.getSessionId() != null){
	    	    ConnectorConfig config = new ConnectorConfig();
			    //config.setUsername(USERNAME);
			    //config.setPassword(PASSWORD);
			    config.setSessionId(response.getSessionId());
			    config.setServiceEndpoint("https://srinivasy-dev-ed.my.salesforce.com/services/Soap/u/33.0/00D90000000f356");
			    config.setAuthEndpoint("https://login.salesforce.com/services/Soap/u/33.0");
			    

	     		connection = Connector.newConnection(config);
	     	
	      
		      // display some current settings
		      System.out.println("Auth EndPoint: "+config.getAuthEndpoint());
		      System.out.println("Service EndPoint: "+config.getServiceEndpoint());
		      System.out.println("Username: "+config.getUsername());
		      System.out.println("SessionId: "+config.getSessionId());
		      
		      // run the different examples
		     return queryContacts();
	     }
	     else {
	    	 message = response.getErrorCause();
	    	 return "Salesforce connectivity failed. Failed with reason: " + message;
	     }
	     	 
	      
	    } catch (Exception e1) {
	        e1.printStackTrace();
	        throw e1;
	    }
		
	  }
	  
	  // queries and displays the 5 newest contacts
	  private static String  queryContacts() {
	    
	    System.out.println("Querying for the 5 newest Contacts...");
	
	    String record = null;
	    try {
	       
	      // query for the 5 newest contacts      
	      QueryResult queryResults = connection.query("SELECT Id, FirstName, LastName, Account.Name " +
	      		"FROM Contact WHERE AccountId != NULL ORDER BY CreatedDate DESC LIMIT 5");
	      if (queryResults.getSize() > 0) {
	    	  for (com.sforce.soap.partner.sobject.SObject s: queryResults.getRecords()) {
	    	    System.out.println("Id: " + s.getId() + " " + s.getField("FirstName") + " " + 
	    	        s.getField("LastName") + " - " + s.getChild("Account").getField("Name"));
	    	    record = "Id: " + s.getId() + " " + s.getField("FirstName") + " " + 
		    	        s.getField("LastName") + " - " + s.getChild("Account").getField("Name");
	    	    break;
	    	    
	    	  }
	    	  return "Salesforce connectivity successful. Record found:  " + record;
	    	}
	      
	    } catch (Exception e) {
	      e.printStackTrace();
	    }   
	    return record;
	    
	  }
	 
	public Response connectWithOAuth() throws Exception{
		Response sfResponse = new Response();
		String sessionId = null;
		String header = "{\"alg\":\"RS256\"}";
	    String claimTemplate = "'{'\"iss\": \"{0}\", \"sub\": \"{1}\", \"aud\": \"{2}\", \"exp\": \"{3}\"'}'";

	    StringBuffer token = new StringBuffer();

	    //Encode the JWT Header and add it to our string to sign
        token.append(Base64.encodeBase64URLSafeString(header.getBytes("UTF-8")));

        //Separate with a period
        token.append(".");

        //Create the JWT Claims Object
	      String[] claimArray = new String[4];
	      claimArray[0] = "3MVG9Y6d_Btp4xp7.6VqDVAugwQ5FMCwabBqnszsi6kPHvV.f0l0PAnzjvnhgVi8xr3PI_9FFRS00.EmFhwwB";
	      claimArray[1] = "srinivas@yellapantula.com";
	      claimArray[2] = "https://login.salesforce.com";
	      claimArray[3] = Long.toString( ( System.currentTimeMillis()/1000 ) + 60);
	      MessageFormat claims;
	      claims = new MessageFormat(claimTemplate);
	      String payload = claims.format(claimArray);

      //Add the encoded claims object
      token.append(Base64.encodeBase64URLSafeString(payload.getBytes("UTF-8")));

      //Load the private key from a keystore
      KeyStore keystore = KeyStore.getInstance("JKS");
      keystore.load(this.getClass().getResourceAsStream("/keystore.jks"), "jyothi".toCharArray());
      PrivateKey privateKey = (PrivateKey) keystore.getKey("cert", "jyothi".toCharArray());

      //Sign the JWT Header + "." + JWT Claims Object
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initSign(privateKey);
      signature.update(token.toString().getBytes("UTF-8"));
      String signedPayload = Base64.encodeBase64URLSafeString(signature.sign()).toString();

      //Separate with a period
      token.append(".");

      //Add the encoded signature
      token.append(signedPayload);
      URL url = new URL("https://login.salesforce.com/services/oauth2/token");
      //URL url = new URL("https://httpbin.org/post"); // good for testing

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      connection.setDoInput(true);
      connection.setDoOutput(true);
      
      HashMap<String, String> params = new HashMap<String, String>();
      params.put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
      params.put("assertion", token.toString());
      

          
      
      StringBuilder postData = new StringBuilder();
      for (Map.Entry<String, String> param : params.entrySet()) {
          if (postData.length() != 0) {
              postData.append('&');
          }
          postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
          postData.append('=');
          if(!param.getKey().equals("assertion"))
        	  postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
          else 
        	  postData.append(String.valueOf(param.getValue()));
      }
      System.out.println("Post Encoded data : " + postData.toString());
      byte[] postDataBytes = postData.toString().getBytes("UTF-8");
      
      connection.getOutputStream().write(postDataBytes);

      int responseCode = connection.getResponseCode();
      
      BufferedReader in;
      
       if(responseCode != 200){
    	   in = new BufferedReader(
   		        new InputStreamReader(connection.getErrorStream()));
    	   StringBuffer response = new StringBuffer();
    		String inputLine;
	   		while ((inputLine = in.readLine()) != null) {
	   			response.append(inputLine);
	   		}
	   		in.close();
	        
	   		sfResponse.setSuccess(false);
	   		sfResponse.setErrorCause(response.toString());
	   		//print result
	   		System.out.println(response.toString());
       }
       else {

		in = new BufferedReader(
		        new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
			
		}
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(response.toString());
		sessionId = (String)json.get("access_token");
		in.close();
		
		sfResponse.setSuccess(true);
		sfResponse.setSessionId(sessionId);
   	
		//print result
		System.out.println(response.toString()); 
       }	
       return sfResponse;
	}

	@RequestMapping(method=RequestMethod.POST)
	public String processSubmit(@Valid FormBean formBean, BindingResult result, 
								@ModelAttribute("ajaxRequest") boolean ajaxRequest, 
								Model model, RedirectAttributes redirectAttrs) {
		if (result.hasErrors()) {
			return null;
		}
		// Typically you would save to a db and clear the "form" attribute from the session 
		// via SessionStatus.setCompleted(). For the demo we leave it in the session.
		String message = "Form submitted successfully.  Bound " + formBean;
		// Success response handling
		if (ajaxRequest) {
			// prepare model for rendering success message in this request
			model.addAttribute("message", message);
			return null;
		} else {
			// store a success message for rendering on the next request after redirect
			// redirect back to the form to render the success message along with newly bound values
			redirectAttrs.addFlashAttribute("message", message);
			return "redirect:/form";			
		}
	}
	
	
	
}