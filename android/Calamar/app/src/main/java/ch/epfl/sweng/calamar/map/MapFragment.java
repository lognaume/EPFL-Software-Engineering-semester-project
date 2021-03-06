package ch.epfl.sweng.calamar.map;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.epfl.sweng.calamar.BaseActivity;
import ch.epfl.sweng.calamar.CalamarApplication;
import ch.epfl.sweng.calamar.R;
import ch.epfl.sweng.calamar.client.DatabaseClientException;
import ch.epfl.sweng.calamar.client.DatabaseClientLocator;
import ch.epfl.sweng.calamar.condition.PositionCondition;
import ch.epfl.sweng.calamar.item.FileItem;
import ch.epfl.sweng.calamar.item.ImageItem;
import ch.epfl.sweng.calamar.item.Item;
import ch.epfl.sweng.calamar.recipient.User;
import ch.epfl.sweng.calamar.utils.StorageCallbacks;


/**
 * A simple {@link Fragment} subclass holding the calamar map !.
 */
public final class MapFragment extends android.support.v4.app.Fragment implements OnMapReadyCallback {

    public static final String LATITUDEKEY = PositionCondition.class.getCanonicalName() + ":LATITUDEKEY";
    public static final String LONGITUDEKEY = PositionCondition.class.getCanonicalName() + ":LONGITUDEKEY";

    // TODO maybe change for release or future
    public static final double DEFAULTLATITUDE = 46.518797; // guess ^^;
    public static final double DEFAULTLONGITUDE = 6.561908;

    private static final String TAG = MapFragment.class.getSimpleName();

    //When the condition is okay, we update the item description
    private final Item.Observer detailsItemObserver = new Item.Observer() {
        @Override
        public void update(Item item) {
            if (isAdded()) {
                //Update the dialog with the new view.
                View itemView = item.getView(getActivity());
                detailsViewDialog.removeAllViews();
                detailsViewDialog.addView(itemView);
            }
        }
    };

    // The condition is updated when the location change and if the value(true/false) of the
    // condition change -> The item is updated, if all are true
    // -> we get updated and update the value of the marker on the map.
    private final Item.Observer itemObserver = new Item.Observer() {
        @Override
        public void update(Item item) {
            if (isAdded()) {
                Marker updatedMarker = markers.get(item);
                updatedMarker.setTitle(getLockStringForItem(item));
                updatedMarker.setIcon(BitmapDescriptorFactory.fromResource(getLockIdForItem(item)));
            }
        }
    };

    // TODO : Use a bidirectional map ?
    private Map<Item, Marker> markers;
    private Map<Marker, Item> itemFromMarkers;
    private Set<Item> items;
    private LinearLayout detailsViewDialog;

    private GoogleMap map; // Might be null if Google Play services APK is not available.
    // however google play services are checked at app startup...and
    // maps fragment will do all the necessary if gplay services apk not present
    // see comment on setupMapIfNeeded
    // ....maybe delegate all the work to the map fragment, I think google has correctly done the job...


    private double initialLat;
    private double initialLong;

    // public static final String POSITIONKEY = MapFragment.class.getCanonicalName() + ":POSITION";

    // TODO : do we save state of fragment/map using a bundle ?

    public MapFragment() {
        // required
    }

    // *********************************************************************************************
    // map fragment lifecycle callbacks
    // https://developer.android.com/guide/components/fragments.html

    /**
     * Replaces the "onCreate" method from an Activity
     *
     * @param inflater           The LayoutInflater to be used
     * @param container          The parent container
     * @param savedInstanceState The saved state
     * @return The view of the fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        markers = new HashMap<>();
        itemFromMarkers = new HashMap<>();
        items = new HashSet<>();

        detailsViewDialog = new LinearLayout(getActivity());
        detailsViewDialog.setOrientation(LinearLayout.VERTICAL);

        Bundle args = getArguments();
        initialLat = args.getDouble(MapFragment.LATITUDEKEY);
        initialLong = args.getDouble(MapFragment.LONGITUDEKEY);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    // is called after activity.onStop !!!
    // https://developer.android.com/guide/components/fragments.html#Lifecycle
    @Override
    public void onResume() {
        super.onResume();

        if (isVisible()) {
            GPSProvider.getInstance().checkSettingsAndLaunchIfOK((BaseActivity) getActivity());
        }

        // REFRESH BUTTON
        View v = getView();
        if (v != null) {
            getView().findViewById(R.id.refreshButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addAllItemsInRegionToMap();
                }
            });
        } else {
            throw new IllegalStateException("View is null.");
        }


        //TODO : Make it more efficient.
        setUpMapIfNeeded();
        //We (indirectly) go through all items on the local database every times we resume on the
        //main activity, I don't think it's an efficient idea, but it's the easiest way to directly
        //update the map if we add a new private item.
        //The problem is that we add a new item from an other activity ( create Item activity ),
        //if we want to solve this problem, we have to find a way to only retrieve new private item.
        //The same problem occurs on the chat activity, in a less severe way.
        addAllPrivateItem();
    }

    @Override
    public void onPause() {
        super.onPause();
        // if provider started, stop
        GPSProvider.getInstance().stopLocationUpdates();
    }

    // map setup here :
    @Override
    public void onMapReady(final GoogleMap map) {
        this.map = map;
        map.setMyLocationEnabled(true);
        map.setOnMarkerClickListener(new MarkerWithStorageCallBackListener());
        LatLng initialLoc = new LatLng(initialLat, initialLong);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(initialLoc, 18.0f));

        addAllItemsInRegionToMap();
        addAllPrivateItem();
    }


    // *********************************************************************************************

    /**
     * Adds all private items in the visible region to the map
     */
    private void addAllPrivateItem() {
        if (null != map) {
            List<Item> localizedItems = CalamarApplication.getInstance().getDatabaseHandler().getAllLocalizedItems();
            for (Item i : localizedItems) {
                if (!items.contains(i)) {
                    addItemToMap(i);
                }
            }
        }
    }

    /**
     * Adds all items in the visible region to the map
     */
    private void addAllItemsInRegionToMap() {
        if (null != map) {
            VisibleRegion visibleRegion = map.getProjection().getVisibleRegion();
            new RefreshTask(visibleRegion, (BaseActivity) getActivity()).execute();
        } else {
            throw new IllegalStateException(CalamarApplication.getInstance().getString(R.string.map_refreshed_not_ready));
        }
    }

    /**
     * Add an item to the googleMap, and fill the map markers
     */
    private void addItemToMap(Item item) {
        if (!item.hasLocation()) {
            throw new IllegalStateException(CalamarApplication.getInstance().getString(R.string.item_map_no_location));
        }
        Location location = item.getLocation();

        MarkerOptions marker = new MarkerOptions()
                .position(new LatLng(location.getLatitude(), location.getLongitude()));
        marker.icon(BitmapDescriptorFactory.fromResource(getLockIdForItem(item)));

        marker.title(getLockStringForItem(item));
        item.addObserver(itemObserver);

        Marker finalMarker = map.addMarker(marker);
        markers.put(item, finalMarker);
        itemFromMarkers.put(finalMarker, item);
        items.add(item);
    }

    /**
     * Return the id of the lock (private/public and lock/unlock) for a given item.
     *
     * @param i Item for which we want the id.
     * @return id of the lock
     */
    private int getLockIdForItem(Item i) {
        if (i.getTo().getID() == User.PUBLIC_ID) {
            if (i.isLocked()) {
                return R.drawable.lock;
            } else {
                return R.drawable.unlock;
            }
        } else {
            //The item is private !
            if (i.isLocked()) {
                return R.drawable.lock_private;
            } else {
                return R.drawable.unlock_private;
            }
        }
    }

    /**
     * Returns a string depending on the state of the item
     *
     * @param item The item
     * @return the correct lock label for <i>item</i>
     */
    private String getLockStringForItem(Item item) {
        return item.isLocked() ? getString(R.string.label_locked_item) :
                getString(R.string.label_unlocked_item);
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * access map once when {@link #map} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map))
                    .getMapAsync(this);
        }
    }

    /**
     * Async task for refreshing / getting new localized items.
     */
    private class RefreshTask extends AsyncTask<Void, Void, List<Item>> {

        private final VisibleRegion visibleRegion;
        private final BaseActivity context;

        public RefreshTask(VisibleRegion visibleRegion, BaseActivity context) {
            if (null == visibleRegion || null == context) {
                throw new IllegalArgumentException(CalamarApplication.getInstance().getString(R.string.refreshtask_visibleregion_or_context_null));
            }
            this.visibleRegion = visibleRegion;
            this.context = context;
        }

        @Override
        protected List<Item> doInBackground(Void... v) {
            try {
                // TODO enhancement: custom request specifying the items already fetched
                // + bundle saving state of fragment
                // now all items in region are fetched, and if not already in set, displayed
                // on map
                return DatabaseClientLocator.getDatabaseClient().getAllItems(
                        CalamarApplication.getInstance().getCurrentUser(),
                        new Date(0),
                        visibleRegion);

            } catch (DatabaseClientException e) {
                Log.e(MapFragment.TAG, e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Item> receivedItems) {
            if (receivedItems != null) {

                for (Item item : receivedItems) {
                    if (!items.contains(item)) {
                        addItemToMap(item);
                    }
                }

                Log.i(MapFragment.TAG, context.getString(R.string.map_refreshed));

                Toast.makeText(context, R.string.refresh_message,
                        Toast.LENGTH_SHORT).show();
            } else {
                if (isAdded()) {
                    context.displayErrorMessage(getString(R.string.unable_to_refresh_message), false);
                }
            }
        }
    }


    private class MarkerWithStorageCallBackListener implements GoogleMap.OnMarkerClickListener, StorageCallbacks {

        private AlertDialog dialog;
        private Item item;

        @Override
        public boolean onMarkerClick(Marker marker) {
            final Item item = itemFromMarkers.get(marker);
            this.item = item;
            CalamarApplication.getInstance().getStorageManager().getCompleteItem(item, this);

            AlertDialog.Builder itemDescription = new AlertDialog.Builder(getActivity());
            itemDescription.setTitle(R.string.item_details_alertDialog_title);

            itemDescription.setPositiveButton(R.string.alert_dialog_default_positive_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    item.removeObserver(detailsItemObserver);
                }
            });

            //OnCancel is called when we press the back button.
            itemDescription.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    item.removeObserver(detailsItemObserver);
                }
            });

            //Create a new view
            detailsViewDialog = new LinearLayout(getActivity());
            detailsViewDialog.setOrientation(LinearLayout.VERTICAL);

            detailsViewDialog.addView(this.item.getView(getActivity()));


            itemDescription.setView(detailsViewDialog);

            item.addObserver(detailsItemObserver);

            dialog = itemDescription.show();
            return false;
        }


        @Override
        public void onItemRetrieved(Item i) {
            item = i;
            //getCompleteItem may return item before dialog is created
            if (dialog != null) {
                dialog.setView(item.getView(MapFragment.this.getActivity()));
            }
        }

        @Override
        public void onDataRetrieved(byte[] data) {
            switch (item.getType()) {
                case SIMPLETEXTITEM:
                    break;
                case FILEITEM:
                    item = new FileItem(item.getID(), item.getFrom(), item.getTo(), item.getDate(), item.getCondition(), data, ((FileItem) item).getPath());
                    break;
                case IMAGEITEM:
                    item = new ImageItem(item.getID(), item.getFrom(), item.getTo(), item.getDate(), item.getCondition(), data, ((ImageItem) item).getPath());
                    break;
                default:
                    throw new IllegalArgumentException(CalamarApplication.getInstance().getString(R.string.unexpected_item_type, item.getType().name()));
            }
            dialog.setView(item.getView(MapFragment.this.getActivity()));
        }
    }

}
