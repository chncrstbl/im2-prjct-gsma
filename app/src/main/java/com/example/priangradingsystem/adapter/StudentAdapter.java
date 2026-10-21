package com.example.priangradingsystem.adapter;

import android.annotation.SuppressLint;
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
import com.example.priangradingsystem.model.StudentItem;

import java.util.List;
import java.util.function.Consumer;

@SuppressLint({"ConcatenationWithLocaleDependantString", "DiscouragedApi", "SetTextI18n"})
@SuppressWarnings({"FieldCanBeLocal", "unused", "ConstantConditions", "RedundantSuppression"})
public class StudentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<StudentItem> studentItemList;
    private final Consumer<StudentItem> onStudentClick;

    public StudentAdapter(List<StudentItem> studentItemList, Consumer<StudentItem> onStudentClick) {
        this.studentItemList = studentItemList;
        this.onStudentClick = onStudentClick;
    }

    @Override
    public int getItemViewType(int position) {
        try {
            StudentItem item = studentItemList.get(position);
            if (item != null) {
                return item.getItemViewType();
            }
        } catch (Exception ignored) {}
        return StudentItem.TYPE_STUDENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        try {
            if (viewType == StudentItem.TYPE_HEADER) {
                View view = inflater.inflate(R.layout.item_section_header, parent, false);
                return new SectionHeaderViewHolder(view);
            } else if (viewType == StudentItem.TYPE_GENDER_HEADER) {
                View view = inflater.inflate(R.layout.item_gender_header, parent, false);
                return new GenderHeaderViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_student, parent, false);
                return new StudentViewHolder(view);
            }
        } catch (Exception e) {
            View fallbackView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            return new StudentViewHolder(fallbackView);
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
            studentHolder.tvStudentAge.setText(item.getAge());

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
        }
    }

    @Override
    public int getItemCount() {
        return studentItemList != null ? studentItemList.size() : 0;
    }

    public static class SectionHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderTitle;
        ImageView imgHeaderIcon;

        public SectionHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeaderTitle = itemView.findViewById(R.id.tvHeaderTitle);
            if (tvHeaderTitle == null) tvHeaderTitle = itemView.findViewById(android.R.id.text1);
            imgHeaderIcon = itemView.findViewById(R.id.imgHeaderIcon);
        }

        public void bind(StudentItem item) {
            try {
                if (tvHeaderTitle != null) {
                    tvHeaderTitle.setText(item.getHeaderTitle() != null ? item.getHeaderTitle() : "Section");
                }
                if (imgHeaderIcon != null && item.getHeaderIconResId() != 0) {
                    imgHeaderIcon.setImageResource(item.getHeaderIconResId());
                    imgHeaderIcon.setVisibility(View.VISIBLE);
                } else if (imgHeaderIcon != null) {
                    imgHeaderIcon.setVisibility(View.GONE);
                }
            } catch (Exception ignored) {}
        }
    }

    public static class GenderHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvGenderHeader;

        public GenderHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGenderHeader = itemView.findViewById(R.id.tvGenderHeader);
            if (tvGenderHeader == null) tvGenderHeader = itemView.findViewById(android.R.id.text1);
        }

        public void bind(StudentItem item) {
            try {
                if (tvGenderHeader != null) {
                    tvGenderHeader.setText(item.getHeaderTitle() != null ? item.getHeaderTitle() : "");
                }
            } catch (Exception ignored) {}
        }
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAge;
        ImageView ivAvatar;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvAge = itemView.findViewById(R.id.tvStudentAge);
            ivAvatar = itemView.findViewById(R.id.imgStudentAvatar);
        }

        public void bind(StudentItem item, Consumer<StudentItem> onClickListener) {
            try {
                if (tvName != null) {
                    String name = item.getName();
                    tvName.setText(name != null && !name.trim().isEmpty() ? name : "Unnamed Student");
                }
                if (tvAge != null) {
                    String age = item.getAge();
                    tvAge.setText("Age " + (age != null ? age.replace("Age ", "") : "5"));
                }

                if (ivAvatar != null && item.getPhotoUri() != null && !item.getPhotoUri().isEmpty()) {
                    try {
                        Uri uri = Uri.parse(item.getPhotoUri());
                        try {
                            itemView.getContext().getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                        } catch (Exception ignored) {}
                        ivAvatar.setImageURI(uri);
                    } catch (Exception e) {
                        ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                } else if (ivAvatar != null) {
                    ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
                }

                itemView.setOnClickListener(v -> {
                    if (onClickListener != null) {
                        onClickListener.accept(item);
                    }
                });

            } catch (Exception ignored) {}
        }
    }
}