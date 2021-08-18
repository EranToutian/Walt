package com.walt;

import com.walt.dao.CustomerRepository;
import com.walt.dao.DeliveryRepository;
import com.walt.dao.DriverRepository;
import com.walt.model.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;


@Service
public class WaltServiceImpl implements WaltService{

    @Resource
    DriverRepository driverRepository;

    @Resource
    DeliveryRepository deliveryRepository;

    @Resource
    CustomerRepository customerRepository;

    /**
     *  The function receives details of a customer, restaurant and the time when he wants to order the delivery.
     *  The function finds a available driver with the minimum distance covered and gives him the delivery.
     *  If there is no such driver or the customer orders from a restaurant outside his city, the function will register an appropriate message and return null.
     * @param customer
     * @param restaurant
     * @param deliveryTime
     * @return Delivery with all the details of the customer restaurant and driver that related to the delivery.
     */
    @Override
    public Delivery createOrderAndAssignDriver(Customer customer, Restaurant restaurant, Date deliveryTime) {

        /**
         * It was not clear whether an unregistered user should be allowed to place an order or not,
         * I chose not to let an order be made and make an appropriate comment.
         * If the intention was to let unregistered users also place an order then just delete this condition.
         */
        if(customerRepository.findByName(customer.getName()) == null)
        {
            WaltApplication.getLog().error("An unregistered user is trying to place an order.");
            return null;
        }

        City customerCity = customer.getCity();
        City restaurantCity = restaurant.getCity();

        /**
         * Check if the customer orders from a restaurant in his city
         **/
        if(!customerCity.getId().equals(restaurantCity.getId()))
        {
            WaltApplication.getLog().error("Customer tried to order from a restaurant that is out of his delivery range.");
            return null;
        }

        /**
         * Looking for a driver available in the city of delivery,
         * If null returned there is no available driver in the city.
         **/
        Driver driver = getAvailableDriverInCity(customerCity, deliveryTime);

        if(driver == null)
        {
            WaltApplication.getLog().error("There is no driver available in the city of delivery.");
            return null;
        }

        Random rand = new Random();

        long distanceToCustomer = rand.nextInt(20);

        Delivery delivery = new Delivery(driver, restaurant, customer, deliveryTime);
        delivery.setDistance(distanceToCustomer);

        driver.addDistance(distanceToCustomer);

        driver.setLastStartTimeDelivery(deliveryTime);


        WaltApplication.getLog().info("Driver " + driver.getName() + " made the delivery from "
                + restaurant.getName() + " restaurant to " + customer.getName() + " at " + deliveryTime);

        deliveryRepository.save(delivery);

        driverRepository.save(driver);

        return delivery;
    }

    /**
     * @return List of drivers sorted by the distance they covered (In descending order).
     */
    @Override
    public List<DriverDistance> getDriverRankReport() { return getSortedDriversByList(driverRepository.findAll()); }

    /**
     * @param city
     * @return List of drivers in a given city sorted by the distance they covered (In descending order).
     */
    @Override
    public List<DriverDistance> getDriverRankReportByCity(City city) { return getSortedDriversByList(driverRepository.findAllDriversByCity(city)); }


    /**
     * Helper function
     * @param drivers list of drivers
     * @return the drivers sorted by their covered distance.
     */
    private List<DriverDistance> getSortedDriversByList(Iterable<Driver> drivers){
        List<DriverDistance> driverDistances = new LinkedList<>();
        for (Driver driver:drivers)
        {
            DriverDistance driverDistance = new DriverDistance() {
                @Override
                public Driver getDriver() {
                    return driver;
                }

                @Override
                public Long getTotalDistance() {
                    return driver.getTotalDistance();
                }
            };
            driverDistances.add(driverDistance);
        }

        Collections.sort(driverDistances, new Comparator<DriverDistance>() {
            @Override
            public int compare(DriverDistance o1, DriverDistance o2) {
                return o1.getTotalDistance().compareTo(o2.getTotalDistance());
            }
        });

        Collections.reverse(driverDistances);

        return driverDistances;
    }

    /**
     * Help function to get available driver
     * @param city of the delivery
     * @param deliveryTime of the delivery
     * @return the driver with the minimum distance covered from all
     * the drivers that available in the city and the time of the delivery
     */
    private Driver getAvailableDriverInCity(City city, Date deliveryTime){
        List<Driver> drivers = driverRepository.findAllDriversByCity(city);
        Driver chosenDriver = null;
        for (Driver driver: drivers) {
            if(driver.availableToDelivery(deliveryTime))
            {
                if(chosenDriver == null)
                {
                    chosenDriver = driver;
                }else if(driver.getTotalDistance() < chosenDriver.getTotalDistance())
                {
                    chosenDriver = driver;
                }
            }
        }
        return chosenDriver;
    }


}
