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
    TextView partIDDisplay;
    TextView partDescriptionDisplay;
    TextView lastChangedDisplay;
    Long partQuantity;
    int currentStock;
    String partDescriptionFromDB;
    String partExtendedDescriptionFromDB;
    Connection connection;

    String currentTime;

    private final static String SCAN_ACTION = ScanManager.ACTION_DECODE;
    private ScanManager mScanManager;
    private BroadcastReceiver mScanReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast errorToast = Toast.makeText(getApplicationContext(), "Error, press back to scan" +
                            " new item",
                    Toast.LENGTH_SHORT);
            errorToast.show();
        }
    };

    private void initScan() {
        // TODO Auto-generated method stub
        mScanManager = new ScanManager();
        mScanManager.openScanner();

        mScanManager.switchOutputMode(0);
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
        if (mScanManager != null) {
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
        partIDDisplay = (TextView) findViewById(R.id.partIDDisplay);
        partDescriptionDisplay = (TextView) findViewById(R.id.partDescriptionDisplay);
        lastChangedDisplay = (TextView) findViewById(R.id.lastChangedDisplay);
        itemQuantity = (EditText) findViewById(R.id.itemQuantity);

        currentStock = retrieveDBValue();
        partDescriptionFromDB = retrievePartDescription();
        partExtendedDescriptionFromDB = retrieveExtendedPartDescription();

        currentStockDisplay.setText(Integer.toString(currentStock));                                // Use values from database passed from previous check
        partIDDisplay.setText(partDescriptionFromDB);                                               // ""
        partDescriptionDisplay.setText(partExtendedDescriptionFromDB);                              // ""
        partBarcodeDisplay.setText(getIntent().getStringExtra("BARCODE_STRING"));                   // Barcode is passed from previous screen
    }

    public int retrieveDBValue() {
        int DBValue = 0;
        try {
            connection = attemptConnection(getIntent().getStringExtra("USERNAME"),
                    getIntent().getStringExtra("PASSWORD"), getIntent().getStringExtra("DATABASE"),
                    getIntent().getStringExtra("IP"));
            if (connection == null) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                        Toast.LENGTH_SHORT);
                errorToast.show();
            }
            else {
                String query = "SELECT \"Cur#Cost\" FROM inventory WHERE \"P/N\" = '" +
                        getIntent().getStringExtra("BARCODE_STRING").trim() + "'";
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query);
                if (result.next()) {
                    DBValue = result.getInt(1);
                }
                else {
                    Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                            Toast.LENGTH_SHORT);
                    errorToast.show();
                }
            }
        }
        catch (Exception ex) {
        }

        return DBValue;
    }

    public String retrievePartDescription() {
        String DBValue = "";
        try {
            connection = attemptConnection(getIntent().getStringExtra("USERNAME"),
                    getIntent().getStringExtra("PASSWORD"), getIntent().getStringExtra("DATABASE"),
                    getIntent().getStringExtra("IP"));
            if (connection == null) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                        Toast.LENGTH_SHORT);
                errorToast.show();
            }
            else {
                String query = "SELECT \"Title\" FROM inventory WHERE \"P/N\"" +
                        "= '" + getIntent().getStringExtra("BARCODE_STRING").trim() + "'";
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query);
                if (result.next()) {
                    DBValue = result.getString(1);
                }
                else {
                    DBValue = "NULL";
                }
            }
        }
        catch (Exception ex) {
        }

        return DBValue;
    }

    public String retrieveExtendedPartDescription() {
        String DBValue = "";
        try {
            connection = attemptConnection(getIntent().getStringExtra("USERNAME"),
                    getIntent().getStringExtra("PASSWORD"), getIntent().getStringExtra("DATABASE"),
                    getIntent().getStringExtra("IP"));
            if (connection == null) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                        Toast.LENGTH_SHORT);
                errorToast.show();
            }
            else {
                String query = "SELECT \"Detail\" FROM inventory WHERE " +
                "\"P/N\" = '" + getIntent().getStringExtra("BARCODE_STRING").trim() + "'";
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query);
                if (result.next()) {
                    DBValue = result.getString(1);
                }
                else {
                    DBValue = "NULL";
                }
            }
        }
        catch (Exception ex) {
        }

        return DBValue;
    }

    public void onStoreTap(View view) {                                                             // Store function
        if (itemQuantity.getText().toString().equals("")) {                                         // Checks for empty input field
            Toast errorToast = Toast.makeText(getApplicationContext(), "Error, empty input",
                    Toast.LENGTH_SHORT);
            errorToast.show();
        } else {
            partQuantity = Long.parseLong(itemQuantity.getText().toString());                       // Record user input as int
            if (partQuantity > 0 && partQuantity < 2147483647 &&                                    // Check for non negative and int overflow input
                    (currentStock + partQuantity) <= 2147483647 &&                                  // Check for overflow result
                    (currentStock + partQuantity) >= 0) {                                           // Check for negative result
                if (currentStock == retrieveDBValue()) {                                            // If calculated value equals the expected value of the database value at the moment and the user input
                    writeDBValueAdd(partQuantity);                                                  // Update the database with new value
                    currentStockDisplay.setText(Integer.toString(currentStock));                    // Update display with new stock
                    currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    lastChangedDisplay.setText(currentTime);
                } else {                                                                            // Other error
                    Toast errorToast = Toast.makeText(getApplicationContext(),
                            "Error, updating current inventory", Toast.LENGTH_SHORT);
                    errorToast.show();
                    currentStock = retrieveDBValue();
                    currentStockDisplay.setText(Integer.toString(currentStock));
                }
            }
        }
    }

    public void onWithdrawTap(View view) {                                                          // Withdraw function, same as above but with one extra check to prevent over withdrawal
        if (itemQuantity.getText().toString().equals("")) {                                         // Checks for empty input field
            Toast errorToast = Toast.makeText(getApplicationContext(), "Error, empty input",
                    Toast.LENGTH_SHORT);
            errorToast.show();
        } else {
            partQuantity = Long.parseLong(itemQuantity.getText().toString());                       // Record user input as int
            if (partQuantity > 0 && partQuantity < 2147483647 &&                                    // Check for non negative and int overflow input
                    (currentStock - partQuantity) <= 2147483647 &&                                  // Check for overflow result
                    (currentStock - partQuantity) >= 0) {                                           // Check for negative result
                if (currentStock == retrieveDBValue()) {
                    writeDBValueSub(partQuantity);
                    currentStockDisplay.setText(Integer.toString(currentStock));                    // Update display with new stock
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

    public void updateLog(String type, Long partQuantity) {
        try {
            connection = attemptConnection(getIntent().getStringExtra("USERNAME"),
                    getIntent().getStringExtra("PASSWORD"), getIntent().getStringExtra("DATABASE"),
                    getIntent().getStringExtra("IP"));
            if (connection == null) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                        Toast.LENGTH_SHORT);
                errorToast.show();
            }
            String query = "UPDATE Transactions SET \"Username\" = '" +
                    getIntent().getStringExtra("USERNAME").trim() + "', \"Date/Time\" = " +
                    "GETDATE(),\"P/N\" = '" +
                    getIntent().getStringExtra("BARCODE_STRING").trim() + "', \"Type\" = '" + type
                    +"'," + " \"Quantity\" = '" + partQuantity +"' WHERE \"Date/Time\" IN (SELECT" +
                    " TOP (1) \"Date/Time\" FROM Transactions ORDER BY \"Date/Time\")";
            Statement statement = connection.createStatement();
            statement.executeQuery(query);
        }
        catch (Exception ex) {
        }

    }

    public void writeDBValueAdd(long partQuantity) {
        try {
            connection = attemptConnection(getIntent().getStringExtra("USERNAME"),
                    getIntent().getStringExtra("PASSWORD"), getIntent().getStringExtra("DATABASE"),
                    getIntent().getStringExtra("IP"));
            if (connection == null) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                        Toast.LENGTH_SHORT);
                errorToast.show();
            }
            else {
                currentStock += partQuantity;
                String query = "SELECT * FROM inventory WHERE \"P/N\" = '" +
                        getIntent().getStringExtra("BARCODE_STRING").trim() + "'";
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query);
                if (result.next()) {
                    updateLog("add", partQuantity);
                    Toast withdrawToast = Toast.makeText(getApplicationContext(), partQuantity
                            + " part(s)" + " stored", Toast.LENGTH_SHORT);
                    withdrawToast.show();
                } else {
                    Toast errorToast = Toast.makeText(getApplicationContext(), "Error, part(s) " +
                            "not stored", Toast.LENGTH_SHORT);
                    errorToast.show();
                }
                query = "UPDATE inventory SET \"Cur#Cost\" = '" + currentStock +
                        "' WHERE \"P/N\" = '" +
                        getIntent().getStringExtra("BARCODE_STRING").trim() + "'";
                statement = connection.createStatement();
                statement.executeQuery(query);
            }
        }
        catch (Exception ex) {
        }
    }

    public void writeDBValueSub(long partQuantity) {
        try {
            connection = attemptConnection(getIntent().getStringExtra("USERNAME"),
                    getIntent().getStringExtra("PASSWORD"), getIntent().getStringExtra("DATABASE"),
                    getIntent().getStringExtra("IP"));
            if ((connection == null) && (currentStock -= partQuantity) >= 0) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Connection error ",
                        Toast.LENGTH_SHORT);
                errorToast.show();
            }
            else {
                currentStock -= partQuantity;
                String query = "SELECT * FROM inventory WHERE \"P/N\" = '" +
                        getIntent().getStringExtra("BARCODE_STRING").trim() + "'";
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query);
                if (result.next()) {
                    updateLog("sub", partQuantity);
                    Toast withdrawToast = Toast.makeText(getApplicationContext(), partQuantity
                            + " part(s)" + " withdrawn", Toast.LENGTH_SHORT);
                    withdrawToast.show();
                } else {
                    Toast errorToast = Toast.makeText(getApplicationContext(), "Error, part(s) " +
                            "not stored", Toast.LENGTH_SHORT);
                    errorToast.show();
                }
                query = "UPDATE inventory SET \"Cur#Cost\" = '" + currentStock +
                        "' WHERE \"P/N\" = '" +
                        getIntent().getStringExtra("BARCODE_STRING").trim() + "'";
                statement = connection.createStatement();
                statement.executeQuery(query);
            }
        }
        catch (Exception ex) {
        }
    }

    public void retakeInventory(View view){
        Intent intent = new Intent(getBaseContext(), retake_inventory.class);
        intent.putExtra("BARCODE_STRING", getIntent().getStringExtra("BARCODE_STRING"));
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
