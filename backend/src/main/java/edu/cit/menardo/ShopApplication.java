package edu.cit.menardo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Parent package, so component scanning picks up both
 * edu.cit.menardo.shop and edu.cit.menardo.inventory.
 */
@SpringBootApplication
public class ShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }
}
