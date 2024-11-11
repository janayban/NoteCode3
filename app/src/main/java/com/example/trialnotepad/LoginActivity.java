package com.example.trialnotepad;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    //Global Variables
    EditText emailEditText, passwordEditText;
    Button loginButton;
    ProgressBar loginProgressBar;
    TextView createAccountTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Variable Declarations
        emailEditText               = (EditText) findViewById(R.id.emailEditTextText);
        passwordEditText            = (EditText) findViewById(R.id.passwordEditTextText);
        loginButton                 = (Button) findViewById(R.id.loginButton);
        loginProgressBar            = (ProgressBar) findViewById(R.id.loginProgressBar);
        createAccountTextView       = (TextView) findViewById(R.id.createAccountTextView);

        //Event Management of Buttons
        loginButton.setOnClickListener((v)-> loginUser());
        createAccountTextView.setOnClickListener((v)-> startActivity(new Intent(
                              LoginActivity.this, CreateAccountActivity.class)));

    }

    //Account Validation Function
    void loginUser()
    {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        boolean isValidated = validateData(email, password);
        if(!isValidated)
        {
            return;
        }

        loginAccountInFirebase(email, password);
    }

    // Login Function
    void loginAccountInFirebase(String email, String password)
    {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        changeInProgress(true);
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                changeInProgress(false);

                if(task.isSuccessful())
                {
                    //Login is Successful
                    if(firebaseAuth.getCurrentUser().isEmailVerified())
                    {
                        //Go to Main Activity
                        startActivity(new Intent(LoginActivity.this,
                                                 MainActivity.class));
                        finish();
                    }
                    else
                    {
                        Utility.showToast(LoginActivity.this,
                                          "Email not verified, please check your email.");
                    }

                }
                else
                {
                    //Login Failed
                    Utility.showToast(LoginActivity.this, task.getException().
                            getLocalizedMessage());
                }
            }
        });
    }

    //Login Button Animation
    void changeInProgress(boolean inProgress)
    {
        if(inProgress)
        {
            loginProgressBar.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
        }
        else
        {
            loginProgressBar.setVisibility(View.INVISIBLE);
            loginButton.setVisibility(View.VISIBLE);
        }

    }

    //Function to validate the data that are input by user.
    boolean validateData(String email, String password)
    {
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            emailEditText.setError("Email is invalid");
            return false;
        }
        if(password.length()<6)
        {
            passwordEditText.setError("Password length is invalid");
            return false;
        }
        return true;
    }
}