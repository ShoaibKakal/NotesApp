package com.shoaib.notesapp.activies.adapters;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.makeramen.roundedimageview.RoundedImageView;
import com.shoaib.notesapp.R;
import com.shoaib.notesapp.entities.Note;
import com.shoaib.notesapp.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> notes;
    private NotesListener notesListener;
    private Timer timer;
    private List<Note>  notesSource;


    public NotesAdapter(List<Note> notes, NotesListener notesListener) {
        this.notes = notes;
        this.notesListener = notesListener;
        this.notesSource = notes;
    }



    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_note,parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, final int position) {
        final Note note = notes.get(position);
        holder.setNote(note);

        holder.layoutNoteContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                notesListener.onNoteClicked(note, position);
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }


    @Override
    public int getItemCount() {
        return notes.size();
    }

    public class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textSubtitle, textDateTime;
        LinearLayout layoutNoteContainer;
        RoundedImageView noteImage;
        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textSubtitle = itemView.findViewById(R.id.textSubtitle);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            layoutNoteContainer = itemView.findViewById(R.id.layoutNote);
            noteImage = itemView.findViewById(R.id.imageNote);
        }
        public void setNote(Note note){
            textTitle.setText(note.getTitle());

            if (note.getSubtitle().trim().isEmpty()){
                textSubtitle.setVisibility(View.GONE);
            }else {
                textSubtitle.setText(note.getSubtitle());
            }
            textDateTime.setText(note.getDateTime());

            GradientDrawable gradientDrawable = (GradientDrawable) layoutNoteContainer.getBackground();
            if (note.getColor() != null){
                gradientDrawable.setColor(Color.parseColor(note.getColor()));
            }
            else{
                gradientDrawable.setColor(Color.parseColor("#333333"));
            }

            if (note.getImagePath() != null){
                noteImage.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
                noteImage.setVisibility(View.VISIBLE);
            }
            else{
                noteImage.setVisibility(View.GONE);
            }

        }
    }

    public void searchNotes(final String searchKeyword){
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (searchKeyword.trim().isEmpty()){
                    notes = notesSource;
                }
                else{
                    ArrayList<Note> temp = new ArrayList<>();
                    for (Note note: notesSource){
                        if (note.getTitle().toLowerCase().contains(searchKeyword.toLowerCase())
                            || note.getSubtitle().toLowerCase().contains(searchKeyword.toLowerCase())
                            || note.getNoteText().toLowerCase().contains(searchKeyword.toLowerCase())){

                            temp.add(note);
                        }
                    }
                    notes = temp;
                }
               new Handler(Looper.getMainLooper()).post(new Runnable() {
                   @Override
                   public void run() {
                        notifyDataSetChanged();
                   }
               });


            }
        }, 500);
    }

    public void cancelTimer(){
        if (timer != null){
            timer.cancel();
        }
    }
}
