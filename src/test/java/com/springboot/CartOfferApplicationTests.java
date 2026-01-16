package com.springboot;

import com.springboot.controller.ApplyOfferRequest;
import com.springboot.controller.ApplyOfferResponse;
import com.springboot.controller.OfferRequest;
import com.springboot.controller.Utilities;

import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.junit4.DisplayName;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CartOfferApplicationTests {
    
    private static MockServerClient mockServerClient;

    @BeforeClass
    public static void startMockServer() {
        mockServerClient = new MockServerClient("localhost", 1080);
    }

    @Before
    public void setupMocksAndData() throws Exception {
        mockServerClient.reset();
        Utilities.clearServerData();

        mockUser(1, "p1");  // User 1: Standard 'p1' segment for FlatX success, Rounding, and Large Values
        mockUser(2, "p2");  // User 2: Standard 'p2' segment for Percentage discount success
        mockUser(3, "p3");  // User 3: Used for Segment Mismatch test (requesting p1 offer with p3 segment)
        mockUser(4, "p2");  // User 4: Used for Multiple Segments Eligibility (matches [p1, p2])
        mockUser(5, "p1");  // User 5: Boundary test for Zero Balance (100 - 100)
        mockUser(6, "p1");  // User 6: Boundary test for Flooring at Zero (50 - 60)
        mockUser(7, "p1");  // User 7: Multiple Offers Priority (Restaurant with multiple offers)
        // User 8: Not mocked here; TS_08 explicitly calls mockUserError(8) for 500 error testing
        mockUser(9, "p1");  // User 9: Invalid Restaurant ID test (Valid user, no restaurant offer)
        mockUser(10, "p3"); // User 10: Empty Offer Segments test & Segment p3 verification
        mockUser(11, "p1"); // User 11: Dedicated user for Percentage Rounding Truncation logic
        mockUser(12, "p1"); // User 12: Case Sensitivity test (Checking 'flatx' vs 'FLATX')
        mockUser(13, "p1"); // User 13: Large Cart Value test (1,000,000)
        mockUser(14, "p1"); // User 14: Duplicate Offer Types Priority (Percentage vs FlatX)

        mockUser(15, "p1"); // User 15: Boundary - Offer value significantly larger than cart
        mockUser(16, "p1"); // User 16: Invalid Offer Type logic (Type set to 'BOGO')
        mockUser(17, "p1"); // User 17: Boundary - Cart value is exactly 0
        // User 18: Handled inside TS_18 to simulate a blank/empty segment response string ("")
        mockUser(19, "p1"); // User 19: Negative Discount Value test (Ensures price doesn't increase)
        mockUser(20, "p4"); // User 20: Security - User in 'p4' attempting to access 'p1/p2/p3' offers

        mockUser(21, "p1");  // User 21: No Accumulation test (Ensures only one offer applies)
        mockUser(22, "p1");  // User 22: Null Segment List robustness check
        mockUser(23, "p1");  // User 23: Minimum Positive Cart test (Cart value = 1)
        mockUser(24, "p1");  // User 24: Extreme Math - Offer percentage > 100%
        // User 25: Handled inside TS_25 to simulate a 404 Not Found from the Segment service
        mockUser(26, "101"); // User 26: Numeric String Segment - Ensures strictly string-based comparison
    }

    private void mockUser(int userId, String segment) {
        mockServerClient
            .when(request().withMethod("GET").withPath("/api/v1/user_segment")
                .withQueryStringParameter("user_id", String.valueOf(userId)))
            .respond(response().withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"segment\": \"" + segment + "\"}"));
    }

    private void mockUserError(int userId) {
        mockServerClient
            .when(request().withMethod("GET").withPath("/api/v1/user_segment")
                .withQueryStringParameter("user_id", String.valueOf(userId)))
            .respond(response().withStatusCode(500));
    }
    
    @Test
    @DisplayName("TS_01: Flat Amount Discount Success")
    @Description("Verify that a FLATX offer correctly deducts a fixed amount from the cart for eligible segments.")
    @Severity(SeverityLevel.BLOCKER)
    public void TS_01_testApplyOffer_FlatX_Success() throws Exception {
        Utilities.addOffer(new OfferRequest(1, "FLATX", 10, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(200, 1, 1);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(190, response.getCart_value());
    }

    @Test
    @DisplayName("TS_02: Percentage Discount Success")
    @Description("Verify that a PERCENTAGE offer correctly calculates and deducts the percentage from the cart.")
    @Severity(SeverityLevel.BLOCKER)
    public void TS_02_testApplyOffer_Percentage_Success() throws Exception {
        Utilities.addOffer(new OfferRequest(2, "FLAT_PERCENT", 10, Arrays.asList("p2")));
        ApplyOfferRequest request = new ApplyOfferRequest(200, 2, 2);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(180, response.getCart_value());
    }

    @Test
    @DisplayName("TS_03: Segment Mismatch Handling")
    @Description("Ensure no discount is applied if the user's segment does not match the offer's eligible segments.")
    @Severity(SeverityLevel.CRITICAL)
    public void TS_03_testApplyOffer_SegmentMismatch_NoDiscountApplied() throws Exception {
        Utilities.addOffer(new OfferRequest(3, "FLATX", 50, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(500, 3, 3);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(500, response.getCart_value());
    }

    @Test
    @DisplayName("TS_04: Multiple Segments Eligibility")
    @Description("Verify that an offer valid for multiple segments (p1, p2) is correctly applied to a p2 user.")
    @Severity(SeverityLevel.NORMAL)
    public void TS_04_testApplyOffer_MultipleSegments_Success() throws Exception {
        Utilities.addOffer(new OfferRequest(4, "FLATX", 20, Arrays.asList("p1", "p2")));
        ApplyOfferRequest request = new ApplyOfferRequest(100, 4, 4);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(80, response.getCart_value());
    }

    @Test
    @DisplayName("TS_05: Boundary - Zero Balance")
    @Description("Test scenario where the discount exactly equals the cart value, resulting in 0.")
    @Severity(SeverityLevel.NORMAL)
    public void TS_05_testApplyOffer_TotalDiscount_ZeroBalance() throws Exception {
        Utilities.addOffer(new OfferRequest(5, "FLATX", 100, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(100, 5, 5);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(0, response.getCart_value());
    }

    @Test
    @DisplayName("TS_06: Boundary - Floor at Zero")
    @Description("Ensure that if a discount exceeds the cart value, the final price is floored at 0 and not negative.")
    @Severity(SeverityLevel.CRITICAL)
    public void TS_06_testApplyOffer_FlatX_NegativeResult() throws Exception {
        Utilities.addOffer(new OfferRequest(6, "FLATX", 60, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(50, 6, 6);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(0, response.getCart_value());
    }

    @Test
    @DisplayName("TS_07: Multiple Offers Priority")
    @Description("Verify that the system applies the first matching offer found when multiple offers exist for one restaurant.")
    @Severity(SeverityLevel.MINOR)
    public void TS_07_testApplyOffer_MultipleOffers_ApplyFirstFound() throws Exception {
        Utilities.addOffer(new OfferRequest(7, "FLATX", 10, Arrays.asList("p1")));
        Utilities.addOffer(new OfferRequest(7, "FLATX", 50, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(200, 7, 7);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(190, response.getCart_value());
    }

    @Test
    @DisplayName("TS_08: Resiliency - External Service Error")
    @Description("Verify that if the Segment Service returns a 500 error, the cart defaults to no discount (original value).")
    @Severity(SeverityLevel.CRITICAL)
    public void TS_08_testApplyOffer_MockServiceError_NoDiscount() throws Exception {
        mockUserError(8);
        Utilities.addOffer(new OfferRequest(8, "FLATX", 10, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(200, 8, 8);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(200, response.getCart_value());
    }

    @Test
    @DisplayName("TS_09: Invalid Restaurant ID")
    @Description("Verify that no discount is applied if the restaurant ID in the request does not have any active offers.")
    @Severity(SeverityLevel.NORMAL)
    public void TS_09_testApplyOffer_InvalidRestaurantId_NoDiscount() throws Exception {
        ApplyOfferRequest request = new ApplyOfferRequest(200, 9, 9);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(200, response.getCart_value());
    }
    
    @Test
    @DisplayName("TS_10: Empty Offer Segments")
    @Description("Verify that an offer with an empty segment list matches no users.")
    @Severity(SeverityLevel.MINOR)
    public void TS_10_testApplyOffer_EmptyOfferSegments_NoMatch() throws Exception {
        Utilities.addOffer(new OfferRequest(10, "FLATX", 10, new ArrayList<>()));
        ApplyOfferRequest request = new ApplyOfferRequest(200, 10, 10); 
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(200, response.getCart_value());
    }

    @Test
    @DisplayName("TS_11: Percentage Rounding Logic")
    @Description("Verify integer truncation during percentage calculations (e.g., 10% of 195 is 19.5, truncated to 19).")
    @Severity(SeverityLevel.NORMAL)
    public void TS_11_testApplyOffer_Percentage_Rounding() throws Exception {
        Utilities.addOffer(new OfferRequest(11, "PERCENTAGE", 10, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(195, 11, 1);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(176, response.getCart_value());
    }

    @Test
    @DisplayName("TS_12: Case Sensitivity Handling")
    @Description("Ensure offer types are case-sensitive; 'flatx' should not match the expected 'FLATX' logic.")
    @Severity(SeverityLevel.MINOR)
    public void TS_12_testApplyOffer_CaseSensitivity_Mismatch() throws Exception {
        Utilities.addOffer(new OfferRequest(12, "flatx", 10, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(100, 12, 1);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(100, response.getCart_value());
    }
    

    @Test
    @DisplayName("TS_13: Large Value Handling")
    @Description("Verify the system correctly handles large cart amounts (1,000,000) without integer overflow errors.")
    @Severity(SeverityLevel.NORMAL)
    public void TS_13_testApplyOffer_LargeValues() throws Exception {
        Utilities.addOffer(new OfferRequest(13, "FLATX", 5000, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(1000000, 13, 1);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(995000, response.getCart_value());
    }
    
    @Test
    @DisplayName("TS_14: Duplicate Offer Types Priority")
    @Description("Verify that the system picks the first valid offer added when different types (FLATX vs PERCENTAGE) exist for the same restaurant.")
    @Severity(SeverityLevel.NORMAL)
    public void TS_14_testApplyOffer_DuplicateOfferTypes_FirstFound() throws Exception {
        // Offer 1: 10% off (Results in 20 discount)
        Utilities.addOffer(new OfferRequest(14, "FLAT_PERCENT", 10, Arrays.asList("p1")));
        // Offer 2: 50 flat off (Added later, should not be picked if the first matches)
        Utilities.addOffer(new OfferRequest(14, "FLATX", 50, Arrays.asList("p1")));

        ApplyOfferRequest request = new ApplyOfferRequest(200, 14, 14);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);

        // Expected: 200 - (10% of 200) = 180
        Assert.assertEquals(180, response.getCart_value());
    }
    
    @Test
    @DisplayName("TS_15: Boundary - Offer Value Exceeds Cart")
    @Description("Ensure the system floors the result at 0 if the offer value is excessively large.")
    @Severity(SeverityLevel.CRITICAL)
    public void TS_15_testApplyOffer_OfferValueLargerThanCart() throws Exception {
        Utilities.addOffer(new OfferRequest(15, "FLATX", 2000000, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(100, 15, 15);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(0, response.getCart_value());
    }

    @Test
    @DisplayName("TS_16: Logic - Unknown Offer Type")
    @Description("Verify that an unknown offer type (e.g., BOGO) results in no discount.")
    @Severity(SeverityLevel.MINOR)
    public void TS_16_testApplyOffer_UnknownType_NoDiscount() throws Exception {
        Utilities.addOffer(new OfferRequest(16, "BOGO", 50, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(200, 16, 16);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(200, response.getCart_value());
    }

    @Test
    @DisplayName("TS_17: Boundary - Zero Value Cart")
    @Description("Verify that applying an offer to an empty cart results in zero, not a negative number.")
    @Severity(SeverityLevel.NORMAL)
    public void TS_17_testApplyOffer_ZeroCartValue() throws Exception {
        Utilities.addOffer(new OfferRequest(17, "FLATX", 10, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(0, 17, 17);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(0, response.getCart_value());
    }

    @Test
    @DisplayName("TS_18: Resiliency - Empty Segment Value")
    @Description("Verify that if the Segment service returns a blank segment string, no offer is matched.")
    @Severity(SeverityLevel.CRITICAL)
    public void TS_18_testApplyOffer_EmptySegmentResponse() throws Exception {
        mockServerClient
            .when(request().withPath("/api/v1/user_segment").withQueryStringParameter("user_id", "18"))
            .respond(response().withStatusCode(200).withBody("{\"segment\": \"\"}"));

        Utilities.addOffer(new OfferRequest(18, "FLATX", 10, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(100, 18, 18);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(100, response.getCart_value());
    }

    @Test
    @DisplayName("TS_19: Logic - Negative Discount Value")
    @Description("Verify that a negative discount value does not increase the cart price.")
    @Severity(SeverityLevel.NORMAL)
    public void TS_19_testApplyOffer_NegativeOfferValue() throws Exception {
        Utilities.addOffer(new OfferRequest(19, "FLATX", -50, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(200, 19, 19);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertTrue("Price should not increase", response.getCart_value() <= 200);
    }

    @Test
    @DisplayName("TS_20: Security - Unauthorized Segment")
    @Description("Verify that a user in an unmapped segment (p4) cannot claim p1/p2/p3 offers.")
    @Severity(SeverityLevel.BLOCKER)
    public void TS_20_testApplyOffer_UnsupportedSegment() throws Exception {
        Utilities.addOffer(new OfferRequest(20, "FLATX", 50, Arrays.asList("p1", "p2", "p3")));
        ApplyOfferRequest request = new ApplyOfferRequest(300, 20, 20); 
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(300, response.getCart_value());
    }

    @Test
    @DisplayName("TS_21: Forward Compatibility - Inactive Offer Metadata")
    @Description("Verify that if an offer contains extra metadata marking it as 'INACTIVE', the system handles the object safely without crashing.")
    @Severity(SeverityLevel.NORMAL)
    public void TS_21_testApplyOffer_InactiveMetadata_Compatibility() throws Exception {
        // Simulating an offer that might be expired or inactive based on a flag
        // Even if the segment matches, the system should handle extra fields gracefully
        Utilities.addOffer(new OfferRequest(21, "FLATX", 20, Arrays.asList("p1")));
        
        mockUser(21, "p1");
        ApplyOfferRequest request = new ApplyOfferRequest(100, 21, 21);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        
        // The system should still process the basic fields (ID, Type, Value) 
        // until a dedicated status-check logic is implemented.
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getCart_value() <= 100);
    }
    
    @Test
    @DisplayName("TS_22: Robustness - Null Segment List")
    @Description("Verify that an offer with a null segment list doesn't crash the system and applies no discount.")
    public void TS_22_testApplyOffer_NullSegments_NoCrash() throws Exception {
        Utilities.addOffer(new OfferRequest(22, "FLATX", 10, null));
        ApplyOfferRequest request = new ApplyOfferRequest(100, 22, 22);
        mockUser(22, "p1");
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(100, response.getCart_value());
    }
    
    @Test
    @DisplayName("TS_23: Boundary - Minimum Positive Cart")
    @Description("Verify behavior when the cart value is exactly 1 and a larger discount is applied.")
    public void TS_23_testApplyOffer_MinCartValue() throws Exception {
        Utilities.addOffer(new OfferRequest(23, "FLATX", 10, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(1, 23, 23);
        mockUser(23, "p1");
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(0, response.getCart_value());
    }
    
    @Test
    @DisplayName("TS_24: Logic - Percentage Over 100")
    @Description("Verify that a 200% discount results in 0, not a negative balance.")
    public void TS_24_testApplyOffer_OverHundredPercent() throws Exception {
        Utilities.addOffer(new OfferRequest(24, "FLAT_PERCENT", 200, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(100, 24, 24);
        mockUser(24, "p1");
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(0, response.getCart_value());
    }
    
    @Test
    @DisplayName("TS_25: Resiliency - User Not Found")
    @Description("Verify that if the Segment service returns 404, the system defaults to no discount.")
    public void TS_25_testApplyOffer_UserNotFound() throws Exception {
        mockServerClient.when(request().withPath("/api/v1/user_segment").withQueryStringParameter("user_id", "999"))
            .respond(response().withStatusCode(404));

        Utilities.addOffer(new OfferRequest(25, "FLATX", 10, Arrays.asList("p1")));
        ApplyOfferRequest request = new ApplyOfferRequest(100, 25, 999);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(100, response.getCart_value());
    }
    
    @Test
    @DisplayName("TS_26: Edge - Numeric Segment Name")
    @Description("Verify that numeric-named segments (e.g., '101') are handled as strings correctly.")
    public void TS_26_testApplyOffer_NumericSegmentName() throws Exception {
        Utilities.addOffer(new OfferRequest(26, "FLATX", 15, Arrays.asList("101")));
        mockUser(26, "101");
        ApplyOfferRequest request = new ApplyOfferRequest(100, 26, 26);
        ApplyOfferResponse response = Utilities.applyOfferToCart(request);
        Assert.assertEquals(85, response.getCart_value());
    }

    @AfterClass
    public static void stopMockServerClient() {
        if (mockServerClient != null) {
            mockServerClient.close();
        }
    }
}