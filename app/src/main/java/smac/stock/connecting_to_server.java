package smac.stock;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class connecting_to_server extends AppCompatActivity {

    EditText usernamePrompt;
    EditText passwordPrompt;
    Button loginButton;
    String username;
    String password;
    Connection connection;
    final String database = "Smac_Inventory";
    String ip = "192.168.0.105";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connecting_to_server);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        usernamePrompt = (EditText) findViewById(R.id.usernamePrompt);
        passwordPrompt = (EditText) findViewById(R.id.passwordPrompt);
        loginButton = (Button) findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences addressPreference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                ip = addressPreference.getString("example_text", "192.168.0.5");

                CheckLogin checkLogin = new CheckLogin();
                checkLogin.execute("");
            }
        });
    }

        public class CheckLogin extends AsyncTask<String, String, String> {
            String z = "";
            boolean successFlag = false;

//            @Override
//            protected void onPreExecute() {
//            }

            @Override
            protected void onPostExecute(String r) {
                Toast errorToast = Toast.makeText(getApplicationContext(), r, Toast.LENGTH_SHORT);
                errorToast.show();
                if (successFlag) {
                    Intent intent = new Intent(getApplicationContext(), waiting_for_scan.class);
                    intent.putExtra("USERNAME", username);
                    intent.putExtra("PASSWORD", password);
                    intent.putExtra("DATABASE", database);
                    intent.putExtra("IP", ip);
                    startActivity(intent);
                }
            }

            @Override
            protected String doInBackground(String... params) {
                username = usernamePrompt.getText().toString();
                password = passwordPrompt.getText().toString();

                if ((username.trim().equals("")) || (password.trim().equals(""))) {
                    z = "Enter a username and password";
                } else {
                    try {
                        connection = attemptConnection(username, password, database, ip);
                        if (connection == null) {
                            z = "Connection Error";
                        } else {
                            String query = "SELECT * FROM login where Username = '" + username +
                                    "' and Password = '" + password + "'  ";
                            Statement statement = connection.createStatement();
                            ResultSet result = statement.executeQuery(query);
                            if (result.next()) {
                                z = "Login Successful";
                                successFlag = true;
                                connection.close();
                            } else {
                                z = "Invalid credentials";
                                successFlag = false;
                            }
                        }
                    }
                    catch (Exception ex) {
                        successFlag = false;
                        z = ex.getMessage();
                    }
                }
                return z;
            }
        }



    @SuppressLint("NewApi")
    public Connection attemptConnection(String username, String password, String database,
                                        String ip) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Connection connection = null;
        String connectionURL;

        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            connectionURL = "jdbc:jtds:sqlserver://" + ip + ";databaseName=" + database + ";user=" + username +
                    ";password=" + password + ";";
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connecting_to_server, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, Settings.class);
            startActivity(settingsIntent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
