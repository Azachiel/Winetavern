package winetavern.model.menu;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.mockito.Mockito.*;
import static org.salespointframework.core.Currencies.EURO;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

/**
 * @author Niklas Wünsche
 */
public class DayMenuTests {

    // TODO PreRemove?
    private DayMenu dayMenu;

    @Before
    public void before() {
        dayMenu = new DayMenu(LocalDate.now());
    }

    @Test(expected = NullPointerException.class)
    public void throwWhenCalendarNull() {
        new DayMenu(null);
    }

    @Test(expected = NullPointerException.class)
    public void throwWhenSetCalendarNull() {
        dayMenu.setDay(null);
    }

    @Test
    public void getReadableDayRight() {

        assertThat(dayMenu.getReadableDay(), is(dayMenu.getDay().toString()));
    }

    @Test
    public void storeDayMenusRight() {
        addItemsToDayMenu();

        assertThat(dayMenu.getDayMenuItems().size(), is(2));
    }

    private DayMenuItem addItemsToDayMenu() {
        DayMenuItem mockedItem1 = mock(DayMenuItem.class);
        DayMenuItem mockedItem2 = mock(DayMenuItem.class);

        dayMenu.addMenuItem(mockedItem1);
        dayMenu.addMenuItem(mockedItem2);

        return mockedItem1;
    }

    @Test
    public void removeItemRight() {
        DayMenuItem mockedItem = addItemsToDayMenu();

        dayMenu.removeMenuItem(mockedItem);

        assertThat(dayMenu.getDayMenuItems().contains(dayMenu), is(false));
        assertThat(dayMenu.getDayMenuItems().size(), is(1));
    }

    @Test
    public void addAndRemoveDayMenuItemRight() {
        // TODO mocked Item + DayMenuItem tests
        DayMenuItem newItem = new DayMenuItem("Pepse", "Awesome", Money.of(2, EURO), 3.0);
        dayMenu.addMenuItem(newItem);

        assertThat(dayMenu.getDayMenuItems().contains(newItem), is(true));
        assertThat(newItem.getDayMenus().contains(dayMenu), is(true));

        dayMenu.removeMenuItem(newItem);

        assertThat(dayMenu.getDayMenuItems().contains(newItem), is(false));
        assertThat(newItem.getDayMenus().contains(dayMenu), is(false));

    }

}
