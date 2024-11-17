package com.example.trialnotepad;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

public class NoteDetailsActivity extends AppCompatActivity {

    ImageButton backImageButton, undoImageButton, redoImageButton, saveImageButton;
    EditText titleEditText, contentEditText;
    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //Initialize the components
        backImageButton = findViewById(R.id.backImageButton);
        saveImageButton = findViewById(R.id.saveImageButton);
        contentEditText = findViewById(R.id.contentEditTextText);
        titleEditText = findViewById(R.id.titleEditTextText);
        bottomNav = findViewById(R.id.bottomNav);



        backImageButton.setOnClickListener((v)-> back());
        saveImageButton.setOnClickListener((v)-> saveNote());





        // Clear any default selection
        bottomNav.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNav.getMenu().size(); i++) {
            bottomNav.getMenu().getItem(i).setChecked(false);
        }

        // Set an item selection listener
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Toggle the checked state of the selected item
            boolean isChecked = !item.isChecked();
            item.setChecked(isChecked);


            // Return false to allow multiple selections (so it doesn't handle the selection automatically)
            return false;
        });

    }

    void back()
    {
        startActivity(new Intent(NoteDetailsActivity.this,MainActivity.class));
    }

    void saveNote()
    {
        String noteTitle = titleEditText.getText().toString();
        String noteContent = contentEditText.getText().toString();
        if(noteTitle == null || noteTitle.isEmpty())
        {
            titleEditText.setError("Title is required");
            return;
        }
        NoteModel note = new NoteModel();
        note.setTitle(noteTitle);
        note.setContent(noteContent);
        note.setTimestamp(Timestamp.now());

        saveNoteToFirebase(note);
    }

    void saveNoteToFirebase(NoteModel note)
    {
        DocumentReference documentReference;
        documentReference = Utility.getCollectionReferenceForNotes().document();

        documentReference.set(note).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(task.isSuccessful())
                {
                    //Note is Added to Firebase Firestore
                    Utility.showToast(NoteDetailsActivity.this,
                            "Note added successfully");
                    finish();
                }
                else
                {
                    Utility.showToast(NoteDetailsActivity.this,
                            "Failed while adding note");
                }

            }
        });
    }

}