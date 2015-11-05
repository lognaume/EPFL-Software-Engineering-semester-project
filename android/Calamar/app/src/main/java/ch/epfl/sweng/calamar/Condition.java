package ch.epfl.sweng.calamar;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by pierre on 10/27/15.
 */
public abstract class Condition {
    /**
     * test if parameters obj satisfy condition
     * @return if obj match condition or not
     */
    public abstract boolean matches();

    /**
     * compose this Condition in the json object
     * @param json jsonObject to put this in
     * @throws JSONException
     */
    protected abstract void compose(JSONObject json) throws JSONException;

    /**
     * get a JSON description of this
     * @return JSONObject describing this
     * @throws JSONException
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject ret = new JSONObject();
        this.compose(ret);
        return ret;
    }

    /**
     * create a Condition from a JSONObject
     * @param json Object in JSON format
     * @return the desired condition Condition
     * @throws JSONException
     * @throws IllegalArgumentException
     */
    public static Condition fromJSON(JSONObject json) throws JSONException, IllegalArgumentException {
        if (null == json || json.isNull("type")) {
            throw new IllegalArgumentException("malformed json, either null or no 'type' value");
        }
        Condition cond;
        String type = json.getString("type");
        switch(type) {
            case "position":
                cond = PositionCondition.fromJSON(json);
                break;
            case "and":
                cond = and(fromJSON(json.getJSONObject("a")), fromJSON(json.getJSONObject("b")));
                break;
            case "or":
                cond = or(fromJSON(json.getJSONObject("a")), fromJSON(json.getJSONObject("b")));
                break;
            case "not":
                cond = not(fromJSON(json.getJSONObject("val")));
                break;
            case "true":
                cond = trueCondition();
            case "false":
                cond = falseCondition();
            default:
                throw new IllegalArgumentException("Unexpected Item type (" + type + ")");
        }
        return cond;
    }

    /**
     * Create an always true condition
     * @return true condition
     */
    public static Condition trueCondition()
    {
        return new Condition() {

            @Override
            public boolean matches() {
                return true;
            }

            @Override
            protected void compose(JSONObject json) throws JSONException {
                json.accumulate("type", "true");
            }
        };
    }

    /**
     * Create an always false condition
     * @return false condition
     */
    public static Condition falseCondition()
    {
        return new Condition() {

            @Override
            public boolean matches() {
                return false;
            }

            @Override
            protected void compose(JSONObject json) throws JSONException {
                json.accumulate("type", "false");
            }
        };
    }

    /**
     * create a condition that represent the intersection of two conditions
     * @param c1 first condition
     * @param c2 second condition
     * @return a new condition that is the intersection of c1 and c2
     */
    public static Condition and(final Condition c1, final Condition c2)
    {
        return new Condition() {
            @Override
            public boolean matches() {
                return c1.matches() && c2.matches();
            }

            @Override
            protected void compose(JSONObject json) throws JSONException {
                json.accumulate("type", "and");
                json.accumulate("a" , c1.toJSON());
                json.accumulate("b", c2.toJSON());
            }
        };
    }

    /**
     * create a condition that represent the union of two conditions
     * @param c1 first condition
     * @param c2 second condition
     * @return a new condition that is the union of c1 and c2
     */
    public static Condition or(final Condition c1, final Condition c2)
    {
        return new Condition() {
            @Override
            public boolean matches() {
                return c1.matches() || c2.matches();
            }

            @Override
            protected void compose(JSONObject json) throws JSONException {
                json.accumulate("type", "or");
                json.accumulate("a" , c1.toJSON());
                json.accumulate("b" , c2.toJSON());
            }
        };
    }

    /**
     * negats a condition
     * @param c condition to negate
     * @return a condition that is true when c is false and false when c is true
     */
    public static Condition not(final Condition c)
    {
        return new Condition() {
            @Override
            public boolean matches() {
                return !c.matches();
            }

            @Override
            protected void compose(JSONObject json) throws JSONException {
                json.accumulate("type", "not");
                json.accumulate("val" , c.toJSON());
            }
        };
    }

    /**
     * A Builder for {@link Condition}, has no build() method since Item isn't instantiable,
     * is used by the child builders (in {@link PositionCondition} or...) to build the "Condition
     * part of the object". currently only used to parse JSON
     */
    protected static class Builder {

        public Builder parse(JSONObject o) throws JSONException {
            return this;
        }
    }
}
