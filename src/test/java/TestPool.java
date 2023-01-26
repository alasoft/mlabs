import org.junit.Test;
import pool.GenericPool;
import resource.Person;

import java.util.List;

public class TestPool {

    private final GenericPool<Person> pool = new GenericPool<>();
    private final Person person = new Person(1, "firstName", "lastName");
    private final List<Person> persons = Person.generate(3);

    private void poolInitPerson() {
        pool.open();
        pool.clean();
        pool.add(this.person);
    }

    @Test
    public void testAddAcquireResource() throws InterruptedException {
        this.poolInitPerson();
        Person person = pool.acquire();
        assert (this.person.equals(person));
    }

    @Test
    public void testAddAcquireRelease() throws InterruptedException {
        this.poolInitPerson();
        Person person = pool.acquire();
        boolean b = pool.release(person);
        assert (b == true);
    }

}
