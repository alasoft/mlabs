import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Test {

    static private final Logger logger = LogManager.getLogger(Test.class);
    static private final Integer personsQuantity = 100;
    static private final Random random = new Random();

    private final GenericPool<Person> pool = new GenericPool<Person>();
    private final List<Person> persons = generatePersons();

    static private List<Person> generatePersons() {
        List<Person> list = new ArrayList<>();
        for (int i = 0; i < personsQuantity; i++) {
            list.add(new Person(i, "firstName " + i, "lastName " + i));
        }
        return list;
    }

    static public void main(String[] args) {
        new Test().test();
    }

    private void test() {
        this.pool.open();
        this.addPersonsTask();
        this.acquirePersonTask();
        this.releasePersonTask();
    }

    private void addPersonsTask() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                for (Person person : persons) {
                    if (pool.add(person)) {
                        logger.info("Person added: " + person);
                    }
                }
            }
        };

        Timer timer = new Timer();
        long delay = 1000L;
        long period = 1000L;
        timer.scheduleAtFixedRate(timerTask, delay, period);
    }

    private void acquirePersonTask() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    Person person = pool.acquire();
                    if (person != null) {
                        logger.info("Person acquired: " + person);
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

    private void releasePersonTask() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 10; i++) {
                        Person person = persons.get(random.nextInt(personsQuantity));
                        if (pool.release(person)) {
                            logger.info("Person released: " + person);
                        }
                    }
                } catch (
                        Exception e) {
                    logger.info(e.getMessage());
                }
            }
        };

        Timer timer = new Timer();
        long delay = 1000L;
        long period = 3000L;
        timer.scheduleAtFixedRate(timerTask, delay, period);
    }

}

@Data
class Person {

    private Integer id;
    private String firstName;
    private String lastName;

    public Person(Integer id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

}