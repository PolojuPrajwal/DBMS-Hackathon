package com.smartparking;

import com.smartparking.model.*;
import com.smartparking.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class SmartParkingApplication {
    public static void main(String[] args) { SpringApplication.run(SmartParkingApplication.class, args); }

    @Bean WebMvcConfigurer cors() {
        return new WebMvcConfigurer() {
            public void addCorsMappings(CorsRegistry r) { r.addMapping("/api/**").allowedOrigins("*").allowedMethods("*").allowedHeaders("*"); }
        };
    }

    @Bean CommandLineRunner seed(LocationRepository locs, SlotRepository slots) {
        return a -> {
            if (locs.count() > 0) return;
            for (String n : new String[]{"City Mall Parking", "Railway Station Lot"}) {
                Location l = new Location(); l.setName(n); l.setAddress(n + ", Main Road"); l.setRatePerHour(n.startsWith("City") ? 30 : 20); locs.save(l);
                for (int i = 1; i <= 10; i++) {
                    Slot s = new Slot(); s.setLocation(l); s.setCode("S" + i);
                    s.setType(i <= 2 ? "EV" : "CAR"); slots.save(s);
                }
            }
        };
    }

    @Bean CommandLineRunner seedUsers(UserRepository users, PasswordEncoder enc) {
        return a -> {
            if (users.count() > 0) return;
            users.save(new AppUser(null, "admin", enc.encode("admin123"), "ADMIN"));
            users.save(new AppUser(null, "user", enc.encode("user123"), "USER"));
        };
    }
}
