package com.flybynt.booking_service.concurrency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;
/** JVM-only lock for single-instance MVP. NOT distributed; keys retained for process lifetime. */
@Component
public class PassengerLockManager {

    private final ConcurrentHashMap<String,ReentrantLock> locks=new ConcurrentHashMap<>();

    public ReentrantLock forPassenger(String passengerId){
        return locks.computeIfAbsent(passengerId,id->new ReentrantLock());
    }
}
