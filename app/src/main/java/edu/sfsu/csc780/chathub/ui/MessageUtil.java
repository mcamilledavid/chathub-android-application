package edu.sfsu.csc780.chathub.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.sfsu.csc780.chathub.model.ChatMessage;
import edu.sfsu.csc780.chathub.R;
import vc908.stickerfactory.StickersManager;
import vc908.stickerfactory.ui.fragment.StickersFragment;

public class MessageUtil {
    private static final String LOG_TAG = MessageUtil.class.getSimpleName();
    private static DatabaseReference sFirebaseDatabaseReference =
            FirebaseDatabase.getInstance().getReference();
    private static FirebaseStorage sStorage = FirebaseStorage.getInstance();
    private static FirebaseStorage aStorage = FirebaseStorage.getInstance();
    private static FirebaseStorage dStorage = FirebaseStorage.getInstance();
    private static MessageLoadListener sAdapterListener;
    private static FirebaseAuth sFirebaseAuth;
    public interface MessageLoadListener { public void onLoadComplete(); }

    public static void send(ChatMessage chatMessage, String mChannelName) {
        sFirebaseDatabaseReference.child(mChannelName).push().setValue(chatMessage);
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public static View.OnClickListener sMessageViewListener;
        public TextView messageTextView;
        public ImageView messageImageView;
        public TextView messengerTextView;
        public CircleImageView messengerImageView;
        public TextView timestampTextView;
        public View messageLayout;
        public Button messageButtonView;
        public String stickerCode;
        public ImageView messageSticker;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
            messageImageView = (ImageView) itemView.findViewById(R.id.messageImageView);
            timestampTextView = (TextView) itemView.findViewById(R.id.timestampTextView);
            messageLayout = (View) itemView.findViewById(R.id.messageLayout);
            v.setOnClickListener(sMessageViewListener);
            messageButtonView = (Button) itemView.findViewById(R.id.messageButtonView);
            messageSticker = (ImageView) itemView.findViewById(R.id.chat_item_sticker);
        }
    }

    public static FirebaseRecyclerAdapter getFirebaseAdapter(final Activity activity,
                                                             MessageLoadListener listener,
                                                             final LinearLayoutManager linearManager,
                                                             final RecyclerView recyclerView,
                                                             final View.OnClickListener clickListener) {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(activity);
        sAdapterListener = listener;
        MessageViewHolder.sMessageViewListener = clickListener;
        final FirebaseRecyclerAdapter adapter = new FirebaseRecyclerAdapter<ChatMessage,
                MessageViewHolder>(
                ChatMessage.class,
                R.layout.item_message,
                MessageViewHolder.class,
                sFirebaseDatabaseReference.child(ChannelActivity.mChannelName)) {
            @Override
            protected void populateViewHolder(final MessageViewHolder viewHolder,
                                              ChatMessage chatMessage, int position) {
                sAdapterListener.onLoadComplete();
                viewHolder.messageTextView.setText(chatMessage.getText());
                viewHolder.messengerTextView.setText(chatMessage.getName());
                if (chatMessage.getPhotoUrl() == null) {
                    viewHolder.messengerImageView
                            .setImageDrawable(ContextCompat
                                    .getDrawable(activity,
                                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    SimpleTarget target = new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap, GlideAnimation glideAnimation) {
                            viewHolder.messengerImageView.setImageBitmap(bitmap);
                            final String palettePreference = activity.getString(R.string
                                    .auto_palette_preference);

                            if (preferences.getBoolean(palettePreference, false)) {
                                DesignUtils.setBackgroundFromPalette(bitmap, viewHolder
                                        .messageLayout);
                            } else {
                                viewHolder.messageLayout.setBackground(
                                        activity.getResources().getDrawable(
                                                R.drawable.message_background));
                            }

                        }
                    };
                    Glide.with(activity)
                            .load(chatMessage.getPhotoUrl())
                            .asBitmap()
                            .into(target);
                }

                if(chatMessage.getUri() !=null){

                    viewHolder.messageImageView.setVisibility(View.GONE);
                    viewHolder.messageTextView.setVisibility(View.GONE);
                    viewHolder.messageButtonView.setVisibility(View.VISIBLE);
                    viewHolder.messageSticker.setVisibility(View.GONE);

                    try{
                        final StorageReference audioReference = aStorage.getReferenceFromUrl(chatMessage.getUri());
                        audioReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {

                            @Override
                            public void onSuccess(final Uri uri) {
                                viewHolder.messageButtonView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        MediaPlayer mSound;
                                        mSound = new MediaPlayer();
                                        mSound.setAudioStreamType(AudioManager.STREAM_MUSIC);

                                        try {
                                            mSound.setDataSource(activity, uri);
                                        } catch (IllegalArgumentException e) {
                                            Toast.makeText(activity, "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
                                        } catch (SecurityException e) {
                                            Toast.makeText(activity, "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
                                        } catch (IllegalStateException e) {
                                            Toast.makeText(activity, "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        try {
                                            mSound.prepare();
                                        } catch (IllegalStateException e) {
                                            Toast.makeText(activity, "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
                                        } catch (IOException e) {
                                            Toast.makeText(activity, "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
                                        }

                                        mSound.start();
                                    }
                                });
                            }
                        });
                    } catch (IllegalArgumentException e) {
                        viewHolder.messageTextView.setText("Error loading audio");
                        Log.e(LOG_TAG, e.getMessage() + " : " + chatMessage.getUri());
                    }

                }

                if (chatMessage.getImageUrl() != null) {

                    viewHolder.messageImageView.setVisibility(View.VISIBLE);
                    viewHolder.messageTextView.setVisibility(View.GONE);
                    viewHolder.messageButtonView.setVisibility(View.GONE);
                    viewHolder.messageSticker.setVisibility(View.GONE);

                    try {
                        final StorageReference gsReference =
                                sStorage.getReferenceFromUrl(chatMessage.getImageUrl());
                        gsReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Glide.with(activity)
                                        .load(uri)
                                        .into(viewHolder.messageImageView);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Log.e(LOG_TAG, "Could not load image for message", exception);
                            }
                        });
                    } catch (IllegalArgumentException e) {
                        viewHolder.messageTextView.setText("Error loading image");
                        Log.e(LOG_TAG, e.getMessage() + " : " + chatMessage.getImageUrl());
                    }
                } else {
                    viewHolder.messageImageView.setVisibility(View.GONE);
                    viewHolder.messageTextView.setVisibility(View.VISIBLE);
                    viewHolder.messageSticker.setVisibility(View.GONE);
                }

                if (chatMessage.getDrawingUrl() != null) {

                    viewHolder.messageImageView.setVisibility(View.VISIBLE);
                    viewHolder.messageTextView.setVisibility(View.GONE);
                    viewHolder.messageButtonView.setVisibility(View.GONE);
                    viewHolder.messageSticker.setVisibility(View.GONE);

                    try {
                        final StorageReference gsReference =
                                dStorage.getReferenceFromUrl(chatMessage.getDrawingUrl());
                        gsReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Glide.with(activity)
                                        .load(uri)
                                        .into(viewHolder.messageImageView);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Log.e(LOG_TAG, "Could not load drawing for message", exception);
                            }
                        });
                    } catch (IllegalArgumentException e) {
                        viewHolder.messageTextView.setText("Error loading drawing");
                        Log.e(LOG_TAG, e.getMessage() + " : " + chatMessage.getDrawingUrl());
                    }
                }

                if (chatMessage.getStickerCode() != null) {

                    viewHolder.messageImageView.setVisibility(View.GONE);
                    viewHolder.messageTextView.setVisibility(View.GONE);
                    viewHolder.messageButtonView.setVisibility(View.GONE);
                    viewHolder.messageSticker.setVisibility(View.VISIBLE);

                    ((ChannelActivity)activity).loadSticker(viewHolder.messageSticker, chatMessage.getStickerCode());

                }

                long timestamp = chatMessage.getTimestamp();
                if (timestamp == 0 || timestamp == chatMessage.NO_TIMESTAMP ) {
                    viewHolder.timestampTextView.setVisibility(View.GONE);
                } else {
                    viewHolder.timestampTextView.setText(DesignUtils.formatTime(activity,
                            timestamp));
                    viewHolder.timestampTextView.setVisibility(View.VISIBLE);
                }
            }
        };

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int messageCount = adapter.getItemCount();
                int lastVisiblePosition = linearManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (messageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    recyclerView.scrollToPosition(positionStart);
                }
            }
        });
        return adapter;
    }

    public static StorageReference getAudioStorageReference(FirebaseUser user, Uri uri){
        long nowMs = Calendar.getInstance().getTimeInMillis();

        return aStorage.getReference().child("Audio"+ "/" + user.getUid() + "/" + uri
                .getLastPathSegment());
    }

    public static StorageReference getImageStorageReference(FirebaseUser user, Uri uri) {
        //Create a blob storage reference with path : bucket/userId/timeMs/filename
        long nowMs = Calendar.getInstance().getTimeInMillis();

        return sStorage.getReference().child(user.getUid() + "/" + nowMs + "/" + uri
                .getLastPathSegment());
    }

    public static StorageReference getDrawingStorageReference(FirebaseUser user, Uri uri) {
        //Create a blob storage reference with path : bucket/userId/timeMs/filename
        long nowMs = Calendar.getInstance().getTimeInMillis();

        return dStorage.getReference().child("Drawing" + "/" + user.getUid() + "/" + uri
                .getLastPathSegment());
    }

}