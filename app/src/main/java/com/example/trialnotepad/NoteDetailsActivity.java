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

import java.io.FileOutputStream;

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
        backImageButton = (ImageButton) findViewById(R.id.backImageButton);
        saveImageButton = (ImageButton) findViewById(R.id.saveImageButton);
        contentEditText = (EditText) findViewById(R.id.contentEditTextText);
        titleEditText   = (EditText) findViewById(R.id.titleEditTextText);
        bottomNav       = (BottomNavigationView) findViewById(R.id.bottomNav);



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

        saveNoteToLocal(noteTitle, noteContent);
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
                            "Note added successfully to cloud");
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

    void saveNoteToLocal(String title, String content) {
        String filename = title.replaceAll("[ /\\\\\"<>|]", "_") + ".txt"; // Replace spaces with underscores for file naming
        String fileContent = "Title: " + title + "\n\nContent:\n" + content;

        try {
            // Writing to internal storage
            FileOutputStream fos = openFileOutput(filename, MODE_PRIVATE);
            fos.write(fileContent.getBytes());
            fos.close();

            Toast.makeText(this, "Note saved locally as " + filename, Toast.LENGTH_SHORT).show();
            finish();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save note locally", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

}