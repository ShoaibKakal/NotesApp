package com.shoaib.notesapp.listeners;

import com.shoaib.notesapp.entities.Note;

public interface NotesListener {

    void onNoteClicked(Note note, int position);

}
