package com.walt;

import com.walt.dao.*;
import com.walt.model.*;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.event.annotation.AfterTestMethod;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

@SpringBootTest()
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WaltTest {

    @TestConfiguration
    static class WaltServiceImplTestContextConfiguration {

        @Bean
        public WaltService waltService() {
            return new WaltServiceImpl();
        }
    }

    @Autowired
    WaltService waltService;

    @Resource
    CityRepository cityRepository;

    @Resource
    CustomerRepository customerRepository;

    @Resource
    DriverRepository driverRepository;

    @Resource
    DeliveryRepository deliveryRepository;

    @Resource
    RestaurantRepository restaurantRepository;

    @BeforeEach()
    public void prepareData(){

        City jerusalem = new City("Jerusalem");
        City tlv = new City("Tel-Aviv");
        City bash = new City("Beer-Sheva");
        City haifa = new City("Haifa");

        cityRepository.save(jerusalem);
        cityRepository.save(tlv);
        cityRepository.save(bash);
        cityRepository.save(haifa);

        createDrivers(jerusalem, tlv, bash, haifa);

        createCustomers(jerusalem, tlv, haifa);

        createRestaurant(jerusalem, tlv);
    }

    private void createRestaurant(City jerusalem, City tlv) {
        Restaurant meat = new Restaurant("meat", jerusalem, "All meat restaurant");
        Restaurant vegan = new Restaurant("vegan", tlv, "Only vegan");
        Restaurant cafe = new Restaurant("cafe", tlv, "Coffee shop");
        Restaurant chinese = new Restaurant("chinese", tlv, "chinese restaurant");
        Restaurant mexican = new Restaurant("restaurant", tlv, "mexican restaurant ");

        restaurantRepository.saveAll(Lists.newArrayList(meat, vegan, cafe, chinese, mexican));
    }

    private void createCustomers(City jerusalem, City tlv, City haifa) {
        Customer beethoven = new Customer("Beethoven", tlv, "Ludwig van Beethoven");
        Customer mozart = new Customer("Mozart", jerusalem, "Wolfgang Amadeus Mozart");
        Customer chopin = new Customer("Chopin", haifa, "Frédéric François Chopin");
        Customer rachmaninoff = new Customer("Rachmaninoff", tlv, "Sergei Rachmaninoff");
        Customer bach = new Customer("Bach", tlv, "Sebastian Bach. Johann");

        customerRepository.saveAll(Lists.newArrayList(beethoven, mozart, chopin, rachmaninoff, bach));
    }

    private void createDrivers(City jerusalem, City tlv, City bash, City haifa) {
        Driver mary = new Driver("Mary", tlv);
        Driver patricia = new Driver("Patricia", tlv);
        Driver jennifer = new Driver("Jennifer", haifa);
        Driver james = new Driver("James", bash);
        Driver john = new Driver("John", bash);
        Driver robert = new Driver("Robert", jerusalem);
        Driver david = new Driver("David", jerusalem);
        Driver daniel = new Driver("Daniel", tlv);
        Driver noa = new Driver("Noa", haifa);
        Driver ofri = new Driver("Ofri", haifa);
        Driver nata = new Driver("Neta", jerusalem);

        driverRepository.saveAll(Lists.newArrayList(mary, patricia, jennifer, james, john, robert, david, daniel, noa, ofri, nata));
    }

    @Test
    public void testBasics(){

        assertEquals(((List<City>) cityRepository.findAll()).size(),4);
        assertEquals((driverRepository.findAllDriversByCity(cityRepository.findByName("Beer-Sheva")).size()), 2);

    }

    /**
     * Test to check the initial state of the delivery
     */
    @Test
    public void emptyDeliveryTest(){

        assertEquals(((List<Delivery>) deliveryRepository.findAll()).size(),0);

    }

    /**
     * Test for right case
     */
    @Test
    public void insertDeliveryTest(){

        insertDelivery("Mozart", "meat", new Date());
        assertEquals(((List<Delivery>) deliveryRepository.findAll()).size(),1);

    }

    /**
     * Test case when customer try order from restaurant that out of his range
     */
    @Test
    public void insertWrongDeliveryTest(){

        assertEquals(insertDelivery("Mozart", "vegan", new Date()),null);

    }

    /**
     * Test case to check if we get the same driver in city
     * we have 3 drivers in tlv:
     * in the first case we want to see that we get difference drivers.
     * in the second case we make 3 order at the same hour and after that we will try to make another order,
     * we except to get null delivery because all the drivers in the city are unavailable.
     */
    @Test
    public void insertToSameCitySameTimeTest(){

        Delivery firstDelivery = insertDelivery("Beethoven", "vegan", new Date());
        Delivery secondDelivery = insertDelivery("Bach", "cafe", new Date());
        assertEquals(firstDelivery.getDriver().getName().equals(secondDelivery.getDriver().getName()), false);
        Delivery thirdDelivery = insertDelivery("Rachmaninoff", "chinese", new Date());
        Delivery lastDelivery = insertDelivery("Beethoven", "restaurant", new Date());
        assertEquals(lastDelivery, null);

    }

    /**
     * Test case to check if we get the driver with the minimum distance covered
     * we have 3 drivers in tlv and we make 3 different delivers.
     * then we will create another delivery in tlv and we except to see that the driver who take it
     * is the driver that had the lower distance covered before the last delivery.
     */
    @Test
    public void insertToSameCityDiffTimeTest(){

        Delivery firstDelivery = insertDelivery("Beethoven", "vegan", new Date(2014, 02, 11));
        Delivery secondDelivery = insertDelivery("Bach", "cafe", new Date(2014, 02, 12));
        Delivery thirdDelivery = insertDelivery("Rachmaninoff", "chinese", new Date(2014, 02, 13));
        Delivery lastDelivery = insertDelivery("Beethoven", "restaurant", new Date(2014, 02, 14));

        double [] dist = new double[2];
        int i = 0;

        if(!firstDelivery.getDriver().getName().equals(lastDelivery.getDriver().getName())){
            dist[i] = firstDelivery.getDriver().getTotalDistance();
            i++;
        }
        if(!secondDelivery.getDriver().getName().equals(lastDelivery.getDriver().getName())){
            dist[i] = secondDelivery.getDriver().getTotalDistance();
            i++;
        }
        if(!thirdDelivery.getDriver().getName().equals(lastDelivery.getDriver().getName())){
            dist[i] = thirdDelivery.getDriver().getTotalDistance();
            i++;
        }

        double distanceBefore = lastDelivery.getDriver().getTotalDistance() - lastDelivery.getDistance();

        assertEquals(distanceBefore <= dist[0], true);
        assertEquals(distanceBefore <= dist[1], true);

    }

    /**
     * Test that checks the report on all drivers,
     * if it is sorted by the rating of the total distance covered by the driver.
     */
    @Test
    public void getRankedDriversTest(){

        insertDelivery("Beethoven", "vegan", new Date(2014, 02, 11));
        insertDelivery("Bach", "cafe", new Date(2014, 02, 12));
        insertDelivery("Mozart", "meat", new Date(2014, 02, 14));

        List<DriverDistance> rankedDrivers = waltService.getDriverRankReport();

        assertEquals(rankedDrivers.size(), 11);

        for (int i = 0; i < rankedDrivers.size() - 1; i++) {
            assertEquals(rankedDrivers.get(i).getDriver().getTotalDistance() >= rankedDrivers.get(i+1).getTotalDistance() ,true);
        }

    }

    /**
     *  Test that checks the report on all drivers in a given city,
     *  if it is sorted by the rating of the total distance covered by the driver.
     */
    @Test
    public void getRankedDriversInSameCityTest(){

        insertDelivery("Beethoven", "vegan", new Date(2014, 02, 11));
        insertDelivery("Bach", "cafe", new Date(2014, 02, 12));
        insertDelivery("Rachmaninoff", "chinese", new Date(2014, 02, 13));
        insertDelivery("Beethoven", "restaurant", new Date(2014, 02, 14));

        List<DriverDistance> rankedDrivers = waltService.getDriverRankReportByCity(cityRepository.findByName("Tel-Aviv"));

        assertEquals(rankedDrivers.size(), 3);


        for (int i = 0; i < rankedDrivers.size() - 1; i++) {
            assertEquals(rankedDrivers.get(i).getDriver().getTotalDistance() >= rankedDrivers.get(i+1).getTotalDistance() ,true);
            assertEquals(rankedDrivers.get(i).getDriver().getCity().getId() ,cityRepository.findByName("Tel-Aviv").getId());

        }

    }

    /**
     * Help function for create a delivery between given names of customer and restaurant
     * @param customer name of customer
     * @param restaurant name of restaurant
     * @return delivery with the order and the driver who made it.
     */
    private Delivery insertDelivery(String customer, String restaurant, Date date){
        Delivery deliveryTest = waltService.createOrderAndAssignDriver(customerRepository.findByName(customer), restaurantRepository.findByName(restaurant), date);
        return deliveryTest;
    }


}
