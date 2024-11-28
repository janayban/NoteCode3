package com.example.trialnotepad;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
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
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NoteDetailsActivity extends AppCompatActivity {

    // UI components
    ImageButton backImageButton, deleteImageButton, saveImageButton, saveAsImageButton;
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
        saveAsImageButton = (ImageButton) findViewById(R.id.saveAsImageButton);
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

        saveAsImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create the PopupMenu when the button is clicked
                PopupMenu popupMenu = new PopupMenu(NoteDetailsActivity.this, saveAsImageButton, Gravity.CENTER,
                                                    0, R.style.PopupMenuStyle);

                // Add menu items
                popupMenu.getMenu().add("Save as PDF");
                popupMenu.getMenu().add("Save as Docx");

                // Show the popup menu
                popupMenu.show();

                // Set the listener for menu item clicks
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        // Check which item was clicked
                        switch (item.getTitle().toString()) {
                            case "Save as PDF":
                                // Call the saveAs() method to save as PDF
                                saveAsPDF();
                                return true;
                            case "Save as Docx":
                                // Call the saveAsDocs() method to save as Docs (you can implement this)
                                saveAsDocx();
                                return true;
                            default:
                                return false;
                        }
                    }
                });
            }
        });

        //qrCodeGenerator();

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
            else if (itemId == R.id.item_qrcode_scanner)
            {
                scanCode();
            }
            else if (itemId == R.id.item_qrcode_generator)
            {
            // Handle QR code generator action
            String titleText = titleEditText.getText().toString();
            String contentText = contentEditText.getText().toString();

                if (titleText.isEmpty() && contentText.isEmpty())
                {
                    Toast.makeText(this, "Note is empty. Cannot generate QR code.", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Intent intent = new Intent(this, QRCodeGenerationActivity.class);
                    intent.putExtra("keyTitle", titleText);
                    intent.putExtra("keyContent", contentText);
                    startActivity(intent);
                }
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
        finish();
        //startActivity(new Intent(NoteDetailsActivity.this,MainActivity.class));
    }

    void deleteNoteFromFirebase()
    {
        PopupMenu popupMenu = new PopupMenu(NoteDetailsActivity.this, deleteImageButton, Gravity.CENTER,
                                            0, R.style.PopupMenuStyle);
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
                                //finish();
                            }
                            else
                            {
                                Utility.showToast(NoteDetailsActivity.this,
                                        "Failed while deleting the note");
                                //finish();
                            }

                        }
                    });
                    finish();
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
        //saveNoteToLocal(noteTitle, htmlContent);
        //saveNoteToLocal(noteTitle, noteContent);
    }

    // Save note to Firebase Firestore
    void saveNoteToFirebase(NoteModel note)
    {

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
                            "Note saved to the cloud successfully");
                }
                else
                {
                    Utility.showToast(NoteDetailsActivity.this,
                            "Note saved to the local storage");
                }

            }
        }); finish();

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

    // QR Scanner Function
    void scanCode()
    {
        ScanOptions options = new ScanOptions();
        options.setPrompt("  Press Volume Up to turn the flash on\nPress Volume Down to turn the flash off");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }

    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            // Get the current content of the EditText
            Editable existingText = contentEditText.getText();

            // Clean up the scanned content
            String scannedContent = result.getContents().trim(); // Remove leading and trailing whitespace

            // If there is existing text, add a newline before appending scanned content
            if (existingText.length() > 0) {
                existingText.append("\n\n");
            }

            // Append the scanned content
            existingText.append(scannedContent);

            // Set the updated content back to the EditText (styles will persist)
            contentEditText.setText(existingText);
        }
    });

    void saveAsPDF() {
        // Get the title and content text
        String titleText = titleEditText.getText().toString().trim();
        String contentText = contentEditText.getText().toString().trim();

        // Check if both title and content are empty
        if (titleText.isEmpty() && contentText.isEmpty()) {
            // Show a toast message indicating the note is empty
            Toast.makeText(NoteDetailsActivity.this, "The note is empty. Please provide a title or content.", Toast.LENGTH_SHORT).show();
            return; // Return early and do not proceed with PDF generation
        }

        // File name
        String fileName = titleText.isEmpty() ? "Untitled" : titleText; // Default to "Untitled" if no title
        fileName += ".pdf"; // Always save as a PDF

        // Log the path for debugging
        Log.d("PDF Path", "Saving PDF as: " + fileName);

        // Get a reference to the ContentResolver
        ContentResolver contentResolver = getContentResolver();

        // Create the content values for the new PDF file
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");

        // Specify the location in the Downloads folder
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/");

        // For Android 10 and above, use MediaStore to get the URI
        Uri pdfUri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues);

        if (pdfUri != null) {
            try (OutputStream outputStream = contentResolver.openOutputStream(pdfUri)) {
                // Create a new PdfDocument to generate the PDF
                PdfDocument pdfDocument = new PdfDocument();

                // A4 size in points (8.27 x 11.69 inches at 72 points per inch)
                int pageWidth = 595; // 8.27 inches * 72
                int pageHeight = 842; // 11.69 inches * 72
                int margin = 72; // 1 inch margin (72 points)

                // Create a page with A4 dimensions
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                // Get the Canvas to draw on the PDF page
                Canvas canvas = page.getCanvas();

                // Set up Paint for text appearance
                Paint titlePaint = new Paint();
                titlePaint.setTextSize(14);
                titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); // Bold title
                titlePaint.setColor(Color.BLACK);

                Paint contentPaint = new Paint();
                contentPaint.setTextSize(12);
                contentPaint.setColor(Color.BLACK);

                // Add titleText to the PDF (if title is not empty)
                int startX = margin; // Start at the left margin
                int startY = margin + 20; // Start at the top margin, slightly offset

                if (!titleText.isEmpty()) {
                    // Draw the title if available
                    canvas.drawText("Title: " + titleText, startX, startY, titlePaint);
                    startY += 25; // Add space below the title
                }

                // After the title, adjust startY for content (if content exists)
                startY += 10; // Add a slight space after title before content

                // Process the content text
                int maxTextWidth = pageWidth - 2 * margin; // Maximum width for text within margins
                Paint.FontMetrics fontMetrics = contentPaint.getFontMetrics(); // Get font metrics for accurate line spacing
                float lineHeight = fontMetrics.bottom - fontMetrics.top + 5; // Line height

                // Split content into paragraphs (even if it's empty)
                String[] paragraphs = contentText.split("\n");

                for (String paragraph : paragraphs) { // Process each paragraph
                    String[] words = paragraph.split(" "); // Split the paragraph into words
                    StringBuilder line = new StringBuilder();

                    for (String word : words) {
                        String testLine = line.length() > 0 ? line + " " + word : word;
                        float textWidth = contentPaint.measureText(testLine);

                        if (textWidth > maxTextWidth) {
                            // Draw the current line and reset for the next line
                            canvas.drawText(line.toString(), startX, startY, contentPaint);
                            startY += lineHeight; // Move to next line

                            line = new StringBuilder(word); // Start a new line

                            // Check if we need a new page
                            if (startY + lineHeight > pageHeight - margin) {
                                pdfDocument.finishPage(page);
                                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                                page = pdfDocument.startPage(pageInfo);
                                canvas = page.getCanvas();
                                startY = margin; // Reset Y position for the new page
                            }
                        } else {
                            line.append(" ").append(word);

                        }
                    }

                    // Draw the last line of the paragraph
                    if (line.length() > 0) {
                        canvas.drawText(line.toString(), startX, startY, contentPaint);
                        startY += lineHeight; // Move to next line
                    }


                    // Check if we need a new page for the next paragraph
                    if (startY + lineHeight > pageHeight - margin) {
                        pdfDocument.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                        page = pdfDocument.startPage(pageInfo);
                        canvas = page.getCanvas();
                        startY = margin; // Reset Y position for the new page
                    }
                }

                // Finish and close the last page
                pdfDocument.finishPage(page);

                // Save the document to the specified file using the output stream
                pdfDocument.writeTo(outputStream);
                pdfDocument.close();

                Toast.makeText(NoteDetailsActivity.this, "PDF saved to Downloads folder!", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(NoteDetailsActivity.this, "Error creating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(NoteDetailsActivity.this, "Failed to create file URI!", Toast.LENGTH_SHORT).show();
        }
    }


    void saveAsDocx() {
        // Get the title and content text
        String titleText = titleEditText.getText().toString().trim();
        String contentText = contentEditText.getText().toString().trim();

        // Check if both title and content are empty
        if (titleText.isEmpty() && contentText.isEmpty()) {
            Toast.makeText(NoteDetailsActivity.this, "The note is empty. Please provide a title or content.", Toast.LENGTH_SHORT).show();
            return; // Return early
        }

        // File name
        String fileName = titleText.isEmpty() ? "Untitled" : titleText;
        fileName += ".docx";

        // Log the file name for debugging
        Log.d("DOCX Path", "Saving DOCX as: " + fileName);

        // Get a reference to the ContentResolver
        ContentResolver contentResolver = getContentResolver();

        // Create the content values for the new DOCX file
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/");

        // Create the URI for the DOCX file
        Uri docxUri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues);

        if (docxUri != null) {
            try (OutputStream outputStream = contentResolver.openOutputStream(docxUri)) {
                // Create a new Word document
                XWPFDocument document = new XWPFDocument();

                // Create a title paragraph if the title is not empty
                if (!titleText.isEmpty()) {
                    XWPFParagraph titleParagraph = document.createParagraph();
                    titleParagraph.setAlignment(ParagraphAlignment.CENTER); // Center-align the title
                    XWPFRun titleRun = titleParagraph.createRun();
                    titleRun.setBold(true);
                    titleRun.setFontSize(16);
                    titleRun.setText(titleText);
                }

                // Add a blank paragraph to separate title and content
                document.createParagraph();

                // Add the content paragraph
                XWPFParagraph contentParagraph = document.createParagraph();
                contentParagraph.setAlignment(ParagraphAlignment.LEFT); // Left-align the content
                XWPFRun contentRun = contentParagraph.createRun();
                contentRun.setFontSize(12);
                contentRun.setText(contentText);

                // Save the document to the output stream
                document.write(outputStream);
                document.close();

                Toast.makeText(NoteDetailsActivity.this, "DOCX saved to Downloads folder!", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(NoteDetailsActivity.this, "Error creating DOCX: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(NoteDetailsActivity.this, "Failed to create file URI!", Toast.LENGTH_SHORT).show();
        }
    }

}