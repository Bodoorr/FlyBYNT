package com.flybynt.booking_service.client;

import com.flybynt.booking_service.dto.*;
import com.flybynt.booking_service.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class SeatClient {
    private final RestClient http;
    public SeatClient(RestClient.Builder builder, @Value("${tripsync.seat-url}") String url){
        http=builder.baseUrl(url).build();
    }

    /** Atomically holds AVAILABLE -> HELD for bookingId. Retrying with the same bookingId returns the same hold. */
    public String hold(String flightId,String seatId,String bookingId,String token){

        SeatHoldResponse response=http.post().uri("/seats/hold")
                .headers(h->auth(h,token))
                .body(new SeatHoldRequest(flightId,seatId,bookingId)).retrieve().body(SeatHoldResponse.class);

        if(response==null || !response.held() || response.holdToken()==null)
            throw new ApiException(HttpStatus.CONFLICT,"Seat unavailable");
        return response.holdToken();
    }

    /** HELD -> BOOKED. Idempotent: confirming an already BOOKED hold succeeds again. */
    public void confirm(String holdToken,String token){
        http.post().uri("/seats/holds/{holdToken}/confirm",holdToken)
                .headers(h->auth(h,token)).retrieve().toBodilessEntity();
    }

    public void release(String holdToken,String token){
        http.post().uri("/seats/holds/{holdToken}/release",holdToken)
                .headers(h->auth(h,token)).retrieve().toBodilessEntity();
    }

    /** Idempotent: nothing left to cancel is still a success. */
    public void cancel(String bookingId,String token){
        http.post().uri("/seats/bookings/{id}/cancel",bookingId)
                .headers(h->auth(h,token)).retrieve().toBodilessEntity();
    }

    /** Current seat status: AVAILABLE, HELD or BOOKED. */
    public String status(String flightId,String seatId,String token){
        SeatStatusResponse response=http.get().uri("/flights/{flightId}/seats/{seatId}",flightId,seatId)
                .headers(h->auth(h,token)).retrieve().body(SeatStatusResponse.class);
        return response==null?null:response.seatStatus();
    }

    // the reconciler runs without a caller, so there is no token to forward
    private static void auth(HttpHeaders h,String token){
        if(token!=null)h.setBearerAuth(token);
    }
}
