package com.example.skypeclone;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.HashMap;
public class CallingActivity extends AppCompatActivity {
    private TextView nameContact;
    private ImageView profileImage;
    private ImageView cancelCallBtn,acceptCallBtn;
    private String receiverUserID="",receiverUserImage="",receiverUserName="";
    private String senderUserID="",senderUserImage="",senderUserName="",checker="";
    private String callingID="",ringingID="";
    private DatabaseReference usersRef;
    private MediaPlayer mediaPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);
        senderUserID= FirebaseAuth.getInstance().getCurrentUser().getUid();
        receiverUserID=getIntent().getExtras().get("visit_user_id").toString();
        usersRef= FirebaseDatabase.getInstance().getReference().child("users");
        mediaPlayer=MediaPlayer.create(this,R.raw.ringing);
        nameContact=findViewById(R.id.name_calling);
        profileImage=findViewById(R.id.profile_image_calling);
        cancelCallBtn=findViewById(R.id.cancel_call);
        acceptCallBtn=findViewById(R.id.make_call);
        cancelCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                cancelCallingUser();
            }
        });
        acceptCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                final HashMap<String ,Object> callingPickUpMap=new HashMap<>();
                callingPickUpMap.put("picked","picked");
                usersRef.child(senderUserID).child("Ringing")
                        .updateChildren(callingPickUpMap)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Intent intent=new Intent(CallingActivity.this,VideoChatActivity.class);
                                    startActivity(intent);
                                }
                            }
                        });
            }
        });
        getAndSetUserProfileInfo();
    }
    private void getAndSetUserProfileInfo() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.child(receiverUserID).exists()){
                    receiverUserImage=dataSnapshot.child(receiverUserID).child("image").getValue().toString();
                    receiverUserName=dataSnapshot.child(receiverUserID).child("name").getValue().toString();
                    nameContact.setText(receiverUserName);
                    Picasso.get().load(receiverUserImage).placeholder(R.drawable.profile_image).into(profileImage);
                }
                if(dataSnapshot.child(senderUserID).exists()){
                    senderUserImage=dataSnapshot.child(senderUserID).child("image").getValue().toString();
                    senderUserName=dataSnapshot.child(senderUserID).child("name").getValue().toString();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        mediaPlayer.start();
        usersRef.child(receiverUserID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(!checker.equals("clicked") && !dataSnapshot.hasChild("Calling") && !dataSnapshot.hasChild("Ringing")){
                            final HashMap<String,Object> callingInfo=new HashMap<>();
                            callingInfo.put("calling",receiverUserID);
                            usersRef.child(senderUserID)
                                    .child("Calling")
                                    .updateChildren(callingInfo)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                final HashMap<String,Object> ringingInfo=new HashMap<>();
                                                ringingInfo.put("ringing",senderUserID);
                                                usersRef.child(receiverUserID)
                                                        .child("Ringing")
                                                        .updateChildren(ringingInfo);
                                            }
                                        }
                                    });
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.child(senderUserID).hasChild("Ringing") && !dataSnapshot.child(senderUserID).hasChild("Calling")){
                    acceptCallBtn.setVisibility(View.VISIBLE);
                }
                if(dataSnapshot.child(receiverUserID).child("Ringing").hasChild("picked")){
                    mediaPlayer.stop();
                    Intent intent=new Intent(CallingActivity.this,VideoChatActivity.class);
                    startActivity(intent);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    public void cancelCallingUser(){
//sender
        usersRef.child(senderUserID)
                .child("Calling")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists() && dataSnapshot.hasChild("calling")){
                            callingID=dataSnapshot.child("calling").getValue().toString();
                            usersRef.child(callingID)
                                    .child("Ringing")
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                usersRef.child(senderUserID)
                                                        .child("Calling")
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                startActivity(new Intent(CallingActivity.this,RegistrationActivity.class));
                                                                finish();
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                        else {
                            startActivity(new Intent(CallingActivity.this,RegistrationActivity.class));
                            finish();
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
//receiver
        usersRef.child(senderUserID)
                .child("Ringing")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists() && dataSnapshot.hasChild("ringing")){
                            ringingID=dataSnapshot.child("ringing").getValue().toString();
                            usersRef.child(ringingID)
                                    .child("Calling")
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                usersRef.child(senderUserID)
                                                        .child("Ringing")
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                startActivity(new Intent(CallingActivity.this,RegistrationActivity.class));
                                                                finish();
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                        else {
                            startActivity(new Intent(CallingActivity.this,RegistrationActivity.class));
                            finish();
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }
}
