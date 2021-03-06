package winetavern.splitter;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Niklas Wünsche
 */
public class SplitBuilder<T> {

    private Collection<T> collection;

    public static <T> SplitBuilder<T> splitCollection(Collection<T> collection) {
        SplitBuilder<T> splitBuilder = new SplitBuilder<T>(collection);
        return splitBuilder;
    }

    private SplitBuilder(Collection<T> collection) {
        this.collection = collection;
    }

    public Splitter<T> splitBy(Predicate<T> splitBy) {
        Stream<T> passed = collection.stream().filter(splitBy);
        Stream<T> notPassed = collection.stream().filter(splitBy.negate());

        return new Splitter<T>(passed, notPassed);
    }

}
