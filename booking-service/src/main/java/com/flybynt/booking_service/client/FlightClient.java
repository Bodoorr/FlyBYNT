package com.flybynt.booking_service.client;

import com.flybynt.booking_service.dto.FlightResponse;
import com.flybynt.booking_service.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class FlightClient {
    private final RestClient http;
    public FlightClient(RestClient.Builder builder,@Value("${tripsync.flight-url}") String url){
        http=builder.baseUrl(url).build();
    }

    /** TODO(team): confirm path /flights/{id} and response schema. */
    public FlightResponse get(String id){
        FlightResponse f=http.get().uri("/flights/{id}",id).retrieve().body(FlightResponse.class);

        if(f==null)
            throw new ApiException(HttpStatus.BAD_GATEWAY,"Empty flight response");return f;
    }
}
