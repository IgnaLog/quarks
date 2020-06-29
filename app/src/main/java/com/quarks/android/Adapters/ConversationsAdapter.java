package com.quarks.android.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.quarks.android.ChatActivity;
import com.quarks.android.Items.ConversationItem;
import com.quarks.android.R;
import com.quarks.android.Utils.Functions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;


public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ViewHolder> {
    private ArrayList<ConversationItem> alConversations = new ArrayList<ConversationItem>();
    private Context mContext;

    public ConversationsAdapter(Context context, ArrayList<ConversationItem> alConversations) {
        mContext = context;
        this.alConversations = alConversations;
    }

    @NonNull
    @Override
    public ConversationsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ConversationsAdapter.ViewHolder holder, final int position) {
        String urlPhoto = alConversations.get(position).getUrlPhoto();
        String filename = alConversations.get(position).getFilename();
        final String username = alConversations.get(position).getUsername();
        final String userId = alConversations.get(position).getUserId();
        String lastMessage = alConversations.get(position).getLastMessage();
        String time = alConversations.get(position).geTime();

        if(!urlPhoto.equals("")){ //  if(!filename.equals("")){
            //Picasso.get().load(mContext.getResources().getString(R.string.url_get_image) + filename).fit().centerCrop().into(holder.civAvatar); // From my server
            Picasso.get().load(urlPhoto).fit().centerCrop().into(holder.civAvatar); // From Cloudinary server. Need to change server response!
        }
        holder.tvUsername.setText(username);
        holder.tvTypingAndLastMessage.setText(lastMessage);
        holder.tvDate.setText(Functions.formatConversationDate(time, mContext));

        holder.itemConversation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, ChatActivity.class);
                intent.putExtra("receiverId", userId);
                intent.putExtra("receiverUsername", username);
                holder.context.startActivity(intent);
            }
        });
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private Context context;
        private ConstraintLayout itemConversation;
        private CircleImageView civAvatar;
        private TextView tvUsername, tvTypingAndLastMessage, tvDate, tvBadge;

        ViewHolder(View itemView) {
            super(itemView);
            this.context = itemView.getContext();
            itemConversation = itemView.findViewById(R.id.itemConversation);
            civAvatar = itemView.findViewById(R.id.civAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvTypingAndLastMessage = itemView.findViewById(R.id.tvTypingAndLastMessage);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvBadge = itemView.findViewById(R.id.tvBadge);
        }
    }

    @Override
    public int getItemCount() {
        return alConversations.size();
    }

    public void Clear() {
        alConversations.clear();
    }

    public void removeAt(int position) {
        alConversations.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, alConversations.size());
    }
}

