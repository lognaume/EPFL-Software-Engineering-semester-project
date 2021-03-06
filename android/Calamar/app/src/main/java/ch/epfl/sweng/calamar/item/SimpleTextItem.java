package ch.epfl.sweng.calamar.item;

import android.app.Activity;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import ch.epfl.sweng.calamar.CalamarApplication;
import ch.epfl.sweng.calamar.R;
import ch.epfl.sweng.calamar.condition.Condition;
import ch.epfl.sweng.calamar.recipient.Recipient;
import ch.epfl.sweng.calamar.recipient.User;

/**
 * Models a simple text {@link Item}, (no special properties, just a text message).<br><br>
 * SimpleTextItem is immutable
 */
public final class SimpleTextItem extends Item {
    private final static Type ITEM_TYPE = Type.SIMPLETEXTITEM;

    /**
     * Instantiates a new SimpleTextItem with the following parameters
     *
     * @param ID        the ID
     * @param from      the 'from' field of the Item (sender)
     * @param to        the 'to' field of the Item (recipient)
     * @param date      the creation/posting date of the Item
     * @param message   the content (text message)
     * @param condition the condition
     * @see Item#Item(int, User, Recipient, Date, Condition, String)
     */

    public SimpleTextItem(int ID, User from, Recipient to, Date date, Condition condition, String message) {
        super(ID, from, to, date, condition, message);
        if (null == message || message.length() == 0) {
            throw new IllegalArgumentException(CalamarApplication.getInstance().getString(R.string.simpletext_message_null));
        }
    }

    public SimpleTextItem(int ID, User from, Recipient to, Date date, String message) {
        this(ID, from, to, date, Condition.trueCondition(), message);
    }

    @Override
    public Type getType() {
        return ITEM_TYPE;
    }

    @Override
    public View getItemView(Activity context) {
        return null;
    }

    /**
     * Appends the fields of SimpleTextItem to the given JSONObject. <br><br>
     * Should <b>NOT</b> be used alone
     *
     * @param json the json to which we append data
     * @throws JSONException
     */
    @Override
    protected void compose(JSONObject json) throws JSONException {
        super.compose(json);
        json.accumulate(JSON_TYPE, ITEM_TYPE.name());
    }

    /**
     * Parses a SimpleTextItem from a JSONObject.<br>
     *
     * @param json the well formed {@link JSONObject json} representing the {@link SimpleTextItem item}
     * @return a {@link SimpleTextItem} parsed from the JSONObject
     * @throws JSONException
     * @see Item#fromJSON(JSONObject) Recipient.fromJSON
     */

    public static SimpleTextItem fromJSON(JSONObject json) throws JSONException {
        return new SimpleTextItem.Builder().parse(json).build();
    }

    /**
     * @return a JSONObject representing a {@link SimpleTextItem}
     * @throws JSONException
     */
    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject ret = new JSONObject();
        this.compose(ret);
        return ret;
    }

    /**
     * test if this is equals to other Object, true when object is a SimpleTextItem and content truly equals
     *
     * @param o Obect to compare this with
     * @return true if two SimpleTextItems are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleTextItem)) return false;
        SimpleTextItem that = (SimpleTextItem) o;
        return super.equals(that);
    }

    /**
     * A Builder for {@link SimpleTextItem}, currently only used to parse JSON (little overkill..but ..)
     *
     * @see Item.Builder
     */
    protected static class Builder extends Item.Builder {

        @Override
        public Builder parse(JSONObject json) throws JSONException {
            super.parse(json);
            String type = json.getString(JSON_TYPE);
            if (!type.equals(SimpleTextItem.ITEM_TYPE.name())) {
                throw new IllegalArgumentException(CalamarApplication.getInstance().getString(R.string.expected_but_was, SimpleTextItem.ITEM_TYPE.name(), type));
            }
            return this;
        }

        @Override
        protected SimpleTextItem build() {
            return new SimpleTextItem(super.ID, super.from, super.to, super.date, super.condition, super.message);
        }
    }
}
