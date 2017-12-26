package smac.stock;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.device.scanner.configuration.PropertyID;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class new_part extends AppCompatActivity {

    TextView IDDisplay;
    String userPrompt;
    Connection connection;

    private final static String SCAN_ACTION = ScanManager.ACTION_DECODE;
    private ScanManager mScanManager;
    private BroadcastReceiver mScanReceiver = new BroadcastReceiver() {

        // User started a new scan
        // Prompt the user to return to scanning screen to do that
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast errorToast = Toast.makeText(getApplicationContext(), "Error, press back to" +
                            " scan new item",
                    Toast.LENGTH_SHORT);
            errorToast.show();
        }
    };

    private void initScan() {
        mScanManager = new ScanManager();
        mScanManager.openScanner();

        mScanManager.switchOutputMode(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mScanManager != null) {
            mScanManager.stopDecode();
        }
        unregisterReceiver(mScanReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initScan();
        IntentFilter filter = new IntentFilter();
        int[] idBuffer = new int[]{PropertyID.WEDGE_INTENT_ACTION_NAME,
                PropertyID.WEDGE_INTENT_DATA_STRING_TAG};
        String[] valueBuffer = mScanManager.getParameterString(idBuffer);
        if (valueBuffer != null && valueBuffer[0] != null && !valueBuffer[0].equals("")) {
            filter.addAction(valueBuffer[0]);
        } else {
            filter.addAction(SCAN_ACTION);
        }

        registerReceiver(mScanReceiver, filter);
    }

    // Create the message asking if this is the right barcode to add
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_part);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        userPrompt = "Part " + getIntent().getStringExtra("BARCODE_STRING").trim() +
                " is not in the database.\nDo you want to add it?";

        IDDisplay = (TextView) findViewById(R.id.IDDisplay);

        IDDisplay.setText(userPrompt);
    }

    // Add new item into database
    public void onYesClick(View view) {
        try {
            connection = attemptConnection(getIntent().getStringExtra("USERNAME"),
                    getIntent().getStringExtra("PASSWORD"),
                    getIntent().getStringExtra("DATABASE"),
                    getIntent().getStringExtra("IP"));
            if (connection == null) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                        Toast.LENGTH_SHORT);
                errorToast.show();
            } else {
                String query = "INSERT INTO inventory (\"P/N\", \"Cur#Cost\", \"Location\")" +
                        " VALUES ('" +
                        getIntent().getStringExtra("BARCODE_STRING").trim() + "', 0," +
                        getIntent().getStringExtra("LOCATION_STRING").trim() + ")";
                Statement statement = connection.createStatement();
                statement.executeQuery(query);
                Toast toast = Toast.makeText(getApplicationContext(), "Part added",
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        catch (Exception ex) {
        }
        Intent returnIntent = new Intent();
        setResult(waiting_for_scan.RESULT_CANCELED, returnIntent);
        finish();
    }

    // Cancel button clicked
    // Return to scanning screen
    public void onNoClick(View view) {
        Intent returnIntent = new Intent();
        setResult(waiting_for_scan.RESULT_CANCELED, returnIntent);
        finish();
    }

    @SuppressLint("NewApi")
    public Connection attemptConnection (String username, String password, String database,
                                         String ip) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Connection connection = null;
        String connectionURL;

        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            connectionURL = "jdbc:jtds:sqlserver://" + ip + ";databaseName=" + database + ";user="
                    + username + ";password=" + password + ";";
            connection = DriverManager.getConnection(connectionURL);
        }
        catch (SQLException se) {
            Log.e("error here 1 : ", se.getMessage());
        }
        catch (ClassNotFoundException e) {
            Log.e("error here 2 : ", e.getMessage());
        }
        catch (Exception e) {
            Log.e("error here 3 : ", e.getMessage());
        }

        return connection;
    }
}
