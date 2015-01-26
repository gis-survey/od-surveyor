package com.meyersj.mobilesurveyor.app.survey;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.ItemizedIconOverlay;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MBTilesLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.meyersj.mobilesurveyor.app.R;
import com.meyersj.mobilesurveyor.app.util.Cons;
import com.meyersj.mobilesurveyor.app.util.PathUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


public abstract class MapFragment extends Fragment {

    protected final String TAG = "MapFragment";
    protected final File TILESPATH = new File(Environment.getExternalStorageDirectory(), "maps/mbtiles");
    protected final String TILESNAME = "OSMTriMet.mbtiles";
    private final String ODK_ACTION = "com.meyersj.mobilesurveyor.app.ODK_SURVEY";

    protected Activity activity;
    protected Context context;
    protected View view;
    protected MapView mv;
    protected ItemizedIconOverlay surveyOverlay;
    protected ArrayList<Marker> surveyList = new ArrayList<Marker>();
    protected HashMap<String,ArrayList<PathOverlay>> addedRoutes = new HashMap<String, ArrayList<PathOverlay>>();
    protected HashMap<String,ArrayList<PathOverlay>> routesCache = new HashMap<String, ArrayList<PathOverlay>>();
    protected String line;
    protected String dir;

    public MapFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_default_map, container, false);
        activity = getActivity();
        context = activity.getApplicationContext();
        getODKExtras();
        surveyOverlay = new ItemizedIconOverlay(context, surveyList,
                new ItemizedIconOverlay.OnItemGestureListener<Marker>() {
                    public boolean onItemSingleTapUp(final int index, final Marker item) {
                        return true;
                    }
                    public boolean onItemLongPress(final int index, final Marker item) {
                        return true;
                    }
                }
        );
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    protected void setTiles(MapView mv) {
        ILatLng startingPoint = new LatLng(45.52186, -122.679005);
        ITileLayer mbTilesSource;
        ITileLayer osmSource = new WebSourceTileLayer("openstreetmap",
                "http://tile.openstreetmap.org/{z}/{x}/{y}.png").setName("OpenStreetMap")
                .setAttribution("© OpenStreetMap Contributors");
        try {
            File tiles = new File(TILESPATH, TILESNAME);
            mbTilesSource = new MBTilesLayer(tiles);
            mv.setTileSource(mbTilesSource);
        }
        catch(Exception e) {
            Log.e(TAG, "unable to open local mbtiles");
            mv.setTileSource(osmSource);
        }
        mv.setMinZoomLevel(mv.getTileProvider().getMinimumZoomLevel());
        mv.setMaxZoomLevel(mv.getTileProvider().getMaximumZoomLevel());
        mv.setCenter(startingPoint);
        mv.setZoom(14);
    }

    protected void getODKExtras() {
        Intent intent = activity.getIntent();
        String action = intent.getAction();
        if (action.equals(ODK_ACTION)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (extras.containsKey(Cons.LINE)) {
                    line = extras.getString(Cons.LINE);
                }
                if (extras.containsKey(Cons.DIR)) {
                    dir = extras.getString(Cons.DIR);
                }
            }
        }
        else {
            // for testing and demos
            line = "9";
            dir = "0";
        }
    }

    protected ArrayList<PathOverlay> addRoute(Context context, String line, String dir, Boolean store) {
        String key = line + "_" + dir;
        String geoJSONName = key + "_routes.geojson";
        if(store) {
            if(addedRoutes.containsKey(key))
                return addedRoutes.get(key);
            else if(routesCache.containsKey(key)) {
                ArrayList<PathOverlay> cachePaths = routesCache.get(key);
                addedRoutes.put(key, new ArrayList<PathOverlay>());
                ArrayList<PathOverlay> paths = addedRoutes.get(key);
                for(PathOverlay mPath: cachePaths) {
                    paths.add(mPath);
                    mv.addOverlay(mPath);
                }
                return addedRoutes.get(key);
            }
            addedRoutes.put(key, new ArrayList<PathOverlay>());
            routesCache.put(key, new ArrayList<PathOverlay>());
        }

        Paint pathPaint1 = new Paint();
        pathPaint1.setColor(getResources().getColor(R.color.blacker));
        pathPaint1.setAntiAlias(true);
        pathPaint1.setStrokeWidth(5.0f);
        pathPaint1.setPathEffect(new DashPathEffect(new float[] {5,5}, 0));
        pathPaint1.setStyle(Paint.Style.STROKE);

        Paint pathPaint2 = new Paint();
        pathPaint2.setColor(getResources().getColor(R.color.bluer_lighter));
        pathPaint2.setAntiAlias(true);
        pathPaint2.setStrokeWidth(3.5f);
        pathPaint2.setStyle(Paint.Style.STROKE);

        Paint pathPaint = pathPaint1;

        ArrayList<PathOverlay> paths = PathUtils.getPathFromAssets(context, "geojson/" + geoJSONName);
        if (paths != null) {
            ArrayList<PathOverlay> pathOverlays = addedRoutes.get(key);
            ArrayList<PathOverlay> cacheOverlays = routesCache.get(key);
            for(PathOverlay mPath: paths) {
                if(store && pathOverlays != null) {
                    pathPaint = pathPaint2;
                    pathOverlays.add(mPath);
                    cacheOverlays.add(mPath);
                };
                mPath.setPaint(pathPaint);
                mv.addOverlay(mPath);
            }
        }
        return paths;
    }

    protected void clearRoute(String line, String dir) {
        String key = line + "_" + dir;
        if (addedRoutes.containsKey(key)) {
            ArrayList<PathOverlay> paths = addedRoutes.get(key);
            for (PathOverlay path : paths) {
                mv.removeOverlay(path);
            }
            addedRoutes.remove(key);
        }
    }


    public void removeLocation(Marker loc) {
        surveyOverlay.removeItem(loc);
    }

    public void updateView(SurveyManager manager) {
        BoundingBox boundingBox = null;
        mv.removeOverlay(surveyOverlay);
        surveyOverlay.removeAllItems();

        Marker orig = manager.getOrig();
        Marker dest = manager.getDest();
        Marker onStop = manager.getOnStop();
        Marker offStop = manager.getOffStop();

        if(this instanceof PickLocationFragment) {
            PickLocationFragment fragment = (PickLocationFragment) this;
            if(fragment.getMode().equals("origin") && orig != null) {
                surveyOverlay.addItem(orig);
            }
            else if(fragment.getMode().equals("destination") && dest != null) {
                surveyOverlay.addItem(dest);
            }
        }
        else {
            if(orig != null) {
                surveyOverlay.addItem(orig);
            }
            if(dest != null) {
                surveyOverlay.addItem(dest);
            }
            if(orig != null && dest != null) {
                ArrayList<Marker> markers = new ArrayList<Marker>();
                markers.add(orig);
                markers.add(dest);
                Log.d(TAG, String.valueOf(markers.size()));
                boundingBox = getBoundingBox(null, markers, markers.size());
                Log.d(TAG, boundingBox.toString());
            }
            if(onStop != null) {
                surveyOverlay.addItem(onStop);
            }
            if(offStop != null) {
                surveyOverlay.addItem(offStop);
            }
        }
        mv.addItemizedOverlay(surveyOverlay);
        if(boundingBox != null) {
            mv.zoomToBoundingBox(boundingBox, true, true);
        }
    }

    public class Bounds {
        double north = 0;
        double east = 0;
        double south = 0;
        double west = 0;
        double BUFFER = 0.01;

        public Bounds() {}

        public void addMarker(Marker marker) {
            LatLng latLng = marker.getPoint();
            if(north == 0 || latLng.getLatitude() > north) {
                north = latLng.getLatitude();
            }
            if(south == 0 || latLng.getLatitude() < south) {
                south = latLng.getLatitude();
            }
            if(west == 0 || latLng.getLongitude() < west) {
                west = latLng.getLongitude();
            }
            if(east == 0 || latLng.getLongitude() > east) {
                east = latLng.getLongitude();
            }
        }

        public BoundingBox getBounds() {
            return new BoundingBox(north + BUFFER, east + BUFFER, south - BUFFER, west - BUFFER);
        }

    }
    protected BoundingBox getBoundingBox(Bounds bounds, ArrayList<Marker> markers, int count) {
        if (count <= 0) {
            return bounds.getBounds();
        }
        if (bounds == null) {
            bounds = new Bounds();
        }
        bounds.addMarker(markers.get(count - 1));
        return this.getBoundingBox(bounds, markers, count - 1);
    }
}