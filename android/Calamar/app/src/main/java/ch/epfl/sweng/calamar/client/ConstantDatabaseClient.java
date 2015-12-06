package ch.epfl.sweng.calamar.client;

import android.location.Location;

import com.google.android.gms.maps.model.VisibleRegion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.epfl.sweng.calamar.condition.Condition;
import ch.epfl.sweng.calamar.condition.PositionCondition;
import ch.epfl.sweng.calamar.item.Item;
import ch.epfl.sweng.calamar.item.SimpleTextItem;
import ch.epfl.sweng.calamar.recipient.Recipient;
import ch.epfl.sweng.calamar.recipient.User;

/**
 * Created by Quentin Jaquier, sciper 235825 on 23.10.2015.
 */
public class ConstantDatabaseClient implements DatabaseClient {

    private final User ALICE = new User(1, "Alice");
    private final User BOB = new User(2, "Bob");

    private final User  RANDOM = new User(3, "Random");

    List<Item> toRetrieve = new ArrayList<>();

    private final Location location = new Location("abc");

    private final Item itemFrom = new SimpleTextItem(1, ALICE, BOB, new Date(1445198510), Condition.trueCondition(), "Hello Bob, it's Alice !");
    private final Item itemTo = new SimpleTextItem(1, BOB, ALICE, new Date(1445198510), Condition.and(Condition.falseCondition(), new PositionCondition(location)), "Hello Alice, it's Bob !");
    private final Item randomMessage = new SimpleTextItem(1, RANDOM, BOB, new Date(1445198510), Condition.and(Condition.falseCondition(), new PositionCondition(location)), "Hello Bob, it's Random !");



    @Override
    public List<Item> getAllItems(Recipient recipient, Date from, VisibleRegion visibleRegion)
            throws DatabaseClientException {
        return getAllItems(null, null);
    }

    @Override
    public List<Item> getAllItems(Recipient recipient, Date from) throws DatabaseClientException {
        List<Item> items = new ArrayList<>();
        items.add(itemFrom);
        items.add(itemTo);
        items.add(randomMessage);
        items.addAll(toRetrieve);
        return items;
    }

    @Override
    public Item send(Item item) throws DatabaseClientException {
        return item;
    }

    @Override
    public User findUserByName(String name) throws DatabaseClientException {
        return new User(1, "Bob");
    }

    @Override
    public int newUser(String email, String deviceId) throws DatabaseClientException {
        return 0;
    }

    public void addItem(Item i) {
        toRetrieve.add(i);
    }
}
