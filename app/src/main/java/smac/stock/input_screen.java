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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

public class input_screen extends AppCompatActivity {

    EditText itemQuantity;
    TextView currentStockDisplay;
    TextView partBarcodeDisplay;
    TextView lastChangedDisplay;
    TextView locationDisplay;
    Long partQuantity;
    int currentStock;
    Connection connection;
    String currentTime;

    private final static String SCAN_ACTION = ScanManager.ACTION_DECODE;
    private ScanManager mScanManager;
    private BroadcastReceiver mScanReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast errorToast = Toast.makeText(getApplicationContext(), "Error, press back " +
                            "to scan new item", Toast.LENGTH_SHORT);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_screen);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        currentStockDisplay = (TextView) findViewById(R.id.currentStockDisplay);
        partBarcodeDisplay = (TextView) findViewById(R.id.partBarcodeDisplay);
        lastChangedDisplay = (TextView) findViewById(R.id.lastChangedDisplay);
        locationDisplay = (TextView) findViewById(R.id.locationDisplay);
        itemQuantity = (EditText) findViewById(R.id.itemQuantity);

        // Attempt to fetch data from the database
        currentStock = retrieveDBValue();
        currentStockDisplay.setText(Integer.toString(currentStock));

        // Values come from the previous screen
        partBarcodeDisplay.setText(getIntent().getStringExtra("BARCODE_STRING"));
        locationDisplay.setText(getIntent().getStringExtra("LOCATION_STRING"));
    }

    // Function fetches the current inventory of the specific item
    public int retrieveDBValue() {
        int DBValue = 0;
        try {
            connection = attemptConnection(getIntent().getStringExtra("USERNAME"),
                    getIntent().getStringExtra("PASSWORD"),
                    getIntent().getStringExtra("DATABASE"),
                    getIntent().getStringExtra("IP"));
            if (connection == null) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                        Toast.LENGTH_SHORT);
                errorToast.show();
            }
            else {
                String query = "SELECT \"Cur#Cost\" FROM inventory WHERE \"P/N\" = '" +
                        getIntent().getStringExtra("BARCODE_STRING").trim() + "' AND " +
                        "\"Location\" = " + getIntent().getStringExtra("LOCATION_STRING");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query);
                if (result.next()) {
                    DBValue = result.getInt(1);
                }
                else {
                    Toast errorToast = Toast.makeText(getApplicationContext(),
                            "Connection error ",
                            Toast.LENGTH_SHORT);
                    errorToast.show();
                }
            }
        }
        catch (Exception ex) {
        }

        return DBValue;
    }

    // Function checks add value for valid input
    public void onStoreTap(View view) {
        if (itemQuantity.getText().toString().equals("")) {
            Toast errorToast = Toast.makeText(getApplicationContext(), "Error, empty input",
                    Toast.LENGTH_SHORT);
            errorToast.show();
        } else {
            partQuantity = Long.parseLong(itemQuantity.getText().toString());
            if (partQuantity > 0 && partQuantity < 2147483647 &&
                    (currentStock + partQuantity) <= 2147483647 &&
                    (currentStock + partQuantity) >= 0) {
                if (currentStock == retrieveDBValue()) {
                    writeDBValueAdd(partQuantity);
                    currentStockDisplay.setText(Integer.toString(currentStock));
                    currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    lastChangedDisplay.setText(currentTime);
                } else {
                    Toast errorToast = Toast.makeText(getApplicationContext(),
                            "Error, updating current inventory", Toast.LENGTH_SHORT);
                    errorToast.show();
                    currentStock = retrieveDBValue();
                    currentStockDisplay.setText(Integer.toString(currentStock));
                }
            }
        }
    }

    // Function checks withdraw value for valid input
    public void onWithdrawTap(View view) {
        if (itemQuantity.getText().toString().equals("")) {
            Toast errorToast = Toast.makeText(getApplicationContext(), "Error, empty input",
                    Toast.LENGTH_SHORT);
            errorToast.show();
        } else {
            partQuantity = Long.parseLong(itemQuantity.getText().toString());
            if (partQuantity > 0 && partQuantity < 2147483647 &&
                    (currentStock - partQuantity) <= 2147483647 &&
                    (currentStock - partQuantity) >= 0) {
                if (currentStock == retrieveDBValue()) {
                    writeDBValueSub(partQuantity);
                    currentStockDisplay.setText(Integer.toString(currentStock));
                    currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    lastChangedDisplay.setText(currentTime);
                } else {
                    Toast errorToast = Toast.makeText(getApplicationContext(),
                            "Error, updating current inventory", Toast.LENGTH_SHORT);
                    errorToast.show();
                    currentStock = retrieveDBValue();
                    currentStockDisplay.setText(Integer.toString(currentStock));
                }
            }
        }
    }

    // Function updates the transaction log with the new changes
    public void updateLog(String type, Long partQuantity) {
        try {
            connection = attemptConnection(getIntent().getStringExtra("USERNAME"),
                    getIntent().getStringExtra("PASSWORD"),
                    getIntent().getStringExtra("DATABASE"),
                    getIntent().getStringExtra("IP"));
            if (connection == null) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                        Toast.LENGTH_SHORT);
                errorToast.show();
            }
            String query = "UPDATE Transactions SET \"Username\" = '" +
                    getIntent().getStringExtra("USERNAME").trim() + "', \"Date/Time\" = " +
                    "GETDATE(),\"P/N\" = '" +
                    getIntent().getStringExtra("BARCODE_STRING").trim() + "', \"Type\" = '" +
                    type + "'," + " \"Quantity\" = " + partQuantity + ", \"Location\" = " +
                    getIntent().getStringExtra("LOCATION_STRING").trim() + " WHERE " +
                    "\"Date/Time\" IN (SELECT TOP (1) \"Date/Time\" FROM Transactions ORDER BY " +
                    "\"Date/Time\")";
            Statement statement = connection.createStatement();
            statement.executeQuery(query);
        }
        catch (Exception ex) {
        }

    }

    // Function deposits stock into inventory after input validation
    public void writeDBValueAdd(long partQuantity) {
        try {
            connection = attemptConnection(getIntent().getStringExtra("USERNAME"),
                    getIntent().getStringExtra("PASSWORD"),
                    getIntent().getStringExtra("DATABASE"),
                    getIntent().getStringExtra("IP"));
            if (connection == null) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                        Toast.LENGTH_SHORT);
                errorToast.show();
            }
            else {
                currentStock += partQuantity;
                String query = "SELECT * FROM inventory WHERE \"P/N\" = '" +
                        getIntent().getStringExtra("BARCODE_STRING").trim() + "' AND " +
                        "\"Location\" = " +
                        getIntent().getStringExtra("LOCATION_STRING").trim();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query);
                if (result.next()) {
                    updateLog("add", partQuantity);
                    Toast withdrawToast = Toast.makeText(getApplicationContext(), partQuantity
                            + " part(s)" + " stored", Toast.LENGTH_SHORT);
                    withdrawToast.show();
                }
                else {
                    Toast errorToast = Toast.makeText(getApplicationContext(), "Error, " +
                            "part(s) not stored", Toast.LENGTH_SHORT);
                    errorToast.show();
                }
                query = "UPDATE inventory SET \"Cur#Cost\" = '" + currentStock +
                        "' WHERE \"P/N\" = '" +
                        getIntent().getStringExtra("BARCODE_STRING").trim() + "' AND " +
                        "\"Location\" = " +
                        getIntent().getStringExtra("LOCATION_STRING").trim();
                statement = connection.createStatement();
                statement.executeQuery(query);
            }
        }
        catch (Exception ex) {

        }
    }

    // Function withdraws stock from inventory after input validation
    public void writeDBValueSub(long partQuantity) {
        try {
            connection = attemptConnection(getIntent().getStringExtra("USERNAME"),
                    getIntent().getStringExtra("PASSWORD"),
                    getIntent().getStringExtra("DATABASE"),
                    getIntent().getStringExtra("IP"));
            if ((connection == null) && (currentStock -= partQuantity) >= 0) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                        Toast.LENGTH_SHORT);
                errorToast.show();
            }
            else {
                currentStock -= partQuantity;
                String query = "SELECT * FROM inventory WHERE \"P/N\" = '" +
                        getIntent().getStringExtra("BARCODE_STRING").trim() + "' AND " +
                        "\"Location\" = " +
                        getIntent().getStringExtra("LOCATION_STRING").trim();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query);
                if (result.next()) {
                    updateLog("sub", partQuantity);
                    Toast withdrawToast = Toast.makeText(getApplicationContext(), partQuantity
                            + " part(s)" + " withdrawn", Toast.LENGTH_SHORT);
                    withdrawToast.show();
                } else {
                    Toast errorToast = Toast.makeText(getApplicationContext(), "Error, " +
                            "part(s) not stored", Toast.LENGTH_SHORT);
                    errorToast.show();
                }
                query = "UPDATE inventory SET \"Cur#Cost\" = '" + currentStock +
                        "' WHERE \"P/N\" = '" +
                        getIntent().getStringExtra("BARCODE_STRING").trim() + "' AND " +
                        "\"Location\" = " +
                        getIntent().getStringExtra("LOCATION_STRING").trim();
                statement = connection.createStatement();
                statement.executeQuery(query);
            }
        }
        catch (Exception ex) {
        }
    }

    // Function lets the user assign a new value
    // Usually done after taking a complete restock
    // Passes values onto next screen
    public void retakeInventory(View view){
        Intent intent = new Intent(getBaseContext(), retake_inventory.class);
        intent.putExtra("BARCODE_STRING",
                getIntent().getStringExtra("BARCODE_STRING"));
        intent.putExtra("LOCATION_STRING",
                getIntent().getStringExtra("LOCATION_STRING"));
        intent.putExtra("USERNAME", getIntent().getStringExtra("USERNAME"));
        intent.putExtra("PASSWORD", getIntent().getStringExtra("PASSWORD"));
        intent.putExtra("DATABASE", getIntent().getStringExtra("DATABASE"));
        intent.putExtra("IP", getIntent().getStringExtra("IP"));
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        currentStock = retrieveDBValue();
        currentStockDisplay.setText(Integer.toString(currentStock));
        currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
        lastChangedDisplay.setText(currentTime);
    }

    public void onCancelClick(View view) {
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
