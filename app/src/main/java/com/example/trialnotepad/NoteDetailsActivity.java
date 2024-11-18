package com.example.trialnotepad;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ScrollView;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import java.io.FileOutputStream;

public class NoteDetailsActivity extends AppCompatActivity {

    // UI components
    ImageButton backImageButton, deleteImageButton, saveImageButton;
    EditText titleEditText, contentEditText;
    BottomNavigationView bottomNav;

    // Note data variables for Editing the note
    String title, content, docId;
    boolean isEditMode = false;

    // Style state trackers
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

        // Initialize UI components
        backImageButton   = (ImageButton) findViewById(R.id.backImageButton);
        deleteImageButton = (ImageButton) findViewById(R.id.deleteImageButton);
        saveImageButton   = (ImageButton) findViewById(R.id.saveImageButton);
        contentEditText   = (EditText) findViewById(R.id.contentEditTextText);
        titleEditText     = (EditText) findViewById(R.id.titleEditTextText);
        bottomNav         = (BottomNavigationView) findViewById(R.id.bottomNav);

        //Retrieve Data to be Edited
        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content");
        docId = getIntent().getStringExtra("docId");

        if(docId!=null && !docId.isEmpty())
        {
            isEditMode = true;
        }

        // Set the note title and content
        titleEditText.setText(title);

        // Convert HTML content back to styled text for editing
        if (content != null) {
            Editable spannableContent = Editable.Factory.getInstance().newEditable(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
            contentEditText.setText(spannableContent);
        }

        if(isEditMode)
        {
            deleteImageButton.setVisibility(View.VISIBLE);
        }

        // Set up button click listeners
        backImageButton.setOnClickListener((v)-> back());
        deleteImageButton.setOnClickListener((v)-> deleteNoteFromFirebase());
        saveImageButton.setOnClickListener((v)-> saveNote());



        // Initialize Bottom Navigation Buttons for Text Formatting
        bottomNav.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNav.getMenu().size(); i++)
        {
            bottomNav.getMenu().getItem(i).setChecked(false);
        }

        // Handle formatting actions (bold, italic, underline)
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


        // Dynamically apply styles to new text as user types
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

    // Navigate back to the main activity
    void back()
    {
        startActivity(new Intent(NoteDetailsActivity.this,MainActivity.class));
    }

    void deleteNoteFromFirebase()
    {
        PopupMenu popupMenu = new PopupMenu(NoteDetailsActivity.this, deleteImageButton);
        popupMenu.getMenu().add("Delete");
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if(menuItem.getTitle()=="Delete")
                {
                    DocumentReference documentReference;

                    documentReference = Utility.getCollectionReferenceForNotes().document(docId);

                    documentReference.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful())
                            {
                                //Note is Deleted to Firebase Firestore
                                Utility.showToast(NoteDetailsActivity.this,
                                        "Note deleted successfully");
                                finish();
                            }
                            else
                            {
                                Utility.showToast(NoteDetailsActivity.this,
                                        "Failed while deleting the note");
                                finish();
                            }

                        }
                    });
                    return true;
                }
                return false;
            }
        });



    }

    // Save the note to both Firebase and local storage
    void saveNote()
    {
        String noteTitle = titleEditText.getText().toString();
        String noteContent = contentEditText.getText().toString();
        // Save as HTML
        String htmlContent = convertToHtml(contentEditText.getText());

        if(noteTitle == null || noteTitle.isEmpty())
        {
            titleEditText.setError("Title is required");
            return;
        }

        // Create a NoteModel object with the title and content
        NoteModel note = new NoteModel();
        note.setTitle(noteTitle);
        note.setContent(htmlContent);
        //note.setContent(noteContent);
        note.setTimestamp(Timestamp.now());

        saveNoteToFirebase(note);
        saveNoteToLocal(noteTitle, htmlContent);
        //saveNoteToLocal(noteTitle, noteContent);
    }

    // Save note to Firebase Firestore
    void saveNoteToFirebase(NoteModel note)
    {
        //Log.d("NoteDetails", "Saving Note to Firestore: "
          //      + note.getTitle() + " - " + note.getContent());

        DocumentReference documentReference;

        if(isEditMode)
        {       //Update the Note
            documentReference = Utility.getCollectionReferenceForNotes().document(docId);
        }
        else
        {       //Create New Note
            documentReference = Utility.getCollectionReferenceForNotes().document();
        }

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

    // Save note locally as an HTML file
    void saveNoteToLocal(String title, String content)
    {
        String filename = title.replaceAll("[ /\\\\\"<>|]", "_") + ".html"; //
        String fileContent = convertToHtml(contentEditText.getText()); //Convert to HTML format
        //String fileContent = "Title: " + title + "\n\nContent:\n" + content;
        Log.d("NoteDetails", "Saving Local File with content: " + fileContent);

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

    // Apply or remove bold/italic styles on selected text
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

    // Apply or remove underline style on selected text
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

    // Convert Editable text to HTML format
    private String convertToHtml(Editable text)
    {
        return Html.toHtml(text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
    }

}