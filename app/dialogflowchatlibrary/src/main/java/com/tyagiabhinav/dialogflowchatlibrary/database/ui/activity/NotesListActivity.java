package com.tyagiabhinav.dialogflowchatlibrary.database.ui.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.room.Room;

import com.tyagiabhinav.dialogflowchatlibrary.ChatbotActivity;
import com.tyagiabhinav.dialogflowchatlibrary.R;
import com.tyagiabhinav.dialogflowchatlibrary.database.AppConstants;
import com.tyagiabhinav.dialogflowchatlibrary.database.dao.DaoAccess;
import com.tyagiabhinav.dialogflowchatlibrary.database.db.NoteDatabase;
import com.tyagiabhinav.dialogflowchatlibrary.database.model.Note;
import com.tyagiabhinav.dialogflowchatlibrary.database.repository.NoteRepository;
import com.tyagiabhinav.dialogflowchatlibrary.database.ui.adapter.NotesListAdapter;
import com.tyagiabhinav.dialogflowchatlibrary.database.util.RecyclerItemClickListener;
import com.tyagiabhinav.dialogflowchatlibrary.database.util.TimestampConverter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class NotesListActivity extends AppCompatActivity implements
        AppConstants, View.OnClickListener {

    private RecyclerView recyclerView;
    private NotesListAdapter notesListAdapter;
    private Button floatingActionButton;
    public Date date;
    public String stringDate;

    private NoteRepository noteRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        noteRepository = new NoteRepository(getApplicationContext());

        recyclerView = findViewById(R.id.task_list);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2 , StaggeredGridLayoutManager.VERTICAL));
//        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, this));

        //floatingActionButton = findViewById(R.id.day);
        final Button Day = findViewById(R.id.day);
        final TextView textView = findViewById(R.id.textView);
        final Button all = findViewById(R.id.all);

//        final DaoAccess daoAccess = new DaoAccess() {
//            @Override
//            public Long insertTask(Note note) {
//                return null;
//            }
//
//            @Override
//            public LiveData<List<Note>> fetchAllTasks() {
//                return null;
//            }
//
//            @Override
//            public LiveData<List<Note>> fetchUserByUserDOB(Date calendar) {
//                return null;
//
//            };
//
//            @Override
//            public LiveData<Note> getTask(int taskId) {
//                return null;
//            }
//
//            @Override
//            public void updateTask(Note note) {
//
//            }
//
//            @Override
//            public void deleteTask(Note note) {
//
//            }
//        };

        Day.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);
                DatePickerDialog picker = new DatePickerDialog(NotesListActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                SimpleDateFormat DateFor = new SimpleDateFormat("dd/MM/yyyy");
                                try {
                                    date = DateFor.parse(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                                    Log.d("Nazar", date.toString());
                                    DateFor = new SimpleDateFormat("dd MMMM");
                                    stringDate = DateFor.format(date);
                                    textView.setText(stringDate);
                                    noteRepository.filterDaily(TimestampConverter.dateToTimestamp(date)).observe(NotesListActivity.this, new Observer<List<Note>>() {
                                        @Override
                                        public void onChanged(List<Note> notes) {
                                            ((NotesListAdapter) recyclerView.getAdapter()).addTasks(notes);
                                        }
                                    });

//                                    noteRepository.getTasks().observe(NotesListActivity.this, new Observer<List<Note>>() {
//                                        @Override
//                                        public void onChanged(List<Note> notes) {
//                                            System.out.println();
//                                        }
//                                    });

                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, year, month, day);
                picker.show();
            }

        });



        all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTaskList();
            }

        });



        //floatingActionButton.setOnClickListener(this);

        if (getIntent()!=null) {
            String title = getIntent().getStringExtra("Title");
            Date date = (Date) getIntent().getSerializableExtra("Date");

            //System.out.println("Ziya:: " + title);
            noteRepository.insertTask(title, date);
            updateTaskList();
        }


        updateTaskList();
    }

    private void updateTaskList() {
        noteRepository.getTasks().observe(this, new Observer<List<Note>>() {
            @Override
            public void onChanged(@Nullable List<Note> notes) {
                if(notes.size() > 0) {
                    recyclerView.setVisibility(View.VISIBLE);
                    if (notesListAdapter == null) {
                        notesListAdapter = new NotesListAdapter(notes);
                        recyclerView.setAdapter(notesListAdapter);

                    } else notesListAdapter.addTasks(notes);
                } else updateEmptyView();
            }
        });
    }

    private void updateEmptyView() {
        recyclerView.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            String title = data.getStringExtra(INTENT_TITLE);
                Date createdDate = (Date) data.getSerializableExtra(String.valueOf(INTENT_DATE));
                noteRepository.insertTask(title, createdDate);
            updateTaskList();
        }
    }

    @Override
    public void onClick(View view) {

    }
}
