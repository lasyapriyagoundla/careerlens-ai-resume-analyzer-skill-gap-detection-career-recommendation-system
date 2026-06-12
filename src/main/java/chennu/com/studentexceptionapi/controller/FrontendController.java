 package chennu.com.studentexceptionapi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FrontendController {

    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "Application is working!";
    }
}