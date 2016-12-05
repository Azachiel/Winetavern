package winetavern.model.accountancy;

import static org.salespointframework.core.Currencies.EURO;
import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.*;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.salespointframework.useraccount.UserAccount;
import winetavern.model.user.Employee;


/**
 * @author Niklas Wünsche
 */

public class ExpenseTests {

    private Expense expense;
    private Employee mockedEmployee;
    private ExpenseGroup mockedExpenseGroup;
    private String description;

    @Before
    public void before() {
        description = "Description";

        mockedEmployee = mock(Employee.class);
        mockedExpenseGroup = mock(ExpenseGroup.class);
        when(mockedExpenseGroup.getName()).thenReturn("Name");

        expense = new Expense(Money.of(3, EURO), description, mockedEmployee, mockedExpenseGroup);
    }

    @Test(expected = NullPointerException.class)
    public void throwIfEmployeeNull() {
        expense = new Expense(Money.of(3, EURO), description, null, mockedExpenseGroup);
    }

    @Test(expected = NullPointerException.class)
    public void throwIfExpenseGroupNull() {
        expense = new Expense(Money.of(3, EURO), description, mockedEmployee, null);
    }

    @Test
    public void coverWhenAbrechnung() {
        when(mockedExpenseGroup.getName()).thenReturn("Abrechnung");
        expense = new Expense(Money.of(3, EURO), description, mockedEmployee, mockedExpenseGroup);

        assertThat(expense.isCovered(), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void throwWhen2TimesCovered() {
        expense.cover();
        expense.cover();
    }

    @Test
    public void isCoveredRight() {
        assertThat(expense.isCovered(), is(false));

        expense.cover();
        assertThat(expense.isCovered(), is(true));
    }

    @Test
    public void compareRight() {
        Expense expense2 = setUpExpensesToCompare("Müller", "Schulz");

        assertThat(expense.compareTo(expense2), is(lessThan(0)));
    }

    private Expense setUpExpensesToCompare(String lastName1, String lastName2) {
        UserAccount mockedUserAccount1 = mock(UserAccount.class);
        UserAccount mockedUserAccount2 = mock(UserAccount.class);
        when(mockedUserAccount1.getLastname()).thenReturn(lastName1);
        when(mockedUserAccount2.getLastname()).thenReturn(lastName2);

        Employee mockedEmployee2 = mock(Employee.class);
        when(mockedEmployee.getUserAccount()).thenReturn(mockedUserAccount1);
        when(mockedEmployee2.getUserAccount()).thenReturn(mockedUserAccount2);

        Expense expense2 = new Expense(Money.of(3, EURO), "Abrechnung", mockedEmployee2, mockedExpenseGroup);

        return expense2;
    }

}