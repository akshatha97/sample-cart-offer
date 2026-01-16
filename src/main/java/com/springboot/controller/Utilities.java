package com.springboot.controller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Utilities {
	
	public static boolean addOffer(OfferRequest offerRequest) throws Exception {
		String urlString = "http://localhost:8080/api/v1/offer";
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestProperty("Content-Type", "application/json");

		ObjectMapper mapper = new ObjectMapper();

		String POST_PARAMS = mapper.writeValueAsString(offerRequest);
		OutputStream os = con.getOutputStream();
		os.write(POST_PARAMS.getBytes());
		os.flush();
		os.close();
		int responseCode = con.getResponseCode();
		System.out.println("POST Response Code :: " + responseCode);

		if (responseCode == HttpURLConnection.HTTP_OK) { //success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			// print result
			System.out.println(response.toString());
		} else {
			System.out.println("POST request did not work.");
		}
		return true;
	}
	
	public static ApplyOfferResponse applyOfferToCart(ApplyOfferRequest request) throws Exception {
	    // 1. Define the Endpoint URL (Must match the controller)
	    String urlString = "http://localhost:8080/api/v1/cart/apply_offer";
	    URL url = new URL(urlString);
	    HttpURLConnection con = (HttpURLConnection) url.openConnection();
	    
	    // 2. Setup the Request
	    con.setRequestMethod("POST");
	    con.setRequestProperty("Content-Type", "application/json");
	    con.setRequestProperty("Accept", "application/json");
	    con.setDoOutput(true);

	    // 3. Serialize the Java Object to JSON
	    ObjectMapper mapper = new ObjectMapper();
	    String jsonInputString = mapper.writeValueAsString(request);

	    // 4. Send the Request
	    try (java.io.OutputStream os = con.getOutputStream()) {
	        byte[] input = jsonInputString.getBytes("utf-8");
	        os.write(input, 0, input.length);
	    }

	    // 5. Handle the Response
	    if (con.getResponseCode() == 200) {
	        InputStream responseStream = con.getInputStream();
	        // Deserialize the JSON response back into a Java Object
	        return mapper.readValue(responseStream, ApplyOfferResponse.class);
	    } else {
	        throw new RuntimeException("Failed to apply offer. HTTP error code: " + con.getResponseCode());
	    }
	}
	
	public static void clearServerData() throws Exception {
	    URL url = new URL("http://localhost:8080/api/v1/test/reset");
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    
	    // 1. Setup Request
	    conn.setRequestMethod("POST");
	    conn.setConnectTimeout(5000);
	    conn.setReadTimeout(5000);
	    
	    // 2. Enable output for POST
	    conn.setDoOutput(true); 
	    
	    // 3. Set length to 0 since reset has no body
	    conn.setFixedLengthStreamingMode(0); 

	    // 4. Trigger the request
	    int status = conn.getResponseCode(); 
	    
	    if (status != 200) {
	        System.err.println("Warning: Data reset returned status " + status);
	    }
	    
	    conn.disconnect();
	}
}
