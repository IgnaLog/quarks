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
import com.quarks.android.Items.ContactItem;
import com.quarks.android.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;


public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {
    private ArrayList<ContactItem> alContacts = new ArrayList<ContactItem>();
    private Context mContext;

    public ContactsAdapter(Context context, ArrayList<ContactItem> alContacts) {
        mContext = context;
        this.alContacts = alContacts;
    }

    @NonNull
    @Override
    public ContactsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ContactsAdapter.ViewHolder holder, final int position) {
        String urlPhoto = alContacts.get(position).getUrlPhoto();
        String filename = alContacts.get(position).getFilename();
        final String username = alContacts.get(position).getUsername();
        final String userId = alContacts.get(position).getUserId();

        if(!urlPhoto.equals("")){ //  if(!filename.equals("")){
            //Picasso.get().load(mContext.getResources().getString(R.string.url_get_image) + filename).fit().centerCrop().into(holder.civAvatar); // From my server
            Picasso.get().load(urlPhoto).fit().centerCrop().into(holder.civAvatar); // From cloudinary server. Need to change server response!
        }
        holder.tvUsername.setText(username);

        holder.itemContact.setOnClickListener(new View.OnClickListener() {
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
        private ConstraintLayout itemContact;
        private CircleImageView civAvatar;
        private TextView tvUsername;

        ViewHolder(View itemView) {
            super(itemView);
            this.context = itemView.getContext();
            itemContact = itemView.findViewById(R.id.itemContact);
            civAvatar = itemView.findViewById(R.id.civAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
        }
    }

    @Override
    public int getItemCount() {
        return alContacts.size();
    }

    public void Clear() {
        alContacts.clear();
    }

    public void removeAt(int position) {
        alContacts.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, alContacts.size());
    }
}

