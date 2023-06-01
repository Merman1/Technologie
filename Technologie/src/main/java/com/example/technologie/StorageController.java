package com.example.technologie;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class StorageController {
    @RequestMapping("/home")
    public String index() {
        return "hello";
    }
}
