package winetavern.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by Michel on 11/4/2016.
 */
@Controller
public class MenuManager {
    @RequestMapping("/admin/menu/showMenus")
    public String showMenus() {
        return "daymenulist";
    }

    @RequestMapping("/admin/menu/addMenu")
    public String addMenu() {
        return "daymenulist";
    }

    @RequestMapping("/admin/menu/removeMenu")
    public String removeMenu() {
        return "daymenulist";
    }

    @RequestMapping("/admin/menu/getDayMenuByDay")
    public String getDayMenuByDay() {
        return "daymenu";
    }
}
