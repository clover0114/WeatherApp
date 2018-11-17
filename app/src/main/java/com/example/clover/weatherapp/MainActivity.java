package com.example.clover.weatherapp;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_CODE_ADDCITY = 0;
    ListView lv_preflist = findViewById(R.id.lv_preflist);
    List<Map<String, String>> cityList = new ArrayList<>();

    public int getREQUEST_CODE_ADDCITY() {
        return REQUEST_CODE_ADDCITY;
    }

    //空のリストを生成
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        String[] from = {"cityName"};
        int[] to = {android.R.id.text1};
        SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, cityList, android.R.layout.simple_expandable_list_item_1, from, to);
        lv_preflist.setAdapter(adapter);
        lv_preflist.setOnItemClickListener(new onListItemSelectedListener());
    }

    //addCityActivityからの戻りを受け取ったら、リストに都市を追加
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == getREQUEST_CODE_ADDCITY() && resultCode == Activity.RESULT_OK){
            Bundle extras = data.getExtras();

            Map<String, String> city = new HashMap<>();
            city.put("cityTitle", extras.getString("cityTitle"));
            city.put("cityTag", extras.getString("cityTag"));
            cityList.add(city);

            String[] from = {"cityName"};
            int[] to = {android.R.id.text1};
            SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, cityList, android.R.layout.simple_expandable_list_item_1, from, to);
            lv_preflist.setAdapter(adapter);
            lv_preflist.setOnItemClickListener(new onListItemSelectedListener());

            /**
             * test
             */
            Toast.makeText(this, "onActivityResult", Toast.LENGTH_SHORT).show();
            Toast.makeText(MainActivity.this, extras.getString("cityTitle")
                    + "と" + extras.getString("cityTag") + "が選択されました", Toast.LENGTH_SHORT).show(); //値がきてますテスト
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
            String urlStr = "http://weather.livedoor.com/forecast/webservice/json/v1?city=";

            try {
                URL url = new URL(urlStr + cityTag);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();

                is = con.getInputStream();
                result = is2String(is);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                con.disconnect();
                try {
                    is.close();
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
                cityTelop = rootJSON.getJSONArray("forecasts").getString(0);
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
}
