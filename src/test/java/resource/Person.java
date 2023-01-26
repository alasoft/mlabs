package resource;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// Resource class to try GenericPool
@Data
public class Person extends Object {

    private Integer id;
    private String firstName;
    private String lastName;

    public Person(Integer id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public static List<Person> generate(int q) {
        List<Person> list = new ArrayList<>();
        for (int i = 0; i < q; i++) {
            list.add(new Person(i, "firstName " + i, "lastName " + i));
        }
        return list;
    }

}
