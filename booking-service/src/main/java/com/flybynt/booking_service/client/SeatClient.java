package com.flybynt.booking_service.client;

import com.flybynt.booking_service.dto.*;
import com.flybynt.booking_service.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class SeatClient {
    private final RestClient http;
    public SeatClient(RestClient.Builder builder, @Value("${tripsync.seat-url}") String url){
        http=builder.baseUrl(url).build();
    }

    /** TODO(team): Seat Service must atomically hold AVAILABLE -> HELD for bookingId. */
    public String hold(String flightId,String seatId,String bookingId,String token){

        SeatHoldResponse response=http.post().uri("/seats/hold")
                .headers(h->h.setBearerAuth(token))
                .body(new SeatHoldRequest(flightId,seatId,bookingId)).retrieve().body(SeatHoldResponse.class);

        if(response==null || !response.held() || response.holdId()==null)
            throw new ApiException(HttpStatus.CONFLICT,"Seat unavailable");
        return response.holdId();
    }
    /** TODO(team): these operations must be idempotent; contract needs agreement.
     * Does Seat Service actually provide the endpoint /seats/holds/{id}/confirm?
     * What should happen if Booking Service sends the same confirmation request multiple times? */
    public void confirm(String holdId,String token){
        http.post().uri("/seats/holds/{id}/confirm",holdId)
                .headers(h->h.setBearerAuth(token)).retrieve().toBodilessEntity();
    }

    public void release(String holdId,String token){
        http.post().uri("/seats/holds/{id}/release",holdId)
                .headers(h->h.setBearerAuth(token)).retrieve().toBodilessEntity();
    }

    public void cancel(String bookingId,String token){
        http.post().uri("/seats/bookings/{id}/cancel",bookingId)
                .headers(h->h.setBearerAuth(token)).retrieve().toBodilessEntity();
    }
}
