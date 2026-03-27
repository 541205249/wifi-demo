package com.example.wifidemo.sample.network.data;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.example.wifidemo.sample.network.model.EchoEnvelope;
import com.example.wifidemo.sample.network.model.EchoJsonRequest;
import com.example.wifidemo.sample.network.model.NetworkDemoResult;
import com.wifi.lib.log.DLog;
import com.wifi.lib.network.ApiResult;
import com.wifi.lib.network.BaseNetworkRepository;
import com.wifi.lib.network.NetworkConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;

public class NetworkDemoRepository extends BaseNetworkRepository {
    private static final String TAG = "NetworkDemoRepo";
    private static final String BASE_URL = "https://httpbin.org/";
    private static final String DEMO_DEVICE_ID = "DEMO-HC25";
    private static volatile NetworkDemoRepository instance;

    private final Context appContext;
    private final EchoApiService apiService;
    private final EchoEnvelopeParser envelopeParser = new EchoEnvelopeParser();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public interface Callback {
        void onResult(@NonNull NetworkDemoResult result);
    }

    private NetworkDemoRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        NetworkConfig config = new NetworkConfig.Builder(BASE_URL)
                .addHeader("X-Demo-App", "wifi-demo")
                .addHeader("X-Demo-Architecture", "mvvm-repository")
                .dynamicHeadersProvider(() -> {
                    Map<String, String> headers = new LinkedHashMap<>();
                    headers.put("X-Trace-Time", String.valueOf(System.currentTimeMillis()));
                    return headers;
                })
                .build();
        this.apiService = createService(config, EchoApiService.class);
        DLog.i(TAG, "网络示例仓库初始化完成，baseUrl=" + BASE_URL);
    }

    @NonNull
    public static NetworkDemoRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (NetworkDemoRepository.class) {
                if (instance == null) {
                    instance = new NetworkDemoRepository(context);
                }
            }
        }
        return instance;
    }

    @NonNull
    public String getBaseUrl() {
        return BASE_URL;
    }

    public void requestGetExample(@NonNull Callback callback) {
        String requestAt = nowText();
        String requestPreview = "GET /get\n"
                + "module=optometry\n"
                + "deviceId=" + DEMO_DEVICE_ID + "\n"
                + "requestAt=" + requestAt;
        DLog.i(TAG, "发起 GET 示例请求");
        dispatchCall(
                "GET 查询示例",
                requestPreview,
                apiService.requestGetExample("optometry", DEMO_DEVICE_ID, requestAt),
                callback
        );
    }

    public void requestPostJsonExample(@NonNull Callback callback) {
        EchoJsonRequest request = new EchoJsonRequest();
        request.setModule("refraction");
        request.setDeviceId(DEMO_DEVICE_ID);
        request.setCommandCode("120301");
        request.setOperator("demo_doctor");
        request.setRequestAt(nowText());
        List<String> checkpoints = new ArrayList<>();
        checkpoints.add("prepare");
        checkpoints.add("auto_focus");
        checkpoints.add("result_confirm");
        request.setCheckpoints(checkpoints);

        String requestPreview = "POST /post\n"
                + "{\n"
                + "  \"module\": \"" + request.getModule() + "\",\n"
                + "  \"deviceId\": \"" + request.getDeviceId() + "\",\n"
                + "  \"commandCode\": \"" + request.getCommandCode() + "\",\n"
                + "  \"operator\": \"" + request.getOperator() + "\",\n"
                + "  \"requestAt\": \"" + request.getRequestAt() + "\",\n"
                + "  \"checkpoints\": " + request.getCheckpoints() + "\n"
                + "}";
        DLog.i(TAG, "发起 POST JSON 示例请求");
        dispatchCall(
                "POST JSON 示例",
                requestPreview,
                apiService.requestPostJsonExample(request),
                callback
        );
    }

    public void requestPostFormExample(@NonNull Callback callback) {
        String patientId = "P20260327001";
        String examMode = "vision_screening";
        String operator = "demo_nurse";
        String note = "表单方式提交验光前问诊信息";
        String requestPreview = "POST /post (application/x-www-form-urlencoded)\n"
                + "patientId=" + patientId + "\n"
                + "examMode=" + examMode + "\n"
                + "operator=" + operator + "\n"
                + "note=" + note;
        DLog.i(TAG, "发起表单提交示例请求");
        dispatchCall(
                "表单提交示例",
                requestPreview,
                apiService.requestPostFormExample(patientId, examMode, operator, note),
                callback
        );
    }

    public void requestUploadExample(@NonNull Callback callback) {
        String scenarioTitle = "文件上传示例";
        File uploadFile;
        try {
            uploadFile = prepareDemoFile();
        } catch (IOException exception) {
            DLog.e(TAG, "准备上传文件失败", exception);
            callback.onResult(new NetworkDemoResult(
                    scenarioTitle,
                    "文件上传示例失败：准备缓存文件异常",
                    "POST /post (multipart/form-data)\n准备缓存文件失败",
                    buildThrowablePreview(exception),
                    false
            ));
            return;
        }

        String requestPreview = "POST /post (multipart/form-data)\n"
                + "description=上传一份缓存中的演示报告\n"
                + "deviceId=" + DEMO_DEVICE_ID + "\n"
                + "fileName=" + uploadFile.getName() + "\n"
                + "fileSize=" + uploadFile.length() + " bytes";

        RequestBody descriptionBody = RequestBody.create(
                MediaType.parse("text/plain; charset=utf-8"),
                "上传一份缓存中的演示报告"
        );
        RequestBody deviceIdBody = RequestBody.create(
                MediaType.parse("text/plain; charset=utf-8"),
                DEMO_DEVICE_ID
        );
        RequestBody fileBody = RequestBody.create(
                MediaType.parse("text/plain; charset=utf-8"),
                uploadFile
        );
        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                "file",
                uploadFile.getName(),
                fileBody
        );
        DLog.i(TAG, "发起文件上传示例请求，file=" + uploadFile.getAbsolutePath());
        dispatchCall(
                scenarioTitle,
                requestPreview,
                apiService.requestUploadExample(descriptionBody, deviceIdBody, filePart),
                callback
        );
    }

    private void dispatchCall(
            @NonNull String scenarioTitle,
            @NonNull String requestPreview,
            @NonNull Call<ResponseBody> call,
            @NonNull Callback callback
    ) {
        enqueue(call, envelopeParser, result -> callback.onResult(buildResult(scenarioTitle, requestPreview, result)));
    }

    @NonNull
    private NetworkDemoResult buildResult(
            @NonNull String scenarioTitle,
            @NonNull String requestPreview,
            @NonNull ApiResult<EchoEnvelope> result
    ) {
        if (result.isSuccess()) {
            String statusText = scenarioTitle + " 成功，HTTP " + result.getHttpCode() + " / 业务码 " + result.getCode();
            return new NetworkDemoResult(
                    scenarioTitle,
                    statusText,
                    requestPreview,
                    buildResponsePreview(result.getData()),
                    true
            );
        }

        StringBuilder builder = new StringBuilder();
        builder.append("message: ").append(result.getMessage());
        if (!TextUtils.isEmpty(result.getErrorBody())) {
            builder.append("\n\nerrorBody:\n").append(result.getErrorBody());
        }
        if (result.getThrowable() != null) {
            builder.append("\n\nthrowable:\n").append(buildThrowablePreview(result.getThrowable()));
        }
        return new NetworkDemoResult(
                scenarioTitle,
                scenarioTitle + " 失败，HTTP " + result.getHttpCode() + " / 结果码 " + result.getCode() + "，" + result.getMessage(),
                requestPreview,
                builder.toString(),
                false
        );
    }

    @NonNull
    private String buildResponsePreview(EchoEnvelope envelope) {
        if (envelope == null) {
            return "响应体为空";
        }
        StringBuilder builder = new StringBuilder();
        appendTextLine(builder, "url", envelope.getUrl());
        appendTextLine(builder, "origin", envelope.getOrigin());
        appendTextLine(builder, "method", envelope.getMethod());
        appendTextLine(builder, "data", envelope.getData());
        appendStringMapSection(builder, "args", envelope.getArgs());
        appendStringMapSection(builder, "form", envelope.getForm());
        appendStringMapSection(builder, "files", envelope.getFiles());
        appendObjectMapSection(builder, "json", envelope.getJson());
        appendStringMapSection(builder, "headers", filterHeaders(envelope.getHeaders()));
        return builder.length() == 0 ? "响应体为空" : builder.toString().trim();
    }

    @NonNull
    private Map<String, String> filterHeaders(Map<String, String> headers) {
        Map<String, String> filtered = new LinkedHashMap<>();
        if (headers == null || headers.isEmpty()) {
            return filtered;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            if (TextUtils.isEmpty(key)) {
                continue;
            }
            String lowerKey = key.toLowerCase(Locale.US);
            if (lowerKey.startsWith("x-demo")
                    || lowerKey.startsWith("x-trace")
                    || lowerKey.equals("content-type")
                    || lowerKey.equals("user-agent")) {
                filtered.put(key, entry.getValue());
            }
        }
        return filtered;
    }

    private void appendTextLine(@NonNull StringBuilder builder, @NonNull String label, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n");
        }
        builder.append(label).append(": ").append(value);
    }

    private void appendStringMapSection(
            @NonNull StringBuilder builder,
            @NonNull String title,
            Map<String, String> map
    ) {
        if (map == null || map.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append(title).append(":");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            builder.append("\n  ")
                    .append(entry.getKey())
                    .append(" = ")
                    .append(entry.getValue());
        }
    }

    private void appendObjectMapSection(
            @NonNull StringBuilder builder,
            @NonNull String title,
            Map<String, Object> map
    ) {
        if (map == null || map.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append(title).append(":");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            builder.append("\n  ")
                    .append(entry.getKey())
                    .append(" = ")
                    .append(renderValue(entry.getValue(), 1));
        }
    }

    @NonNull
    private String renderValue(Object value, int depth) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) value;
            StringBuilder builder = new StringBuilder("{");
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                builder.append("\n")
                        .append(indent(depth + 1))
                        .append(entry.getKey())
                        .append(": ")
                        .append(renderValue(entry.getValue(), depth + 1));
            }
            if (!map.isEmpty()) {
                builder.append("\n").append(indent(depth));
            }
            builder.append("}");
            return builder.toString();
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            StringBuilder builder = new StringBuilder("[");
            for (Object item : list) {
                builder.append("\n")
                        .append(indent(depth + 1))
                        .append(renderValue(item, depth + 1));
            }
            if (!list.isEmpty()) {
                builder.append("\n").append(indent(depth));
            }
            builder.append("]");
            return builder.toString();
        }
        return String.valueOf(value);
    }

    @NonNull
    private String indent(int depth) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < depth; index++) {
            builder.append("  ");
        }
        return builder.toString();
    }

    @NonNull
    private File prepareDemoFile() throws IOException {
        File file = new File(appContext.getCacheDir(), "network-demo-report.txt");
        StringBuilder builder = new StringBuilder();
        builder.append("deviceId=").append(DEMO_DEVICE_ID).append('\n');
        builder.append("reportTime=").append(nowText()).append('\n');
        builder.append("examMode=refraction").append('\n');
        builder.append("note=This is a demo upload generated from cache.");
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file, false),
                StandardCharsets.UTF_8
        )) {
            writer.write(builder.toString());
            writer.flush();
        }
        return file;
    }

    @NonNull
    private String nowText() {
        return timeFormat.format(new Date());
    }

    @NonNull
    private String buildThrowablePreview(@NonNull Throwable throwable) {
        if (TextUtils.isEmpty(throwable.getMessage())) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
    }
}
