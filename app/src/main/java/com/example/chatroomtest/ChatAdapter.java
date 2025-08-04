package com.example.chatroomtest;

import static androidx.fragment.app.FragmentManager.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatroomtest.databinding.ItemMessageReceivedBinding;
import com.example.chatroomtest.databinding.ItemMessageSentBinding;
import com.example.chatroomtest.databinding.ItemMessageSystemBinding;

import java.lang.ref.WeakReference;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SYSTEM = 3;
    private Context context;

    private List<ChatMessage> messageList;

    public ChatAdapter(Context context, List<ChatMessage> messageList) {
        this.context = context.getApplicationContext();
        this.messageList = messageList;
    }

    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        if (message.isSystem()) {
            return VIEW_TYPE_SYSTEM;
        }
        return message.isSent() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            ItemMessageSentBinding binding = ItemMessageSentBinding.inflate(inflater, parent, false);
            return new SentMessageHolder(binding);
        } else if (viewType == VIEW_TYPE_RECEIVED) {
            ItemMessageReceivedBinding binding = ItemMessageReceivedBinding.inflate(inflater, parent, false);
            return new ReceivedMessageHolder(binding);
        } else {
            ItemMessageSystemBinding binding = ItemMessageSystemBinding.inflate(inflater, parent, false);
            return new SystemMessageHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        if (holder instanceof SentMessageHolder) {
            ((SentMessageHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageHolder) {
            ((ReceivedMessageHolder) holder).bind(message);
        } else if (holder instanceof SystemMessageHolder) {
            ((SystemMessageHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList == null ? 0 : messageList.size();
    }

    static class SystemMessageHolder extends RecyclerView.ViewHolder {
        private final ItemMessageSystemBinding binding;

        SystemMessageHolder(ItemMessageSystemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message) {
            binding.tvMessage.setText(message.getContent());
        }
    }

    public interface OnAvatarClickListener {
        void onAvatarClick(String username);
    }

    private OnAvatarClickListener avatarClickListener;
    public void setOnAvatarClickListener(OnAvatarClickListener listener) {
        this.avatarClickListener = listener;
    }
    class SentMessageHolder extends RecyclerView.ViewHolder {
        private final ItemMessageSentBinding binding;
        SentMessageHolder(ItemMessageSentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        void bind(ChatMessage message) {
            binding.tvMessage.setText(message.getContent());
            Bitmap avatar = AvatarCache.getInstance().loadAvatar(
                    context,
                    message.getSender(),
                    message.getAvatarPath());
            binding.ivHeadSculpture.setImageBitmap(avatar);
            binding.ivHeadSculpture.setOnClickListener(v -> {
                if (avatarClickListener != null) {
                    avatarClickListener.onAvatarClick(message.getSender());
                } else {
                    Log.e("AvatarClick", "Listener is null! Check adapter initialization");
                }
            });
        }
    }

    class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        private final ItemMessageReceivedBinding binding;
        ReceivedMessageHolder(ItemMessageReceivedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        void bind(ChatMessage message) {
            binding.tvMessage.setText(message.getContent());
            binding.tvUserName.setText(message.getSender());

            Bitmap avatar = AvatarCache.getInstance().loadAvatar(
                    context,
                    message.getSender(),
                    message.getAvatarPath());

            binding.ivHeadSculpture.setImageBitmap(avatar);
            binding.ivHeadSculpture.setOnClickListener(v -> {
                if (avatarClickListener != null) {
                    avatarClickListener.onAvatarClick(message.getSender());
                } else {
                    Log.e("AvatarClick", "Listener is null! Check adapter initialization");
                }
            });
        }
    }
}
