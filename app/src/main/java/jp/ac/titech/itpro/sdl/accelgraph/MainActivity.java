package jp.ac.titech.itpro.sdl.accelgraph;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {

    private final static String TAG = "MainActivity";

    private TextView rateView, accuracyView;
    private GraphView xView, yView, zView;

    private SensorManager sensorMgr;
    private Sensor magnetic_field;

    private final static long GRAPH_REFRESH_WAIT_MS = 20;

    private GraphRefreshThread th = null;
    private Handler handler;

    private float vx, vy, vz;
    private float rate;
    private int accuracy;
    private long prevts;

    private final static float alpha = 0.075F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        rateView = (TextView) findViewById(R.id.rate_view);
        accuracyView = (TextView) findViewById(R.id.accuracy_view);
        xView = (GraphView) findViewById(R.id.x_view);
        yView = (GraphView) findViewById(R.id.y_view);
        zView = (GraphView) findViewById(R.id.z_view);

        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        magnetic_field = sensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magnetic_field == null) {
            Toast.makeText(this, getString(R.string.toast_no_accel_error),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        sensorMgr.registerListener(this, magnetic_field, SensorManager.SENSOR_DELAY_NORMAL);
        th = new GraphRefreshThread();
        th.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        th = null;
        sensorMgr.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        vx = (alpha * vx + (1 - alpha) * event.values[0]) / 10;
        vy = (alpha * vx + (1 - alpha) * event.values[1])/ 10;
        vz = (alpha * vx + (1 - alpha) * event.values[2]) / 10;
        rate = ((float) (event.timestamp - prevts)) / (1000 * 1000);
        prevts = event.timestamp;
        final float[] ev = event.values;
        Log.i(TAG, "ax=" + ev[0] + ", ay=" + ev[1] + ", az=" + ev[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "onAccuracyChanged: ");
        this.accuracy = accuracy;
    }

    private class GraphRefreshThread extends Thread {
        public void run() {
            try {
                while (th != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            rateView.setText(String.format(Locale.getDefault(), "%f", rate));
                            accuracyView.setText(String.format(Locale.getDefault(), "%d", accuracy));
                            xView.addData(vx, true);
                            yView.addData(vy, true);
                            zView.addData(vz, true);
                        }
                    });
                    Thread.sleep(GRAPH_REFRESH_WAIT_MS);
                }
            }
            catch (InterruptedException e) {
                Log.e(TAG, e.toString());
                th = null;
            }
        }
    }
}
