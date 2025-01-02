package com.example.moblieapplication.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.moblieapplication.R;
import com.example.moblieapplication.chatbot.Gemini;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CallGeminiActivity extends AppCompatActivity {
    private TextView tv_output;
    private Button bt_apply_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_gemini);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Đặt tiêu đề cho ActionBar
            actionBar.setTitle("AI ChatBot");//Title
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        tv_output = findViewById(R.id.text);
        bt_apply_data = findViewById(R.id.apply_data);

        String userMessage = getIntent().getStringExtra("userMessage");
        bt_apply_data.setVisibility(View.GONE);
        Gemini gemini = new Gemini();
        callGeminiTextResult(Gemini.GEMINI_API_KEY, userMessage);

        bt_apply_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callGeminiJsonResult(Gemini.GEMINI_API_KEY, userMessage);
                Intent intent = new Intent(CallGeminiActivity.this, ControlDeviceActivity.class);
                startActivity(intent);
            }
        });
    }


    public void callGeminiTextResult(String apiKey, String userMessage) {
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 1f;
        configBuilder.topK = 40;
        configBuilder.topP = 0.95f;
        configBuilder.maxOutputTokens = 8192;
        configBuilder.responseMimeType = "text/plain";
        GenerationConfig generationConfig = configBuilder.build();
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", apiKey, generationConfig);

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String inputText = "Bạn một trợ lý ảo AI giúp người dùng trong việc trồng cây. Việc của bạn là cung cấp câu trả lời ngắn gọn đặc điểm sinh trưởng cùng với thông số nhiệt độ, độ ẩm, độ ẩm đất, thời gian chiếu sáng phù hợp để trồng cây, ngoài ra còn đưa them thông số đề xuất ( tính bằng số trung bình cộng của min và max khoảng giá trị mà bạn đưa ra) theo mẫu định dạng sau:\n" +
                "Tên loại cây:\n" +
                "Đặc điểm sinh trưởng:\n" +
                "Nhiệt độ\n" +
                "Độ ẩm\n" +
                "Độ ẩm đất\n" +
                "Thời gian chiếu sáng:\n" +
                "Tôi đề xuất cho bạn các thông số thích hợp\n" +
                "Nhiệt độ đề xuất:\n" +
                "Độ ẩm đề xuất\n" +
                "Độ ẩm đất đề xuất\n" +
                "Thời gian chiếu sáng đề xuất:\n" +
                "Khi người dùng cung cấp cho bạn loại cây mà họ muốn trồng, bạn hãy kiểm tra xem loại cây mà người dùng yêu cầu có tồn tại không, nếu không thì thông báo là cây đó không tồn tại, còn nếu có thì hãy đưa tra cho họ câu trả lời kèm theo trích nguồn thông tin mà bạn đưa ra\n" +
                "yêu cầu từ người dùng: " + userMessage + "hãy dẫn nguồn thông tin uy tín mà bạn đưa ra và có đường link tham khảo";
        Content content = new Content.Builder()
                .addText(inputText)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                tv_output.setText(resultText);
                bt_apply_data.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        }, this.getMainExecutor());
    }

    public void callGeminiJsonResult(String apiKey, String userMessage) {
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 1f;
        configBuilder.topK = 40;
        configBuilder.topP = 0.95f;
        configBuilder.maxOutputTokens = 8192;
        configBuilder.responseMimeType = "application/json";
        GenerationConfig generationConfig = configBuilder.build();
        GenerativeModel gm = new GenerativeModel(/* modelName */ "gemini-1.5-flash", apiKey, generationConfig);

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String inputText = "Bạn một trợ lý ảo AI giúp người dùng trong việc trồng cây. Tôi cần câu trả lời đều lưu dưới dạng JSON để lưu vào FireBase, file JSON có dạng dưới đây\n" +
                "{\n" +
                "   \"soilMoisture\":70,\n" +
                "   \"lightingDuration\":7\n" +
                "}\n" +
                "với soilMoisture là độ ẩm đất còn lightingDuration là thời gian chiếu sáng phù hợp mà bạn đề suất, bạn phải đưa ra con số cụ thể (tính bằng số trung bình cộng của min và max khoảng giá trị mà bạn đưa ra và phải là số tự nhiên, nếu là số thập phân thì làm tròn xuống)\n" +
                "yêu của cầu người dùng là:" + userMessage;
        Content content = new Content.Builder()
                .addText(inputText)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                tv_output.setText(resultText);
                try {
                    // Chuyển đổi kết quả JSON từ API thành JSONObject
                    JSONObject jsonObject = new JSONObject(resultText);

                    // Kết nối tới Firebase Database
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference myRef = database.getReference("json/dataFromAI");

                    // Tạo Map để lưu trữ dữ liệu gửi lên Firebase
                    Map<String, Object> map = new HashMap<>();

                    // Duyệt qua các keys trong JSONObject và chuyển giá trị thành Long
                    for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                        String key = it.next();
                        Object value = jsonObject.get(key);

                        // Chuyển giá trị thành Long (nếu giá trị là số)
                        if (value instanceof Number) {
                            map.put(key, ((Number) value).longValue()); // Lưu dưới dạng Long
                        } else {
                            // Nếu không phải kiểu số, thử chuyển thành Long từ String
                            try {
                                map.put(key, Long.parseLong(value.toString()));
                            } catch (NumberFormatException e) {
                                // Nếu không thể chuyển đổi thành Long, lưu dưới dạng String
                                map.put(key, value.toString());
                            }
                        }
                    }

                    // Gửi dữ liệu lên Firebase
                    myRef.setValue(map);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // Thông báo thành công
                Toast.makeText(CallGeminiActivity.this, "Apply Data from AI successfully !!!", Toast.LENGTH_SHORT).show();
            }


            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        }, this.getMainExecutor());
    }


}