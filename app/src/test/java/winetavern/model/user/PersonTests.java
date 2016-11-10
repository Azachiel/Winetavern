package winetavern.model.user;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

import org.junit.Before;
import org.junit.Test;
import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.UserAccountManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import winetavern.AbstractIntegrationTests;
import winetavern.model.DateParameter;

/**
 * Test class for {@link Person}
 */

@Transactional
public class PersonTests extends AbstractIntegrationTests{

    @Autowired private UserAccountManager userAccountManager;
    @Autowired private PersonManager personManager;

    private Person person;
    private Address address;
    private UserAccount acc;
    DateParameter birthday;

    @Before
    public void setUp() {
        acc = userAccountManager.create("testAccount", "1234", Role.of(Roles.SERVICE.getNameOfRoleWithPrefix()));
        address = new Address("Straße", "3a", "02374", "Berlin");
        birthday = new DateParameter();
        birthday.setYear(1980);
        birthday.setMonth(12);
        birthday.setDay(30);
    }

    @Test
    public void newPersonInDB() {
        person = new Person(acc, address, birthday);
        personManager.save(person);
        assertThat(personManager.findOne(person.getId()).isPresent(), is(true));
    }

    @Test
    public void newPersonInDBWithoutAddress() {
        address = new Address(null, null, null, null);
        person = new Person(acc, address, birthday);
        personManager.save(person);
        assertThat(personManager.findOne(person.getId()).get().getAddress().toString(), is(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwWhenPersonHas2Roles() {
        acc = userAccountManager.create("testAccount", "1234", Roles.SERVICE.getRole(), Roles.ACCOUNTANT.getRole());
        person = new Person(acc, address, birthday);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwWhenPersonHas0Roles() {
        acc = userAccountManager.create("testAccount", "1234");
        person = new Person(acc, address, birthday);
    }


}
