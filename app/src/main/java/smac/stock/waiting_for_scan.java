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
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class waiting_for_scan extends AppCompatActivity {

    private ScanManager mScanManager = new ScanManager();
    private final static String SCAN_ACTION = ScanManager.ACTION_DECODE;
    int i = 0;
    Connection connection;

    private BroadcastReceiver mScanReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String barcodeStr;

            byte[] barcode = intent.getByteArrayExtra(ScanManager.DECODE_DATA_TAG);
            int barcodeLength = intent.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0);
            barcodeStr = new String(barcode, 0, barcodeLength);

            for (i = 0; i < barcodeStr.length(); i++) {
                if (barcodeStr.charAt(i) == '\n') {
                    break;
                }
            }
            databaseCheck(barcodeStr, i);
        }
    };

    public void databaseCheck(String barcodeStr, int i) {
        try {
            connection = attemptConnection(getIntent().getStringExtra("USERNAME"),
                    getIntent().getStringExtra("PASSWORD"), getIntent().getStringExtra("DATABASE"),
                    getIntent().getStringExtra("IP"));
            if (connection == null) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                        Toast.LENGTH_SHORT);
                errorToast.show();
            } else {
                String query = "SELECT * FROM inventory where \"P/N\" = '" +
                        barcodeStr.substring(0, i).trim() + "'";
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query);
                if (result.next()) {
                    Intent newScreen = new Intent(getBaseContext(), input_screen.class);
                    newScreen.putExtra("BARCODE_STRING", barcodeStr.substring(0, i).trim());
                    newScreen.putExtra("USERNAME", getIntent().getStringExtra("USERNAME"));
                    newScreen.putExtra("PASSWORD", getIntent().getStringExtra("PASSWORD"));
                    newScreen.putExtra("DATABASE", getIntent().getStringExtra("DATABASE"));
                    newScreen.putExtra("IP", getIntent().getStringExtra("IP"));
                    startActivityForResult(newScreen, 0);                               // Add part quantity into .putExtra
                }
                else {
                    Intent newScreen = new Intent(getBaseContext(), new_part.class);
                    newScreen.putExtra("BARCODE_STRING", barcodeStr.substring(0, i).trim());
                    newScreen.putExtra("USERNAME", getIntent().getStringExtra("USERNAME"));
                    newScreen.putExtra("PASSWORD", getIntent().getStringExtra("PASSWORD"));
                    newScreen.putExtra("DATABASE", getIntent().getStringExtra("DATABASE"));
                    newScreen.putExtra("IP", getIntent().getStringExtra("IP"));
                    startActivityForResult(newScreen, 1);
                }
            }
        }
        catch (Exception ex) {
            Toast errorToast = Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT);
            errorToast.show();
        }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_for_scan);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

    }

    private void initScan() {
        // TODO Auto-generated method stub
        mScanManager = new ScanManager();
        mScanManager.openScanner();

        mScanManager.switchOutputMode( 0);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if(mScanManager != null) {
            mScanManager.stopDecode();
        }
        unregisterReceiver(mScanReceiver);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        initScan();
        IntentFilter filter = new IntentFilter();
        int[] idBuffer = new int[]{PropertyID.WEDGE_INTENT_ACTION_NAME,
                PropertyID.WEDGE_INTENT_DATA_STRING_TAG};
        String[] valueBuffer = mScanManager.getParameterString(idBuffer);
        if(valueBuffer != null && valueBuffer[0] != null && !valueBuffer[0].equals("")) {
            filter.addAction(valueBuffer[0]);
        } else {
            filter.addAction(SCAN_ACTION);
        }

        registerReceiver(mScanReceiver, filter);
    }
}
