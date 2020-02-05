package com.example.administrator.networktest;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.administrator.networktest.bean.CheckinRecord;
import com.example.administrator.networktest.bean.GetAccessInfo;
import com.example.administrator.networktest.http.CharsetJsonRequest;
import com.example.administrator.networktest.util.ExcelUtils;
import com.example.administrator.networktest.util.Util;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {
    private com.example.administrator.networktest.http.VolleyResponseErrorListener VolleyResponseErrorListener;
    private Button sendRequest;
    private Button save2file;
    private TextView responseText;
    private EditText startTimeEditText;
    private EditText endTimeEditText;
    private RequestQueue mRequestQueue;
    private ListView listView;
    private CheckinAdapter checkinAdapter;
    private String accessToken;
    private String department_id="1";
    private String start_time;
    private String end_time;
    private int offset=0;
    private int size=20;
    private String status=null;
    private boolean hasMoreRecords;
    private File file;
    private String fileName;
    private ArrayList<ArrayList<String>> recordList;
    private static String[] title = { "姓名","签到时间","签到地点" };

    private List<CheckinRecord> checkinList=new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendRequest = (Button) findViewById(R.id.send_request);
        save2file=(Button) findViewById(R.id.save2file);
        startTimeEditText = (EditText) findViewById(R.id.startTime);
        endTimeEditText=(EditText) findViewById(R.id.endTime);
        responseText = (TextView) findViewById(R.id.response_text);
        listView = (ListView) findViewById(R.id.list_view);
        sendRequest.setOnClickListener(this);
        save2file.setOnClickListener(this);
        getRequestQueue().add(getAccessTokenRequest());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // 取得ViewHolder对象
                CheckinAdapter.ViewHolder viewHolder = (CheckinAdapter.ViewHolder) view.getTag();
// 改变CheckBox的状态
                viewHolder.useIt.toggle();
// 将CheckBox的选中状况记录下来
                checkinList.get(position).setUse_it(viewHolder.useIt.isChecked());
                responseText.setText(checkinList.get(position).toString());
                // 刷新
                checkinAdapter.notifyDataSetChanged();

            }

        });
        //初始化参数
        initParam();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_request:

                setParam();
                try {

                    //hasMoreRecords=true;

                        getRequestQueue().add(getCheckinRecorderRequest());



                } catch (ParseException e) {
                    e.printStackTrace();
                }


                break;
            case R.id.save2file:
                exportExcel();
                break;
            default:
                break;
        }
    }

    private void setParam() {
        try {
            offset=0;
            size=20;
            department_id="1";
            checkinList=new ArrayList<>();
            start_time=Util.dateToStamp(startTimeEditText.getText().toString());
            end_time=Util.dateToStamp(endTimeEditText.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    private void initParam() {

            startTimeEditText.setText(Util.getCurrentTime("YYYY-MM-dd")+" 00:00:00");
            endTimeEditText.setText(Util.getCurrentTime("YYYY-MM-dd HH:mm:ss"));


        offset=0;
        size=20;
        department_id="1";
        try {
            start_time=Util.dateToStamp(startTimeEditText.getText().toString());
            end_time=Util.dateToStamp(endTimeEditText.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }


    }

    private JsonObjectRequest getAccessTokenRequest() {
        String url="https://oapi.dingtalk.com/gettoken?appkey=dinga0qflpvyas9jsjpu&appsecret=4iQpRbpUok0lz49Jpgcfc6LdSqhwxKXrm4-4VJDwcPqJp9cypejIJU-gPD0AhvU5";
        CharsetJsonRequest request=new CharsetJsonRequest(url, null,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    status = response.getString("errcode");
                    Log.d("getAccessToke", status.toString());
                    if (!status.equals("0") )
                        Log.d("getAccessToke", response.getString("errmsg").toString());
                    else {
                        Log.e("success",response.toString());
                        Gson gson=new Gson();
                        GetAccessInfo info=gson.fromJson(response.toString(),GetAccessInfo.class);
                        accessToken = info.getAccess_token();
                        // 在这里进行UI操作，将结果显示到界面上
                        responseText.setText("初始化完成!");
                        Toast.makeText(MainActivity.this, "初始化完成!",
                                Toast.LENGTH_SHORT).show();
                    }
                    // Log.d("TAG", response.getString("status").toString());
                } catch (JSONException e) {
                    Log.e("TAG", e.getMessage(), e);
                    e.printStackTrace();
                }

            }
        }, VolleyResponseErrorListener);
        return request;
    }

    private CharsetJsonRequest getCheckinRecorderRequest() throws ParseException,ParseException {

        String url="https://oapi.dingtalk.com/checkin/record?access_token=" + accessToken + "&department_id=" + department_id + "&end_time=" + end_time + "&start_time=" + start_time + "&offset=" + offset + "&size=" + size;
        Log.d("getAcceurl", url);
        CharsetJsonRequest request=new CharsetJsonRequest(url, null,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    status = response.getString("errcode");
                    Log.d("getcheckin", status.toString());
                    if (!status.equals("0") )
                        Log.d("getCheckinerror", response.getString("errmsg").toString());
                    else {
                        Log.e("getCheckinsuccess",response.toString());
                        JSONArray jsonArray = response.getJSONArray("data");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                            Gson gson = new Gson();
                            CheckinRecord rd = gson.fromJson(jsonObject.toString(), CheckinRecord.class);
                            checkinList.add(rd);
                            Log.d("getCheckinrd", rd.toString());
                        }
                        if(jsonArray.length()<size){
                            Collections.sort(checkinList);
                            checkinAdapter = new CheckinAdapter(MainActivity.this,
                                    R.layout.checkin_record, checkinList);
                            listView.setAdapter(checkinAdapter);

                        }else{
                            offset+=size;
                            try {
                                getRequestQueue().add(getCheckinRecorderRequest());
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    // Log.d("TAG", response.getString("status").toString());
                } catch (JSONException e) {
                    Log.e("TAG", e.getMessage(), e);
                    e.printStackTrace();
                }
                 //Log.d("data", response.toString());
            }
        }, VolleyResponseErrorListener);
        return request;
    }
    /**
     * @return The Volley Request queue, the queue will be created if it is null
     */
    public RequestQueue getRequestQueue() {
        // lazy initialize the request queue, the queue instance will be
        // created when it is accessed for the first time
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }
    /**
     * 导出excel

     */
    public void exportExcel() {
        file = new File(getSDPath() + "/Record");
        makeDir(file);
        String timestamp=Util.getCurrentTime("YYYY-MM-dd");
        ExcelUtils.initExcel(file.toString() + "/"+timestamp+"大客户服务科.xls", title);
        fileName = getSDPath() + "/Record/"+timestamp+"大客户服务科.xls";
        ExcelUtils.writeObjListToExcel(getRecordData(), fileName, this);
        Toast.makeText(MainActivity.this, "文件保存成功！",
                Toast.LENGTH_SHORT).show();
        responseText.setText("文件成功保存在："+"Record/"+timestamp+"大客户服务科.xls");
    }

    /**
     * 将数据集合 转化成ArrayList<ArrayList<String>>
     * @return
     */
    private  ArrayList<ArrayList<String>> getRecordData() {
        recordList = new ArrayList<>();
        for (int i = 0; i <checkinList.size(); i++) {
            CheckinRecord checkinRecord = checkinList.get(i);
            ArrayList<String> beanList = new ArrayList<String>();
            if(checkinRecord.isUse_it()) {
                beanList.add(checkinRecord.getName());
                beanList.add(Util.stampToDate(checkinRecord.getTimestamp()));
                beanList.add(Util.getCity(checkinRecord.getDetailPlace()));
                recordList.add(beanList);
            }
        }
        return recordList;
    }

    private  String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
        }
        String dir = sdDir.toString();
        return dir;
    }

    public  void makeDir(File dir) {
        if (!dir.getParentFile().exists()) {
            makeDir(dir.getParentFile());
        }
        dir.mkdir();
    }

    /**
     * 创建文件夹
     */
    public void createFolder() {
        //获取SD卡的路径
        //String path = MyApplication.getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getPath();
        String Tag = "filetest";
        //getFilesDir()获取你app的内部存储空间
        File Folder = new File(Environment.
                getExternalStorageDirectory(), "Record");

        if (!Folder.exists())//判断文件夹是否存在，不存在则创建文件夹，已经存在则跳过
        {
            Folder.mkdir();//创建文件夹
            //两种方式判断文件夹是否创建成功
            //Folder.isDirectory()返回True表示文件路径是对的，即文件创建成功，false则相反
            boolean isFilemaked1 = Folder.isDirectory();
            //Folder.mkdirs()返回true即文件创建成功，false则相反
            boolean isFilemaked2 = Folder.mkdirs();

            if (isFilemaked1 || isFilemaked2) {
                Log.i(Tag, "创建文件夹成功"+Folder.getAbsolutePath());
            } else {
                Log.i(Tag, "创建文件夹失败");
            }

        } else {
            Log.i(Tag, "文件夹已存在");
        }

    }

    public void newFile(String _path, String _fileName) {
        File file = new File(_path + "/" + _fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
