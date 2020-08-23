package com.example.skypeclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class ProfileActivity extends AppCompatActivity {
    private String receiverUserID="";
    private String receiverUserImage="";
    private String receiverUserName="";
    private ImageView background_profile_view;
    private TextView name_profile;
    private Button add_friends,decline_friends_request;
    private FirebaseAuth mAuth;
    private String senderUserID;
    private String currentState="new";
    private DatabaseReference friendRequestRef,contactRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        friendRequestRef= FirebaseDatabase.getInstance().getReference().child("Friend Request");
        contactRef=FirebaseDatabase.getInstance().getReference().child("Contacts");
        mAuth=FirebaseAuth.getInstance();
        senderUserID=mAuth.getCurrentUser().getUid();
        receiverUserID=getIntent().getExtras().get("visit_user_id").toString();
        receiverUserImage=getIntent().getExtras().get("profile_image").toString();
        receiverUserName=getIntent().getExtras().get("profile_name").toString();

        background_profile_view=findViewById(R.id.background_profile_view);
        name_profile=findViewById(R.id.name_profile);
        add_friends=findViewById(R.id.add_friend);
        decline_friends_request=findViewById(R.id.decline_friend_request);
        Picasso.get().load(receiverUserImage).into(background_profile_view);
        name_profile.setText(receiverUserName);
        manageClickEvents();


    }

    private void manageClickEvents() {

        friendRequestRef.child(senderUserID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(receiverUserID)){
                            String requestType=dataSnapshot.child(receiverUserID).child("request_type").getValue().toString();

                            if(requestType.equals("sent")){
                                    currentState="request_sent";
                                    add_friends.setText("Cancel Friend Request");


                            }
                            else if (requestType.equals("received")){

                                currentState="request_received";
                                add_friends.setText("Accept Friend Request");

                                decline_friends_request.setVisibility(View.VISIBLE);
                                decline_friends_request.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        CancelFriendRequest();


                                    }
                                });

                            }
                        }
                        else{
                            contactRef.child(senderUserID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.hasChild(receiverUserID)){
                                                currentState="Friends";
                                                add_friends.setText("Delete Contact");
                                            }
                                            else {
                                                currentState="new";
                                            }

                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        if(senderUserID.equals(receiverUserID)){
            add_friends.setVisibility(View.GONE);
        }
        else {
            add_friends.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(currentState.equals("new")){
                        sendFriendRequest();

                    }
                    if (currentState.equals("request_sent")){
                        CancelFriendRequest();

                    }

                    if (currentState.equals("request_received")){
                            AcceptFriendRequest();

                    }

                    if (currentState.equals("request_sent")){

                        CancelFriendRequest();
                    }
                }
            });
        }


    }
    private void AcceptFriendRequest() {

        contactRef.child(senderUserID).child(receiverUserID)
                .child("contacts").setValue("saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if(task.isSuccessful()){

                            contactRef.child(receiverUserID).child(senderUserID)
                                    .child("contacts").setValue("saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){

                                                friendRequestRef.child(senderUserID).child(receiverUserID)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){


                                                                    friendRequestRef.child(receiverUserID).child(senderUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                                    if(task.isSuccessful()){

                                                                                        currentState="Friends";
                                                                                        add_friends.setText("Delete Contacts");
                                                                                        decline_friends_request.setVisibility(View.GONE);

                                                                                    }

                                                                                }
                                                                            });
                                                                }

                                                            }
                                                        });


                                            }

                                        }
                                    });

                        }

                    }
                });
    }





    private void CancelFriendRequest() {

        friendRequestRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){


                            friendRequestRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if(task.isSuccessful()){

                                                currentState="new";
                                                add_friends.setText("Add Friends");
                                            }

                                        }
                                    });
                        }

                    }
                });
    }


    private void sendFriendRequest() {

        friendRequestRef.child(senderUserID).child(receiverUserID)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if(task.isSuccessful()){

                            friendRequestRef.child(receiverUserID).child(senderUserID)
                                    .child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if (task.isSuccessful()){

                                                currentState="request_sent";
                                                add_friends.setText("Cancel Friend Request");
                                                Toast.makeText(ProfileActivity.this,"Friend Request sent",Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    });
                        }

                    }
                });



    }
}
