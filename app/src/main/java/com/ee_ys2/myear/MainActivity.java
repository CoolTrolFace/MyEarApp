package com.ee_ys2.myear;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity {

    private MaterialButton recordingBtn;        // Custom button from MaterialAPI
    private TextView infoText, result;          // Text views
    private MediaRecorder mediaRecorder;        // Media recorder to record sound
    private VisualizerView visualizerView;      // Visualizer
    private static MainActivity instance;
    private WebManager webManager;              // Web manager to communicate with back-end
    private FileDescriptor fileDescriptor;      // Necessary classes to get byte-array of sound
    private InputStream inputStream;            // **
    private long lastRecord;
    private byte[] data = new byte[16384];      // Sound byte-array data
    private Handler handler = new Handler();
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    private RecordingState recordingState = RecordingState.NOT_RECORDING;

    /**
     * onCreate method of the application
     * Simply sets all views and creates webManager
     * Also sets onClickListener of the recording button
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance=this;
        setContentView(R.layout.activity_main);

        recordingBtn=findViewById(R.id.recordingBtn);
        infoText=findViewById(R.id.infoText);
        result=findViewById(R.id.result);
        visualizerView= (VisualizerView) findViewById(R.id.visualizerView);

        webManager = new WebManager(getInstance(),result);

        if(CheckPermissions2()){
            Log.i("INFO","internet");
        }


        recordingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recordingState==RecordingState.NOT_RECORDING){
                    startRecord();
                }else{
                    stopRecord();
                }
            }
        });

    }


    /**
     * This class runs as user starts recording.
     * Application checks necessary permissions and then starts running.
     * File descriptor and media recorder is prepares.
     * Then if everything is okay, media recorder starts recording the sound.
     *
     * Button sets to record mode and handler sends tasks to control visualizer and write sound data.
     */
    public void startRecord(){
        if(CheckPermissions()){                             // Controlling permissions
            mediaRecorder = new MediaRecorder();

            try{
                ParcelFileDescriptor[] descriptors = ParcelFileDescriptor.createPipe();
                ParcelFileDescriptor parcelRead = new ParcelFileDescriptor(descriptors[0]);
                ParcelFileDescriptor parcelWrite = new ParcelFileDescriptor(descriptors[1]);
                fileDescriptor = parcelWrite.getFileDescriptor();
                inputStream = new ParcelFileDescriptor.AutoCloseInputStream(parcelRead);

                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mediaRecorder.setOutputFile(fileDescriptor);
                try {
                    mediaRecorder.setPreviewDisplay(null);
                    mediaRecorder.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaRecorder.start();


                setButtonRecording();
                recordingState=RecordingState.RECORDING;
                lastRecord=System.currentTimeMillis();
                handler.postDelayed(soundRecordTask,250);
                handler.postDelayed(updateVisualizer,40);
            }catch (Exception e){

            }
        }else{
            RequestPermissions();
        }
    }

    /**
     * Sound recording task writes sound data to byte array.
     * After every 3 seconds it runs sendData() method and clears output stream.
     * It runs as loop of 250 milliseconds.
     */
    private Runnable soundRecordTask = new Runnable(){
        int read;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        @Override
        public void run() {
            int secondsLeft = (int) ((lastRecord/1000)+ 3 - (System.currentTimeMillis()/1000));
            if(secondsLeft<=0){     // Next send time
                sendData();
                try {
                    byteArrayOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                lastRecord=System.currentTimeMillis();
            }else{

                try {
                    read = inputStream.read(data, 0, data.length);
                    byteArrayOutputStream.write(data, 0, read);
                } catch (IOException e) {
                    e.printStackTrace();
                }




                switch(infoText.length()){
                    case 10:
                        infoText.setText("Recording..");
                        break;
                    case 11:
                        infoText.setText("Recording...");
                        break;
                    default:
                        infoText.setText("Recording.");
                        break;
                }
            }
            handler.postDelayed(soundRecordTask,250);
        }
    };


    /**
     * Visualizer updating task keeps updating visualizer as amplitude level.
     * It runs as loop of 40 milliseconds.
     */
    Runnable updateVisualizer = new Runnable() {
        @Override
        public void run() {
            if (recordingState==RecordingState.RECORDING)
            {
                // get the current amplitude
                int x = mediaRecorder.getMaxAmplitude();
                visualizerView.setSize(x);
                visualizerView.invalidate(); // refresh the VisualizerView

                // update in 40 milliseconds
                handler.postDelayed(this, 40);
            }
        }
    };

    /**
     * Stops all recording and writing data process.
     * Sets everything to default value.
     */
    public void stopRecord(){
        handler.removeCallbacks(soundRecordTask);
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder=null;
        infoText.setText(null);
        visualizerView.reset();
        recordingState=RecordingState.NOT_RECORDING;
        setButtonNotRecording();

        data = new byte[16384];
        result.setText(null);
    }

    private enum RecordingState{
        RECORDING,
        NOT_RECORDING
    }

    /**
     * Permission check of recording audio.
     * @return
     */
    public boolean CheckPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED;
    }
    /**
     * Permission check of accessing internet.
     * @return
     */
    public boolean CheckPermissions2() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), INTERNET);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Prepares JSON object to send to back-end.
     * It encodes byte-array to string and writes into JSON object.
     * Then calls WebManager's sendpost() to send data to back-end.
     */
    public void sendData(){
        result.setText("Analyzing...");
        String byteArray = Base64.encodeToString(data, Base64.DEFAULT);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.accumulate("data",byteArray);
            getWebManager().sendPost("getdata",jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        data = new byte[16384];
    }

    /**
     * Checks if permission is granted by the user.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecord) {
                        Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }


    public static MainActivity getInstance() {
        return instance;
    }
    public WebManager getWebManager() {
        return webManager;
    }

    /**
     * Sets button style to recording.
     */
    public void setButtonRecording(){
        recordingBtn.setBackgroundTintList(this.getResources().getColorStateList(R.color.recording));
        recordingBtn.setIcon(getResources().getDrawable(R.drawable.pause_button));
    }
    /**
     * Sets button style to not-recording.
     */
    public void setButtonNotRecording(){
        recordingBtn.setBackgroundTintList(this.getResources().getColorStateList(R.color.not_recording));
        recordingBtn.setIcon(getResources().getDrawable(R.drawable.play_button));
    }

    /**
     * Request permission of recording audio.
     */
    private void RequestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION_CODE);
    }

}