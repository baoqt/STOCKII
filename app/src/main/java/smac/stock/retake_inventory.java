package smac.stock;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class retake_inventory extends AppCompatActivity {

    EditText newValue;
    Long newQuantity;
    Connection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retake_inventory);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        newValue = (EditText) findViewById(R.id.newValue);
    }

    public void setNewValue(View view) {
        if (newValue.getText().toString().equals("")) {                                             // Checks for empty input field
            Toast errorToast = Toast.makeText(getApplicationContext(), "Error, empty input",
                    Toast.LENGTH_SHORT);
            errorToast.show();
        }
        else {
            newQuantity = Long.parseLong(newValue.getText().toString());
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
                    String query = "SELECT * FROM inventory WHERE \"P/N\" = '" +
                            getIntent().getStringExtra("BARCODE_STRING").trim() + "'";
                    Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(query);
                    if (result.next()) {
                        updateLog(newQuantity);
                        Toast toast = Toast.makeText(getApplicationContext(), "Stock updated",
                                Toast.LENGTH_SHORT);
                        toast.show();

                    } else {
                        Toast errorToast = Toast.makeText(getApplicationContext(), "Error, part(s) " +
                                "not stored", Toast.LENGTH_SHORT);
                        errorToast.show();
                    }
                    query = "UPDATE inventory SET \"Cur#Cost\" = '" + newQuantity +
                            "' WHERE \"P/N\" = '" +
                            getIntent().getStringExtra("BARCODE_STRING").trim() + "'";
                    statement = connection.createStatement();
                    statement.executeQuery(query);
                }
            }
            catch (Exception ex) {
            }
            Intent returnIntent = new Intent();
            setResult(waiting_for_scan.RESULT_CANCELED, returnIntent);
            finish();
        }
    }

    public void updateLog(Long quantity) {
        if (newValue.getText().toString().equals("")) {                                             // Checks for empty input field
            Toast errorToast = Toast.makeText(getApplicationContext(), "Error, empty input",
                    Toast.LENGTH_SHORT);
            errorToast.show();
        }
        else {
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
                        getIntent().getStringExtra("BARCODE_STRING").trim() + "', \"Type\" = " +
                        "'Update '," + " \"Quantity\" = '" + quantity +
                        "' WHERE \"Date/Time\" IN (SELECT TOP (1) \"Date/Time\" FROM Transactions" +
                        "ORDER BY \"Date/Time\")";
                Statement statement = connection.createStatement();
                statement.executeQuery(query);
            }
            catch (Exception ex) {
            }
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
}

