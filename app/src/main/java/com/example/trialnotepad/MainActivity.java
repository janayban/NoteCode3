package com.example.trialnotepad;

import android.app.DownloadManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton addNoteButton;
    TextView logOutTextView;
    RecyclerView recyclerView;
    ImageButton menuImageButton;
    NoteAdapter noteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        addNoteButton   = (FloatingActionButton) findViewById(R.id.addFloatingActionButton);
        logOutTextView  = (TextView) findViewById(R.id.logOutTextView);
        recyclerView    = (RecyclerView) findViewById(R.id.recyclerView);
        menuImageButton = (ImageButton) findViewById(R.id.menuImageButton);


        addNoteButton.setOnClickListener((v)-> startActivity(new Intent(
                      MainActivity.this, NoteDetailsActivity.class)));
        menuImageButton.setOnClickListener((v)-> showMenu());
        logOutTextView.setOnClickListener((v)-> logout());

        setupRecyclerView();


    }

    void setupRecyclerView()
    {
        Query query = Utility.getCollectionReferenceForNotes().orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<NoteModel> options = new FirestoreRecyclerOptions.Builder<NoteModel>()
                .setQuery(query, NoteModel.class).build();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        noteAdapter = new NoteAdapter(options, this);
        recyclerView.setAdapter(noteAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        noteAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        noteAdapter.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        noteAdapter.notifyDataSetChanged();
    }

    //Menu for log out
    void showMenu()
    {

    }

    void logout()
    {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}