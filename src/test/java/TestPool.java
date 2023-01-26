import org.apache.log4j.Logger;
import org.junit.Test;
import pool.GenericPool;
import resource.Person;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class TestPool {

    static private final Logger logger = Logger.getLogger(TestPool.class);
    static private final Random random = new Random();
    static private final Integer personsQuantity = 10;

    private final GenericPool<Person> pool = new GenericPool<>();
    private final Person person = new Person(1, "firstName", "lastName");
    private final List<Person> persons = Person.generate(personsQuantity);

    private void poolInitPerson() {
        pool.open();
        pool.clean();
        pool.add(this.person);
    }

    // Trivial test
    @Test
    public void testAddAcquireResource() throws InterruptedException {
        this.poolInitPerson();
        Person person = pool.acquire();
        assert (this.person.equals(person));
    }

    // Trivial test
    @Test
    public void testAddAcquireRelease() throws InterruptedException {
        this.poolInitPerson();
        Person person = pool.acquire();
        boolean b = pool.release(person);
        assert (b == true);
    }

    // Async test, checking all persons added async are acquired syncronically (blocking acquire
    // with timeout)
    @Test
    public void testAcquireAllWhileAddingAsync() throws InterruptedException {

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                int i = random.nextInt(personsQuantity);
                Person person = persons.get(i);
                if (pool.add(person)) {
                    logger.info("pool.add() -> resource.Person added: " + person);
                }
            }
        };

        Timer timer = new Timer();
        long delay = 500L;
        long period = 1000L;
        timer.scheduleAtFixedRate(timerTask, delay, period);

        List<Person> acquiredList = new ArrayList<>();

        this.pool.open();

        Person person;

        while ((person = this.pool.acquire(10L, TimeUnit.SECONDS)) != null) {
            logger.info("pool.acquire() -> resource.Person acquired: " + person);
            acquiredList.add(person);
        }

        assert (this.persons.size() == this.pool.size());

    }

}
