package ch.epfl.sweng.calamar.client;

import com.google.android.gms.maps.model.VisibleRegion;

import java.util.Date;
import java.util.List;

import ch.epfl.sweng.calamar.item.Item;
import ch.epfl.sweng.calamar.recipient.Recipient;
import ch.epfl.sweng.calamar.recipient.User;

/**
 * Created by LPI on 19.10.2015.
 */
public interface DatabaseClient {
    /**
     * gets from a database all items that have <i>recipient</i> into their recipient field
     * AND whose date is greater than <i>from</i>
     * AND that are located near <i>nearLocation</i>
     *
     * @param recipient     the items we want must have recipient in their destination field
     * @param from          the items have been sent after from
     * @param visibleRegion the square visible region of the map
     * @return a list of {@link Item items}
     */
    List<Item> getAllItems(Recipient recipient, Date from, VisibleRegion visibleRegion)
            throws DatabaseClientException;


    /**
     * gets from a database all items that have <i>recipient</i> into their recipient field
     * AND whose date is greater than <i>from</i>
     *
     * @param recipient the items we want must have recipient in their destination field
     * @param from      the items have been sent after from
     * @return a list of {@link Item items}
     */
    List<Item> getAllItems(Recipient recipient, Date from) throws DatabaseClientException;

    /**
     * send an item to a database
     *
     * @param item the item to send
     */
    Item send(Item item) throws DatabaseClientException;


    /**
     * Retrieve an user from the server from his name.
     *
     * @param name The name of the user
     */
    User findUserByName(String name) throws DatabaseClientException;

    /**
     * Create a new user on the database
     *
     * @param email email of the new user.
     * @param token registration token of the new user .
     * @return the id of the new user given by the database
     * @throws DatabaseClientException
     */

    int newUser(String email, String token) throws DatabaseClientException;
}
