package com.example.trialnotepad;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class NoteAdapter extends FirestoreRecyclerAdapter<NoteModel, NoteAdapter.NoteViewHolder> {
    Context context;


    public NoteAdapter(@NonNull FirestoreRecyclerOptions<NoteModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull NoteViewHolder holder, int position, @NonNull NoteModel note) {
        holder.titleTextView.setText(note.title);
        holder.contentTextView.setText(note.content);
        String previewContent = Html.fromHtml(note.getContent(), Html.FROM_HTML_MODE_LEGACY).toString();

        holder.contentTextView.setText(previewContent.length() > 100
                ? previewContent.substring(0, 100) + "..."
                : previewContent);

        holder.timestampTextView.setText(Utility.timestampToString(note.timestamp));

        holder.itemView.setOnClickListener((v)-> {
            Intent intent = new Intent(context, NoteDetailsActivity.class);
            intent.putExtra("title", note.title);
            intent.putExtra("content", note.getContent());
            //intent.putExtra("content", note.content);

            String docId = this.getSnapshots().getSnapshot(position).getId();
            intent.putExtra("docId", docId);

            context.startActivity(intent);

        });

    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_note_item, parent, false);

        return new NoteViewHolder(view);
    }

    class NoteViewHolder extends RecyclerView.ViewHolder{

        TextView titleTextView, contentTextView, timestampTextView;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);

            titleTextView       = itemView.findViewById(R.id.noteTitleTextView);
            contentTextView     = itemView.findViewById(R.id.noteContentTextView);
            timestampTextView   = itemView.findViewById(R.id.noteTimestampTextView);
        }
    }
}
