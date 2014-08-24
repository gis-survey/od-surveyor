package com.meyersj.locationsurvey.app.stops;

import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.meyersj.locationsurvey.app.PostService;
import com.meyersj.locationsurvey.app.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
//import android.support.v7.internal.widget.AdapterViewICS;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.ItemizedIconOverlay;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MBTilesLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.meyersj.locationsurvey.app.util.PathUtils;
import com.meyersj.locationsurvey.app.util.Utils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class OnOffMapActivity extends ActionBarActivity {
    private final String TAG = "OnOffMapActivity";
    private final String ODK_ACTION = "com.meyersj.locationsurvey.app.ODK_ONOFFMAP";
    private final String ONOFF_ACTION = "com.meyersj.locationsurvey.app.ONOFFMAP";
    private final String URL = "url";
    private final String LINE = "rte";
    private final String DIR = "dir";
    private final String DATE = "date";
    private final String ON_STOP = "on_stop";
    private final String OFF_STOP = "off_stop";
    private final String TYPE = "type";
    private final String ODK_BOARD = "board_id";
    private final String ODK_ALIGHT = "alight_id";
    private static final String USER_ID = "user_id";

    private final File TILESPATH = new File(Environment.getExternalStorageDirectory(), "maps/mbtiles");
    private final File GEOJSONPATH = new File(Environment.getExternalStorageDirectory(), "maps/geojson/trimet");
    private final String TILES = "OSMTriMet.mbtiles";
    private final String BOARD = "Boarding";
    private final String ALIGHT = "Alighting";

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Context context;
    private Button submit;
    private MapView mv;
    private String line;
    private String dir;
    private String url;
    private String user_id;


    private Marker board;
    private Marker alight;
    private Marker current;


    private String locType;
    private Drawable onIcon;
    private Drawable offIcon;
    private Drawable stopIcon;
    private AutoCompleteTextView stopName;
    private ImageButton clear;




    // for drop down stop sequence lists
    private ListView onSeqListView;
    private ListView offSeqListView;
    private Button onSeqBtn;
    private Button offSeqBtn;
    private StopSequenceAdapter onSeqListAdapter;
    private StopSequenceAdapter offSeqListAdapter;


    private ItemizedIconOverlay locOverlay;
    private ArrayList<Marker> locList = new ArrayList<Marker>();
    private BoundingBox bbox;
    private HashMap<String, Marker> stopsMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_off_map);

        context = getApplicationContext();
        onIcon = getResources().getDrawable(R.drawable.transit_green_40);
        offIcon = getResources().getDrawable(R.drawable.transit_red_40);
        stopIcon = getResources().getDrawable(R.drawable.circle_filled_black_30);
        //stopIcon = getResources().getDrawable(R.drawable.circle);

        //clear = (Button) findViewById(R.id.clear);
        submit = (Button) findViewById(R.id.submit);
        mv = (MapView) findViewById(R.id.mapview);
        clear = (ImageButton) findViewById(R.id.clear_input_stop);

        stopName = (AutoCompleteTextView) findViewById(R.id.input_stop);

        stopName.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                stopName.selectAll();
                return false;
            }
        });


        onSeqBtn = (Button) findViewById(R.id.on_stops_btn);
        offSeqBtn = (Button) findViewById(R.id.off_stops_btn);

        onSeqListView = (ListView) findViewById(R.id.on_stops_seq);
        offSeqListView = (ListView) findViewById(R.id.off_stops_seq);


        //mv.setMapViewListener(new MyMapViewListener());

        getExtras();
        setTiles(mv);



        if (line != null && dir != null) {
            Log.d(TAG, "getStops");
            locList = getStops(line, dir);

            // set listener for when marker tooltip is selected
            for (Marker marker: locList)
                setToolTipListener(marker);

            if(bbox == null)
                Log.d(TAG, "bbox null");
            else
                mv.zoomToBoundingBox(bbox, true, false, true, true);

            setItemizedOverlay(mv, locList);
            addRoute(line, dir);

            //TODO
            //TODO
            //TODO

            ArrayList<Stop> stops = prepareListData(locList);

            onSeqListAdapter = new StopSequenceAdapter(this, stops);
            offSeqListAdapter = new StopSequenceAdapter(this, stops);

            stopSequenceSetup(onSeqListView, onSeqListAdapter, onSeqBtn);
            stopSequenceSetup(offSeqListView, offSeqListAdapter, offSeqBtn);


            //TODO
            //TODO
            //TODO


            String[] stopNames = buildStopsArray(locList);
            //String[] stopNames = {"N Lombard TC MAX Station", "SW 6th & Madison St MAX Station","13123", "11512" };
            final ArrayList<String> stopsList = new ArrayList<String>();
            Collections.addAll(stopsList, stopNames);

            StopSearchAdapter adapter = new StopSearchAdapter
                    (this,android.R.layout.simple_list_item_1,stopsList);
            stopName.setAdapter(adapter);


            stopName.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                                        long id) {
                    stopName.setText("");

                    //close keypad
                    InputMethodManager inputManager = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);

                    selectLocType(stopsMap.get(stopsList.get(position)));
                }
            });

            clear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopName.clearListSelection();
                    stopName.setText("");
                }
            });

        }

        MarkerPopUpListener listener = new MarkerPopUpListener(mv, locList);
        mv.addListener(listener);

        Intent i = this.getIntent();
        String action = i.getAction();


        if (action.equals(ONOFF_ACTION)) {
            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (validSelection(board, alight)) {
                        //verify correct locations
                        verifyAndSubmitLocationsPOST();
                    }
                }
            });
        }
        else if (action.equals(ODK_ACTION)) {
            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (validSelection(board, alight)) {
                        //verifyAndSubmitLocationsODK();
                        String onStop = board.getDescription();
                        String offStop = alight.getDescription();
                        exitWithStopIDs(onStop, offStop);
                    }
                }
            });
        }

        if (!Utils.isNetworkAvailable(getApplicationContext())) {
            Toast toast = Toast.makeText(getApplicationContext(), "Please enable network connections.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }


    protected void stopSequenceSetup(final ListView listView, final StopSequenceAdapter adapter, final Button button) {
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                adapter.setSelectedIndex(position);

                Stop stop = (Stop) adapterView.getAdapter().getItem(position);


                if (listView == onSeqListView) {
                    Log.d(TAG, "on: " + stop.getDescription());

                    if (board != null) {
                        Log.d(TAG, "board not null");
                    }

                    saveSequenceMarker(BOARD, stop);

                }
                else {
                    saveSequenceMarker(ALIGHT, stop);
                }

                Toast.makeText(getBaseContext(), stop.getDesc(), Toast.LENGTH_SHORT)
                        .show();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                if (listView.getVisibility() == View.INVISIBLE) {
                    listView.setVisibility(View.VISIBLE);
                } else if (listView.getVisibility() == View.VISIBLE) {
                    listView.setVisibility(View.INVISIBLE);
                }

            }
        });


    }



    protected ArrayList<Stop> prepareListData(final ArrayList<Marker> locList) {

        ArrayList<Stop> stops = new ArrayList<Stop>();

        Log.d(TAG, "\n\nUnsorted\n");
        for(Marker marker: locList) {
            //Stop stop = (Stop) marker;
            stops.add((Stop) marker);
            Log.d(TAG, ((Stop) marker).getStopSeq().toString());
        }

        Collections.sort(stops);
        Log.d(TAG, "\n\nSorted\n");

        for(Stop stop: stops) {;
            Log.d(TAG, stop.getStopSeq().toString());
        }

        return stops;
    }








    protected boolean validSelection(Marker board, Marker alight) {
        if(board == null || alight == null) {
            Toast.makeText(getApplicationContext(),"Both boarding and alighting locations must be set",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        else {
            return true;
        }
    }

    protected String[] buildStopsArray(ArrayList<Marker> locList) {

        stopsMap = new HashMap<String, Marker>();

        for(Marker m: locList) {
            stopsMap.put(m.getTitle(), m);
            stopsMap.put(m.getDescription(), m);
        }

        String[] stopNames = new String[stopsMap.size()];

        Integer i = 0;
        for (String key : stopsMap.keySet()) {
            stopNames[i] = key;
            i += 1;
        }

        return stopNames;
    }


    protected void postResults(String onStop, String offStop) {
        Date date = new Date();

        Bundle extras = new Bundle();
        extras.putString(URL, url);
        extras.putString(LINE, line);
        extras.putString(DIR, dir);
        extras.putString(DATE, dateFormat.format(date));
        extras.putString(ON_STOP, onStop);
        extras.putString(OFF_STOP, offStop);
        extras.putString(USER_ID, user_id);
        extras.putString(TYPE, "pair");

        Intent post = new Intent(getApplicationContext(), PostService.class);
        post.putExtras(extras);
        getApplicationContext().startService(post);
    }

    protected void resetMap() {
        clearCurrentMarker();
        if (alight != null) {
            alight.setMarker(stopIcon);
            alight = null;
        }
        if (board != null) {
            board.setMarker(stopIcon);
            board = null;
        }
        mv.zoomToBoundingBox(bbox, true, false, true, true);
    }


    protected void setTiles(MapView mv) {
        ILatLng startingPoint = new LatLng(45.52186, -122.679005);
        ITileLayer source;
        BoundingBox box = new BoundingBox(46.0 ,-122 ,45.0, -123.5);

        try {
            File mbTiles = new File(TILESPATH, TILES);
            source = new MBTilesLayer(mbTiles);
            mv.setTileSource(source);
            box = source.getBoundingBox();
        }
        catch(Exception e) {
            Log.d(TAG, "unable to open local mbtiles");
            source = new WebSourceTileLayer("openstreetmap",
                    "http://tile.openstreetmap.org/{z}/{x}/{y}.png").setName("OpenStreetMap")
                    .setAttribution("© OpenStreetMap Contributors")
                    .setMinimumZoomLevel(11)
                    .setMaximumZoomLevel(17);
            mv.setTileSource(source);
        }

        mv.setScrollableAreaLimit(box);
        mv.setMinZoomLevel(mv.getTileProvider().getMinimumZoomLevel());
        mv.setMaxZoomLevel(mv.getTileProvider().getMaximumZoomLevel());
        mv.setCenter(startingPoint);
        //mv.setZoom(14);
    }


    protected void setItemizedOverlay(MapView mv, ArrayList<Marker> locList) {
        final MapView mapView = mv;

        locOverlay = new ItemizedIconOverlay(mv.getContext(), locList,

                new ItemizedIconOverlay.OnItemGestureListener<Marker>() {
                    public boolean onItemSingleTapUp(final int index, final Marker item) {
                        //do nothing
                        return true;
                    }

                    public boolean onItemLongPress(final int index, final Marker item) {
                        Log.d(TAG, "locOverlay item LongPress");
                        Log.d(TAG, item.getTitle());
                        selectLocType(item);
                        return true;
                    }



                }
        );
        mv.addItemizedOverlay(locOverlay);
    }

    //Used to set current marker and type chosen in AlertDialog for boarding and alighting location
    protected void setCurrentMarker(Marker currentMarker, String type) {
        current = currentMarker;
        locType = type;
    }

    //clears selected marker if user selects 'Cancel' in AlertDialog for boarding and alighting location
    protected void clearCurrentMarker() {
        current = null;
        locType = null;
    }

    protected void saveSequenceMarker(String mode, Marker newMarker) {

        if (mode.equals(BOARD)) {
            if (board != null) {
                board.setMarker(stopIcon);
            }
            if(newMarker == alight) {
                alight.setMarker(stopIcon);
            }
            board = newMarker;
            board.setMarker(onIcon);
        }
        else {
            if (alight != null) {
                alight.setMarker(stopIcon);
            }
            if(newMarker == board) {
                board.setMarker(stopIcon);
            }
            alight = newMarker;
            alight.setMarker(offIcon);
        }

    }

    //saves selected marker and type if user selects 'OK' in AlertDialog for boarding and alighting location
    protected void saveCurrentMarker() {
        if (locType != null) {

            //if board or alight marker is already set switch it back to default icon
            if (alight != null && alight == current) {
                alight.setMarker(stopIcon);
                alight = null;
            }
            if (board != null && board == current) {
                board.setMarker(stopIcon);
                board = null;
            }

            if (locType.equals(BOARD)) {
                if(board != null) {
                    board.setMarker(stopIcon);
                }
                board = current;
                board.setMarker(onIcon);
                Log.d(TAG, BOARD + ": " + board.getTitle());
            }
            else {
                if (alight != null) {
                    alight.setMarker(stopIcon);
                }
                alight = current;
                alight.setMarker(offIcon);
                Log.d(TAG, ALIGHT + ": " + alight.getTitle());
            }
            current = null;
            locType = null;
        }
    }



    protected void selectLocType(final Marker selectedMarker) {
        String message = selectedMarker.getTitle();

        final CharSequence[] items = {BOARD, ALIGHT};
        AlertDialog.Builder builder = new AlertDialog.Builder(OnOffMapActivity.this);
        builder.setTitle(message)
                .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String choice = items[i].toString();
                        Log.d(TAG, "Choice: " + choice);
                        setCurrentMarker(selectedMarker, choice);

                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "Clicked OK");
                        saveCurrentMarker();
                        mv.zoomToBoundingBox(bbox, true, false, true, true);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "Clicked Cancel");
                        clearCurrentMarker();
                    }
                });

        AlertDialog select = builder.create();
        select.show();
    }

    protected void verifyAndSubmitLocationsPOST() {
        if (Utils.isNetworkAvailable(getApplicationContext())) {
            String boardLoc = board.getTitle();
            String alightLoc = alight.getTitle();
            String message = "Boarding: " + boardLoc + "\n\nAlighting: " + alightLoc;
            AlertDialog.Builder builder = new AlertDialog.Builder(OnOffMapActivity.this);
            builder.setTitle("Are you sure you want to submit these locations?")
                    .setMessage(message)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //get stop ids
                            String onStop = board.getDescription();
                            String offStop = alight.getDescription();
                            //call function to post on off pair
                            postResults(onStop, offStop);
                            resetMap();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //do nothing
                        }
                    });

            AlertDialog select = builder.create();
            select.show();
        }
        else {
            Log.d(TAG, "No network connection");
            Toast toast = Toast.makeText(getApplicationContext(), "Please enable network connections.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    /*
    protected void verifyAndSubmitLocationsODK() {
        String boardLoc = board.getTitle();
        String alightLoc = alight.getTitle();
        String message = "Boarding: " + boardLoc + "\n\nAlighting: " + alightLoc;
        AlertDialog.Builder builder = new AlertDialog.Builder(OnOffMapActivity.this);
        builder.setTitle("Are you sure you want to submit these locations?")
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //get stop ids
                        String onStop = board.getDescription();
                        String offStop = alight.getDescription();
                        exitWithStopIDs(onStop, offStop);

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //do nothing
                    }
                });

        AlertDialog select = builder.create();
        select.show();
    }
    */

    private boolean exitWithStopIDs(String onStop, String offStop) {
        Intent intent = new Intent();
        intent.putExtra(ODK_BOARD, onStop);
        intent.putExtra(ODK_ALIGHT, offStop);
        setResult(RESULT_OK, intent);
        finish();
        return true;
    }


    protected void getExtras() {
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            if(extras.containsKey(LINE)) {
                Log.d(TAG, extras.getString(LINE));
                line = extras.getString(LINE);
            }
            if(extras.containsKey(DIR)) {
                Log.d(TAG, extras.getString(DIR));
                dir = extras.getString(DIR);
            }
            if(extras.containsKey(URL)) {
                Log.d(TAG, extras.getString(URL));
                url = extras.getString(URL);
            }
            if(extras.containsKey(USER_ID)) {
                Log.d(TAG, extras.getString(USER_ID));
                user_id = extras.getString(USER_ID);
            }
        }
    }

    /* open stops geojson for current route
     * parse into ArrayList of markers
     * each marker contains stop description, stop id, stop sequence and LatLng
     */
    protected ArrayList<Marker> getStops(String line, String dir) {
        String geoJSONName = line + "_" + dir + "_stops.geojson";
        //File stopsFile = new File(GEOJSONPATH, geoJSONName);

        //if (stopsFile.exists()) {
        BuildStops stops = new BuildStops(context, mv, "geojson/" + geoJSONName);
        bbox = stops.getBoundingBox();
        //Log.d(TAG, bbox.toString());
        //zoom to extent of stops
        mv.zoomToBoundingBox(bbox, true, false, true, true);
        return stops.getStops();

    }


    //modify mView for each toolTip in each marker to prevent closing it when touched
    protected void setToolTipListener(Marker marker) {

        final Marker aMarker = marker;

        View mView = aMarker.getToolTip(mv).getView();
        //mView.setClickable(true);
        //mView.setLongClickable(true);

        //from InfoWindow Constructor but commented out close
        mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    //do nothing (don't close)
                    //close();
                }
                return false;
            }
        });

        //prompt user to pick location type if LongClick
        mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d(TAG, "locOverlay toolTip LongPress");
                Log.d(TAG, aMarker.getTitle());
                selectLocType(aMarker);
                return true;
            }
        });


    }

    protected void addRoute(String line, String dir) {
        String geoJSONName = line + "_" + dir + "_routes.geojson";

        ArrayList<PathOverlay> paths = PathUtils.getPathFromAssets(this, "geojson/" + geoJSONName);

        if (paths != null) {
            for(PathOverlay path: paths)
                mv.addOverlay(path);
        }

    }


}
