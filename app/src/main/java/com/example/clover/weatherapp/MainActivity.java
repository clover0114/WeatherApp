package com.example.clover.weatherapp;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: Activityが停止された時、onSaveInstanceStateで値を保存
 * TODO: [need fix]Listが生成されない
 * TODO: Spinnerの表示を大きくする
 */
public class MainActivity extends AppCompatActivity {
    private final int REQUEST_CODE_ADDCITY = 0;
    private List<Map<String, String>> cityList = new ArrayList<>();

    private ArrayList<String> cityTitle = new ArrayList<>();
    private ArrayList<String> cityTag = new ArrayList<>();

    public List<Map<String, String>> getCityList() {
        return cityList;
    }

    public ArrayList<String> getCityTitle() {
        return cityTitle;
    }

    public ArrayList<String> getCityTag() {
        return cityTag;
    }

    public int getREQUEST_CODE_ADDCITY() {
        return REQUEST_CODE_ADDCITY;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toast.makeText(this, "onCreate()", Toast.LENGTH_SHORT).show();

        listGenerator();
    }

    //都市リストを生成
    /**
     * TODO: リストに値を追加する前にPreferenceファイルにデータを保存
     * TODO: cityTitle, cityTagをArrayListに変更
     * TODO: それぞれのgetter setterを設置 > arrayListはgetter.add / getter.getのみですんだ！
     */
    private void listGenerator(){
        Log.d("log","listgenerator()");
        if (1 <= getCityTitle().size()){
            ListView lv_preflist = findViewById(R.id.lv_preflist);
            Log.d("log","listgenerator() > if()");
            for (int i = 1; i <= getCityTitle().size(); i++) { //ここのint i;は、1でないといけない。なぜならgetCityTitle()はaddされたとき、indexの1から足されるから。
                Log.d("log","listgenerator() > if() > for()");
                Map<String, String> cityMap = new HashMap<>();
                cityMap.put("cityTitle", getCityTitle().get(i));
                cityMap.put("cityTag", getCityTag().get(i));
                getCityList().add(cityMap);
            }

            String[] from = {"cityTitle"};
            int[] to = {android.R.id.text1};
            SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, getCityList(), android.R.layout.simple_expandable_list_item_1, from, to);
            lv_preflist.setAdapter(adapter);
            lv_preflist.setOnItemClickListener(new onListItemSelectedListener());
        }
    }

    //addCityActivityからの戻りを受け取る
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == getREQUEST_CODE_ADDCITY() && resultCode == Activity.RESULT_OK){
            Bundle extras = data.getExtras();
            String cityTitle = extras.getString("cityTitle");
            String cityTag = extras.getString("cityTag");

            //値をフィールドにセット
            getCityTitle().add(cityTitle);
            getCityTag().add(cityTag);

            listGenerator();
        }
    }

    //メニューを生成
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_option_menu_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //メニューのアイテムを生成
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId){
            case R.id.add_city:
                Intent intent = new Intent(MainActivity.this, addCityActivity.class);
                startActivityForResult(intent, getREQUEST_CODE_ADDCITY());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //リストのリスナー
    private class onListItemSelectedListener implements android.widget.AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            HashMap<String, String> city = (HashMap)adapterView.getItemAtPosition(i);
//            String cityTitle = city.get("cityTitle");
            String cityTag = city.get("cityTag");
            WeatherReceiver receiver = new WeatherReceiver();
            receiver.execute(cityTag);
        }
    }


    private class WeatherReceiver extends AsyncTask<String, String, String>{
        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection con = null;
            InputStream is = null;
            String result = "";
            String cityTag = strings[0];
            String urlStr = "http://weather.livedoor.com/forecast/webservice/json/v1?city=" + cityTag;

            try {
                URL url = new URL(urlStr);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();

                is = con.getInputStream();
                result = is2String(is);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (con != null) {
                    con.disconnect();
                }
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            TextView tv_pref_title = findViewById(R.id.tv_pref_title);
            TextView tv_pref_weather = findViewById(R.id.tv_pref_weather);
            TextView tv_weather_desc = findViewById(R.id.tv_weather_desc);
            String cityTitle = "";
            String cityTelop = "";
            String cityDesc = "";

            try {
                JSONObject rootJSON = new JSONObject(result);

                cityTitle = rootJSON.getJSONObject("location").getString("city");
                cityTelop = rootJSON.getJSONArray("forecasts").getJSONObject(1).getString("telop");
                cityDesc = rootJSON.getJSONObject("description").getString("text");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            tv_pref_title.setText(cityTitle);
            tv_pref_weather.setText(cityTelop);
            tv_weather_desc.setText(cityDesc);
            super.onPostExecute(result);
        }

        private String is2String(InputStream is) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuffer sb = new StringBuffer();
            char[] b = new char[1024];
            int line;
            while (0 <= (line = (reader.read(b)))){
                sb.append(b, 0, line);
            }
            return sb.toString();
        }
    }

    //Activity保持
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //Activityが停止し始めたら、cityList可変長配列を保存する
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("cityTitleList", getCityTitle());
        outState.putStringArrayList("cityTagList", getCityTag());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //すでに何か保存されている場合は...
        if (savedInstanceState != null){
            //TODO: 保存しておいた配列から取り出した数値を一つずつフィールドの配列に入れ直す
            //保存しておいた配列
            List<String> savedTitlesArray = savedInstanceState.getStringArrayList("cityTitleList");
            List<String> savedTagsArray = savedInstanceState.getStringArrayList("cityTagList");
            for (int i = 1; i <= savedTitlesArray.size(); i++) {
                getCityTitle().add(savedTitlesArray.get(i));
                getCityTag().add(savedTagsArray.get(i));
            }
        }
    }
}
