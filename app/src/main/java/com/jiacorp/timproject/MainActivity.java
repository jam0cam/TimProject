package com.jiacorp.timproject;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener {

    private static final String TAG = MainActivity.class.getName();


    enum ReadMode {
        TITLE, SUMMARY, SUMMARY_PROMPT, NEXT_MESSAGE_STATEMENT, GOOD_BYE;
    }

    private static final int MY_DATA_CHECK_CODE = 1005;
    private static final int REQ_CODE_SPEECH_INPUT = 1004;

    private String cnnUrl = "http://img.brothersoft.com/icon/softimage/c/cnn-465437-1312366903.jpeg";
    private String nytUrl = "http://www.nytimes.com/apple-touch-icon.png";

    @InjectView(R.id.list_view)
    ListView mListView;

    private TextToSpeech mTts;
    List<Message> mItems;
    ListAdapter mAdapter;
    private boolean mStartSpeech;
    private int mCurrentIndex;
    private ReadMode mNextReadMode;
    private String mReadSummaryPrompt = "Hear more? Yes or No";
    private String mNextMessageStatement = "Next news.";
    private String mNoMoreNews = "There are no more news for now. Goodbye.";
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mItems = new ArrayList<>();
        mItems.add(new Message("'Affluenza' teen's mother charged, $1M bond set",
                "Tonya Couch, the mother of \"affluenza\" teen Ethan Couch is back in the United States and has been charged in Texas with hindering the apprehension of a felon.",
                cnnUrl));

        mItems.add(new Message(
                "China to build aircraft carrier",
                "China says it's building a new homegrown aircraft carrier as it seeks to expand its military clout.\n" +
                        "\n" +
                        "Beijing already has one aircraft carrier, the Liaoning, which it bought from Ukraine in 1998, spent years refurbishing and put into service in 2012.",
                cnnUrl
        ));

        mItems.add(new Message(
                "A Rockin’ New Year’s Eve for College Football",
                "With the College Football Playoff semifinal games on Thursday, ESPN is aiming to alter the way Americans celebrate the night.",
                nytUrl
        ));

        mHandler = new Handler(getMainLooper());

        mAdapter = new ListAdapter(this, R.layout.list_item, mItems);
        mListView.setAdapter(mAdapter);
        setTitle("News");
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mStartSpeech) {
            menu.findItem(R.id.action_start).setVisible(false);
        } else {
            menu.findItem(R.id.action_start).setVisible(true);
        }

        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_start) {
            startSpeech();
        }
        invalidateOptionsMenu();
        return true;
    }

    private void startSpeech() {
        mStartSpeech = true;

        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);

    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                mTts = new TextToSpeech(this, this);
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        } else if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String response = result.get(0);
                if ("yes".equalsIgnoreCase(response)) {
                    readCurrentSummary();
                } else {
                    readNextTitle();
                }
            }
        }
    }

    @Override
    public void onInit(int status) {
        Log.d(TAG, "onInit");
        mTts.setLanguage(Locale.US);
        if(status == TextToSpeech.SUCCESS) {
            mTts.setOnUtteranceCompletedListener(this);
            mTts.setSpeechRate((float) 1);
            readNextTitle();
        }

    }

    private void readNextTitle() {
        if (mCurrentIndex >= mItems.size()) {
            readNoMoreNews();
            return;
        }

        Message message = mItems.get(mCurrentIndex);
        mCurrentIndex++;
        mNextReadMode = ReadMode.SUMMARY_PROMPT;

        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(mCurrentIndex));
        mTts.speak(message.title, TextToSpeech.QUEUE_ADD, params);
    }


    private void readNoMoreNews() {
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(mCurrentIndex));
        mNextReadMode = ReadMode.GOOD_BYE;
        mTts.speak(mNoMoreNews, TextToSpeech.QUEUE_ADD, params);
    }

    private void readSummaryPrompt() {
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(mCurrentIndex));
        mNextReadMode = ReadMode.SUMMARY;
        mTts.speak(mReadSummaryPrompt, TextToSpeech.QUEUE_ADD, params);
    }

    private void readCurrentSummary() {

        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(mCurrentIndex));

        Message message = mItems.get(mCurrentIndex-1);
        mNextReadMode = ReadMode.NEXT_MESSAGE_STATEMENT;
        mTts.speak(message.message, TextToSpeech.QUEUE_ADD, params);

    }

    private void sayNextMessagePrompt() {
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(mCurrentIndex));

        mNextReadMode = ReadMode.TITLE;
        mTts.speak(mNextMessageStatement, TextToSpeech.QUEUE_ADD, params);
    }

    @Override
    public void onUtteranceCompleted(String utteranceId) {
        Log.d(TAG, "onUtteranceCompleted");
        //Reading of title is complete. Prompt for reading of Summary

        if (mNextReadMode == ReadMode.SUMMARY) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startSpeechInput();
                }
            }, 500);
        } else if (mNextReadMode == ReadMode.SUMMARY_PROMPT) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    readSummaryPrompt();
                }
            }, 500);

        } else if (mNextReadMode == ReadMode.NEXT_MESSAGE_STATEMENT) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sayNextMessagePrompt();
                }
            }, 500);
        } else if (mNextReadMode == ReadMode.TITLE) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    readNextTitle();
                }
            }, 500);
        } else if (mNextReadMode == ReadMode.GOOD_BYE) {
            finish();
        }
    }

    public void startSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    static class ListAdapter extends ArrayAdapter<Message> {

        private static final String TAG = ListAdapter.class.getName();
        private int mResource;

        static class ViewHolder {
            TextView mTxtTitle;
            TextView mTxtMessage;
            ImageView mAvatar;
        }


        public ListAdapter(Context context, int resource, List<Message> objects) {
            super(context, resource, objects);
            mResource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View vi = convertView;             //trying to reuse a recycled view
            ViewHolder holder = null;
            if (vi == null) {
                //The view is not a recycled one: we have to inflate
                LayoutInflater inflater = LayoutInflater.from(getContext());
                vi = inflater.inflate(mResource, parent, false);
                holder = new ViewHolder();
                holder.mTxtTitle = (TextView) vi.findViewById(R.id.txt_title);
                holder.mTxtMessage = (TextView) vi.findViewById(R.id.txt_message);
                holder.mAvatar = (ImageView) vi.findViewById(R.id.img_avatar);
                vi.setTag(holder);
            } else {
                // View recycled !
                // no need to inflate
                // no need to findViews by id
                holder = (ViewHolder) vi.getTag();
            }

            Message item = getItem(position);
            holder.mTxtTitle.setText(item.title);
            holder.mTxtMessage.setText(item.message);

            Glide.with(vi.getContext())
                    .load(item.avatarUrl)
                    .centerCrop()
                    .crossFade()
                    .bitmapTransform(new CircleTransform(vi.getContext()))
                    .placeholder(R.drawable.person)
                    .into(holder.mAvatar);
            return vi;
        }
    }
}
