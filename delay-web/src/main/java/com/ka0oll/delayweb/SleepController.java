package com.ka0oll.delayweb;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("")
public class SleepController {


    @GetMapping("/sleep")
    public String sleep() throws InterruptedException {
        Thread.sleep(2000);
        return "sleep";
    }

}
