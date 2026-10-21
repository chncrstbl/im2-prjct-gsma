package com.example.priangradingsystem.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.grades.GradeEntryActivity;
import com.example.priangradingsystem.model.StudentItem;
import java.util.List;

@SuppressWarnings("all")
public class StudentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<StudentItem> studentList;

    public StudentAdapter(List<StudentItem> studentList) {
        this.studentList = studentList;
    }

    @Override
    public int getItemViewType(int position) {
        return studentList.get(position).getItemViewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == StudentItem.TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
            return new StudentViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        StudentItem item = studentList.get(position);

        if (holder.getItemViewType() == StudentItem.TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.tvHeaderTitle.setText(item.getHeaderTitle());

            if (item.getHeaderIconResId() != 0) {
                headerHolder.imgHeaderIcon.setImageResource(item.getHeaderIconResId());
                headerHolder.imgHeaderIcon.setVisibility(View.VISIBLE);
            } else {
                headerHolder.imgHeaderIcon.setVisibility(View.GONE);
            }
        } else {
            StudentViewHolder studentHolder = (StudentViewHolder) holder;
            studentHolder.tvStudentName.setText(item.getName());
            studentHolder.tvStudentAge.setVisibility(View.GONE);

            try {
                if (item.getPhotoUri() != null && !item.getPhotoUri().isEmpty()) {
                    Uri uri = Uri.parse(item.getPhotoUri());
                    try {
                        holder.itemView.getContext().getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception ignored) {}

                    studentHolder.imgStudentAvatar.setImageURI(uri);
                } else {
                    studentHolder.imgStudentAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } catch (Exception e) {
                studentHolder.imgStudentAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            studentHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), GradeEntryActivity.class);
                intent.putExtra("STUDENT_NAME", item.getName());
                String level = item.getLevel() != null ? item.getLevel() : item.getSection();
                intent.putExtra("STUDENT_LEVEL", level);
                if (item.getPhotoUri() != null) {
                    intent.putExtra("STUDENT_PHOTO_URI", item.getPhotoUri());
                }
                v.getContext().startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderTitle;
        ImageView imgHeaderIcon;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeaderTitle = itemView.findViewById(R.id.tvHeaderTitle);
            imgHeaderIcon = itemView.findViewById(R.id.imgHeaderIcon);
        }
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgStudentAvatar;
        TextView tvStudentName, tvStudentAge;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgStudentAvatar = itemView.findViewById(R.id.imgStudentAvatar);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentAge = itemView.findViewById(R.id.tvStudentAge);
        }
    }
}
