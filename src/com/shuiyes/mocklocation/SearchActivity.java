package com.shuiyes.mocklocation;

import java.util.ArrayList;
import java.util.List;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Slide;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

@SuppressLint("NewApi")
public class SearchActivity extends Activity implements OnGetSuggestionResultListener, OnItemClickListener {

    private SuggestionSearch mSuggestionSearch = null;

    private AutoCompleteTextView mKeyword;
    private EditText mCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // 初始化建议搜索模块，注册建议搜索事件监听
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(this);

        mCity = (EditText) this.findViewById(R.id.et_city);
        mKeyword = (AutoCompleteTextView) this.findViewById(R.id.et_keyword);
        sugAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line);
        mKeyword.setAdapter(sugAdapter);
        mKeyword.setOnItemClickListener(this);
        mKeyword.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before,int count) {
                if (s.length() <= 0) {
                    return;
                }
                //使用建议搜索服务获取建议列表，结果在onSuggestionResult()中更新
                mSuggestionSearch.requestSuggestion((new SuggestionSearchOption()).keyword(s.toString()).city(mCity.getText().toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mKeyword.requestFocus();

        // 小米还是无效
        //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.showSoftInputFromInputMethod(mKeyword.getWindowToken(),0);
    }
    
    private List<String> suggest;
    private List<LatLng> latlngList;
    private ArrayAdapter<String> sugAdapter = null;

    @Override
    public void onGetSuggestionResult(SuggestionResult res) {
        final List<SuggestionResult.SuggestionInfo> infos = res.getAllSuggestions();
        if (res == null || infos == null) return;
        suggest = new ArrayList<String>();
        latlngList = new ArrayList<LatLng>();
        for (SuggestionResult.SuggestionInfo info : infos) {
            if (info.key != null && !suggest.contains(info.key)) {
                suggest.add(info.key);
                latlngList.add(info.pt);
            }
        }
        sugAdapter = new ArrayAdapter<String>(this, R.layout.my_list_item, suggest);
        mKeyword.setAdapter(sugAdapter);
        sugAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setResult(RESULT_OK,getIntent().putExtra("latlng", latlngList.get(position)));
        finish();
    }

    @Override
    public void onBackPressed(){
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
    }
}
