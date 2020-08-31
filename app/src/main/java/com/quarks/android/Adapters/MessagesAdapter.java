package com.quarks.android.Adapters;

import android.content.Context;
import android.database.Cursor;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.quarks.android.ChatActivity;
import com.quarks.android.Items.ConversationItem;
import com.quarks.android.Items.MessageItem;
import com.quarks.android.R;
import com.quarks.android.Utils.DataBaseHelper;
import com.quarks.android.Utils.Functions;
import com.quarks.android.Utils.MessageBubbleLayout;

import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessagesViewHolder> {

    private ArrayList<MessageItem> listMessage;
    private Context context;
    private boolean isLastItem = false;
    private DataBaseHelper dataBaseHelper;
    private String senderId;

    private static final int FIRST_BUBBLE_OUTGOING = 1;
    private static final int BUBBLE_OUTGOING = 2;
    private static final int FIRST_BUBBLE_INCOMING = 3;
    private static final int BUBBLE_INCOMING = 4;

    private static final int STATELESS = -1;
    private static final int NOT_SENT = 0;
    private static final int SENT = 1;
    private static final int RECEIVED = 2;
    private static final int VIEWED = 3;

    public MessagesAdapter(Context context, ArrayList<MessageItem> listMessage, String senderId) {
        this.context = context;
        this.listMessage = listMessage;
        this.senderId = senderId;
        dataBaseHelper = new DataBaseHelper(context);
    }

    @NonNull
    @Override
    public MessagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);

        return new MessagesViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessagesViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            // Perform a full update
            onBindViewHolder(holder, position);
        } else {
            // Perform a partial update
            if (payloads.get(0) instanceof Integer) {
                Integer status = (Integer) payloads.get(0);
                switch (status) {
                    case STATELESS:
                        holder.ivTick.setImageResource(R.drawable.ic_check);
                    case NOT_SENT:
                        holder.ivTick.setImageResource(R.drawable.ic_clock);
                        break;
                    case SENT:
                        holder.ivTick.setImageResource(R.drawable.ic_check);
                        break;
                    case RECEIVED:
                        holder.ivTick.setImageResource(R.drawable.ic_double_check);
                        break;
                    case VIEWED:
                        holder.ivTick.setColorFilter(R.color.bg_blue_2);
                        break;
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MessagesViewHolder holder, final int position) {

        /** DESIGN */

        LinearLayout.LayoutParams lyMessageParams = (LinearLayout.LayoutParams) holder.lyMessage.getLayoutParams();
        if (listMessage.get(position).getMessageChannel() == 1) { // ISSUER
            lyMessageParams.gravity = Gravity.END;
            holder.ivTick.setVisibility(View.VISIBLE);
            if (position > 0) {
                if (listMessage.get(position - 1).getMessageChannel() == 1) {
                    if(!listMessage.get(position).getDate().equalsIgnoreCase(listMessage.get(position - 1).getDate()) || listMessage.get(position).getPendingMessages() > 0){
                        stylizeBubble(holder, lyMessageParams, FIRST_BUBBLE_OUTGOING);
                    }else{
                        stylizeBubble(holder, lyMessageParams, BUBBLE_OUTGOING);
                    }
                } else {
                    stylizeBubble(holder, lyMessageParams, FIRST_BUBBLE_OUTGOING);
                }
            } else {
                Cursor cursor = dataBaseHelper.getPreviousMessage(senderId, listMessage.get(position).getMessageId());
                int channel = getChannelFromCursor(cursor);
                String date = getDateFromCursor(cursor);
                if (channel == 1) {
                    if(!date.equals("") && !listMessage.get(position).getDate().equalsIgnoreCase(date) || listMessage.get(position).getPendingMessages() > 0){
                        stylizeBubble(holder, lyMessageParams, FIRST_BUBBLE_OUTGOING);
                    }else{
                        stylizeBubble(holder, lyMessageParams, BUBBLE_OUTGOING);
                    }
                } else {
                    stylizeBubble(holder, lyMessageParams, FIRST_BUBBLE_OUTGOING);
                }
            }
        } else if (listMessage.get(position).getMessageChannel() == 2) { // RECEIVER
            lyMessageParams.gravity = Gravity.START;
            holder.ivTick.setVisibility(View.GONE);
            if (position > 0) {
                if (listMessage.get(position - 1).getMessageChannel() == 2) {
                    if(!listMessage.get(position).getDate().equalsIgnoreCase(listMessage.get(position - 1).getDate()) || listMessage.get(position).getPendingMessages() > 0){
                        stylizeBubble(holder, lyMessageParams, FIRST_BUBBLE_INCOMING);
                    }else{
                        stylizeBubble(holder, lyMessageParams, BUBBLE_INCOMING);
                    }
                } else {
                    stylizeBubble(holder, lyMessageParams, FIRST_BUBBLE_INCOMING);
                }
            } else {
                Cursor cursor = dataBaseHelper.getPreviousMessage(senderId, listMessage.get(position).getMessageId());
                int channel = getChannelFromCursor(cursor);
                String date = getDateFromCursor(cursor);
                if (channel == 2) {
                    if(!date.equals("") && !listMessage.get(position).getDate().equalsIgnoreCase(date) || listMessage.get(position).getPendingMessages() > 0){
                        stylizeBubble(holder, lyMessageParams, FIRST_BUBBLE_INCOMING);
                    }else{
                        stylizeBubble(holder, lyMessageParams, BUBBLE_INCOMING);
                    }
                } else {
                    stylizeBubble(holder, lyMessageParams, FIRST_BUBBLE_INCOMING);
                }
            }
        }
        holder.lyMessage.setLayoutParams(lyMessageParams);

        /** SETTERS */

        /* New messages */
        if (listMessage.get(position).getPendingMessages() > 0) {
            holder.lyNewMessages.setVisibility(View.VISIBLE);
            if (listMessage.get(position).getPendingMessages() == 1) {
                String strNewMessages = "1 " + context.getResources().getString(R.string.unread_message);
                holder.tvNewMessages.setText(strNewMessages);
            } else {
                String strNewMessages = listMessage.get(position).getPendingMessages() + " " + context.getResources().getString(R.string.unread_messages);
                holder.tvNewMessages.setText(strNewMessages);
            }
        } else {
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
        switch (listMessage.get(position).getStatus()) {
            case STATELESS:
                holder.ivTick.setImageResource(R.drawable.ic_check);
            case NOT_SENT:
                holder.ivTick.setImageResource(R.drawable.ic_clock);
                break;
            case SENT:
                holder.ivTick.setImageResource(R.drawable.ic_check);
                break;
            case RECEIVED:
                holder.ivTick.setImageResource(R.drawable.ic_double_check);
                break;
            case VIEWED:
                holder.ivTick.setImageResource(R.drawable.ic_viewed);
                break;
        }
    }

    private String getDateFromCursor(Cursor c){
        String date = "";
        if (c != null) {
            while (!c.isAfterLast()) {
                c.moveToFirst();
                date = c.getString(c.getColumnIndex("date"));
                c.close();
            }
        }
        return date;
    }

    private int getChannelFromCursor(Cursor c){
        int channel = -1;
        if (c != null) {
            while (!c.isAfterLast()) {
                c.moveToFirst();
                channel = c.getInt(c.getColumnIndex("channel"));
                c.close();
            }
        }
        return channel;
    }

    private void stylizeBubble(MessagesViewHolder holder, LinearLayout.LayoutParams lyMessageParams, int typeBubble) {
        switch (typeBubble) {
            case FIRST_BUBBLE_OUTGOING:
                lyMessageParams.topMargin = (int) Functions.dpToPx(context, 8);
                lyMessageParams.bottomMargin = (int) Functions.dpToPx(context, 0);
                holder.lyMessage.setBackgroundResource(R.drawable.bg_first_bubble_outgoing);
                holder.tvMessage.setPadding((int) Functions.dpToPx(context, 8), 0, (int) Functions.dpToPx(context, 7), 0);
                break;
            case BUBBLE_OUTGOING:
                lyMessageParams.topMargin = (int) Functions.dpToPx(context, 0);
                lyMessageParams.bottomMargin = (int) Functions.dpToPx(context, 0);
                holder.lyMessage.setBackgroundResource(R.drawable.bg_bubble_outgoing);
                holder.tvMessage.setPadding((int) Functions.dpToPx(context, 8), 0, (int) Functions.dpToPx(context, (float) 7.5), 0);
                break;
            case FIRST_BUBBLE_INCOMING:
                lyMessageParams.topMargin = (int) Functions.dpToPx(context, 8);
                lyMessageParams.bottomMargin = (int) Functions.dpToPx(context, 0);
                holder.lyMessage.setBackgroundResource(R.drawable.bg_first_bubble_incoming);
                holder.tvMessage.setPadding((int) Functions.dpToPx(context, 16), 0, (int) Functions.dpToPx(context, 7), 0);
                break;
            case BUBBLE_INCOMING:
                lyMessageParams.topMargin = (int) Functions.dpToPx(context, 1);
                lyMessageParams.bottomMargin = (int) Functions.dpToPx(context, 0);
                holder.lyMessage.setBackgroundResource(R.drawable.bg_bubble_incoming);
                holder.tvMessage.setPadding((int) Functions.dpToPx(context, 16), 0, (int) Functions.dpToPx(context, (float) 7.5), 0);
                break;
        }
    }

    public void isLastItem() {
        isLastItem = true;
    }

    @Override
    public int getItemCount() {
        return listMessage.size();
    }

    static class MessagesViewHolder extends RecyclerView.ViewHolder {

        TextView tvMessage, tvTime, tvDate, tvNewMessages;
        LinearLayout lyMessage, lyTimeAndCheck, lyNewMessages;
        MessageBubbleLayout lyBubbleMessage;
        ImageView ivTick;

        MessagesViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvNewMessages = itemView.findViewById(R.id.tvNewMessages);

            lyMessage = itemView.findViewById(R.id.lyMessage);
            lyTimeAndCheck = itemView.findViewById(R.id.lyTimeAndCheck);
            lyNewMessages = itemView.findViewById(R.id.lyNewMessages);

            lyBubbleMessage = itemView.findViewById(R.id.lyBubbleMessage);

            ivTick = itemView.findViewById(R.id.ivTick);


        }
    }
}
