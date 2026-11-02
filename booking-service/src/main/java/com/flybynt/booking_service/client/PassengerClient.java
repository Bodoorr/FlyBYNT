package com.flybynt.booking_service.client;

import com.flybynt.booking_service.dto.PassengerResponse;
import com.flybynt.booking_service.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
@Component
public class PassengerClient {
    private final RestClient http;
    public PassengerClient(RestClient.Builder builder,@Value("${tripsync.passenger-url}") String url){
        http=builder.baseUrl(url).build();
    }

    /**
     * Passenger Service checks ownership itself: it returns the passenger only to the user who owns it
     * (the caller's JWT subject) and 404 otherwise, so a 200 with the same id means verified.
     */
    public void verify(String passengerId,String userId,String bearer){
        PassengerResponse p;
        try {
            p=http.get().uri("/passengers/{id}",passengerId)
                    .headers(h->h.setBearerAuth(bearer)).retrieve().body(PassengerResponse.class);
        }
        catch(HttpClientErrorException.NotFound ex){
            throw new ApiException(HttpStatus.NOT_FOUND,"Passenger not found");
        }
        if(p==null || !passengerId.equals(p.id()))
            throw new ApiException(HttpStatus.FORBIDDEN,"Passenger not authorized");
    }
}
