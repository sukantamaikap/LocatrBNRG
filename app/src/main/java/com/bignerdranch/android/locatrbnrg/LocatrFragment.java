package com.bignerdranch.android.locatrbnrg;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

/**
 * Created by smaikap on 3/9/16.
 */
public class LocatrFragment extends SupportMapFragment {
    private static final String TAG = "LocatrFragment";

    private GoogleApiClient mClient;
    private Bitmap mMapImage;
    private GalleryItem mMapItem;
    private Location mCurrentLocation;
    private GoogleMap mMap;

    public static LocatrFragment newInstance() {
        return new LocatrFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(Boolean.TRUE);
        this.mClient = new GoogleApiClient
                .Builder(this.getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        LocatrFragment.this.getActivity().invalidateOptionsMenu();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();
        this.getMapAsync(new OnMapReadyCallback() {

            @Override
            public void onMapReady(final GoogleMap googleMap) {
                LocatrFragment.this.mMap= googleMap;
                LocatrFragment.this.updateUI();
            }

        });
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_locatr, menu);

        final MenuItem  searchMenu = menu.findItem(R.id.action_locator);
        searchMenu.setEnabled(this.mClient.isConnected());
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_locator:
                this.findImage();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.getActivity().invalidateOptionsMenu();
        this.mClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mClient.disconnect();
    }

    private void findImage() throws SecurityException {
        final LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setNumUpdates(1);
        request.setInterval(0);
        LocationServices.FusedLocationApi.requestLocationUpdates(this.mClient, request, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i(TAG, "Got a fix : " + location);
                new SearchTask().execute(location);
            }
        });
    }

    private void updateUI() {
        if (this.mMap == null || this.mMapImage == null) {
            return;
        }

        final LatLng itemPoint = new LatLng(this.mMapItem.getLat(), this.mMapItem.getLon());
        final LatLng myPoint = new LatLng(this.mCurrentLocation.getLatitude(), this.mCurrentLocation.getLongitude());

        final BitmapDescriptor itemBitmap = BitmapDescriptorFactory.fromBitmap(this.mMapImage);
        final MarkerOptions itemMarker = new MarkerOptions().position(itemPoint).icon(itemBitmap);
        final MarkerOptions myMarker = new MarkerOptions().position(myPoint);

        this.mMap.clear();
        this.mMap.addMarker(itemMarker);
        this.mMap.addMarker(myMarker);

        final LatLngBounds bounds = new LatLngBounds
                .Builder()
                .include(itemPoint)
                .include(myPoint)
                .build();

        final int margin = this.getResources().getDimensionPixelSize(R.dimen.map_inset_margin);
        final CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, margin);
        this.mMap.animateCamera(update);
    }

    private class SearchTask extends AsyncTask<Location, Void, Void> {
        private GalleryItem mGalleryItem;
        private Bitmap mBitmap;
        private Location mLocation;

        @Override
        protected Void doInBackground(final Location... locations) {
            this.mLocation = locations[0];
            final FlickrFetcher fetcher = new FlickrFetcher();
            List<GalleryItem> item = fetcher.searchPhoto(locations[0]);

            if (item.size() == 0) {
                return null;
            }

            this.mGalleryItem = item.get(0);
            try {
                final byte[] bytes = fetcher.getUrlBytes(this.mGalleryItem.getUrl_S());
                SearchTask.this.mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } catch (final IOException ioe) {
                Log.i(TAG, "Unable to download bitmap");
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void aVoid) {
            LocatrFragment.this.mMapImage = this.mBitmap;
            LocatrFragment.this.mMapItem = this.mGalleryItem;
            LocatrFragment.this.mCurrentLocation = this.mLocation;
            LocatrFragment.this.updateUI();
        }
    }
}
