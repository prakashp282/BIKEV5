package com.fyp.bikev5;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.fyp.bikev5.MapsActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private Button btnSignIn, btnSignUp;
    private ProgressBar progressBar;
    private FirebaseAuth.AuthStateListener authListener;
    DatabaseReference userDatabase;


    public static double fuelPrice=78.0;
    public static FirebaseAuth auth;
    public static FirebaseUser currentUser;
    User CU;
    private static final int PERMISSIONS_REQUEST = 1;
    public EditText emailText, passwordText, userText, VechNoText, modelText, mfgDtText;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Check GPS is enabled
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Check location permission is granted - if it is, not granted
        // request the permission
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST);
        }


        FirebaseApp.initializeApp(this);
        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            startTrackerService();

        }

        setContentView(R.layout.activity_signup);

        btnSignIn = (Button) findViewById(R.id.Login);
        btnSignUp = (Button) findViewById(R.id.Register);
        emailText =(EditText) findViewById(R.id.email);
        userText = (EditText) findViewById(R.id.User);
        VechNoText =(EditText) findViewById(R.id.vechNo);
        modelText =(EditText) findViewById(R.id.model);
        mfgDtText = (EditText) findViewById(R.id.mfgDt);
        passwordText = (EditText) findViewById(R.id.password);
        progressBar = (ProgressBar) findViewById(R.id.login_pb);


        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));

            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SIGNUP();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (!(requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            finish();
        }
    }




    void SIGNUP() {
        {
            //String name, String vecicalNumber, String mfgDt, String model, String email, String password

            CU = new User(userText.getText().toString().trim(), VechNoText.getText().toString().trim(), mfgDtText.getText().toString().trim(), modelText.getText().toString().trim(), emailText.getText().toString().trim(), passwordText.getText().toString().trim());

            String user=userText.getText().toString();
            String email=emailText.getText().toString();
            String pass=passwordText.getText().toString();
            String vechno=VechNoText.getText().toString();
            String mfg=mfgDtText.getText().toString();
            String model=modelText.getText().toString();


            if(pass.isEmpty()||user.isEmpty()||model.isEmpty()||mfg.isEmpty()||vechno.isEmpty()||email.isEmpty())
            {    Toast.makeText(SignUpActivity.this, R.string.complete_form_warning, Toast.LENGTH_LONG).show();

            } else if (CU.getPassword().toString().length() < 6) {
                //If password size is less than zero then dont accept
                Toast.makeText(SignUpActivity.this, R.string.password_len, Toast.LENGTH_LONG).show();

            } else {

                progressBar.setVisibility(View.VISIBLE);
                //create user
                auth.createUserWithEmailAndPassword(CU.getEmail(), CU.getPassword())
                        .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Toast.makeText(SignUpActivity.this, "createUserWithEmail:onComplete:" + task.isSuccessful(), Toast.LENGTH_SHORT).show();
                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {
                                    Toast.makeText(SignUpActivity.this, "Authentication failed." + task.getException(),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    //successfullycreated user now login
                                    //authenticate user
                                    auth.signInWithEmailAndPassword(CU.getEmail(), CU.getPassword())
                                            .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                                                @Override
                                                public void onComplete(@NonNull Task<AuthResult> task) {
                                                    progressBar.setVisibility(View.GONE);
                                                    //loged in now write to data base
                                                    currentUser = auth.getCurrentUser();
                                                    Toast.makeText(getApplicationContext(),"loged in", Toast.LENGTH_LONG).show();
                                                    String uid = currentUser.getUid();
                                                    String path= uid + "/User";//userText.getText().toString().trim();
                                                    userDatabase= FirebaseDatabase.getInstance().getReference(path);
                                                    userDatabase.setValue(CU);
                                                    path= uid ;//userText.getText().toString().trim();
                                                    userDatabase= FirebaseDatabase.getInstance().getReference(path);
                                                    userDatabase.child("Sessions").setValue(" ");
                                                    //startTrackerService();
                                                    startActivity(new Intent(SignUpActivity.this, MapsActivity.class));
                                                }
                                            });
                                }
                            }
                        });

            }
        }
    }


    private void startTrackerService(){
        // startService(new Intent(this, TrackingService.class));
        startActivity(new Intent(SignUpActivity.this, MapsActivity.class));

    }





}





