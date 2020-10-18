package com.shoaib.notesapp.activies;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.shoaib.notesapp.R;
import com.shoaib.notesapp.activies.adapters.NotesAdapter;
import com.shoaib.notesapp.database.NotesDatabase;
import com.shoaib.notesapp.entities.Note;
import com.shoaib.notesapp.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity implements NotesListener {

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTE = 3;
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 4;
    public static final int REQUEST_CODE_SELECT_IMAGE = 5;


    private static final String TAG = "mytag";

    private ImageView imageAddNoteMain;
    private EditText inputSearch;
    private RecyclerView notesRecyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;
    private ImageView imageAddNote, imageAddImage, imageAddURL;
    private AlertDialog dialogAddURL;
    private ShimmerFrameLayout shimmerFrameLayout;

    private int noteClickedPosition = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        imageAddNoteMain = findViewById(R.id.imageAddNoteMain);
        inputSearch = findViewById(R.id.inputSearch);
        imageAddNote = findViewById(R.id.imageAddNote);
        imageAddImage = findViewById(R.id.imageAddImage);
        imageAddURL = findViewById(R.id.imageAddWebLink);
        shimmerFrameLayout = findViewById(R.id.shimmerEffect);



        //Hide the keyboard
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this, CreateNoteActivity.class), REQUEST_CODE_ADD_NOTE);
            }
        });

        notesRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL));
        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecyclerView.setAdapter(notesAdapter);
        getNotes(REQUEST_CODE_SHOW_NOTE,false);


        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (noteList.size() != 0){
                    notesAdapter.searchNotes(s.toString());
                }
            }
        });


        //Quick add

        imageAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                startActivityForResult(intent,REQUEST_CODE_ADD_NOTE);
            }
        });


        imageAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);

                }
                else{
                    selectImage();
                }
            }
        });

        imageAddURL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_add_url, (ViewGroup) findViewById(R.id.layoutAddUrlContainer),false);
                builder.setView(view);
                dialogAddURL = builder.create();

                if (dialogAddURL.getWindow() != null){
                    dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                }

                final EditText urlText = view.findViewById(R.id.inputURL);

                view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (urlText.toString().isEmpty()){
                            Toast.makeText(MainActivity.this, "Add Url", Toast.LENGTH_SHORT).show();
                        }
                        else if (!Patterns.WEB_URL.matcher(urlText.getText().toString()).matches()){
                            Toast.makeText(MainActivity.this, "Enter a valid URL", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                            intent.putExtra("inputURL",urlText.getText().toString().trim());
                            intent.putExtra("isQuickURL",true);
                            startActivityForResult(intent,REQUEST_CODE_ADD_NOTE);
                            dialogAddURL.dismiss();
                        }
                    }
                });

                view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialogAddURL.dismiss();
                    }
                });

                dialogAddURL.show();
            }
        });
    } // onCreate ends

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    public void getNotes(final int requestCode, final boolean isNoteDeleted){
        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>>{
            @Override
            protected List<Note> doInBackground(Void... voids) {
                return  NotesDatabase.getDatabase(getApplicationContext()).noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                Log.d(TAG, "onPostExecute: "+ notes.toString());

                if (requestCode == REQUEST_CODE_SHOW_NOTE){
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                    /*
                    Added code here to stop shimmer because this REQUEST_CODE_SHOE_NOTE will only calls when app starts. If i add this code to somewhere else then
                    shimmer will automatically starts whenever i add or view any note.
                     */
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            shimmerFrameLayout.hideShimmer();
                            shimmerFrameLayout.setVisibility(View.GONE);
                            notesRecyclerView.setVisibility(View.VISIBLE);
                        }
                    },4000);
                }
                else if(requestCode == REQUEST_CODE_ADD_NOTE){
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    notesRecyclerView.smoothScrollToPosition(0);
                }
                else if(requestCode == REQUEST_CODE_UPDATE_NOTE){
                    noteList.remove(noteClickedPosition);

                    if (isNoteDeleted){
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                    }
                    else{
                        noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }

                }
            }
        }

        new GetNotesTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK ){
            getNotes(REQUEST_CODE_ADD_NOTE,false);
            notesRecyclerView.setAdapter(notesAdapter);
        }
        else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode ==  RESULT_OK){
            if(data != null){
                getNotes(REQUEST_CODE_UPDATE_NOTE,data.getBooleanExtra("isNoteDeleted",false));
            }
            notesRecyclerView.setAdapter(notesAdapter);
        }
        else if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            if (data != null){
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null){

                    Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                    intent.putExtra("imageUri",selectedImageUri.toString());
                    intent.putExtra("quickImage",true);
                    startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);

                }
            }
        }


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length>0 ){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage();
            }
            else{
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onNoteClicked(Note note, int position) {

        //Hide keyboard
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        startActivityForResult(intent,REQUEST_CODE_UPDATE_NOTE);

    }


}