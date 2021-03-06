package ch.epfl.sweng.calamar.chat;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ch.epfl.sweng.calamar.CalamarApplication;
import ch.epfl.sweng.calamar.R;
import ch.epfl.sweng.calamar.item.Item;

public final class ChatAdapter extends BaseAdapter {

    private final List<Item> messages;
    private final Activity context;

    public ChatAdapter(Activity context) {
        assert (context != null);
        this.context = context;
        this.messages = new ArrayList<>();

    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Item getItem(final int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        ViewHolder holder;
        final Item item = getItem(position);
        final LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = li.inflate(R.layout.list_chat_messages, null);
            holder = createViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        final boolean ingoing = item.getTo().getID() == CalamarApplication.getInstance().getCurrentUserID();
        setAlignment(holder, ingoing, item.getCondition().getValue());
        holder.itemView.removeAllViews();
        holder.itemView.addView(item.getPreView(context));
        holder.textTime.setText(item.getDate().toString());
        return convertView;
    }

    /**
     * Add a message to the adapter
     *
     * @param message the message to be added
     */
    public void add(Item message) {
        for (Item i : messages) {
            if (i.getID() == message.getID()) {
                return;
            }
        }
        this.messages.add(message);
        notifyDataSetChanged();
    }

    /**
     * Add a message to the adapter to a given position
     *
     * @param message  the message to be added
     * @param position the position of the new object
     */
    public void set(Item message, int position) {
        if (messages.get(position).getID() == message.getID()) {
            this.messages.set(position, message);
            notifyDataSetChanged();
        } else {
            for (Item i : messages) {
                if (i.getID() == message.getID()) {
                    return;
                }
            }
            this.messages.set(position, message);
            notifyDataSetChanged();
        }
    }

    /**
     * Add a list of messages to the adapter
     *
     * @param messages the list of messages
     */
    public void add(List<Item> messages) {
        for (Item m : messages) {
            add(m);
        }
        notifyDataSetChanged();
    }

    /**
     * Updates an item of the adapter (StorageManager has found the complete data)
     *
     * @param message the item to update
     */
    public void update(Item message) {
        boolean found = false;
        for (int i = 0; i < messages.size() && !found; ++i) {
            if (messages.get(i).getID() == message.getID()) {
                messages.set(i, message);
                found = true;
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Removes an item from the list
     *
     * @param message the message to delete
     */
    public void delete(Item message) {
        messages.remove(message);
        notifyDataSetChanged();
    }

    /**
     * Removes an item from the list with its id
     *
     * @param id the id of the message
     */
    public void delete(int id) {
        boolean removed = false;
        for (int i = 0; i < messages.size() && !removed; ++i) {
            if (messages.get(i).getID() == id) {
                messages.remove(i);
                removed = true;
            }
        }
    }

    /**
     * Returns a copy of the messages history
     *
     * @return the messages history
     */
    public List<Item> getHistory() {
        return new ArrayList<>(messages);
    }


    /**
     * Clear the chat messages.
     */
    public void clear() {
        messages.clear();
        notifyDataSetChanged();
    }


    /**
     * Set the alignment of the messages, depending on if the message is outgoing or ingoing.
     *
     * @param holder  The ViewHolder containing the necessary attributes
     * @param ingoing True if the message is received by the user, false if it is sent.
     */
    private void setAlignment(ViewHolder holder, boolean ingoing, boolean unlocked) {
        if (ingoing) {
            holder.contentWithBG.setBackgroundResource(unlocked ? R.drawable.in_message_bg : R.drawable.in_message_bg_lock);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.contentWithBG.getLayoutParams();
            layoutParams.gravity = Gravity.START;
            holder.contentWithBG.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams rLayoutParams = (RelativeLayout.LayoutParams) holder.content.getLayoutParams();
            if (Build.VERSION.SDK_INT < 17) {
                rLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                rLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            } else {
                rLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                rLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
            }
            holder.content.setLayoutParams(rLayoutParams);
            layoutParams = (LinearLayout.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.gravity = Gravity.START;
            holder.itemView.setLayoutParams(layoutParams);

            layoutParams = (LinearLayout.LayoutParams) holder.textTime.getLayoutParams();
            layoutParams.gravity = Gravity.START;
            holder.textTime.setLayoutParams(layoutParams);
        } else {
            holder.contentWithBG.setBackgroundResource(unlocked ? R.drawable.out_message_bg : R.drawable.out_message_bg_lock);
            LinearLayout.LayoutParams layoutParams =
                    (LinearLayout.LayoutParams) holder.contentWithBG.getLayoutParams();
            layoutParams.gravity = Gravity.END;
            holder.contentWithBG.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams rLayoutParams =
                    (RelativeLayout.LayoutParams) holder.content.getLayoutParams();
            if (Build.VERSION.SDK_INT < 17) {
                rLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                rLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            } else {
                rLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
                rLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            }
            holder.content.setLayoutParams(rLayoutParams);
            layoutParams = (LinearLayout.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.gravity = Gravity.END;
            holder.itemView.setLayoutParams(layoutParams);

            layoutParams = (LinearLayout.LayoutParams) holder.textTime.getLayoutParams();
            layoutParams.gravity = Gravity.END;
            holder.textTime.setLayoutParams(layoutParams);
        }
    }

    /**
     * Creates a ViewHolder containing the text of the message, the time, and two LinearLayout
     * (one for the whole message, and one for the text, contained in a "bubble")
     *
     * @param v The itemView holding those values
     * @return The newly created ViewHolder
     */
    private ViewHolder createViewHolder(View v) {
        final ViewHolder holder = new ViewHolder();
        holder.itemView = (FrameLayout) v.findViewById(R.id.itemView);
        holder.textTime = (TextView) v.findViewById(R.id.textTime);
        holder.content = (LinearLayout) v.findViewById(R.id.content);
        holder.contentWithBG = (LinearLayout) v.findViewById(R.id.contentWithBG);
        return holder;

    }

    /**
     * Avoids using findViewById too much (more efficient and readable)
     */
    private static class ViewHolder {
        protected FrameLayout itemView;
        protected TextView textTime;
        protected LinearLayout content;
        protected LinearLayout contentWithBG;
    }

}
