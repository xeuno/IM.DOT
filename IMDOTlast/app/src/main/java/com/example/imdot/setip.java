package com.example.imdot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class setip extends AppCompatActivity {

    private Button ipbtn;
    private EditText ipadd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setip);

        ipbtn=(Button)findViewById(R.id.ipbtn);
        ipadd=(EditText)findViewById(R.id.ipAddress);

        ipbtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent1 = new Intent(setip.this, MainActivity.class);
                intent1.putExtra("ip_address",ipadd.getText().toString());
                startActivity(intent1);
            }
        });
        //저장된 값을 불러오기 위해 같은 네임파일을 찾음.
        SharedPreferences sf = getSharedPreferences("sFile",MODE_PRIVATE);
        //text라는 key에 저장된 값이 있는지 확인. 아무값도 들어있지 않으면 ""를 반환
        String text = sf.getString("text","");
        if(text!="")
            ipadd.setHint(text);//현재 설정된 ip를 Hint로 표시함.
    }
    @Override
    protected void onStop() {
        super.onStop();

        // Activity가 종료되기 전에 저장
        //SharedPreferences를 sFile이름, 기본모드로 설정
        SharedPreferences sharedPreferences = getSharedPreferences("sFile",MODE_PRIVATE);

        //저장을 하기위해 editor를 이용하여 값을 저장
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String text = ipadd.getText().toString(); // 사용자가 입력한 저장할 데이터
        editor.putString("text",text); // key, value를 이용하여 저장하는 형태

        //최종 커밋
        editor.commit();
    }
}