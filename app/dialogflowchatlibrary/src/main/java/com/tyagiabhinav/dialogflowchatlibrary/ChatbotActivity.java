package com.tyagiabhinav.dialogflowchatlibrary;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.EventInput;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.protobuf.Struct;
import com.tyagiabhinav.dialogflowchatlibrary.database.ui.activity.NotesListActivity;
import com.tyagiabhinav.dialogflowchatlibrary.networkutil.ChatbotCallback;
import com.tyagiabhinav.dialogflowchatlibrary.networkutil.TaskRunner;
import com.tyagiabhinav.dialogflowchatlibrary.templates.TextMessageTemplate;
import com.tyagiabhinav.dialogflowchatlibrary.templateutil.Constants;
import com.tyagiabhinav.dialogflowchatlibrary.templateutil.OnClickCallback;
import com.tyagiabhinav.dialogflowchatlibrary.templateutil.ReturnMessage;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ChatbotActivity extends AppCompatActivity implements ChatbotCallback, OnClickCallback {


    public static int stepCount = 0;
    //------------- STEPS
    enum FitActionRequestCode {
        SUBSCRIBE,
        READ_DATA
    }

    private FitnessOptions fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .build();
    private boolean runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;
    //------------- STEPS

    private static final String TAG = ChatbotActivity.class.getSimpleName();
    private static final int USER = 10001;
    private static final int BOT = 10002;
    private static final int SPEECH_INPUT = 10070;

    public static final String SESSION_ID = "sessionID";


    //UI
    private LinearLayout chatLayout;
    private EditText queryEditText;
    private ImageView chatMic;
    private ImageView sendBtn;

    //Variables
    private SessionsClient sessionsClient;
    private SessionName session;
    private TaskRunner dialogflowTaskRunner;
    private boolean isProgressRunning;
    public Date date;
    public String stringDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //------------- STEPS
        checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE);
        //------------- STEPS


        setContentView(R.layout.activity_chatbot);
        Log.d(TAG, "onCreate: ");

        ChatbotSettings chatSettings = ChatbotSettings.getInstance();

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.statusBarColor));

        Toolbar toolbar = ChatbotSettings.getInstance().getAppToolbar();
        if (toolbar == null) {
            toolbar = findViewById(R.id.toolbar);
            ChatbotSettings.getInstance().setAppToolbar(toolbar);
        }

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        final ScrollView scrollview = findViewById(R.id.chatScrollView);
        scrollview.post(new Runnable() {
            @Override
            public void run() {
                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        chatLayout = findViewById(R.id.chatLayout);

        sendBtn = findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Send click");
                sendMessage(view);
            }
        });

        chatMic = findViewById(R.id.chatMic);
        if (chatSettings.isMicAvailable()) {
            // show mic
            chatMic.setVisibility(View.VISIBLE);
            chatMic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    promptSpeech();
                }
            });
        }

        queryEditText = findViewById(R.id.queryEditText);
        queryEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            sendMessage(sendBtn);
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });

        Bundle bundle = getIntent().getExtras();
        String sessionID = null;
        if (bundle != null) {
            sessionID = bundle.getString(SESSION_ID);
            if (sessionID == null || sessionID.trim().isEmpty()) {
                sessionID = UUID.randomUUID().toString();
            }
        }

        try {
            init(sessionID);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(ChatbotActivity.this, "Error creating a session!", Toast.LENGTH_LONG).show();
        }
    }

//    public void openChatbot() {
//        //provide your Dialogflow's Google Credential JSON saved under RAW folder in resources
//        DialogflowCredentials.getInstance().setInputStream(getResources().openRawResource(R.raw.test_agent_credentials));
//
//        ChatbotSettings.getInstance().setChatbot( new Chatbot.ChatbotBuilder().build());
//        Intent intent = new Intent(ChatbotActivity.this,ChatbotActivity.class);
//        Bundle bundle = new Bundle();
//
//        //provide a UUID for your session with the Dialogflow agent
//        bundle.putString(ChatbotActivity.SESSION_ID, UUID.randomUUID().toString());
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
//        intent.putExtras(bundle);
//        startActivity(intent);
//    }


    /**
     * Initializes a custom log class that outputs both to in-app targets and logcat.
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }



    private void checkPermissionsAndRun(FitActionRequestCode fitActionRequestCode) {
        if (permissionApproved()) {
            Log.d("Nazar", "permission approved");
            fitSignIn(fitActionRequestCode);
        } else {
            Log.d("Nazar", "requestRuntimePermissions");
            requestRuntimePermissions(fitActionRequestCode);
        }
    }

    private void requestRuntimePermissions(final FitActionRequestCode requestCode) {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACTIVITY_RECOGNITION);

        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(findViewById(R.id.chatLayout),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(
                                    ChatbotActivity.this,
                                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                                    requestCode.ordinal());
                        }
                    }).show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    requestCode.ordinal());
        }
    }

    private boolean permissionApproved() {
        if (runningQOrLater) {
            return
                    PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACTIVITY_RECOGNITION);
        } else {
            return true;
        }
    }

    private void fitSignIn(FitActionRequestCode requestCode) {
        if (oAuthPermissionsApproved()) {
            performActionForRequestCode(requestCode);
        } else {
            GoogleSignIn.requestPermissions(
                    ChatbotActivity.this,
                    requestCode.ordinal(),
                    getGoogleAccount(), fitnessOptions);
        }
    }

    private void performActionForRequestCode(FitActionRequestCode requestCode) {
        switch (requestCode) {
            case READ_DATA:
                Log.d("Nazar", "read data");
                readData();
                break;
            case SUBSCRIBE:
                Log.d("Nazar", "subscribe");
                subscribe();
                break;
        }
    }

    public void readData() {
        Fitness.getHistoryClient(this, getGoogleAccount())
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(new OnSuccessListener<DataSet>() {
                    @Override
                    public void onSuccess(DataSet dataSet) {
                        int total = 0;
                        if (!dataSet.isEmpty())
                            total = dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                        Log.i(TAG, "Total steps: " + total);
                        Log.d("Nazar", "Total steps: " + total);
                        stepCount = total;
                        Toast.makeText(ChatbotActivity.this, "Steps: " + total, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "There was a problem getting the step count.", e);
                        Log.d("Nazar", "There was a problem getting the step count." + e.toString());
                    }
                });
    }

    private void subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        Fitness.getRecordingClient(this, getGoogleAccount())
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i(TAG, "Successfully subscribed!");
                            Log.d("Nazar", "succesfull");
                        }
                        else {
                            Log.d("Nazar", "problem subscribe");
                            Log.w(TAG, "There was a problem subscribing.", task.getException());
                        }
                    }
                });
    }


    private boolean oAuthPermissionsApproved() {
        return GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions);
    }

    private GoogleSignInAccount getGoogleAccount() {
        return GoogleSignIn.getAccountForExtension(this, fitnessOptions);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_read_data) {
            fitSignIn(FitActionRequestCode.READ_DATA);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        closeDialog();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_INPUT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
                Log.d(TAG, "onActivityResult: " + result);
                queryEditText.setText(result);
                send(result);
            }
        }

        if (requestCode == FitActionRequestCode.READ_DATA.ordinal() || requestCode == FitActionRequestCode.SUBSCRIBE.ordinal()) {
            switch (resultCode) {
                case RESULT_OK:
                    FitActionRequestCode postSignInAction = FitActionRequestCode.values()[requestCode];
                    performActionForRequestCode(postSignInAction);
                    break;
                default:
                    oAuthErrorMsg(requestCode, resultCode);
                    break;
            }
        }
    }

    private void oAuthErrorMsg(int requestCode, int resultCode) {
        String message = "There was an error signing into Fit. Check the troubleshooting section of the README\n" +
                "            for potential issues.";
        Log.e(TAG, message + requestCode + resultCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) {
            Log.i(TAG, "User interaction was cancelled.");
        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            FitActionRequestCode fitActionRequestCode = FitActionRequestCode.values()[requestCode];
            fitSignIn(fitActionRequestCode);
        } else {
            Snackbar.make(
                    findViewById(R.id.chatLayout),
                    R.string.permission_denied_explanation,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.settings, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    })
                    .show();
        }
    }

    @Override
    public void OnChatbotResponse(DetectIntentResponse response) {
        removeProcessWaitBubble();
        processResponse(response);
    }

    @Override
    public void OnUserClickAction(ReturnMessage msg) {
        String eventName = msg.getEventName();
        Struct param = msg.getParam();
        if (eventName != null && !eventName.trim().isEmpty()) {
            if (param != null && param.getFieldsCount() > 0) {
                EventInput eventInput = EventInput.newBuilder().setName(eventName).setLanguageCode("en-US").setParameters(param).build();
                send(eventInput, msg.getActionText());
            } else {
                EventInput eventInput = EventInput.newBuilder().setName(eventName).setLanguageCode("en-US").build();
                send(eventInput, msg.getActionText());
            }
        } else {
            send(msg.getActionText());
        }
    }

    private void init(String UUID) throws IOException {
        InputStream credentialStream = DialogflowCredentials.getInstance().getInputStream();
        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialStream);
        String projectId = ((ServiceAccountCredentials) credentials).getProjectId();

        SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
        SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
        sessionsClient = SessionsClient.create(sessionsSettings);
        session = SessionName.of(projectId, UUID);

        if (ChatbotSettings.getInstance().isAutoWelcome()) {
            send("hi");
        }
    }

    private void sendMessage(View view) {
        String msg = queryEditText.getText().toString();
        if (msg.trim().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please enter your query!", Toast.LENGTH_LONG).show();
        } else {
            send(msg);
        }
    }

    private void send(String message) {
        Log.d(TAG, "send: 1");
        TextMessageTemplate tmt = new TextMessageTemplate(getApplicationContext(), ChatbotActivity.this, Constants.USER);
        if (!ChatbotSettings.getInstance().isAutoWelcome()) {
            chatLayout.addView(tmt.showMessage(message));
            queryEditText.setText("");
            showProcessWaitBubble();
        } else {
            ChatbotSettings.getInstance().setAutoWelcome(false);
        }
        QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build();
        dialogflowTaskRunner = new TaskRunner(this, session, sessionsClient, queryInput);
        dialogflowTaskRunner.executeChat();
    }

    private void send(EventInput event, String message) {
        Log.d(TAG, "send: 2");
        TextMessageTemplate tmt = new TextMessageTemplate(getApplicationContext(), ChatbotActivity.this, Constants.USER);
        if (!ChatbotSettings.getInstance().isAutoWelcome()) {
            chatLayout.addView(tmt.showMessage(message));
            queryEditText.setText("");
            showProcessWaitBubble();
        } else {
            ChatbotSettings.getInstance().setAutoWelcome(false);
        }

        QueryInput queryInput = QueryInput.newBuilder().setEvent(event).build();
        dialogflowTaskRunner = new TaskRunner(this, session, sessionsClient, queryInput);
        dialogflowTaskRunner.executeChat();
    }

    private void showProcessWaitBubble() {
        TextMessageTemplate tmt = new TextMessageTemplate(getApplicationContext(), ChatbotActivity.this, Constants.BOT);
        chatLayout.addView(tmt.showMessage("..."));
        isProgressRunning = true;
        enableDissableChatUI(false);

    }

    private void removeProcessWaitBubble() {
        enableDissableChatUI(true);
        if (isProgressRunning && chatLayout != null && chatLayout.getChildCount() > 0) {
            chatLayout.removeViewAt(chatLayout.getChildCount() - 1);
            isProgressRunning = false;
        }
    }


    private void processResponse(DetectIntentResponse response) {
        QueryResult queryResult = response.getQueryResult();
        //String IntentName = queryResult.getIntent().getDisplayName();
        Log.d(TAG, "processResponse");
        if (response != null) {
            List<Context> contextList = response.getQueryResult().getOutputContextsList();
            int layoutCount = chatLayout.getChildCount();
            String intentName = queryResult.getIntent().getDisplayName();
            TextMessageTemplate tmt = new TextMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
            switch (intentName) {
                case "Getweather":
                    Log.d(TAG, "processResponse: Intent detected as Getweather");
                    chatLayout.addView(tmt.showMessage(response)); // move focus to text view to automatically make it scroll up if softfocus
                    chatLayout.addView(getAddActivityView());
                    queryEditText.requestFocus();
                    break;
                case "getCalories":
                    Log.d(TAG, "processResponse: Intent detected as getCalories");
                    chatLayout.addView(tmt.showMessage(response)); // move focus to text view to automatically make it scroll up if softfocus
                    queryEditText.requestFocus();
                    break;
                case "getSteps":
                    chatLayout.addView(tmt.showMessage(response)); // move focus to text view to automatically make it scroll up if softfocus
                    queryEditText.requestFocus();
                    break;
                case "ActivateMotivationIntent":
                    chatLayout.addView(tmt.showMessage(response)); // move focus to text view to automatically make it scroll up if softfocus
                    queryEditText.requestFocus();
                    break;
                case "myProgress":
                    //readData();
                    //System.out.println(stepCount);
                    Log.d(TAG, "Nazar: " + stepCount);
                    //setContentView(R.layout.activity_note_list);
                    Intent a = new Intent(ChatbotActivity.this, NotesListActivity.class);
                    startActivity(a);
                    chatLayout.addView(tmt.showMessage(response)); // move focus to text view to automatically make it scroll up if softfocus
                    //chatLayout.addView(getAddActivityView());
                    queryEditText.requestFocus();
                    break;

            }
//            if ("Getweather".equals(intentName)) {
//                Log.d(TAG, "processResponse: Intent detected as Getweather");
//                Intent a = new Intent(ChatbotActivity.this, CircularGaugeActivity.class);
//                startActivity(a);
//                //TextMessageTemplate tmt = new TextMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
//                chatLayout.addView(tmt.showMessage(response)); // move focus to text view to automatically make it scroll up if softfocus
//                //chatLayout.addView(getAddActivityView());
//                queryEditText.requestFocus();
//            }
        }
    }


//    private ConstraintLayout getCircularGauge() {
//        final LayoutInflater inflater = LayoutInflater.from(this);
//        ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.activity_chart_common, null, false);
//
////        AnyChartView anyChartView = layout.findViewById(R.id.any_chart_view);
////        anyChartView.setProgressBar(layout.findViewById(R.id.progress_bar));
////
////        CircularGauge circularGauge = AnyChart.circular();
////        circularGauge.data(new SingleValueDataSet(new String[] { "23", "34", "67", "93", "56", "100"}));
////        circularGauge.fill("#fff")
////                .stroke(null)
////                .padding(0d, 0d, 0d, 0d)
////                .margin(100d, 100d, 100d, 100d);
////        circularGauge.startAngle(0d);
////        circularGauge.sweepAngle(270d);
////
////        Circular xAxis = circularGauge.axis(0)
////                .radius(100d)
////                .width(1d)
////                .fill((Fill) null);
////        xAxis.scale()
////                .minimum(0d)
////                .maximum(100d);
////        xAxis.ticks("{ interval: 1 }")
////                .minorTicks("{ interval: 1 }");
////        xAxis.labels().enabled(false);
////        xAxis.ticks().enabled(false);
////        xAxis.minorTicks().enabled(false);
////
////        circularGauge.label(0d)
////                .text("Temazepam, <span style=\"\">32%</span>")
////                .useHtml(true)
////                .hAlign(HAlign.CENTER)
////                .vAlign(VAlign.MIDDLE);
////        circularGauge.label(0d)
////                .anchor(Anchor.RIGHT_CENTER)
////                .padding(0d, 10d, 0d, 0d)
////                .height(17d / 2d + "%")
////                .offsetY(100d + "%")
////                .offsetX(0d);
////        Bar bar0 = circularGauge.bar(0d);
////        bar0.dataIndex(0d);
////        bar0.radius(100d);
////        bar0.width(17d);
////        bar0.fill(new SolidFill("#64b5f6", 1d));
////        bar0.stroke(null);
////        bar0.zIndex(5d);
////        Bar bar100 = circularGauge.bar(100d);
////        bar100.dataIndex(5d);
////        bar100.radius(100d);
////        bar100.width(17d);
////        bar100.fill(new SolidFill("#F5F4F4", 1d));
////        bar100.stroke("1 #e5e4e4");
////        bar100.zIndex(4d);
////
////        circularGauge.label(1d)
////                .text("Guaifenesin, <span style=\"\">34%</span>")
////                .useHtml(true)
////                .hAlign(HAlign.CENTER)
////                .vAlign(VAlign.MIDDLE);
////        circularGauge.label(1d)
////                .anchor(Anchor.RIGHT_CENTER)
////                .padding(0d, 10d, 0d, 0d)
////                .height(17d / 2d + "%")
////                .offsetY(80d + "%")
////                .offsetX(0d);
////        Bar bar1 = circularGauge.bar(1d);
////        bar1.dataIndex(1d);
////        bar1.radius(80d);
////        bar1.width(17d);
////        bar1.fill(new SolidFill("#1976d2", 1d));
////        bar1.stroke(null);
////        bar1.zIndex(5d);
////        Bar bar101 = circularGauge.bar(101d);
////        bar101.dataIndex(5d);
////        bar101.radius(80d);
////        bar101.width(17d);
////        bar101.fill(new SolidFill("#F5F4F4", 1d));
////        bar101.stroke("1 #e5e4e4");
////        bar101.zIndex(4d);
////
////        circularGauge.label(2d)
////                .text("Salicylic Acid, <span style=\"\">67%</span>")
////                .useHtml(true)
////                .hAlign(HAlign.CENTER)
////                .vAlign(VAlign.MIDDLE);
////        circularGauge.label(2d)
////                .anchor(Anchor.RIGHT_CENTER)
////                .padding(0d, 10d, 0d, 0d)
////                .height(17d / 2d + "%")
////                .offsetY(60d + "%")
////                .offsetX(0d);
////        Bar bar2 = circularGauge.bar(2d);
////        bar2.dataIndex(2d);
////        bar2.radius(60d);
////        bar2.width(17d);
////        bar2.fill(new SolidFill("#ef6c00", 1d));
////        bar2.stroke(null);
////        bar2.zIndex(5d);
////        Bar bar102 = circularGauge.bar(102d);
////        bar102.dataIndex(5d);
////        bar102.radius(60d);
////        bar102.width(17d);
////        bar102.fill(new SolidFill("#F5F4F4", 1d));
////        bar102.stroke("1 #e5e4e4");
////        bar102.zIndex(4d);
////
////        circularGauge.label(3d)
////                .text("Fluoride, <span style=\"\">93%</span>")
////                .useHtml(true)
////                .hAlign(HAlign.CENTER)
////                .vAlign(VAlign.MIDDLE);
////        circularGauge.label(3d)
////                .anchor(Anchor.RIGHT_CENTER)
////                .padding(0d, 10d, 0d, 0d)
////                .height(17d / 2d + "%")
////                .offsetY(40d + "%")
////                .offsetX(0d);
////        Bar bar3 = circularGauge.bar(3d);
////        bar3.dataIndex(3d);
////        bar3.radius(40d);
////        bar3.width(17d);
////        bar3.fill(new SolidFill("#ffd54f", 1d));
////        bar3.stroke(null);
////        bar3.zIndex(5d);
////        Bar bar103 = circularGauge.bar(103d);
////        bar103.dataIndex(5d);
////        bar103.radius(40d);
////        bar103.width(17d);
////        bar103.fill(new SolidFill("#F5F4F4", 1d));
////        bar103.stroke("1 #e5e4e4");
////        bar103.zIndex(4d);
////
////        circularGauge.label(4d)
////                .text("Zinc Oxide, <span style=\"\">56%</span>")
////                .useHtml(true)
////                .hAlign(HAlign.CENTER)
////                .vAlign(VAlign.MIDDLE);
////        circularGauge.label(4d)
////                .anchor(Anchor.RIGHT_CENTER)
////                .padding(0d, 10d, 0d, 0d)
////                .height(17d / 2d + "%")
////                .offsetY(20d + "%")
////                .offsetX(0d);
////        Bar bar4 = circularGauge.bar(4d);
////        bar4.dataIndex(4d);
////        bar4.radius(20d);
////        bar4.width(17d);
////        bar4.fill(new SolidFill("#455a64", 1d));
////        bar4.stroke(null);
////        bar4.zIndex(5d);
////        Bar bar104 = circularGauge.bar(104d);
////        bar104.dataIndex(5d);
////        bar104.radius(20d);
////        bar104.width(17d);
////        bar104.fill(new SolidFill("#F5F4F4", 1d));
////        bar104.stroke("1 #e5e4e4");
////        bar104.zIndex(4d);
////
////        circularGauge.margin(50d, 50d, 50d, 50d);
////        circularGauge.title()
////                .text("Medicine manufacturing progress' +\n" +
////                        "    '<br/><span style=\"color:#929292; font-size: 12px;\">(ACME CORPORATION)</span>")
////                .useHtml(true);
////        circularGauge.title().enabled(true);
////        circularGauge.title().hAlign(HAlign.CENTER);
////        circularGauge.title()
////                .padding(0d, 0d, 0d, 0d)
////                .margin(0d, 0d, 20d, 0d);
////
////        anyChartView.setChart(circularGauge);
//
//        return layout;
//    }


    private ConstraintLayout getAddActivityView() {
        final LayoutInflater inflater = LayoutInflater.from(this);
        ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.add_activity, null, false);
        Date currentDate = new Date();
        final EditText title = layout.findViewById(R.id.title);
        final Button Date = layout.findViewById(R.id.date);
        SimpleDateFormat DateFor = new SimpleDateFormat("dd MMM");
        String stringCurrentDate = DateFor.format(currentDate);
        Date.setText("Date: " + stringCurrentDate);
        Button startTime = layout.findViewById(R.id.startTime);
        SimpleDateFormat timeFor = new SimpleDateFormat("HH:mm");
        String stringCurrentTime = timeFor.format(currentDate);
        startTime.setText("Start: " + stringCurrentTime);
        Button endTime = layout.findViewById(R.id.endTime);
        Date oneHourLater = new Date(System.currentTimeMillis() + 3600 * 1000);
        timeFor = new SimpleDateFormat("HH:mm");
        String stringOneHourLater = timeFor.format(oneHourLater);
        endTime.setText("End: " + stringOneHourLater);

        Date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker((Button) v);
            }
        });

        startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker((Button) v);
            }
        });

        endTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker((Button) v);
            }
        });

        layout.findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent a = new Intent(ChatbotActivity.this, NotesListActivity.class);
                //System.out.println("Ziya: " + title.getText());

                a.putExtra("Title", title.getText().toString());
                a.putExtra("Date", date);
                startActivity(a);

//                Intent a = new Intent(ChatbotActivity.this, CircularGaugeActivity.class);
//                startActivity(a);
            }
        });

//        final Button button = findViewById(R.id.fab);
//        button.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                System.out.println("Ziya: " + "fab button clicked");
//            }
//        });

        return layout;
    }

    private void showDatePicker(final Button button) {
        final Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        // date picker dialog
        DatePickerDialog picker = new DatePickerDialog(ChatbotActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        SimpleDateFormat DateFor = new SimpleDateFormat("dd/MM/yyyy");
                        try {
                            date = DateFor.parse(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                            System.out.println(date);
                            Log.d("Nazar", "date" + date);
                            DateFor = new SimpleDateFormat("dd MMMM");
                            stringDate = DateFor.format(date);
                            button.setText(stringDate);

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }, year, month, day);
        picker.show();

    }

    private void showTimePicker(final Button button) {
        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);
        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(ChatbotActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                button.setText(selectedHour + ":" + selectedMinute);
            }
        }, hour, minute, true);//Yes 24 hour time
        mTimePicker.show();

    }


//    private void processResponse(DetectIntentResponse response) {
//        QueryResult queryResult = response.getQueryResult();
//        // String botReply = response.getQueryResult().getFulfillmentText();
//        //String IntentName = queryResult.getIntent().getDisplayName();
//        //Log.d(TAG, botReply);
//        Log.d(TAG, "processResponse");
//        if (response != null) {
//
//            List<Context> contextList = response.getQueryResult().getOutputContextsList();
//            int layoutCount = chatLayout.getChildCount();
//            if (contextList.size() > 0) {
//                for (Context context : contextList) {
//                    if (context.getName().contains("param_context")) {
//                        Map paramMap = context.getParameters().getFieldsMap();
//                        if (paramMap.containsKey("template")) {
//                            String template = context.getParameters().getFieldsMap().get("template").getStringValue();
//                            switch (template) {
//                                case "text":
//                                    Log.d(TAG, "processResponse: Text Template");
//                                    TextMessageTemplate tmt = new TextMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
//                                    chatLayout.addView(tmt.showMessage(response)); // move focus to text view to automatically make it scroll up if softfocus
//                                    queryEditText.requestFocus();
//                                    break;
//                                case "button":
//                                    Log.d(TAG, "processResponse: Button Template");
//                                    ButtonMessageTemplate bmt = new ButtonMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
//                                    chatLayout.addView(bmt.showMessage(response)); // move focus to text view to automatically make it scroll up if softfocus
//                                    queryEditText.setEnabled(false);
//                                    break;
//                                case "hyperlink":
//                                    Log.d(TAG, "processResponse: Hyperlink Template");
//                                    HyperLinkTemplate blt = new HyperLinkTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
//                                    chatLayout.addView(blt.showMessage(response)); // move focus to text view to automatically make it scroll up if softfocus
//                                    queryEditText.setEnabled(false);
//                                    break;
//                                case "checkbox":
//                                    Log.d(TAG, "processResponse: CheckBox Template");
//                                    CheckBoxMessageTemplate cbmt = new CheckBoxMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
//                                    chatLayout.addView(cbmt.showMessage(response)); // move focus to text view to automatically make it scroll up if softfocus
//                                    queryEditText.setEnabled(false);
//                                    break;
//                                case "card":
//                                    Log.d(TAG, "processResponse: Card Template");
//                                    CardMessageTemplate cmt = new CardMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
//                                    chatLayout.addView(cmt.showMessage(response)); // move focus to text view to automatically make it scroll up if softfocus
//                                    queryEditText.setEnabled(false);
//                                    break;
//                                case "carousel":
//                                    Log.d(TAG, "processResponse: Carousel Template");
//                                    CarouselTemplate crt = new CarouselTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
//                                    chatLayout.addView(crt.showMessage(response)); // move focus to text view to automatically make it scroll up if softfocus
//                                    queryEditText.setEnabled(false);
//                                    break;
//                            }
//                        }
//                    } else {
//                        // when no param context if found... go to default
//                        TextMessageTemplate tmt = new TextMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
//                        chatLayout.addView(tmt.showMessage(response));
//                        queryEditText.requestFocus();
//                    }
//                    if (chatLayout.getChildCount() > layoutCount) {
//                        break; //this check is added as multiple layouts were getting added to chatLayout equal to number of loops
//                    }
//                }
//            } else {
//                // when no param context if found... go to default
//                TextMessageTemplate tmt = new TextMessageTemplate(ChatbotActivity.this, ChatbotActivity.this, Constants.BOT);
//                chatLayout.addView(tmt.showMessage(response));
//                queryEditText.requestFocus();
//            }
//        } else {
//            Log.e(TAG, "processResponse: Null Response");
//        }
//    }

    private void closeDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ChatbotActivity.this);

        alertDialogBuilder.setTitle("Exit Chat?");
        alertDialogBuilder.setMessage("Do you want to exit the chat? You will loose this chat session.");

        alertDialogBuilder
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "clicked: YES");
                                dialog.cancel();
//                                destroyRequestDialogflowTask();
                                ChatbotSettings.getInstance().setAppToolbar(null);
                                ChatbotSettings.getInstance().setAutoWelcome(true);
//                                super.onBackPressed();
                                ChatbotActivity.this.finish();
                            }
                        })
                .setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "clicked: NO");
                                dialog.cancel();
                            }
                        })
                .create()
                .show();

    }

    private void enableDissableChatUI(boolean bool) {
        chatMic.setEnabled(bool);
        chatMic.setClickable(bool);
        queryEditText.setEnabled(bool);
        queryEditText.setClickable(bool);
        sendBtn.setEnabled(bool);
        sendBtn.setClickable(bool);
    }

    private void promptSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Something !");
        try {
            startActivityForResult(intent, SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), "Sorry! Device does not support speech input", Toast.LENGTH_SHORT).show();
        }
    }
}
