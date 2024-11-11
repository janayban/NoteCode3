package com.example.trialnotepad;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class CreateAccountActivity extends AppCompatActivity {

    //Global Variables
    EditText emailEditText, passwordEditText, confirmPasswordEditText;
    Button createAccountButton;
    ProgressBar createAccountProgressBar;
    TextView loginTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Variable Declarations
        emailEditText               = (EditText) findViewById(R.id.emailEditTextText);
        passwordEditText            = (EditText) findViewById(R.id.passwordEditTextText);
        confirmPasswordEditText     = (EditText) findViewById(R.id.confirmPasswordEditTextText);
        createAccountButton         = (Button) findViewById(R.id.createAccountButton);
        createAccountProgressBar    = (ProgressBar) findViewById(R.id.createAccountProgressBar);
        loginTextView               = (TextView) findViewById(R.id.loginTextView);

        //Event Management of Buttons
        createAccountButton.setOnClickListener((v)-> createAccount());
        loginTextView.setOnClickListener((v)-> finish());

    }

    //Function for taking email, password, and confirmPassword
    void createAccount()
    {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        boolean isValidated = validateData(email, password, confirmPassword);
        if(!isValidated)
        {
            return;
        }

        createAccountInFirebase(email, password);
    }

    //Create Account Function
    void createAccountInFirebase(String email, String password)
    {
        changeInProgress(true);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(
                CreateAccountActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        changeInProgress(false);
                        if(task.isSuccessful())
                        {
                            //Creating Account is Successful
                            Utility.showToast(CreateAccountActivity.this,
                                    "Account Created Successfully, check email to verify");
                            firebaseAuth.getCurrentUser().sendEmailVerification();
                            firebaseAuth.signOut();
                            finish();
                        }
                        else
                        {
                            //Creating Account Failed
                            Utility.showToast(CreateAccountActivity.this,task.
                                    getException().getLocalizedMessage());
                        }
                    }
                });
    }

    //Create Account Button Animation
    void changeInProgress(boolean inProgress)
    {
        if(inProgress)
        {
            createAccountProgressBar.setVisibility(View.VISIBLE);
            createAccountButton.setVisibility(View.GONE);
        }
        else
        {
            createAccountProgressBar.setVisibility(View.INVISIBLE);
            createAccountButton.setVisibility(View.VISIBLE);
        }

    }

    //Function to validate the data that are input by user.
    boolean validateData(String email, String password, String confirmPassword)
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
        if(!password.equals(confirmPassword))
        {
            confirmPasswordEditText.setError("Passwords do not match");
            return false;
        }
        return true;
    }

}