package com.tyagiabhinav.dialogflowchatlibrary.database.repository;

import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.room.Room;

import com.tyagiabhinav.dialogflowchatlibrary.database.db.NoteDatabase;
import com.tyagiabhinav.dialogflowchatlibrary.database.model.Note;
import com.tyagiabhinav.dialogflowchatlibrary.database.util.AppUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class NoteRepository {

    private String DB_NAME = "db_task";

    private NoteDatabase noteDatabase;
    public NoteRepository(Context context) {
        noteDatabase = Room.databaseBuilder(context, NoteDatabase.class, DB_NAME).build();
    }

    public void insertTask(String title, Date createdDate,
                           String description) {

        insertTask(title, createdDate);
    }

    public void insertTask(String title,
                           Date createdDate
                           ) {

        Note note = new Note();
        note.setTitle(title);
        note.setCreatedDate(createdDate);


        insertTask(note);
    }

    public void insertTask(final Note note) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                noteDatabase.daoAccess().insertTask(note);
                return null;
            }
        }.execute();
    }


    public void deleteTask(final int id) {
        final LiveData<Note> task = getTask(id);
        if(task != null) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    noteDatabase.daoAccess().deleteTask(task.getValue());
                    return null;
                }
            }.execute();
        }
    }

    public void deleteTask(final Note note) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                noteDatabase.daoAccess().deleteTask(note);
                return null;
            }
        }.execute();
    }

    public LiveData<Note> getTask(int id) {
        return noteDatabase.daoAccess().getTask(id);
    }

    public LiveData<List<Note>> getTasks() {
        return noteDatabase.daoAccess().fetchAllTasks();
    }

    public LiveData<List<Note>> filterDaily(String calendar) {
        return noteDatabase.daoAccess().fetchUserByUserDOB(calendar);
    }
}
