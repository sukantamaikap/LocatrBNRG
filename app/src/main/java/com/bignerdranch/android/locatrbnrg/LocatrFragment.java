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

import java.io.IOException;
import java.util.List;

/**
 * Created by smaikap on 3/9/16.
 */
public class LocatrFragment extends Fragment {
    private static final String TAG = "LocatrFragment";
    private ImageView mImageView;
    private GoogleApiClient mClient;

    public static LocatrFragment newInstance() {
        return new LocatrFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(Boolean.TRUE);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_locatr, container, false);
        this.mImageView = (ImageView) view.findViewById(R.id.image);
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
        return view;
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
            }
        });
    }

    private class SearchTask extends AsyncTask<Location, Void, Void> {
        private GalleryItem mGalleryItem;
        private Bitmap mBitmap;

        @Override
        protected Void doInBackground(final Location... locations) {
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
            LocatrFragment.this.mImageView.setImageBitmap(this.mBitmap);
        }
    }
}
