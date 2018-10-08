package com.example.adi.safetymatters20;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    // Supposedly we got long and lat from GPS using a FusedLocationProviderClient from the com.google.android.gms.location api
    // (We initiate the longitude and latitude with mock data arrays to simulate driving into a dangerous intersection)
    final double[] longitudes = {-0.8689,-0.8681,-0.8673,-0.8665,-0.8657,-0.8649,-0.8641,-0.8633,-0.8625,-0.8617,-0.8609};
    final double[] latitudes = {53.7399,53.7402,53.7406,53.7409,53.7412,53.7415,53.7418,53.7421,53.7425,53.7428,53.7431};

    // Create variables for the safety tag index
    final int SAFETY_TAG_SAFE = 0;
    final int SAFETY_TAG_LOW_RISK = 1;
    final int SAFETY_TAG_HIGH_RISK = 2;

    // Color coding for the safety tag
    final float SAFE_COLOR = BitmapDescriptorFactory.HUE_GREEN;
    final float LOW_RISK_COLOR = BitmapDescriptorFactory.HUE_ORANGE;
    final float HIGH_RISK_COLOR = BitmapDescriptorFactory.HUE_ROSE;

    // Supposedly we sent the current longitude and latitude to a cloud server using ArcGis Runtime SDK,
    // returning a safety tag index, a calculable parameter that is based on the analysis we run on the raw data
    // (We initiate the safety tag with manually calculated data array due to time constraints)
    final int[] safetyTags = {
            SAFETY_TAG_SAFE,SAFETY_TAG_SAFE,SAFETY_TAG_LOW_RISK,SAFETY_TAG_LOW_RISK,
            SAFETY_TAG_LOW_RISK,SAFETY_TAG_HIGH_RISK,SAFETY_TAG_HIGH_RISK,SAFETY_TAG_HIGH_RISK,
            SAFETY_TAG_SAFE,SAFETY_TAG_SAFE,SAFETY_TAG_SAFE};

    // Create a variable for the GPS sample rate
    private int mLocUpdateInterval = 1200;  // In millisec

    // Create variables for the TextView and the current location + safety tag
    private double mCurrentLongitude;
    private double mCurrentLatitude;
    private int mCurrentSafetyTag;

    // Create variables for the audio playback
    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;
    // Set an OnCompletionListener that releases the mMediaPlayer and the audio focus
    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener(){
        @Override
        public void onCompletion(MediaPlayer mp) {
            releaseMediaPlayer();
        }
    };
    // Create an OnAudioFocusChangeListener
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (mMediaPlayer != null) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    // Pause playback because the Audio Focus was stolen temporarily
                    mMediaPlayer.pause();
                    mMediaPlayer.seekTo(0);
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    // Release the media player because the Audio Focus was permanently lost
                    releaseMediaPlayer();
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    // Resume playback, because the app again holds the Audio Focus
                    mMediaPlayer.start();
                }
            }
        }};

    // Map variables
    private GoogleMap mMap;
    private double[] mIntersectionCenter = {latitudes[5],longitudes[5]};
    private float mZoom = 15f; // float between 2.0 (max zoom out) and 21.0 (max zoom in)
    private Marker mMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Create an AudioManager object
        mAudioManager =  (AudioManager) this.getSystemService(this.AUDIO_SERVICE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //GoogleMapOptions options = new GoogleMapOptions().liteMode(true);
        //mMap.setMapType(options.getMapType());

        // Sets the map type to be "hybrid"
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        // Find the intersection center
        LatLng intersectionCenter = new LatLng(mIntersectionCenter[0], mIntersectionCenter[1]);
        // Set the camera to the desired zoom level around the intersection center
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(intersectionCenter,mZoom));

        // Set a code that runs every mLocUpdateInterval millisec for length of longitudes
        // This code will add a marker at the current location with an appropriate color
        // and will play a sound if the safety tag isn't "safe"
        for (int i=0 ; i<longitudes.length ; i++) {
            new ExecuteAsyncTask().execute(new TimePoint(longitudes[i],latitudes[i],safetyTags[i]));
        }
    }

    // Play corresponding beep
    private void playBeep (int soundId){
        // Release the mMediaPlayer object (make sure it's null)
        releaseMediaPlayer();
        // Request audio focus
        int focusState = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,AudioManager.STREAM_ALARM,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        // If audio focus was granted
        if (focusState == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            // Set the mMediaPlayer object to the appropriate audio file and start playing it
            mMediaPlayer = MediaPlayer.create(this, soundId);
            // Set an OnCompletionListener releasing mMediaPlayer when the audio file finishes playing
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            // Start playing the media file
            mMediaPlayer.start();
        }
    }

    // When the user leaves the app, release mMediaPlayer
    @Override
    protected void onStop() {
        super.onStop();
        releaseMediaPlayer();
    }

    // Clean up the media player by releasing its resources
    private void releaseMediaPlayer() {
        // If the media player is not null, then it may be currently playing a sound - release it and the audio focus
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
            // Reset the mMediaPlayer to null
            mMediaPlayer = null;
        }
    }

    class ExecuteAsyncTask extends AsyncTask<TimePoint, Void, Void> {
        @Override
        protected Void doInBackground(TimePoint... timePoint) {
            // pause
            try {
                Thread.sleep(mLocUpdateInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Update the current longitude, latitude and safetyTag
            mCurrentLatitude = timePoint[0].getLatitude();
            mCurrentLongitude = timePoint[0].getLongitute();
            mCurrentSafetyTag = timePoint[0].getSafetyTag();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Update the UI and if there is a problematic safety tag, alert
            switch (mCurrentSafetyTag){
                case SAFETY_TAG_LOW_RISK:
                    updateMarker(LOW_RISK_COLOR);
                    playBeep(R.raw.beep_short);
                    break;
                case SAFETY_TAG_HIGH_RISK:
                    updateMarker(HIGH_RISK_COLOR);
                    playBeep(R.raw.careful);
                    break;
                default:
                    updateMarker(SAFE_COLOR);
                    break;
            }
        }
    }

    // Update the marker to the current location and color corresponding with the safety tag
    private void updateMarker(float markerColor){
        // Set the LatLng and icon based on the current variables
        LatLng latLng = new LatLng(mCurrentLatitude, mCurrentLongitude);
        BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(markerColor);
        // Create a marker if is null
        if (mMarker == null) {
            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(icon));
        // If a marker already exists, edit it
        } else {
            mMarker.setPosition(new LatLng(mCurrentLatitude, mCurrentLongitude));
            mMarker.setIcon(BitmapDescriptorFactory.defaultMarker(markerColor));
        }
    }
}
