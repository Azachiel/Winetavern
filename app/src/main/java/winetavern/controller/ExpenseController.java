package winetavern.controller;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.javamoney.moneta.Money;
import org.salespointframework.accountancy.Accountancy;
import org.salespointframework.accountancy.AccountancyEntry;
import org.salespointframework.time.BusinessTime;
import org.salespointframework.time.Interval;
import org.salespointframework.useraccount.AuthenticationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import winetavern.Helper;
import winetavern.model.accountancy.Expense;
import winetavern.model.accountancy.ExpenseGroup;
import winetavern.model.accountancy.ExpenseGroupRepository;
import winetavern.model.management.TimeInterval;
import winetavern.model.user.*;

import javax.money.MonetaryAmount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.salespointframework.core.Currencies.EURO;

/**
 * @author Louis
 */

@Controller
public class ExpenseController {
    @NonNull @Autowired private Accountancy accountancy;
    @NonNull @Autowired private ExpenseGroupRepository expenseGroups;
    @NonNull @Autowired private EmployeeManager employees;
    @NonNull @Autowired private ExternalManager externals;
    @NonNull @Autowired private PersonManager persons;
    @NonNull @Autowired private BusinessTime bt;
    @NonNull @Autowired private AuthenticationManager am;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * @param group   the ExpenseGroup (long id) to filter with (only remain expenses with this group)
     * @see          ExpenseGroup
     * @param person the Person (long id) to filter with
     * @see          Person
     * @param date   the interval in which the expense must lay in
     *               format: 'dd.MM.yyyy - dd.MM.yyyy'
     * @param cover  if present: cover multiple open expenses (the user checked the checkbox of at least one expense)
     *               format:     'expenseID|expenseId|...|'
     */
    @RequestMapping("/accountancy/expenses")
    public String showExpenses(@ModelAttribute(value="type") Optional<Long> group,
                               @ModelAttribute(value="person") Optional<Long> person,
                               @ModelAttribute(value="date") String date,
                               @ModelAttribute(value="cover") Optional<String> cover, Model model) {

        cover.ifPresent(query -> {
            String[] idQuery = query.split("\\|"); //split into multiple ExpenseID's
            Pair<Expense, MonetaryAmount> result = coverExpenses(idQuery); //the last expense and sum (@see method)

            Expense payoff = new Expense(result.getValue(),
                    "Abrechnung " + bt.getTime().format(formatter) + " durch " +
                            employees.findByUserAccount(am.getCurrentUser().get()).get(),
                    result.getKey().getPerson(),
                    expenseGroups.findByName("Abrechnung").get());

            accountancy.add(payoff);
        });

        Set<Expense> expOpen = filter(group, person, CoverStatus.OPEN, date); //all open expenses with the given filter
        Set<Expense> expCovered = filter(group, person, CoverStatus.CLOSED, date); //all covered expenses with the given filter

        model.addAttribute("expOpenAmount", expOpen.size());
        model.addAttribute("expCoveredAmount", expCovered.size());
        model.addAttribute("expOpen", expOpen);
        model.addAttribute("expCovered", expCovered);
        model.addAttribute("persons", Helper.findAllPersons(employees, externals)); //all employees and externals
        model.addAttribute("groups", expenseGroups.findAll());
        model.addAttribute("selectedType", group.orElse(0l));
        model.addAttribute("selectedPerson", group.orElse(0l));
        model.addAttribute("selectedDate", date);
        return "expenses";
    }

    /**
     * @param expensesToCover the array with expenses to cover given by each String expenseId
     * @return a pair <k, v> with:
     *         Expense k: the expense which was covered last or null if the array was empty
     *         MonetaryAmount v: the sum of all values from the expenses
     */
    private Pair<Expense, MonetaryAmount> coverExpenses(String[] expensesToCover) {
        Expense expense = null;
        MonetaryAmount sum = Money.of(0, EURO);

        for (String expenseId : expensesToCover) {
            expense = Helper.findOne(expenseId, accountancy);
            expense.cover();
            accountancy.add(expense);
            sum = sum.add(expense.getValue());
        }

        return new ImmutablePair<>(expense, sum);
    }

    /**
     * @see Expense
     * @param groupId   the ExpenseGroup (long id) to filter with (only remain expenses with this group)
     * @see            ExpenseGroup
     * @param personId the Person (long id) to filter with
     * @see            Person
     * @param covered  true: only return expenses which are covered
     *                 false: only return open expenses
     * @param interval     the interval in which the expense must lay in
     *                 format: 'dd.MM.yyyy - dd.MM.yyyy'
     * @return Set<Expense> the set filled with all expense that 'passed' the filter criteria
     */
    private Set<Expense> filter(Optional<Long> groupId, Optional<Long> personId, CoverStatus covered, String interval) {

        Optional<TimeInterval> parsedInterval = parseInterval(interval);

        Predicate<Expense> filterInterval = exp -> parsedInterval
                .map(time -> time.timeInInterval(exp.getDate().get()))
                .orElse(true);

        Predicate<Expense> filterGroup = exp -> groupId
                .map(id -> exp.getExpenseGroup().getId() == id.longValue())
                .orElse(true);

        Predicate<Expense> filterPerson = exp -> personId
                .map(id -> exp.getPerson().getId() == id.longValue())
                .orElse(true);

        Predicate<Expense> filterCovered = exp -> exp.isCovered() == covered.getStatus();

        return accountancy.findAll()
                .stream()
                .map(entry -> (Expense) entry)
                .filter(filterInterval)
                .filter(filterGroup)
                .filter(filterPerson)
                .filter(filterCovered)
                .collect(Collectors.toSet());
    }

    private Optional<TimeInterval> parseInterval(String date) {
        if(StringUtils.isBlank(date)) {
            return Optional.empty();
        } else if (date.equals("today")) {
            String currentBusinessTime = bt.getTime().format(formatter);
            String currentBusinessInterval = currentBusinessTime + " - " + currentBusinessTime;
            return Optional.of(parseStringToInterval(currentBusinessInterval));
        } else {
            return Optional.of(parseStringToInterval(date));
        }
    }

    /**
     * @implNote Interval will start at 00:00 of start and end at 24:00 of end
     */
    private TimeInterval parseStringToInterval(String interval) {
        String[] splitInterval = interval.split("(\\s-\\s)");
        LocalDateTime start = LocalDate.parse(splitInterval[0], formatter).atStartOfDay().withNano(1);
        LocalDateTime end = LocalDate.parse(splitInterval[1], formatter).atTime(23, 59, 59, 999999999);
        return new TimeInterval(start, end);
    }

    @RequestMapping("/accountancy/expenses/payoff")
    public String doPayoff(Model model) {
        Set<Employee> service = new TreeSet<>(Comparator.comparing(o -> o.getUserAccount().getLastname()));
        employees.findAll().forEach(service::add);
        model.addAttribute("service", service);
        return "payoff";
    }

    @PostMapping("/accountancy/expenses/payoff")
    public String redirectPayoff(@ModelAttribute("personId") String personId) {
        return "redirect:/accountancy/expenses/payoff/" + personId;
    }

    @RequestMapping("/accountancy/expenses/payoff/{pid}")
    public String doPayoffForPerson(@PathVariable("pid") Long personId, Model model) {
        Employee staff = employees.findOne(personId).get();
        Set<Expense> expenses = filter(idOfGroup("Bestellung"), Optional.of(personId), CoverStatus.OPEN, "");
        model.addAttribute("expenses", expenses);
        model.addAttribute("staff", staff);

        MonetaryAmount sum = expenses
                .stream()
                .map(Expense::getValue)
                .reduce(Money.of(0, EURO), MonetaryAmount::add);

        model.addAttribute("price", sum);
        return "payoff";
    }

    private Optional<Long> idOfGroup(String groupName) {
        return expenseGroups
                .findByName(groupName)
                .map(id -> Optional.of(id.getId()))
                    .orElse(Optional.empty());
    }

    @RequestMapping("/accountancy/expenses/payoff/{pid}/pay")
    public String coverExpensesForPerson(@PathVariable("pid") Long personId) {
        Set<Expense> expenses = filter(idOfGroup("Bestellung"), Optional.of(personId), CoverStatus.OPEN, "");

        MonetaryAmount sum = Money.of(0, EURO);
        for(Expense expense : expenses){
            sum = sum.add(expense.getValue());
            expense.cover();
            accountancy.add(expense);
        }

        accountancy.add(new Expense(sum,
                "Tagesabrechnung " + bt.getTime().toLocalDate().format(formatter) + " durch " +
                        employees.findByUserAccount(am.getCurrentUser().get()).get(),
                employees.findOne(personId).get(),
                expenseGroups.findByName("Abrechnung").get()));

        return "redirect:/accountancy/expenses/payoff";
    }

}

enum CoverStatus {

    CLOSED(true),
    OPEN(false);

    boolean status;

    CoverStatus(boolean status) {
        this.status = status;
    }

    public boolean getStatus() {
        return status;
    }

}