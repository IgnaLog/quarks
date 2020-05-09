package com.quarks.android.Adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.quarks.android.Items.MessageItem;
import com.quarks.android.R;
import com.quarks.android.Utils.Functions;

import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessagesViewHolder> {

    private ArrayList<MessageItem> listMessage;
    private Context context;
    private boolean isLastItem = false;

    public MessagesAdapter(Context context, ArrayList<MessageItem> listMessage) {
        this.context = context;
        this.listMessage = listMessage;
    }

    @NonNull
    @Override
    public MessagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.card_view_message, parent, false);
        return new MessagesViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MessagesViewHolder holder, int position) {

        /** DESIGN */

        RelativeLayout.LayoutParams ryCardView = (RelativeLayout.LayoutParams) holder.cardView.getLayoutParams();
        FrameLayout.LayoutParams fyLayoutMessage = (FrameLayout.LayoutParams) holder.layoutMessage.getLayoutParams();
        LinearLayout.LayoutParams lyTextViewTime = (LinearLayout.LayoutParams) holder.tvTime.getLayoutParams();
        LinearLayout.LayoutParams lyTextViewMessage = (LinearLayout.LayoutParams) holder.tvMessage.getLayoutParams();

        if (listMessage.get(position).getMessageChannel() == 1) { // ISSUER
            holder.layoutMessage.setBackgroundColor(ContextCompat.getColor(context, R.color.colorMensajeEmisor));
            ryCardView.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            ryCardView.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            ryCardView.setMarginEnd(50);
            fyLayoutMessage.gravity = Gravity.RIGHT;
            lyTextViewTime.gravity = Gravity.RIGHT;
            lyTextViewMessage.gravity = Gravity.RIGHT;
            holder.tvMessage.setGravity(Gravity.RIGHT);
        } else if (listMessage.get(position).getMessageChannel() == 2) { // RECEIVER
            holder.layoutMessage.setBackgroundColor(ContextCompat.getColor(context, R.color.colorMensajeReceptor));
            ryCardView.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            ryCardView.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            ryCardView.setMarginStart(50);
            fyLayoutMessage.gravity = Gravity.LEFT;
            lyTextViewTime.gravity = Gravity.RIGHT;
            lyTextViewMessage.gravity = Gravity.LEFT;
            holder.tvMessage.setGravity(Gravity.LEFT);
        }
        holder.cardView.setLayoutParams(ryCardView);
        holder.layoutMessage.setLayoutParams(fyLayoutMessage);
        holder.tvTime.setLayoutParams(lyTextViewTime);
        holder.tvMessage.setLayoutParams(lyTextViewMessage);

        /** SETTERS */

        /* New messages */
        if(listMessage.get(position).getPendingMessages() > 0){
            holder.lyNewMessages.setVisibility(View.VISIBLE);
            if(listMessage.get(position).getPendingMessages() == 1){
                String strNewMessages = "1 "+ context.getResources().getString(R.string.unread_message);
                holder.tvNewMessages.setText(strNewMessages);
                listMessage.get(position).setPendingMessages(0); // Reset pendingMessages to 0 from this position
            }else{
                String strNewMessages = listMessage.get(position).getPendingMessages() + " " + context.getResources().getString(R.string.unread_messages);
                holder.tvNewMessages.setText(strNewMessages);
                listMessage.get(position).setPendingMessages(0); // Reset pendingMessages to 0 from this position
            }
        }else{
            holder.lyNewMessages.setVisibility(View.GONE);
        }

        /* tvDate */
        holder.tvDate.setText(listMessage.get(position).getDate());
        if (position > 0) {
            if (listMessage.get(position).getDate().equalsIgnoreCase(listMessage.get(position - 1).getDate())) {
                holder.tvDate.setVisibility(View.GONE);
            } else {
                holder.tvDate.setVisibility(View.VISIBLE);
            }
        } else {
            if (isLastItem) {
                holder.tvDate.setVisibility(View.VISIBLE);
            } else {
                holder.tvDate.setVisibility(View.GONE);
            }

        }

        /* Rest of views */
        holder.tvMessage.setText(listMessage.get(position).getMessage());
        holder.tvTime.setText(listMessage.get(position).getMessageTime());

    }

    public void isLastItem() {
        isLastItem = true;
    }

    @Override
    public int getItemCount() {
        return listMessage.size();
    }

    static class MessagesViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView tvMessage, tvTime, tvDate, tvNewMessages;
        LinearLayout layoutMessage, lyNewMessages;

        MessagesViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cvMessage);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            layoutMessage = itemView.findViewById(R.id.lyMessage);
            lyNewMessages = itemView.findViewById(R.id.lyNewMessages);
            tvNewMessages = itemView.findViewById(R.id.tvNewMessages);
        }
    }
}
