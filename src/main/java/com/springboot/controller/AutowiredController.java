package com.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class AutowiredController {


	List<OfferRequest> allOffers = new ArrayList<>();

	@PostMapping(path = "/api/v1/offer")
	public ApiResponse postOperation(@RequestBody OfferRequest offerRequest) {
		System.out.println(offerRequest);
		allOffers.add(offerRequest);
		return new ApiResponse("Success");
	}

	@PostMapping(path = "/api/v1/cart/apply_offer")
    public ApplyOfferResponse applyOffer(@RequestBody ApplyOfferRequest applyOfferRequest) throws Exception {
        int cartVal = applyOfferRequest.getCart_value();
        
        // Fetch segment from MockServer (p1, p2, or p3)
        SegmentResponse segmentResponse = getSegmentResponse(applyOfferRequest.getUser_id());
        String userSegment = segmentResponse.getSegment();

        // Find matching offer for Restaurant + Segment
        Optional<OfferRequest> matchRequest = allOffers.stream()
                .filter(x -> x.getRestaurant_id() == applyOfferRequest.getRestaurant_id())
                .filter(x -> x.getCustomer_segment() != null && x.getCustomer_segment().contains(userSegment))
                .findFirst();

        if (matchRequest.isPresent()) {
            OfferRequest gotOffer = matchRequest.get();
            String offerType = gotOffer.getOffer_type();
            int offerValue = gotOffer.getOffer_value();

            System.out.println("Applied Offer: " + offerType + " for Segment: " + userSegment);
            
            if (offerValue > 0) {
	            if (offerType.equals("FLATX")) {
	                cartVal = cartVal - offerValue;
	            } else if (offerType.contains("PERCENT")) { // Handles FLAT_PERCENT or PERCENTAGE
	            	cartVal = (int) Math.round(cartVal - (cartVal * offerValue / 100.0));
	            }
            }
            
            // Safety check for negative values
            cartVal = Math.max(0, cartVal); 
        } else {
            System.out.println("No matching offer found for user " + applyOfferRequest.getUser_id() + " in segment " + userSegment);
        }
        
        return new ApplyOfferResponse(cartVal);
    }

	private SegmentResponse getSegmentResponse(int userid) {
	    try {
	        String urlString = "http://localhost:1080/api/v1/user_segment?user_id=" + userid;
	        URL url = new URL(urlString);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("GET");
	        connection.setRequestProperty("accept", "application/json");

	        // Use try-with-resources to ensure the stream closes automatically
	        try (InputStream responseStream = connection.getInputStream()) {
	            ObjectMapper mapper = new ObjectMapper();
	            return mapper.readValue(responseStream, SegmentResponse.class);
	        } finally {
	            connection.disconnect();
	        }
	    } catch (Exception e) {
	        System.out.println("Error fetching segment: " + e.getMessage());
	        return new SegmentResponse(); // Return empty if failed
	    }
	}

	@PostMapping("/api/v1/test/reset")
	public void resetData() {
	    this.allOffers.clear(); 
	}

}
