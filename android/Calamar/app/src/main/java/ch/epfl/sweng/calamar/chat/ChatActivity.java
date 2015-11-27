package ch.epfl.sweng.calamar.chat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.epfl.sweng.calamar.BaseActivity;
import ch.epfl.sweng.calamar.CalamarApplication;
import ch.epfl.sweng.calamar.R;
import ch.epfl.sweng.calamar.SQLiteDatabaseHandler;
import ch.epfl.sweng.calamar.client.DatabaseClientException;
import ch.epfl.sweng.calamar.client.DatabaseClientLocator;
import ch.epfl.sweng.calamar.item.CreateItemActivity;
import ch.epfl.sweng.calamar.item.Item;
import ch.epfl.sweng.calamar.item.SimpleTextItem;
import ch.epfl.sweng.calamar.recipient.Recipient;
import ch.epfl.sweng.calamar.recipient.User;

//TODO Support other item types

/**
 * This activity manages the chat between two users (or in a group)
 */

public class ChatActivity extends BaseActivity {

    private static final String RECIPIENT_EXTRA_ID = "ID";
    private static final String RECIPIENT_EXTRA_NAME = "Name";
    private static final String TAG = ChatActivity.class.getSimpleName();


    private EditText editText;
    private Button sendButton;
    private Button refreshButton;
    private List<Item> messagesHistory;
    private ListView messagesContainer;
    private ChatAdapter adapter;

    private Recipient correspondent;

    private SQLiteDatabaseHandler databaseHandler;

    private CalamarApplication app;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intent = getIntent();
        String correspondentName = intent.getStringExtra(ChatFragment.EXTRA_CORRESPONDENT_NAME);
        int correspondentID = intent.getIntExtra(ChatFragment.EXTRA_CORRESPONDENT_ID, -1); // -1 = default value

        if (correspondentName == null) {
            correspondentName = "";
        }

        app = CalamarApplication.getInstance();
        correspondent = new User(correspondentID, correspondentName);

        editText = (EditText) findViewById(R.id.messageEdit);

        sendButton = (Button) findViewById(R.id.chatSendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendTextItem();
            }
        });

        refreshButton = (Button) findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh(false);
            }
        });

        messagesHistory = new ArrayList<>();
        messagesContainer = (ListView) findViewById(R.id.messagesContainer);
        adapter = new ChatAdapter(this, messagesHistory);
        messagesContainer.setAdapter(adapter);

        messagesContainer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Item item = messagesHistory.get(position);

                AlertDialog.Builder itemDescription = new AlertDialog.Builder(ChatActivity.this);
                itemDescription.setTitle(R.string.item_details_alertDialog_title);

                itemDescription.setPositiveButton(R.string.alert_dialog_default_positive_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //OK
                    }
                });

                itemDescription.setView(item.getView(ChatActivity.this));

                itemDescription.show();
            }
        });

        TextView recipient = (TextView) findViewById(R.id.recipientLabel);
        recipient.setText(correspondent.getName());

        databaseHandler = app.getDatabaseHandler();

        boolean offline = true;
        refresh(offline);
    }

    /**
     * Gets all messages and display them
     */
    private void refresh(boolean offline) {
        new RefreshTask(app.getCurrentUser(), offline, this).execute();
    }

    /**
     * Sends a new text message
     */
    private void sendTextItem() {
        String message = editText.getText().toString();
        Item textMessage = new SimpleTextItem(1, app.getCurrentUser(), correspondent, new Date(), message);
        adapter.notifyDataSetChanged();
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
        editText.setText("");
        new SendItemTask(textMessage).execute();
    }


    /**
     * Async task for sending a message.
     */
    private class SendItemTask extends AsyncTask<Void, Void, Item> {

        private final Item item;

        public SendItemTask(Item item) {
            this.item = item;
        }

        @Override
        protected Item doInBackground(Void... v) {
            try {
                return DatabaseClientLocator.getDatabaseClient().send(item);
            } catch (DatabaseClientException e) {
                Log.e(ChatActivity.TAG, e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Item item) {
            if (item != null) {
                adapter.add(item);
                messagesHistory.add(item);
                adapter.notifyDataSetChanged();
                messagesContainer.setSelection(messagesContainer.getCount() - 1);
                databaseHandler.addItem(item);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.item_send_error),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Async task for refreshing / getting new messages.
     */
    private class RefreshTask extends AsyncTask<Void, Void, List<Item>> {

        private final Recipient recipient;
        private final boolean offline;
        private final Activity context;

        public RefreshTask(Recipient recipient, boolean offline, Activity context) {
            if(null == recipient || null == context) {
                throw new IllegalArgumentException("ChatActivity.RefreshTask: recipient or context is null");
            }
            this.context = context;
            this.recipient = recipient;
            this.offline = offline;
        }

        @Override
        protected List<Item> doInBackground(Void... v) {
            if (offline) {
                return databaseHandler.getItemsForContact(correspondent);
            } else {
                try {
                    return DatabaseClientLocator.getDatabaseClient().getAllItems(recipient, app.getLastItemsRefresh());
                } catch (DatabaseClientException e) {
                    Log.e(ChatActivity.TAG, e.getMessage());
                    return null;
                }
            }
        }

        @Override
        protected void onPostExecute(List<Item> items) {
            if (items != null) {
                databaseHandler.addItems(items);
                adapter.add(items);
                messagesHistory.addAll(items);

                adapter.notifyDataSetChanged();
                messagesContainer.setSelection(messagesContainer.getCount() - 1);
                Toast.makeText(context, R.string.refresh_message,
                        Toast.LENGTH_SHORT).show();

            } else {
                // TODO same code used in multiple asynctask, ...
                Log.e(ChatActivity.TAG, "unable to refresh");
                // TODO once gave me : android.view.WindowManager$BadTokenException: Unable to add window -- token android.os.BinderProxy@4291e5a0 is not valid; is your activity running ?
                AlertDialog.Builder newUserAlert = new AlertDialog.Builder(context);
                newUserAlert.setTitle(R.string.unable_to_refresh_message);
                newUserAlert.setPositiveButton(R.string.alert_dialog_default_positive_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //OK
                    }
                });
                newUserAlert.show();
            }
        }

    }

    public void createItem(View v) {
        Intent intent = new Intent(this, CreateItemActivity.class);
        intent.putExtra(RECIPIENT_EXTRA_ID, correspondent.getID());
        intent.putExtra(RECIPIENT_EXTRA_NAME, correspondent.getName());
        startActivity(intent);
    }
}
