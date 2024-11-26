package com.example.trialnotepad;

import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
    SearchView searchBar;
    RecyclerView recyclerView;
    ImageButton menuImageButton;
    NoteAdapter noteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        addNoteButton   = (FloatingActionButton) findViewById(R.id.addFloatingActionButton);
        logOutTextView  = (TextView) findViewById(R.id.logOutTextView);
        searchBar       = (SearchView) findViewById(R.id.searchBarSearchView);
        recyclerView    = (RecyclerView) findViewById(R.id.recyclerView);
        menuImageButton = (ImageButton) findViewById(R.id.menuImageButton);


        addNoteButton.setOnClickListener((v) -> startActivity(new Intent(
                MainActivity.this, NoteDetailsActivity.class)));
        menuImageButton.setOnClickListener((v) -> showMenu());

        searchBar.clearFocus();

        setupRecyclerView("");

        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                setupRecyclerView(query.trim());
                return true; // Indicate that the query submission has been handled
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Check if the query is empty or the user pressed "X"
                if (newText == null || newText.trim().isEmpty()) {
                    searchBar.clearFocus(); // Clear focus from SearchView
                    setupRecyclerView(""); // Reset RecyclerView to default state
                } else {
                    setupRecyclerView(newText.trim()); // Update RecyclerView based on query
                }
                return true; // Return true to indicate the change was handled
            }
        });


        searchBar.setOnCloseListener(() -> {
            searchBar.setQuery("", false);
            searchBar.clearFocus(); // Clear focus from the SearchView
            setupRecyclerView("");  // Optionally reset RecyclerView to default state
            return false; // Return false to allow default behavior
        });

        // Add item click listener for RecyclerView
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                // Check if the event is a click
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    // Clear the focus from the SearchView when the RecyclerView is clicked
                    searchBar.clearFocus();
                }
                return false;  // Allow normal processing of the event
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                // No action required for touch event
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                // No action required for disallowing intercept
            }
        });

    }

    // Recycler View Function
    void setupRecyclerView(String searchQuery) {
        String lowercasedQuery = searchQuery.toLowerCase().trim();

        Query query;
        if (lowercasedQuery.isEmpty()) {

            query = Utility.getCollectionReferenceForNotes()
                    .orderBy("timestamp", Query.Direction.DESCENDING);
            searchBar.clearFocus();
        } else {
            // Query notes where title starts with the lowercase search query and order by timestamp
            query = Utility.getCollectionReferenceForNotes()
                    .orderBy("lowercaseTitle")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAt(lowercasedQuery)
                    .endAt(lowercasedQuery + "\uf8ff");
        }

        FirestoreRecyclerOptions<NoteModel> options = new FirestoreRecyclerOptions.Builder<NoteModel>()
                .setQuery(query, NoteModel.class)
                .build();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        noteAdapter = new NoteAdapter(options, this);
        recyclerView.setAdapter(noteAdapter);
        noteAdapter.startListening();  // Start listening for updates
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
    void showMenu() {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, menuImageButton, Gravity.CENTER,
                                            0, R.style.PopupMenuStyle);
        popupMenu.getMenu().add("Logout");
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getTitle() == "Logout") {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

}
