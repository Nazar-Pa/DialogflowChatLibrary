package com.tyagiabhinav.dialogflowchatlibrary.database.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.tyagiabhinav.dialogflowchatlibrary.R;
import com.tyagiabhinav.dialogflowchatlibrary.database.model.Note;
import com.tyagiabhinav.dialogflowchatlibrary.database.util.NoteDiffUtil;
import com.tyagiabhinav.dialogflowchatlibrary.database.util.TimestampConverter;

import java.util.List;

public class NotesListAdapter extends RecyclerView.Adapter<NotesListAdapter.CustomViewHolder> {

    private List<Note> notes;
    public NotesListAdapter(List<Note> notes) {
        this.notes = notes;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_list_item, null);
        CustomViewHolder viewHolder = new CustomViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        Note note = getItem(position);
//My Notes page`inde Title ve Time`in gorunmesi burda olur
        holder.itemTitle.setText(note.getTitle());
        holder.itemDate.setText(TimestampConverter.dateToTimestamp(note.getCreatedDate()));
//        SimpleDateFormat DateFor = new SimpleDateFormat("dd MMMM");
//
//        String stringDate = DateFor.format(note.getCreatedDate());

        //holder.itemTime2.setText(note.getCreatedDate().toString());
        //holder.itemTime2.setText(note.getFormattedDate());


    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public Note getItem(int position) {
        return notes.get(position);
    }

    public void addTasks(List<Note> newNotes) {
        NoteDiffUtil noteDiffUtil = new NoteDiffUtil(notes, newNotes);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(noteDiffUtil);
        notes.clear();
        notes.addAll(newNotes);
        diffResult.dispatchUpdatesTo(this);
    }

    protected class CustomViewHolder extends RecyclerView.ViewHolder {

        private TextView itemTitle, itemDate;
        public CustomViewHolder(View itemView) {
            super(itemView);

            itemTitle = itemView.findViewById(R.id.item_title);
            itemDate = itemView.findViewById(R.id.date);
            //itemTime2 = itemView.findViewById(R.id.item_title2);
        }
    }
}
