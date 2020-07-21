package com.quarks.android.Adapters;

import android.app.Activity;
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
import com.quarks.android.Interfaces.InterfaceClickConversation;
import com.quarks.android.Items.ConversationItem;
import com.quarks.android.R;
import com.quarks.android.Utils.Functions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ViewHolder> {
    private ArrayList<ConversationItem> alConversations;
    private Context mContext;
    private InterfaceClickConversation dtInterface;
    private static final int LAUNCH_SECOND_ACTIVITY = 1;

    public ConversationsAdapter(Context context, ArrayList<ConversationItem> alConversations, InterfaceClickConversation dtInterface) {
        mContext = context;
        this.alConversations = alConversations;
        this.dtInterface = dtInterface;
    }

    @NonNull
    @Override
    public ConversationsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        mContext = parent.getContext();
        return new ConversationsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ConversationsAdapter.ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            // Perform a full update
            onBindViewHolder(holder, position);
        } else {
            // Perform a partial update
            if (payloads.get(0) instanceof String) {
                String lastMessage = (String) payloads.get(0);
                holder.tvTypingAndLastMessage.setText(lastMessage);
            }else if(payloads.get(0) instanceof ConversationItem){
                ConversationItem conversationItem = (ConversationItem) payloads.get(0);
                String lastMessage = conversationItem.getLastMessage();
                String time = conversationItem.geTime();
                int numNewMessages = conversationItem.geNumNewMessages();

                holder.tvTypingAndLastMessage.setText(lastMessage);
                holder.tvDate.setText(Functions.formatConversationDate(time,mContext));
                if(numNewMessages > 0){
                    holder.tvBadge.setText(String.valueOf(numNewMessages));
                    holder.tvBadge.setVisibility(View.VISIBLE);
                }else{
                    holder.tvBadge.setText(String.valueOf(0));
                    holder.tvBadge.setVisibility(View.INVISIBLE);
                }
            }
        }
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

        if(alConversations.get(position).geNumNewMessages() > 0){
            if(alConversations.get(position).geNumNewMessages() > 99){
                String text = "+99";
                holder.tvBadge.setText(text);
            }else{
                holder.tvBadge.setText(String.valueOf(alConversations.get(position).geNumNewMessages()));
            }
            holder.tvBadge.setVisibility(View.VISIBLE);
        }else{
            holder.tvBadge.setText(String.valueOf(0));
            holder.tvBadge.setVisibility(View.INVISIBLE);
        }

        holder.itemConversation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dtInterface.onConversationClick();
                Intent intent = new Intent(mContext, ChatActivity.class);
                intent.putExtra("receiverId", userId);
                intent.putExtra("receiverUsername", username);
                ((Activity) mContext).startActivityForResult(intent, LAUNCH_SECOND_ACTIVITY);
            }
        });

        holder.itemConversation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
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

