package com.walt.model;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;

@Entity
public class Driver extends NamedEntity {

    @ManyToOne
    City city;

    Date lastStartTimeDelivery;

    long totalDistance;

    public Driver(){}

    public Driver(String name, City city){
        super(name);
        this.city = city;
        lastStartTimeDelivery = null;
        totalDistance = 0;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public Date getLastStartTimeDelivery() {
        return lastStartTimeDelivery;
    }

    public void setLastStartTimeDelivery(Date lastStartTimeDelivery) {
        this.lastStartTimeDelivery = lastStartTimeDelivery;
    }

    public long getTotalDistance() {
        return totalDistance;
    }

    public void addDistance(long distance) {
        this.totalDistance += distance;
    }

    public boolean availableToDelivery(Date deliveryDate){
        if (this.lastStartTimeDelivery == null)
            return true;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.lastStartTimeDelivery);
        calendar.add(Calendar.HOUR, 1);
        Date previous_time = calendar.getTime();

        if(previous_time.before(deliveryDate)){
            return true;
        }

        return false;
    }
}
