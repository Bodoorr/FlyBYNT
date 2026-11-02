package com.flybynt.booking_service.client;

import com.flybynt.booking_service.dto.PassengerResponse;
import com.flybynt.booking_service.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class PassengerClient {
    private final RestClient http;
    public PassengerClient(RestClient.Builder builder,@Value("${tripsync.passenger-url}") String url){
        http=builder.baseUrl(url).build();
    }

    /** TODO(team): must pass caller JWT and validate passenger ownership in Passenger Service. */
    public void verify(String passengerId,String userId,String bearer){
        PassengerResponse p=http.get().uri("/passengers/{id}",passengerId)
                .headers(h->h.setBearerAuth(bearer)).retrieve().body(PassengerResponse.class);
        if(p==null || !passengerId.equals(p.id()) || !p.authorized())

            throw new ApiException(HttpStatus.FORBIDDEN,"Passenger not authorized");
    }
}
