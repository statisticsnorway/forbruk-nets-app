package no.ssb.forbruk.nets.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

@RestController
public class GreetingController {

    private static final Logger logger = LoggerFactory.getLogger(GreetingController.class);

    private static final String TEMPLATE = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/greeting")
    public String[] greeting(@RequestParam(value = "name", defaultValue = "World") String name) {

        MDC.put("demo-appname", "forbruk-nets-app");
        MDC.put("demo-parameter", name);

        logger.info("A request was made to the 'greeting' end point. Have a nice day! ");

        MDC.clear();

        return new String[]{String.valueOf(counter.incrementAndGet()), String.format(TEMPLATE, name)};
    }
}
