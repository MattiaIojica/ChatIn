package com.licenta.chatin.adapters;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.licenta.chatin.databinding.ItemContainerRecentConversionBinding;
import com.licenta.chatin.listeners.ConversionListener;
import com.licenta.chatin.models.ChatMessage;
import com.licenta.chatin.models.User;
import com.licenta.chatin.utilities.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.ConversionViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final ConversionListener conversionListener;

    public RecentConversationsAdapter(List<ChatMessage> chatMessages, ConversionListener conversionListener) {
        this.chatMessages = chatMessages;
        this.conversionListener = conversionListener;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public void updateLastMessage(String conversationId, String lastMessage) {
        for (ChatMessage chatMessage : chatMessages) {
            if (chatMessage.conversionId.equals(conversationId)) {
                chatMessage.message = lastMessage;
                notifyItemChanged(chatMessages.indexOf(chatMessage));
                break;
            }
        }
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversionBinding binding;

        ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        @SuppressLint("SetTextI18n")
        void setData(ChatMessage chatMessage) {
            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
            binding.textName.setText(chatMessage.conversionName);

            // Check if the last message is a photo
            if (chatMessage.message != null && chatMessage.message.startsWith(Constants.MESSAGE_IMAGE_PREFIX)) {
                binding.textRecentMessage.setText("Image");
            } else {
                binding.textRecentMessage.setText(chatMessage.message != null ? chatMessage.message : "");
            }

            binding.textDateTime.setText(getReadableDateTime(chatMessage.dateObject));

            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.setId(chatMessage.conversionId);
                user.setName(chatMessage.conversionName);
                user.setImage(chatMessage.conversionImage);
                conversionListener.onConversionClicked(user);
            });
        }
    }

    private Bitmap getConversionImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private String getReadableDateTime(Date date) {
        if (date.getDate() == new Date().getDate()) {
            return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
        }

        return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(date);
    }
}
