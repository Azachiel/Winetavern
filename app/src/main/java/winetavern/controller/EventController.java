package winetavern.controller;

import lombok.NonNull;
import org.javamoney.moneta.Money;
import org.salespointframework.time.BusinessTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import winetavern.model.management.Event;
import winetavern.model.management.EventCatalog;
import winetavern.model.management.TimeInterval;
import winetavern.model.user.External;
import winetavern.model.user.ExternalManager;
import winetavern.model.user.Vintner;
import winetavern.model.user.VintnerManager;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.salespointframework.core.Currencies.EURO;

/**
 * @author Louis
 */

@Controller
public class EventController {
    @NonNull @Autowired private EventCatalog eventCatalog;
    @NonNull @Autowired private ExternalManager externals;
    @NonNull @Autowired private VintnerManager vintnerManager;
    @NonNull @Autowired private BusinessTime businessTime;
    private static final LocalDate dateToCreateVintnerDay = LocalDate.of(2014, 1, 3); //first friday in uneven months

    @RequestMapping("/admin/events")
    public String manageEvents(Model model) {

        //TODO filter for events in the past
        //TODO sort by time, next one first
        checkVintnerDays();
        Event event = eventCatalog.findAll().iterator().next();
        model.addAttribute("test",event.getPrice().getContext());
        model.addAttribute("eventAmount", eventCatalog.count());
        model.addAttribute("events", eventCatalog.findAll());
        model.addAttribute("calendarString", buildCalendarString());
        return "events";
    }

    private void checkVintnerDays() {
        LinkedList<Vintner> vintnerSequence = vintnerManager.findByActiveTrueOrderByPosition(); //active vintners sorted
        if (vintnerSequence.isEmpty())
            return;

        LinkedList<Event> vintnerDays = eventCatalog.findByVintnerDayTrue(); //all vintner days
        Event lastVintnerDay;

        if (vintnerDays.isEmpty()) { //no vintner day yet, begin to save it from 2016 on
            lastVintnerDay = createVintnerDay(vintnerSequence.getFirst(), dateToCreateVintnerDay);
            eventCatalog.save(lastVintnerDay);
            vintnerDays.add(lastVintnerDay);
        } else {
            vintnerDays.sort(Comparator.comparing(o -> o.getInterval().getStart()));
            lastVintnerDay = vintnerDays.getLast(); //get the last vintner day which is actually in the calendar
        }

        while (lastVintnerDay.getInterval().getStart().isBefore(businessTime.getTime())) {
            lastVintnerDay = createVintnerDay(getNextVintner(vintnerSequence, (Vintner) lastVintnerDay.getPerson()),
                    getNextVintnerDayDate(lastVintnerDay.getInterval().getStart().toLocalDate()));

            eventCatalog.save(lastVintnerDay);
        }
    }

    private Vintner getNextVintner(List<Vintner> vintners,Vintner lastVintner) {
        return vintners.get((vintners.indexOf(lastVintner) + 1) % vintners.size());
    }

    private LocalDate getNextVintnerDayDate(LocalDate lastDate) {
        LocalDate nextDate = lastDate.plusMonths(2).withDayOfMonth(1).with(dateToCreateVintnerDay.getDayOfWeek());
        if (nextDate.getMonthValue() % 2 == 0) //date slipped in last month => add one week to get first DayOfWeek in month
            nextDate = nextDate.plusWeeks(1);
        return nextDate;
    }

    /**
     *
     * @param vintner
     * @param date
     * @return
     */
    private Event createVintnerDay(Vintner vintner, LocalDate date) {
        Event vintnerDay = new Event("Weinprobe: " + vintner, Money.of(0, EURO),
                new TimeInterval(date.atStartOfDay(), date.atStartOfDay()),
                "Weinprobe mit Weinen von " + vintner + ". Alle Weine zum halben Preis!", vintner);
        vintnerDay.setVintnerDay(true);
        return vintnerDay;
    }

    /**
     * compiles all events into a String which can be parsed into an Object by JSON (javascript) and then put into the calendar
     * @return JSON parsable String
     */
    private String buildCalendarString() {
        String calendarString = "[";
        boolean noComma = true;

        for (Event event : eventCatalog.findAll()) {
            if (noComma)
                noComma = false;
            else
                calendarString = calendarString + ",";

            TimeInterval interval = event.getInterval();
            calendarString = calendarString +
                    "{\"title\":\"" + event.getName() +
                    "\",\"allDay\":\"" + event.isVintnerDay() +
                    "\",\"start\":\"" + interval.getStart() +
                    "\",\"end\":\"" + interval.getEnd() +
                    "\",\"url\":\"" + "/admin/events/change/" + event.getId() +
                    "\",\"description\":\"" + event.getDescription() + "<br/><br/>" + event.getPerson() +
                    "<br/>" + event.getPrice().getNumber().doubleValue() + "€" + "\"}";
        }

        return calendarString + "]";
    }

    @RequestMapping("/admin/events/add")
    public String showAddEventTemplate(Model model){
        model.addAttribute("externals",externals.findAll());
        return "addevent";
    }

    /**
    @param name         the name of the event
    @param desc         the description of the event
    @param date         the start and end time of the event. Format: 'dd.MM.yyyy HH:mm - dd.MM.yyyy HH:mm'
    @param price        the ticket price for the event (what the customer pays)
    @param external     the id of the external who is featured at this event (the one who gets payed)
                        '0' if a new external will be created
    @param externalName optional - the name of the external to create
    @param externalWage optional - the wage of the external to create
     */
    @PostMapping("/admin/events/add")
    public String addEvent(@RequestParam String name, @RequestParam String desc, @RequestParam String date,
                           @RequestParam String price, @RequestParam String external, @RequestParam String externalName,
                           @RequestParam String externalWage) {

        if (!date.isEmpty()) {
            DateTimeFormatter parser = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            String[] splittedDate = date.split("\\s-\\s");
            if (splittedDate.length == 2) {
                LocalDateTime start = LocalDateTime.parse(splittedDate[0], parser);
                LocalDateTime end = LocalDateTime.parse(splittedDate[1], parser);

                External externalPerson;
                if(external.equals("0")) { //external == '0' => create new external
                    externalPerson = new External(externalName, Money.of(Float.parseFloat(externalWage), EURO));
                } else { //external already exists
                    externalPerson = externals.findOne(Long.parseLong(external)).get();
                }

                eventCatalog.save(new Event(name, Money.of((Float.parseFloat(price)), EURO),
                                  new TimeInterval(start, end), desc, externalPerson));
            }
        }
        return "redirect:/admin/events";
    }

    @RequestMapping("/admin/events/change/{event}")
    public String showChangeModal(@PathVariable Event event, Model model){
        model.addAttribute("externals",externals.findAll());
        model.addAttribute("event",event);
        model.addAttribute("eventAmount", eventCatalog.count());
        model.addAttribute("events", eventCatalog.findAll());
        model.addAttribute("calendarString", buildCalendarString());
        return "events";
    }

    @PostMapping("/admin/events/change/{event}")
    public String changeEvent(@PathVariable Event event, @RequestParam String name, @RequestParam String desc, @RequestParam String date,
                              @RequestParam String price, @RequestParam String external, @RequestParam String externalName,
                              @RequestParam String externalWage){

        DateTimeFormatter parser = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String[] splittedDate = date.split("\\s-\\s");
        if (splittedDate.length == 2) {
            LocalDateTime start = LocalDateTime.parse(splittedDate[0], parser);
            LocalDateTime end = LocalDateTime.parse(splittedDate[1], parser);

            External externalPerson;
            if (external.equals("0")) { //external == '0' => create new external
                externalPerson = new External(externalName,Money.of(BigDecimal.valueOf(Double.parseDouble(externalWage)),
                        EURO));
            } else { //external already exists
                externalPerson = externals.findOne(Long.parseLong(external)).get();
            }

            event.setName(name);
            event.setDescription(desc);
            event.setInterval(new TimeInterval(start, end));
            event.setPrice(Money.of(Float.parseFloat(price),EURO));
            event.setPerson(externalPerson);
            eventCatalog.save(event);
        }

        return "redirect:/admin/events";
    }

    @RequestMapping("/admin/events/vintner")
    public String showVintner(@RequestParam Optional<String> query, Model model){
        if (query.isPresent()) {
            setVintnerSequence(query.get());
        }

        model.addAttribute("vintners", vintnerManager.findByActiveTrueOrderByPosition());
        return "vintner";
    }

    /**
     * splits the query String and saves all vintners in the exact same order by setting the position attribute
     * @see Vintner with attribut (int position), the position in the vintner evening sequence
     * @param query the String which contains all vintners to keep in the system (in that order)
     *              format: vintnerName|vintnerName|...|
     */
    private void setVintnerSequence(String query) {
        String[] vintnerNames = query.split("\\|"); //format: vintnerName|vintnerName|...|
        List<Vintner> vintnersToRemove = vintnerManager.findByActiveTrue();

        for (int i = 0; i < vintnerNames.length; i++) { //iterate through all vintners in the query
            Optional<Vintner> vintnerOptional = vintnerManager.findByName(vintnerNames[i]);

            if (vintnerOptional.isPresent()) { //the vintner already exists in the DB
                Vintner vintner = vintnerOptional.get();
                vintner.setPosition(i); //set vintners position in the vintner evening sequence
                vintner.setActive(true);
                vintnerManager.save(vintner);
                vintnersToRemove.remove(vintner);
            } else {
                vintnerManager.save(new Vintner(vintnerNames[i], i));
            }
        }

        for (Vintner vintnerToRemove : vintnersToRemove) { //hide all unused vintners - keep for events in the past
            vintnerToRemove.setActive(false);
            vintnerManager.save(vintnerToRemove);
        }
    }
}
