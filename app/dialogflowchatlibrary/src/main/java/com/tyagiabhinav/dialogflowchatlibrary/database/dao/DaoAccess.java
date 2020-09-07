package com.tyagiabhinav.dialogflowchatlibrary.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tyagiabhinav.dialogflowchatlibrary.database.model.Note;
import com.tyagiabhinav.dialogflowchatlibrary.database.ui.activity.NotesListActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.List;

@Dao
public interface DaoAccess {

    //NotesListActivity noteList = new NotesListActivity();
    //Date date = noteList.date;

//    Date calendar = Calendar.getInstance().getTime();

    @Insert
    Long insertTask(Note note);

//
    @Query("SELECT * FROM Note ORDER BY created_date desc")
    LiveData<List<Note>> fetchAllTasks();


    @Query("SELECT * FROM Note WHERE created_date=:calendar")
    LiveData<List<Note>> fetchUserByUserDOB(String calendar);

//    @Query("SELECT * FROM Note WHERE created_date=:calendar ")
//    public User[] loadAllUsersOlderThan(int calendar);

//    @Query("SELECT * FROM Note WHERE created_date =:calendar")
//    LiveData<List<Note>> fetchAllTasks(Date calendar);


    @Query("SELECT * FROM Note WHERE id =:taskId")
    LiveData<Note> getTask(int taskId);


    @Update
    void updateTask(Note note);


    @Delete
    void deleteTask(Note note);
}
