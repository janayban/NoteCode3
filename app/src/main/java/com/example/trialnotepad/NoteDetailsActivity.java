package com.example.trialnotepad;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
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

    private boolean isBoldActive = false; // Tracks the bold state
    private boolean isItalicActive = false; // Tracks the italic state
    private boolean isUnderlineActive = false; // Tracks the underline state

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



        // Initialize Bottom Navigation
        bottomNav.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNav.getMenu().size(); i++)
        {
            bottomNav.getMenu().getItem(i).setChecked(false);
        }

        // Set an item selection listener for Bold, Italic, and Underline
        bottomNav.setOnItemSelectedListener(item ->
        {
            int itemId = item.getItemId();
            if (itemId == R.id.item_bold)
            {
                isBoldActive = !item.isChecked();
                item.setChecked(isBoldActive);
                toggleStyleOnSelection(Typeface.BOLD, isBoldActive);
            }
            else if (itemId == R.id.item_italicize)
            {
                isItalicActive = !item.isChecked();
                item.setChecked(isItalicActive);
                toggleStyleOnSelection(Typeface.ITALIC, isItalicActive);
            }
            else if (itemId == R.id.item_underline)
            {
                isUnderlineActive = !item.isChecked();
                item.setChecked(isUnderlineActive);
                toggleUnderlineOnSelection(isUnderlineActive);
            }
            return false;
        });


        // Add a TextWatcher to apply styles dynamically for new text
        contentEditText.addTextChangedListener(new TextWatcher()
        {
            private int startBeforeChange;
            private int endBeforeChange;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                startBeforeChange = start;
                endBeforeChange = start + count;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed during text changes
            }

            @Override
            public void afterTextChanged(Editable s) {
                int start = startBeforeChange;
                int end = startBeforeChange + (s.length() - endBeforeChange);

                if (start < end)
                { // Avoid IndexOutOfBoundsException
                    if (isBoldActive)
                    {
                        StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
                        s.setSpan(boldSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (isItalicActive)
                    {
                        StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);
                        s.setSpan(italicSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (isUnderlineActive)
                    {
                        UnderlineSpan underlineSpan = new UnderlineSpan();
                        s.setSpan(underlineSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
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
                            "Note added to local storage");
                    finish();
                }

            }
        });


    }

    void saveNoteToLocal(String title, String content)
    {
        String filename = title.replaceAll("[ /\\\\\"<>|]", "_") + ".txt"; //
        String fileContent = "Title: " + title + "\n\nContent:\n" + content;

        try
        {
            // Writing to internal storage
            FileOutputStream fos = openFileOutput(filename, MODE_PRIVATE);
            fos.write(fileContent.getBytes());
            fos.close();

            Toast.makeText(this, "Note saved locally as " + filename, Toast.LENGTH_SHORT).show();
            finish();

        }
        catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save note locally", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void toggleStyleOnSelection(int style, boolean makeStyle)
    {
        int start = contentEditText.getSelectionStart();
        int end = contentEditText.getSelectionEnd();

        if (start < end)
        { // Check if there is a valid selection
            Editable text = contentEditText.getText();

            if (makeStyle)
            {
                // Apply style
                StyleSpan styleSpan = new StyleSpan(style);
                text.setSpan(styleSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            else
            {
                // Remove style
                StyleSpan[] spans = text.getSpans(start, end, StyleSpan.class);
                for (StyleSpan span : spans)
                {
                    if (span.getStyle() == style)
                    {
                        text.removeSpan(span);
                    }
                }
            }
        }
    }


    private void toggleUnderlineOnSelection(boolean makeUnderline)
    {
        int start = contentEditText.getSelectionStart();
        int end = contentEditText.getSelectionEnd();

        if (start < end)
        { // Check if there is a valid selection
            Editable text = contentEditText.getText();

            if (makeUnderline)
            {
                // Apply underline
                UnderlineSpan underlineSpan = new UnderlineSpan();
                text.setSpan(underlineSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            else
            {
                // Remove underline
                UnderlineSpan[] spans = text.getSpans(start, end, UnderlineSpan.class);
                for (UnderlineSpan span : spans)
                {
                    text.removeSpan(span);
                }
            }
        }
    }

}