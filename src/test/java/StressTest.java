import org.apache.log4j.Logger;
import pool.GenericPool;
import resource.Person;

import java.util.*;

// This class does not implement formally a 'test'. Instead it allows to try the GenericPool.java class under
// stress conditions, meaning running periodic tasks wich randomly add, acquire, release and remove
// some resources, logging the result of these operations on console and external file.
// The stress context is produced by the periodicity of each task. Every second each of these tasks is
// executed, putting a test on the GenericPool.class.
public class StressTest {

    static private final Logger logger = Logger.getLogger(StressTest.class);
    static private final Integer personsQuantity = 100;
    static private final Random random = new Random();

    private final GenericPool<Person> pool = new GenericPool<Person>();
    private final List<Person> persons = Person.generate(personsQuantity);

    static public void main(String[] args) {
        new StressTest().test();
    }

    // Brute force test
    private void test() {
        this.pool.open();
        this.addPersonsTask();
        this.acquirePersonTask();
        this.releasePersonTask();
        this.removePersonTask();
    }

    // Add all persons to pool, provided they are not already added.
    // When other taks delete some person, this taks will add that person again
    private void addPersonsTask() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                for (Person person : persons) {
                    if (pool.add(person)) {
                        logger.info("pool.add() -> resource.Person added: " + person);
                    }
                }
            }
        };

        Timer timer = new Timer();
        long delay = 1000L;
        long period = 1000L;
        timer.scheduleAtFixedRate(timerTask, delay, period);
    }

    // Regulary try to acquire some 'free' resource (not already acquired)
    private void acquirePersonTask() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    Person person = pool.acquire();
                    if (person != null) {
                        logger.info("pool.acquire() -> resource.Person acquired: " + person);
                    }
                } catch (Exception e) {
                    logger.info(e.getMessage());
                }

            }
        };

        Timer timer = new Timer();
        long delay = 1000L;
        long period = 1000L;
        timer.scheduleAtFixedRate(timerTask, delay, period);
    }

    // Regulary releasing random resources (persons)
    private void releasePersonTask() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 5; i++) {
                        Person person = persons.get(random.nextInt(personsQuantity));
                        if (person != null && pool.release(person)) {
                            logger.info("pool.release() -> resource.Person released: " + person);
                        }
                    }
                } catch (Exception e) {
                    logger.info(e.getMessage());
                }
            }
        };

        Timer timer = new Timer();
        long delay = 1000L;
        long period = 1000L;
        timer.scheduleAtFixedRate(timerTask, delay, period);
    }

    // Regulary removing random persons from the pool (wich will then be added again, by the 'add' task above)
    private void removePersonTask() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 5; i++) {
                        Person person = persons.get(random.nextInt(personsQuantity));
                        if (person != null && pool.remove(person)) {
                            logger.info("pool.remove() -> resource.Person removed: " + person);
                        }
                    }
                } catch (Exception e) {
                    logger.info(e.getMessage());
                }
            }
        };

        Timer timer = new Timer();
        long delay = 1000L;
        long period = 1000L;
        timer.scheduleAtFixedRate(timerTask, delay, period);
    }

}

