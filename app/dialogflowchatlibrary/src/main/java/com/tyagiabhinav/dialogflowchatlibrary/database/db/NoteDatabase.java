package com.tyagiabhinav.dialogflowchatlibrary.database.db;


import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.tyagiabhinav.dialogflowchatlibrary.database.dao.DaoAccess;
import com.tyagiabhinav.dialogflowchatlibrary.database.model.Note;

@Database(entities = {Note.class}, version = 1, exportSchema = false)
public abstract class NoteDatabase extends RoomDatabase {

    public abstract DaoAccess daoAccess();
}
