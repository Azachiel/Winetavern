package winetavern;

import org.springframework.beans.factory.annotation.Autowired;
import winetavern.model.user.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Niklas Wünsche
 */

public class Helper {
    public static <T> List<T> convertToList(Iterable<T> iterable) {
        ArrayList<T> returnList = new ArrayList<T>();
        iterable.forEach(item -> returnList.add(item));
        return returnList;
    }

    public static <T> T[] convertToArray(Iterable<T> iterable) {
        return (T[]) convertToList(iterable).toArray();
    }

    public static <T> T getFirstItem(Iterable<T> stream) {
        return convertToArray(stream)[0];
    }

    public static List<Person> findAllPersons(EmployeeManager employeeManager, ExternalManager externalManager) {
        List<Person> res = new ArrayList<>();
        employeeManager.findAll().forEach(res::add);
        externalManager.findAll().forEach(res::add);
        return res;
    }

    public static Optional<Person> findOnePerson(long personId, EmployeeManager employeeManager, ExternalManager externalManager) {
        Optional<Employee> optEmpl = employeeManager.findOne(personId);
        if (!optEmpl.isPresent()) {
            Optional<External> optExt = externalManager.findOne(personId);
            if (optExt.isPresent())
                return Optional.of(optExt.get());
        } else {
            return Optional.of(optEmpl.get());
        }
        return Optional.empty();
    }
}
